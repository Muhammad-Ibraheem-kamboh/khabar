package com.khabar.app.data.repository

import com.khabar.app.ai.ChatMessage
import com.khabar.app.ai.KeystoreSecretManager
import com.khabar.app.ai.OpenAiCompatibleClient
import com.khabar.app.config.AppConfig
import com.khabar.app.data.local.dao.ChatDao
import com.khabar.app.data.local.dao.ChatSessionInfo
import com.khabar.app.data.local.entities.ChatMessageEntity
import com.khabar.app.data.local.entities.MailEntity
import com.khabar.app.data.local.entities.ReminderEntity
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
    private val openAiClient: OpenAiCompatibleClient
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

    suspend fun sendMessage(
        userQuery: String,
        sessionId: String = "default",
        imageUri: String? = null,
        ocrExtractedText: String? = null,
        explicitLanguage: String? = null
    ): Result<String> {
        val trimmedQuery = userQuery.trim()
        if (trimmedQuery.isBlank() && ocrExtractedText.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Message cannot be blank"))
        }

        val displayContent = if (trimmedQuery.isNotBlank()) trimmedQuery else "Analyzed document / screenshot"
        val sessionTitle = displayContent.take(32).trim() + (if (displayContent.length > 32) "..." else "")

        // 1. Insert user message into Room
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

        // 2. Check if student is updating course info
        detectAndUpdateStudentProfile(trimmedQuery + " " + (ocrExtractedText ?: ""))

        // 3. Gather real-time context
        val profile = profileRepository.profileFlow.first()
        val courses = profileRepository.coursesFlow.first()
        val interests = profileRepository.interestsFlow.first()

        val todayStartMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todayMails = mailRepository.getMailsSince(todayStartMs)
        val allRecentMails = mailRepository.getRecentMails(20)
        val upcomingReminders = reminderRepository.allRemindersFlow.first().filter { it.dueAt >= System.currentTimeMillis() }

        // Language resolution
        val lang = explicitLanguage ?: profile?.summaryLanguage ?: "en"

        // 4. Build System Prompt with rich live database context
        val systemPrompt = buildSystemPrompt(
            studentName = profile?.firstName?.ifBlank { "Student" } ?: "Student",
            program = profile?.programCode ?: "Undergraduate",
            campus = profile?.campus ?: "Main",
            courses = courses.map { "${it.code} (${it.name})" },
            interests = interests.map { it.name },
            todayMails = todayMails,
            allMails = allRecentMails,
            reminders = upcomingReminders,
            language = lang
        )

        // 5. Build recent chat history for this session
        val recentHistory = chatDao.getRecentMessages(sessionId, 12).reversed()
        val apiMessages = mutableListOf<ChatMessage>()
        apiMessages.add(ChatMessage(role = "system", content = systemPrompt))

        for (msg in recentHistory) {
            val role = if (msg.role == "assistant") "assistant" else "user"
            apiMessages.add(ChatMessage(role = role, content = msg.content))
        }

        // If there is OCR text, inject it as context
        if (!ocrExtractedText.isNullOrBlank()) {
            apiMessages.add(
                ChatMessage(
                    role = "user",
                    content = "[ATTACHED IMAGE/DOCUMENT EXTRACTED TEXT]:\n$ocrExtractedText\n\nQuestion/Instruction:\n$trimmedQuery"
                )
            )
        }

        // 6. Resolve Providers & Execute with Smart Auto-Routing / Fallback
        val activeProvider = secretManager.getAiProvider()
        val candidates = determineProviderOrder(activeProvider)

        if (candidates.isEmpty()) {
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
            val model = secretManager.getModelForProvider(provider).ifBlank {
                KeystoreSecretManager.getDefaultModel(provider)
            }

            val isLocalOllama = provider.equals(KeystoreSecretManager.PROVIDER_OLLAMA, ignoreCase = true) ||
                    baseUrl.contains("localhost") || baseUrl.contains("10.0.2.2") || baseUrl.contains(":11434")

            if (apiKey.isBlank() && !isLocalOllama) {
                continue
            }

            val result = openAiClient.multiTurnChatCompletion(
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model,
                messages = apiMessages
            )

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
            val finalReply = if (activeProvider != KeystoreSecretManager.PROVIDER_AUTO && successfulProvider != activeProvider) {
                "*[⚡ Auto-switched to $successfulProvider: $activeProvider was overloaded or hit limit]*\n\n$replyText"
            } else {
                replyText
            }

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
            val errorMsg = "Sorry, could not connect to AI services: ${lastError?.message ?: "Unknown error"}. Check your API keys in Settings."
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    sessionTitle = sessionTitle,
                    role = "assistant",
                    content = errorMsg,
                    timestamp = System.currentTimeMillis()
                )
            )
            return Result.failure(lastError ?: Exception(errorMsg))
        }
    }

    private fun determineProviderOrder(preferredProvider: String): List<String> {
        val configured = secretManager.getConfiguredProviders().toMutableList()
        // If Ollama is configured or chosen, add it
        val ollamaUrl = secretManager.getBaseUrlForProvider(KeystoreSecretManager.PROVIDER_OLLAMA)
        if (ollamaUrl.isNotBlank() && !configured.contains(KeystoreSecretManager.PROVIDER_OLLAMA)) {
            configured.add(KeystoreSecretManager.PROVIDER_OLLAMA)
        }

        if (configured.isEmpty()) {
            return listOf(KeystoreSecretManager.PROVIDER_GEMINI, KeystoreSecretManager.PROVIDER_GROQ)
        }

        if (preferredProvider == KeystoreSecretManager.PROVIDER_AUTO) {
            // Auto prioritizes Groq (fastest) -> Gemini (largest context) -> OpenRouter -> Claude
            val priority = listOf(
                KeystoreSecretManager.PROVIDER_GROQ,
                KeystoreSecretManager.PROVIDER_GEMINI,
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

    private fun buildSystemPrompt(
        studentName: String,
        program: String,
        campus: String,
        courses: List<String>,
        interests: List<String>,
        todayMails: List<MailEntity>,
        allMails: List<MailEntity>,
        reminders: List<ReminderEntity>,
        language: String
    ): String {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))

        val todayMailsFormatted = if (todayMails.isNotEmpty()) {
            "TODAY'S EMAILS (${todayMails.size} received on $todayStr):\n" + todayMails.joinToString("\n\n") { mail ->
                val bullets = com.khabar.app.data.local.entities.CachedJsonParser.parseStringList(mail.keyPointsJson).joinToString(" • ")
                "- [${mail.bucket}] FROM: ${mail.sender} (${mail.senderEmail ?: "Campus"})\n  SUBJECT: ${mail.subject}\n  HEADLINE: ${mail.headline}\n  KEY POINTS: $bullets"
            }
        } else {
            "NO NEW EMAILS TODAY YET ($todayStr)."
        }

        val recentMailsFormatted = if (allMails.isNotEmpty()) {
            "RECENT CAMPUS EMAILS (${allMails.size} in mailbox):\n" + allMails.take(15).joinToString("\n\n") { mail ->
                val bullets = com.khabar.app.data.local.entities.CachedJsonParser.parseStringList(mail.keyPointsJson).joinToString(" • ")
                "- [${mail.bucket}] FROM: ${mail.sender} (${mail.senderEmail ?: "Campus"})\n  SUBJECT: ${mail.subject}\n  HEADLINE: ${mail.headline}\n  KEY POINTS: $bullets"
            }
        } else {
            "No emails currently in database."
        }

        val remindersSection = if (reminders.isNotEmpty()) {
            "UPCOMING DEADLINES & ALARMS:\n" + reminders.joinToString("\n") { r ->
                val due = java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(r.dueAt),
                    ZoneId.systemDefault()
                ).format(DateTimeFormatter.ofPattern("EEE, d MMM h:mm a", Locale.ENGLISH))
                "- ${r.title} (Due: $due)"
            }
        } else {
            "No pending upcoming deadlines."
        }

        val languageDirective = when (language.lowercase()) {
            "ur" -> "CRITICAL MANDATORY LANGUAGE: ACTUAL URDU SCRIPT (اردو رسم الخط). You MUST write your entire answer in actual Urdu alphabet (e.g. 'جی ہاں، آپ کے لئے آج کی اہم اپ ڈیٹس یہ ہیں'). NEVER reply in Roman Urdu or English."
            "roman_urdu" -> "CRITICAL MANDATORY LANGUAGE: ROMAN URDU ONLY. You MUST write your entire answer in conversational Roman Urdu (Urdu written with English alphabet, e.g. 'Haan! Aap ke liye aaj ye notices hain...')."
            else -> "CRITICAL MANDATORY LANGUAGE: CRISP MODERN ENGLISH ONLY. You MUST write your entire answer strictly in English. Even if the student asks in Roman Urdu or Urdu, respond strictly in English."
        }

        return """
You are "Khabri" (خبری), the brilliant, super-helpful personal campus AI assistant for university students.
You have live, direct access to the student's emails, upcoming deadlines, timetable, and campus administrative announcements.

$languageDirective

STUDENT PROFILE:
- Name: $studentName
- Program: $program | Campus: $campus
- Enrolled Courses: ${if (courses.isNotEmpty()) courses.joinToString(", ") else "None listed"}
- Active Interests: ${if (interests.isNotEmpty()) interests.joinToString(", ") else "General"}
- Today's Date: $todayStr

$todayMailsFormatted

$recentMailsFormatted

$remindersSection

CORE INSTRUCTIONS:
1. FULL & COMPLETE SUMMARIES:
   - When asked to summarize emails ("aaj ki mails batao", "what are today's email updates", "summarize today's mails", "whats updates", "sari mail ki summary karo ek ek kar ke"):
     a) If emails were received today, list and explain EVERY single today's email one by one!
     b) If NO emails were received today ($todayStr), clearly inform the student:
        (In selected language): 'You have no new emails received today ($todayStr), but here is the full breakdown of your latest campus notices:'
        AND THEN IMMEDIATELY list and thoroughly summarize their recent campus emails one by one! NEVER give a one-line empty response saying just 'no emails'. Always provide the full breakdown of their campus notices!
   - For every email you summarize, always include:
     • 📬 Sender & Department (e.g. Program Office, Fee Office, Examinations, Instructor name)
     • 📌 Subject & Core message (1-2 clear bullet points)
     • ⚡ Action required (submission link, fee due date, exam hall, or 'FYI only')
2. DEADLINES & DATES:
   - When asked about assignments, quizzes, fees, or deadlines, verify against UPCOMING DEADLINES and mail key points.
3. PERSONALIZATION:
   - If the student provides documents, timetable, or courses, confirm and remember.
   - Use clean Markdown with bullet points and friendly emoji headers.
""".trimIndent()
    }

    private suspend fun detectAndUpdateStudentProfile(text: String) {
        val courseRegex = Regex("""\b([A-Z]{2,4}\s*\d{3})\b""", RegexOption.IGNORE_CASE)
        val matches = courseRegex.findAll(text).map { it.value.uppercase().replace(" ", "") }.toList()

        if (matches.isNotEmpty()) {
            for (code in matches) {
                profileRepository.addCourse(
                    code = code,
                    section = "",
                    name = "Enrolled Course"
                )
            }
        } else if (text.contains("detect", ignoreCase = true) || text.contains("lms", ignoreCase = true) || text.contains("scan", ignoreCase = true)) {
            val recentMails = mailRepository.getRecentMails(30)
            for (mail in recentMails) {
                val mailMatches = courseRegex.findAll("${mail.subject} ${mail.headline}").map { it.value.uppercase().replace(" ", "") }.toList()
                for (code in mailMatches) {
                    profileRepository.addCourse(
                        code = code,
                        section = "",
                        name = "Auto-detected from Campus Emails"
                    )
                }
            }
        }
    }
}
