package com.khabar.app.data.repository

import com.khabar.app.ai.ChatMessage
import com.khabar.app.ai.KeystoreSecretManager
import com.khabar.app.ai.OpenAiCompatibleClient
import com.khabar.app.config.AppConfig
import com.khabar.app.config.KhabriPromptConfig
import com.khabar.app.data.local.dao.ChatDao
import com.khabar.app.data.local.dao.ChatSessionInfo
import com.khabar.app.data.local.entities.ChatMessageEntity
import com.khabar.app.data.local.entities.MailEntity
import com.khabar.app.data.local.entities.ReminderEntity
import com.khabar.app.domain.model.PortalCourseDetector
import com.khabar.app.util.SemesterCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ChatRepository(
    private val chatDao: ChatDao,
    private val mailRepository: MailRepository,
    private val profileRepository: ProfileRepository,
    private val reminderRepository: ReminderRepository,
    private val secretManager: KeystoreSecretManager,
    private val openAiClient: OpenAiCompatibleClient,
    private val aiDefectManager: com.khabar.app.ai.AiDefectManager? = null
) {
    val messagesFlow: Flow<List<ChatMessageEntity>> = chatDao.getAllMessagesFlow()
    val sessionsFlow: Flow<List<ChatSessionInfo>> = chatDao.getAllSessionsFlow()

    fun getMessagesForSessionFlow(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSessionFlow(sessionId)
    }

    suspend fun clearSession(sessionId: String) {
        chatDao.clearSession(sessionId)
    }

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }

    suspend fun renameSession(sessionId: String, newTitle: String) {
        chatDao.renameSession(sessionId, newTitle)
    }

    suspend fun regenerateLastResponse(
        sessionId: String,
        explicitLanguage: String? = null
    ): Result<String> {
        val allMsgs = chatDao.getMessagesForSessionFlow(sessionId).first()
        val lastUserMsg = allMsgs.lastOrNull { it.role == "user" }
            ?: return Result.failure(IllegalStateException("Nothing to regenerate yet."))

        // Delete any trailing assistant replies after the last user prompt
        allMsgs.filter { it.timestamp >= lastUserMsg.timestamp && it.id != lastUserMsg.id }
            .forEach { chatDao.deleteMessage(it.id) }

        return sendMessage(
            userQuery = lastUserMsg.content,
            sessionId = sessionId,
            imageUri = lastUserMsg.imageUri,
            explicitLanguage = explicitLanguage,
            skipUserInsert = true
        )
    }

    suspend fun sendMessage(
        userQuery: String,
        sessionId: String = "default",
        imageUri: String? = null,
        imageBase64: String? = null,
        ocrExtractedText: String? = null,
        explicitLanguage: String? = null,
        skipUserInsert: Boolean = false
    ): Result<String> {
        val trimmedQuery = userQuery.trim()
        if (trimmedQuery.isBlank() && ocrExtractedText.isNullOrBlank() && imageBase64.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be blank"))
        }

        val displayContent = if (trimmedQuery.isNotBlank()) trimmedQuery else "Analyzed document / screenshot"
        val existingTitle = chatDao.getSessionTitle(sessionId)
        val sessionTitle = existingTitle ?: (displayContent.take(36).trim() + (if (displayContent.length > 36) "..." else ""))

        // 1. Insert user message into Room (unless regenerating/retrying an already saved turn)
        if (!skipUserInsert) {
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    sessionTitle = sessionTitle,
                    role = "user",
                    content = displayContent,
                    imageUri = imageUri,
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // 1b. Check if this message is a critique/correction of Khabri's previous answer
        val isCritique = aiDefectManager?.isUserCritique(trimmedQuery) == true
        if (isCritique) {
            try {
                val sessionHistory = chatDao.getMessagesForSessionFlow(sessionId).first()
                val previousAssistantMsg = sessionHistory.filter { it.role == "assistant" }
                    .maxByOrNull { it.timestamp }
                val previousUserMsg = sessionHistory.filter { it.role == "user" && it.timestamp < (previousAssistantMsg?.timestamp ?: Long.MAX_VALUE) }
                    .maxByOrNull { it.timestamp }

                if (previousAssistantMsg != null) {
                    val activeProv = secretManager.getAiProvider()
                    val activeMod = secretManager.getModelForProvider(activeProv)
                    aiDefectManager.recordUserCorrection(
                        userPrompt = previousUserMsg?.content ?: "Previous query",
                        aiPreviousResponse = previousAssistantMsg.content,
                        userCritique = trimmedQuery,
                        provider = activeProv,
                        model = activeMod
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Check if student is syncing courses from portal/OCR attachment or explicit add command
        if (!ocrExtractedText.isNullOrBlank()) {
            detectAndUpdateStudentProfile(ocrExtractedText)
        } else if (trimmedQuery.startsWith("add course", ignoreCase = true) ||
            trimmedQuery.startsWith("register course", ignoreCase = true) ||
            trimmedQuery.startsWith("enrolled in", ignoreCase = true)
        ) {
            detectAndUpdateStudentProfile(trimmedQuery)
        }

        // 3. Gather real-time context
        val profile = profileRepository.profileFlow.first()
        val courses = profileRepository.coursesFlow.first()
        val interests = profileRepository.interestsFlow.first()

        val todayStartMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todayMails = mailRepository.getMailsSince(todayStartMs)
        val allRecentMails = mailRepository.getRecentMails(40)
        val upcomingReminders = reminderRepository.allRemindersFlow.first().filter { it.dueAt >= System.currentTimeMillis() }

        // Language resolution
        val lang = explicitLanguage ?: profile?.summaryLanguage ?: "en"

        // Dynamic active semester calculation
        val semCalc = if (profile != null) {
            SemesterCalculator.calculate(
                joinTerm = profile.joinTerm,
                joinYear = profile.joinYear,
                isGraduate = profile.level.contains("Grad", ignoreCase = true)
            )
        } else null

        val semIndex = if (profile?.semesterOverride == true) profile.semesterIndex else (semCalc?.semesterIndex ?: profile?.semesterIndex ?: 1)
        val standing = if (profile?.semesterOverride == true) profile.standing else (semCalc?.standing ?: profile?.standing ?: "Freshman")
        val currentTerm = semCalc?.currentTerm ?: "Spring"
        val currentYear = semCalc?.currentYear ?: 2026
        val activeSemesterStr = "Semester $semIndex, $standing, $currentTerm $currentYear"

        val formattedCourses = courses.map { course ->
            PortalCourseDetector.formatCoursePromptLine(course)
        }

        // 4. Gather active learned rules and build System Prompt
        val learnedRules = aiDefectManager?.getActivePromptRules(8) ?: emptyList()
        val systemPrompt = buildSystemPrompt(
            studentName = profile?.firstName?.ifBlank { "Student" } ?: "Student",
            program = profile?.programCode ?: "Undergraduate",
            campus = profile?.campus ?: "Main",
            courses = formattedCourses,
            interests = interests.map { it.name },
            todayMails = todayMails,
            allMails = allRecentMails,
            reminders = upcomingReminders,
            language = lang,
            learnedRules = learnedRules,
            activeSemester = activeSemesterStr
        )

        // 5. Build recent chat history for this session
        val recentHistory = chatDao.getRecentMessages(sessionId, 12).reversed()
        val hasRichAttachment = !imageBase64.isNullOrBlank() || !ocrExtractedText.isNullOrBlank()
        // If attaching an image/OCR payload for the current turn, avoid duplicating the just-saved user row
        val historyForApi = if (hasRichAttachment && recentHistory.lastOrNull()?.role == "user") {
            recentHistory.dropLast(1)
        } else {
            recentHistory
        }

        val apiMessages = mutableListOf<ChatMessage>()
        apiMessages.add(ChatMessage(role = "system", content = systemPrompt))

        for (msg in historyForApi) {
            val role = if (msg.role == "assistant") "assistant" else "user"
            apiMessages.add(ChatMessage(role = role, content = msg.content))
        }

        // If current query was an explicit critique, inject a high-priority turn steer
        if (isCritique) {
            apiMessages.add(
                ChatMessage(
                    role = "system",
                    content = """
                    CRITICAL TURN DIRECTIVE:
                    The student has explicitly corrected your previous response: "$trimmedQuery".
                    1. Acknowledge your mistake gracefully and politely with intellectual honesty (e.g. "My apologies! You are completely right.").
                    2. Immediately apply their correction, re-examine the email context, and provide the correct, accurate response.
                    3. Do NOT argue, defend the wrong answer, or repeat the incorrect fact.
                    """.trimIndent()
                )
            )
        }

        // If there is an image attachment or OCR text, inject it cleanly as the final user turn
        if (!imageBase64.isNullOrBlank()) {
            val visionPrompt = buildString {
                append(if (trimmedQuery.isNotBlank()) trimmedQuery else "Inspect this campus screenshot / fee voucher / timetable and extract exact due dates, amounts, room numbers, and actions required.")
                if (!ocrExtractedText.isNullOrBlank()) {
                    append("\n\n[Auxiliary OCR text extracted for verification]:\n$ocrExtractedText")
                }
            }
            apiMessages.add(
                ChatMessage(
                    role = "user",
                    content = visionPrompt,
                    imageBase64 = imageBase64
                )
            )
        } else if (!ocrExtractedText.isNullOrBlank()) {
            apiMessages.add(
                ChatMessage(
                    role = "user",
                    content = "[ATTACHED DOCUMENT TEXT]:\n$ocrExtractedText\n\nQuestion/Instruction:\n${trimmedQuery.ifBlank { "Summarize key dates, amounts, and actions required." }}"
                )
            )
        }

        // 6. Resolve Providers & Execute with Smart Auto-Routing / Fallback
        val activeProvider = secretManager.getAiProvider()
        val candidates = determineProviderOrder(activeProvider, hasImage = !imageBase64.isNullOrBlank())

        // Check if query can be answered locally from campus memory if offline or unconfigured
        val offlineAnswer = generateOfflineFallbackResponse(trimmedQuery)

        if (candidates.isEmpty()) {
            if (offlineAnswer != null) {
                val replyWithNote = "$offlineAnswer\n\n*(Answered from local campus memory • Add an API key in Settings for general reasoning)*"
                chatDao.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        sessionTitle = sessionTitle,
                        role = "assistant",
                        content = replyWithNote,
                        timestamp = System.currentTimeMillis()
                    )
                )
                return Result.success(replyWithNote)
            }
            val noKeyMsg = "No AI API key is configured. Please tap the Settings (Tune) icon at top right to enter a free Gemini or Groq key!"
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    sessionTitle = sessionTitle,
                    role = "assistant",
                    content = noKeyMsg,
                    timestamp = System.currentTimeMillis()
                )
            )
            return Result.success(noKeyMsg)
        }

        var lastError: Throwable? = null
        var successfulProvider: String? = null
        var replyText: String? = null

        for (provider in candidates) {
            val apiKey = secretManager.getApiKeyForProvider(provider)
            val baseUrl = secretManager.getBaseUrlForProvider(provider).ifBlank {
                KeystoreSecretManager.getDefaultBaseUrl(provider)
            }
            val model = if (!imageBase64.isNullOrBlank()) {
                KeystoreSecretManager.getVisionModel(provider)
            } else {
                secretManager.getModelForProvider(provider).ifBlank {
                    KeystoreSecretManager.getDefaultModel(provider)
                }
            }

            val isLocalOllama = provider.equals(KeystoreSecretManager.PROVIDER_OLLAMA, ignoreCase = true) ||
                    baseUrl.contains("localhost") || baseUrl.contains("10.0.2.2") || baseUrl.contains(":11434")

            if (apiKey.isBlank() && !isLocalOllama) {
                continue
            }

            var result = openAiClient.multiTurnChatCompletion(
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model,
                messages = apiMessages
            )

            // Silent Auto-Healing Model Fallback: If 404, 410, 503, transparently try alternative models & providers
            if (result.isFailure) {
                val err = result.exceptionOrNull()?.message ?: ""
                if (OpenAiCompatibleClient.isRecoverableModelError(err)) {
                    val altModels = (KeystoreSecretManager.getFallbackModels(provider) + openAiClient.getCandidateModelsForProvider(provider))
                        .distinct()
                        .filter { it != model }

                    for (alt in altModels) {
                        val retryRes = openAiClient.multiTurnChatCompletion(
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            model = alt,
                            messages = apiMessages
                        )
                        if (retryRes.isSuccess) {
                            result = retryRes
                            // Auto-heal: silently save the working model to keystore
                            secretManager.saveAiSettings(provider, baseUrl, alt)
                            break
                        }
                    }
                }
            }

            if (result.isSuccess) {
                replyText = result.getOrNull()
                successfulProvider = provider
                break
            } else {
                lastError = result.exceptionOrNull()
                // If it was overloaded (503) or rate-limited (429), try next candidate automatically!
            }
        }

        if (replyText != null && successfulProvider != null) {
            val finalReply = replyText

            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    sessionTitle = sessionTitle,
                    role = "assistant",
                    content = finalReply,
                    timestamp = System.currentTimeMillis()
                )
            )
            return Result.success(finalReply)
        } else {
            if (offlineAnswer != null) {
                val fallbackText = "$offlineAnswer\n\n*(Offline Mode: Retrieved from local campus memory)*"
                chatDao.insertMessage(
                    ChatMessageEntity(
                        sessionId = sessionId,
                        sessionTitle = sessionTitle,
                        role = "assistant",
                        content = fallbackText,
                        timestamp = System.currentTimeMillis()
                    )
                )
                return Result.success(fallbackText)
            }

            // Extract clean error message (not raw JSON)
            val rawError = lastError?.message ?: "Unknown error"
            val cleanError = when {
                rawError.contains("INVALID_ARGUMENT") || rawError.contains("Please pass a valid API key") ->
                    "Invalid API key. Please check your key in Settings → AI Settings."
                rawError.contains("401") || rawError.contains("Unauthorized") ->
                    "API key rejected (401). The key may be expired or invalid."
                rawError.contains("403") || rawError.contains("Forbidden") ->
                    "Access denied (403). Check API key permissions or billing."
                rawError.contains("429") || rawError.contains("rate") || rawError.contains("quota") ->
                    "Rate limit hit. Please wait a moment and try again."
                rawError.contains("503") || rawError.contains("overloaded") ->
                    "AI service is busy right now. Try again in a moment."
                rawError.contains("RESOURCE_EXHAUSTED") ->
                    "Free quota exhausted. Add a different AI provider key in Settings."
                rawError.contains("model") && (rawError.contains("not found") || rawError.contains("does not exist")) ->
                    "Model not found. The model name may be outdated — check Settings."
                rawError.contains("timeout") || rawError.contains("timed out") ->
                    "Connection timed out. Check your internet connection."
                else -> "Could not reach AI. ${rawError.take(120)}"
            }
            val errorMsg = "⚠️ $cleanError"
            try {
                val activeModel = secretManager.getModelForProvider(activeProvider)
                aiDefectManager?.logTechnicalBug(
                    provider = activeProvider,
                    model = activeModel,
                    userPrompt = displayContent,
                    errorMessage = rawError
                )
            } catch (_: Exception) {}

            return Result.failure(Exception(cleanError))
        }
    }

    private fun determineProviderOrder(preferredProvider: String, hasImage: Boolean = false): List<String> {
        val configured = secretManager.getConfiguredProviders().toMutableList()
        // Only add Ollama if user explicitly chose it or configured a custom Ollama endpoint URL
        if (preferredProvider.equals(KeystoreSecretManager.PROVIDER_OLLAMA, ignoreCase = true) ||
            secretManager.isOllamaExplicitlyConfigured()
        ) {
            if (!configured.contains(KeystoreSecretManager.PROVIDER_OLLAMA)) {
                configured.add(KeystoreSecretManager.PROVIDER_OLLAMA)
            }
        }

        if (configured.isEmpty()) {
            return if (hasImage) {
                listOf(KeystoreSecretManager.PROVIDER_GEMINI, KeystoreSecretManager.PROVIDER_GROQ)
            } else {
                listOf(KeystoreSecretManager.PROVIDER_GEMINI, KeystoreSecretManager.PROVIDER_GROQ)
            }
        }

        if (hasImage) {
            // For image/vision tasks, always prioritize natively multimodal models (Gemini -> OpenAI -> Claude -> Groq Vision -> OpenRouter)
            val visionPriority = listOf(
                KeystoreSecretManager.PROVIDER_GEMINI,
                KeystoreSecretManager.PROVIDER_OPENAI,
                KeystoreSecretManager.PROVIDER_CLAUDE,
                KeystoreSecretManager.PROVIDER_GROQ,
                KeystoreSecretManager.PROVIDER_OPENROUTER
            )
            val ordered = visionPriority.filter { configured.contains(it) }.toMutableList()
            if (preferredProvider != KeystoreSecretManager.PROVIDER_AUTO &&
                KeystoreSecretManager.isVisionCapableProvider(preferredProvider) &&
                ordered.contains(preferredProvider)
            ) {
                // Keep Gemini ahead only if preferred isn't natively multimodal; otherwise respect preferred vision provider first
                ordered.remove(preferredProvider)
                ordered.add(0, preferredProvider)
            }
            return ordered.ifEmpty { configured }
        }

        if (preferredProvider == KeystoreSecretManager.PROVIDER_AUTO) {
            // Auto prioritizes Groq (fastest) -> Gemini (largest context) -> OpenAI -> OpenRouter -> Claude
            val priority = listOf(
                KeystoreSecretManager.PROVIDER_GROQ,
                KeystoreSecretManager.PROVIDER_GEMINI,
                KeystoreSecretManager.PROVIDER_OPENAI,
                KeystoreSecretManager.PROVIDER_OPENROUTER,
                KeystoreSecretManager.PROVIDER_CLAUDE,
                KeystoreSecretManager.PROVIDER_OLLAMA
            )
            return priority.filter { configured.contains(it) }
        } else {
            // Put preferred provider first, followed by others as fallbacks
            val list = mutableListOf<String>()
            list.add(preferredProvider)
            configured.forEach { if (it != preferredProvider) list.add(it) }
            return list
        }
    }

    companion object {
        data class CategorizedMails(
            val campusAnnouncements: List<MailEntity>,
            val routineSecurityAlerts: List<Pair<MailEntity, Int>>,
            val externalPromotions: List<MailEntity>
        )

        fun categorizeAndDeduplicateMails(mails: List<MailEntity>): CategorizedMails {
            val campusList = mutableListOf<MailEntity>()
            val routineMap = mutableMapOf<String, Pair<MailEntity, Int>>()
            val externalList = mutableListOf<MailEntity>()

            val sorted = mails.sortedByDescending { it.receivedAt }

            for (mail in sorted) {
                val sub = mail.subject.trim()
                val sender = mail.sender.trim()
                val senderEmail = mail.senderEmail?.trim()?.lowercase() ?: ""
                val lowerSub = sub.lowercase()
                val lowerSender = sender.lowercase()
                val normalizedSubject = lowerSub
                    .replace(Regex("""^(re|fwd|fw):\s*"""), "")
                    .replace(Regex("""\s*\(duplicate\)$"""), "")
                    .trim()

                // 1. Routine Login / Security / OTP alerts
                val isRoutine = normalizedSubject.contains("login") ||
                        normalizedSubject.contains("logged in") ||
                        normalizedSubject.contains("sign-in code") ||
                        normalizedSubject.contains("sign in code") ||
                        normalizedSubject.contains("verification code") ||
                        normalizedSubject.contains("otp") ||
                        normalizedSubject.contains("two-factor") ||
                        normalizedSubject.contains("security code") ||
                        normalizedSubject.contains("password reset")

                if (isRoutine) {
                    val routineKey = "${lowerSender}:::${normalizedSubject.replace(Regex("""\d+"""), "#")}"
                    val current = routineMap[routineKey]
                    if (current == null) {
                        routineMap[routineKey] = Pair(mail, 1)
                    } else {
                        routineMap[routineKey] = Pair(current.first, current.second + 1)
                    }
                    continue
                }

                // 2. External third-party promotional newsletters / marketing
                val isExternal = lowerSender.contains("chatgpt") ||
                        lowerSender.contains("openai") ||
                        lowerSender.contains("uber") ||
                        lowerSender.contains("careem") ||
                        lowerSender.contains("daraz") ||
                        lowerSender.contains("linkedin") ||
                        lowerSender.contains("spotify") ||
                        mail.bucket.equals("PROMOTIONS", ignoreCase = true) ||
                        (senderEmail.isNotBlank() && !senderEmail.endsWith(".iba.edu.pk") && !senderEmail.contains("iba") && mail.bucket == "FYI")

                if (isExternal) {
                    val extKey = "${lowerSender}:::${normalizedSubject}"
                    if (externalList.none { "${it.sender.lowercase()}:::${it.subject.lowercase().replace(Regex("""^(re|fwd|fw):\s*"""), "").replace(Regex("""\s*\(duplicate\)$"""), "").trim()}" == extKey }) {
                        externalList.add(mail)
                    }
                    continue
                }

                // 3. Genuine campus announcements & academic notices
                val campusKey = "${lowerSender}:::$normalizedSubject"

                if (campusList.none {
                    val otherNorm = it.subject.lowercase()
                        .replace(Regex("""^(re|fwd|fw):\s*"""), "")
                        .replace(Regex("""\s*\(duplicate\)$"""), "")
                        .trim()
                    "${it.sender.lowercase()}:::$otherNorm" == campusKey
                }) {
                    campusList.add(mail)
                }
            }

            return CategorizedMails(
                campusAnnouncements = campusList,
                routineSecurityAlerts = routineMap.values.toList(),
                externalPromotions = externalList
            )
        }

        fun buildSystemPrompt(
            studentName: String,
            program: String,
            campus: String,
            courses: List<String>,
            interests: List<String>,
            todayMails: List<MailEntity>,
            allMails: List<MailEntity>,
            reminders: List<ReminderEntity>,
            language: String,
            learnedRules: List<String> = emptyList(),
            activeSemester: String? = null
        ): String {
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))

            val categorizedToday = categorizeAndDeduplicateMails(todayMails)
            val categorizedRecent = categorizeAndDeduplicateMails(allMails)

            val todayMailsFormatted = if (todayMails.isNotEmpty()) {
                buildString {
                    appendLine("TODAY'S EMAILS RECEIVED ON $todayStr:")
                    if (categorizedToday.campusAnnouncements.isNotEmpty()) {
                        appendLine("MAJOR CAMPUS NOTICES (${categorizedToday.campusAnnouncements.size} unique):")
                        categorizedToday.campusAnnouncements.forEach { mail ->
                            val bullets = com.khabar.app.data.local.entities.CachedJsonParser.parseStringList(mail.keyPointsJson).joinToString(" • ")
                            appendLine("- FROM: ${mail.sender} (${mail.senderEmail ?: "Campus"})\n  SUBJECT: ${mail.subject}\n  HEADLINE: ${mail.headline}\n  KEY POINTS: $bullets")
                        }
                    } else {
                        appendLine("- No major academic circulars or society notices today.")
                    }

                    if (categorizedToday.routineSecurityAlerts.isNotEmpty()) {
                        val routineSummary = categorizedToday.routineSecurityAlerts.joinToString(", ") { (mail, count) ->
                            if (count > 1) "${mail.subject} ($count times)" else mail.subject
                        }
                        appendLine("\nROUTINE SECURITY / LOGIN ALERTS (Background noise; do NOT list as individual updates): $routineSummary")
                    }

                    if (categorizedToday.externalPromotions.isNotEmpty()) {
                        val promoSummary = categorizedToday.externalPromotions.joinToString(", ") { "${it.sender}: ${it.subject}" }
                        appendLine("\nEXTERNAL / COMMERCIAL EMAILS (Non-campus; do NOT mix with campus news): $promoSummary")
                    }
                }
            } else {
                "No new emails received today ($todayStr)."
            }

            val recentMailsFormatted = if (allMails.isNotEmpty()) {
                buildString {
                    appendLine("RECENT CAMPUS EMAILS (${categorizedRecent.campusAnnouncements.size} unique notices in mailbox):")
                    categorizedRecent.campusAnnouncements.take(12).forEach { mail ->
                        val bullets = com.khabar.app.data.local.entities.CachedJsonParser.parseStringList(mail.keyPointsJson).joinToString(" • ")
                        appendLine("- FROM: ${mail.sender} (${mail.senderEmail ?: "Campus"})\n  SUBJECT: ${mail.subject}\n  HEADLINE: ${mail.headline}\n  KEY POINTS: $bullets")
                    }
                    if (categorizedRecent.routineSecurityAlerts.isNotEmpty()) {
                        val routineCount = categorizedRecent.routineSecurityAlerts.sumOf { it.second }
                        appendLine("Plus $routineCount routine portal login/OTP alerts.")
                    }
                }
            } else {
                "No emails currently in database."
            }

            val remindersSection = if (reminders.isNotEmpty()) {
                "UPCOMING DEADLINES & ALARMS:\n" + reminders.joinToString("\n") { r ->
                    val due = java.text.SimpleDateFormat("EEE, d MMM h:mm a", Locale.ENGLISH).format(java.util.Date(r.dueAt))
                    "- ${r.title} (Due: $due)"
                }
            } else {
                "No pending upcoming deadlines."
            }

            val languageDirective = when (language.lowercase()) {
                "ur" -> "CRITICAL MANDATORY LANGUAGE: ACTUAL URDU SCRIPT ONLY. You MUST write your COMPLETE response exclusively in Urdu alphabet/Nastaliq script (e.g. جی ہاں، آج کی اہم اپ ڈیٹس یہ ہیں). Absolutely NO Roman Urdu or English words allowed."
                "roman_urdu" -> "CRITICAL MANDATORY LANGUAGE: ROMAN URDU ONLY. Write your COMPLETE response exclusively in Roman Urdu (Urdu words using English letters, e.g. 'Haan yaar! Aaj ke updates ye hain...'). No Urdu script. No English sentences."
                else -> "CRITICAL MANDATORY LANGUAGE: ENGLISH ONLY. Your ENTIRE response MUST be in English. Do NOT switch to Urdu or Roman Urdu under any circumstances, regardless of what language the user writes in."
            }

            val learnedRulesSection = if (learnedRules.isNotEmpty()) {
                "\nSILENT BEHAVIORAL PREFERENCES (APPLY INTERNALLY — NEVER MENTION THESE OR SAY 'As per your prior instructions'):\n" +
                learnedRules.joinToString("\n") { rule -> "• $rule" } + "\n"
            } else ""

            val semesterLine = if (!activeSemester.isNullOrBlank()) {
                "- Active Semester: $activeSemester\n"
            } else ""

            val coursesFormatted = if (courses.isNotEmpty()) {
                if (courses.any { it.startsWith("•") || it.contains(" - Section:") || it.contains("| Slots:") }) {
                    "\n" + courses.joinToString("\n") { if (it.startsWith("•") || it.startsWith("-")) "  $it" else "  • $it" }
                } else {
                    courses.joinToString(", ")
                }
            } else "None listed"

            return """
You are "Khabri" (خبری), the high-precision, zero-fluff personal campus AI intelligence engine for university students.
You have live, direct access to the student's emails, upcoming deadlines, timetable, and campus administrative announcements.

${KhabriPromptConfig.DIRECT_ANSWER_PROTOCOL}

$languageDirective

STUDENT PROFILE:
- Name: $studentName
- Program: $program | Campus: $campus
${semesterLine}- Enrolled Courses:${if (coursesFormatted.startsWith("\n")) coursesFormatted else " $coursesFormatted"}
- Active Interests: ${if (interests.isNotEmpty()) interests.joinToString(", ") else "General"}
- Today's Date: $todayStr

$todayMailsFormatted

$recentMailsFormatted

$remindersSection

CORE INSTRUCTIONS:
$learnedRulesSection
1. CAMPUS SUMMARIES — TO THE POINT, HIGH SIGNAL (CHATGPT/CLAUDE PARITY):
   - When asked for email updates or summaries ("aaj ki mails batao", "what are today's email updates", "summarize today's mails", "whats updates"):
     a) If no major emails were received today, check recent notices and state:
        'No new emails received today ($todayStr). Latest campus notices:'
     b) FOCUS ONLY on what actually matters to the student: Deadlines, event dates, trips, registrations, assignments, fees.
     c) For every real campus notice you summarize, structure cleanly:
        **[Sender / Department]** — Subject
        1-2 crisp bullet points explaining the core announcement and action required.
     d) NEVER use rigid robotic templates like "• Core details: ... • Action: ... [📌 Source: ...]". Write naturally.
     e) NEVER output citation tags like "[📌 Source: ...]". The bold sender name is sufficient.
     f) NEVER explain what IBA, UMS, or LMS are. The student already studies at IBA and knows what the portals are.
     g) NEVER list routine security/login alerts (e.g. UMS logins, sign-in OTP codes) as separate news cards! If any exist, dismiss them in at most one single closing sentence (e.g. "Baqi routine portal login alerts aur sign-in code aye hain jinpe koi action needed nahi hai.") or omit them entirely.
     h) NEVER present external third-party commercial marketing (like ChatGPT product announcements, Uber, newsletters) as university news.
     i) SINGLE OUTPUT ONLY: Output your answer ONCE. NEVER repeat the list or generate duplicate summary sections.
2. DEADLINES & DATES:
   - When asked about assignments, quizzes, fees, or deadlines, verify against UPCOMING DEADLINES and mail key points.
3. PERSONALIZATION & STRUCTURE:
   - If the student provides documents, timetable, or courses, confirm and remember.
   - Use clean Markdown with bullet points. Never emit raw pipe tables (| col | col |); format schedules, comparisons, and tabular data as key-value cards:
     **[Course / Subject / Category Name]**
4. EVIDENCE & HONESTY:
   - If a student asks about a deadline or notice that does NOT exist in their emails or reminders, explicitly warn:
     "I could not find an official university email about this in your inbox. Please check with your department or instructor before relying on unverified dates."
   - NEVER fabricate or assume dates that are not in the context.

FINAL REMINDER — LANGUAGE: $languageDirective
""".trimIndent()
        }
    }

    private suspend fun detectAndUpdateStudentProfile(text: String) {
        val detected = PortalCourseDetector.detectCourses(text)
        if (detected.isNotEmpty()) {
            for (course in detected) {
                profileRepository.addCourse(
                    code = course.code,
                    section = course.section,
                    name = course.name
                )
            }
        } else if (text.contains("detect", ignoreCase = true) || text.contains("lms", ignoreCase = true) || text.contains("scan", ignoreCase = true)) {
            val recentMails = mailRepository.getRecentMails(30)
            for (mail in recentMails) {
                val mailDetected = PortalCourseDetector.detectCourses("${mail.subject} ${mail.headline} ${mail.bodyText}")
                for (course in mailDetected) {
                    profileRepository.addCourse(
                        code = course.code,
                        section = course.section,
                        name = course.name
                    )
                }
            }
        }
    }

    suspend fun generateOfflineFallbackResponse(query: String): String? {
        val lower = query.lowercase(Locale.ROOT).trim()

        val isScheduleQuery = lower.contains("class") || lower.contains("timetable") || 
            lower.contains("schedule") || lower.contains("room") || lower.contains("timing") ||
            lower.contains("kahan") || lower.contains("kab") || lower.contains("lecture")

        val isDeadlineQuery = lower.contains("deadline") || lower.contains("assignment") || 
            lower.contains("due") || lower.contains("quiz") || lower.contains("homework") ||
            lower.contains("submit") || lower.contains("submission") || lower.contains("remind")

        val isMailQuery = lower.contains("mail") || lower.contains("email") || 
            lower.contains("notice") || lower.contains("update") || lower.contains("aaj ki") ||
            lower.contains("summary") || lower.contains("khabar")

        val isAbsenceQuery = lower.contains("absence") || lower.contains("absent") || 
            lower.contains("chutti") || lower.contains("attendance")

        if (!isScheduleQuery && !isDeadlineQuery && !isMailQuery && !isAbsenceQuery) {
            return null
        }

        val courses = profileRepository.getAllCourses()
        val nowMs = System.currentTimeMillis()

        return buildString {
            if (isScheduleQuery) {
                val nextStatus = com.khabar.app.util.ClassScheduleHelper.findNextClassStatus(courses, nowMs)
                val todayClasses = com.khabar.app.util.ClassScheduleHelper.getTodayClasses(courses, nowMs)
                append("### 🗓️ Today's Class Schedule\n")
                if (todayClasses.isEmpty()) {
                    append("No classes scheduled for today.\n\n")
                } else {
                    if (nextStatus.isOngoing && nextStatus.activeClass != null) {
                        append("🔴 **Ongoing Now:** ${nextStatus.activeClass.courseCode} in ${nextStatus.activeClass.room} (${nextStatus.countdownText})\n\n")
                    } else if (nextStatus.activeClass != null) {
                        append("🟢 **Next Up:** ${nextStatus.activeClass.courseCode} in ${nextStatus.activeClass.room} (${nextStatus.countdownText})\n\n")
                    }
                    append("**Classes for Today:**\n")
                    for (c in todayClasses) {
                        append("• **${c.courseCode}** (${c.courseName}) — ${c.timeFormatted} | Room: ${c.room}\n")
                    }
                    append("\n")
                }
            }

            if (isDeadlineQuery) {
                val allReminders = reminderRepository.allRemindersFlow.first()
                    .filter { !it.done && it.dueAt >= nowMs }
                    .sortedBy { it.dueAt }
                append("### ⏰ Pending Deadlines & Reminders\n")
                if (allReminders.isEmpty()) {
                    append("No pending deadlines right now. You are all caught up!\n\n")
                } else {
                    for (rem in allReminders.take(5)) {
                        val rel = com.khabar.app.util.DateUtils.formatRelative(rem.dueAt)
                        val codePart = rem.courseCode?.let { "[$it] " } ?: ""
                        append("• $codePart**${rem.title}** — $rel\n")
                    }
                    append("\n")
                }
            }

            if (isAbsenceQuery) {
                append("### 📊 Course Absence Tracker\n")
                if (courses.isEmpty()) {
                    append("No enrolled courses registered yet. Sync from portal or add in Settings.\n\n")
                } else {
                    for (c in courses) {
                        val warning = if (c.absences >= 5) " 🚨 Limit Reached" else if (c.absences >= 4) " ⚠️ 1 Left" else ""
                        append("• **${c.code}** (${c.name}): ${c.absences} / ${c.maxAbsences} absences used$warning\n")
                    }
                    append("\n")
                }
            }

            if (isMailQuery) {
                val topMails = mailRepository.getAllMailsDirect().take(5)
                append("### 📬 Cached Campus Notices\n")
                if (topMails.isEmpty()) {
                    append("No local mails cached.\n\n")
                } else {
                    for (m in topMails) {
                        append("• **${m.sender}** — ${m.headline}\n")
                    }
                    append("\n")
                }
            }
        }.trim()
    }
}
