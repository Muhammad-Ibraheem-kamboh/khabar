package com.khabar.app.ai

import com.khabar.app.data.local.dao.AiDefectLogDao
import com.khabar.app.data.local.entities.AiDefectLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AiDefectManager(
    private val defectDao: AiDefectLogDao
) {

    val allLogsFlow: Flow<List<AiDefectLogEntity>> = defectDao.getAllLogsFlow()
    val logCountFlow: Flow<Int> = defectDao.getLogCountFlow()

    companion object {
        // High-precision regex patterns to detect user critique in English and Roman Urdu
        private val CRITIQUE_PATTERNS = listOf(
            // English & Roman Urdu "wrong" / "ghalat"
            Regex("\\b(ye|yeh|yehi|ye\\s+to|it|that|this)\\s+(is\\s+)?(ghalat|galat|glt|wrong|incorrect|false|fake)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(ghalat|galat|wrong|incorrect)\\s+(he|hai|tha|answer|jawab|info|date|deadline)\\b", RegexOption.IGNORE_CASE),

            // "tm ne sahi nahi bataya" / "not what i asked"
            Regex("\\b(tm|tum|you)\\s+(ne\\s+)?(mere\\s+mutabiq|sahi|theek|properly)\\s+(n(a|h)i|nahi|nhi)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(sahi|theek|correct)\\s+(n(a|h)i|nahi|nhi)\\s+(he|hai|diya|bataya|kia|kiy)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(not\\s+what\\s+i\\s+asked|did\\s*n['o]?t\\s+ask|didn['’]?t\\s+answer|missed\\s+my\\s+question)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(ye\\s+(n(a|h)i|nahi|nhi)\\s+(p(o|u)cha|manga|bola|kaha))\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(mene|maine|men)\\s+ye\\s+(n(a|h)i|nahi|nhi)\\b", RegexOption.IGNORE_CASE),

            // Mistakes / Errors
            Regex("\\b(ghalti|galti|mistake|error|blunder|hallucination)\\s+(ki|he|hai|kar\\s+di|hogai)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(galat|ghalat|wrong)\\s+(bataya|bola|likha|guide\\s+kiya)\\b", RegexOption.IGNORE_CASE),

            // Directives to fix / update / correct
            Regex("\\b(update\\s+karo|correct\\s+this|fix\\s+this|improve\\s+this|theek\\s+karo|sahi\\s+karo)\\b", RegexOption.IGNORE_CASE),

            // Outdated / missed information
            Regex("\\b(puran[ai]|outdated|old)\\s+(mail|email|notice|circular|he|hai)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(miss\\s+kar\\s+diya|miss\\s+kiya|miss\\s+he|ye\\s+point\\s+n(a|h)i\\s+he)\\b", RegexOption.IGNORE_CASE)
        )
    }

    fun isUserCritique(text: String): Boolean {
        if (text.isBlank()) return false
        val clean = text.trim()
        return CRITIQUE_PATTERNS.any { it.containsMatchIn(clean) }
    }

    suspend fun logTechnicalBug(
        provider: String,
        model: String,
        userPrompt: String,
        errorMessage: String,
        suggestedFix: String = "Inspect endpoint parameters and verify active fallback candidate."
    ): Long = withContext(Dispatchers.IO) {
        val title = when {
            errorMessage.contains("503", ignoreCase = true) -> "$provider 503 Server Overload"
            errorMessage.contains("404", ignoreCase = true) -> "$provider 404 Model Deprecated ($model)"
            errorMessage.contains("429", ignoreCase = true) -> "$provider 429 Quota / Rate Limit"
            else -> "$provider Execution Failure"
        }

        val log = AiDefectLogEntity(
            type = "TECHNICAL_BUG",
            title = title,
            userPrompt = userPrompt,
            aiResponse = null,
            userCritique = null,
            rootCauseAnalysis = errorMessage,
            suggestedFix = suggestedFix,
            provider = provider,
            model = model
        )
        defectDao.insertLog(log)
    }

    suspend fun recordUserCorrection(
        userPrompt: String,
        aiPreviousResponse: String?,
        userCritique: String,
        provider: String? = null,
        model: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val rootCause = diagnoseCritiqueRootCause(userPrompt, aiPreviousResponse, userCritique)
        val suggestedFix = formulateCritiqueFix(userPrompt, userCritique, rootCause)

        val log = AiDefectLogEntity(
            type = "ALIGNMENT_CORRECTION",
            title = "User Correction: ${userCritique.take(45)}...",
            userPrompt = userPrompt,
            aiResponse = aiPreviousResponse,
            userCritique = userCritique,
            rootCauseAnalysis = rootCause,
            suggestedFix = suggestedFix,
            provider = provider,
            model = model
        )
        defectDao.insertLog(log)
    }

    private fun diagnoseCritiqueRootCause(
        prompt: String,
        response: String?,
        critique: String
    ): String {
        val cleanCritique = critique.trim()
        return when {
            cleanCritique.contains("date", ignoreCase = true) || cleanCritique.contains("deadline", ignoreCase = true) ->
                "Date or Deadline mismatch. AI likely cited an older campus email or hallucinated a deadline instead of prioritizing the latest circular."
            cleanCritique.contains("fee", ignoreCase = true) || cleanCritique.contains("challan", ignoreCase = true) || cleanCritique.contains("voucher", ignoreCase = true) ->
                "Fee or Voucher misunderstanding. AI failed to verify exact fee structure or extension notices from university accounts."
            cleanCritique.contains("puran", ignoreCase = true) || cleanCritique.contains("old", ignoreCase = true) || cleanCritique.contains("outdated", ignoreCase = true) ->
                "Outdated notice cited. Newer circular superseded the email referenced by AI."
            cleanCritique.contains("miss", ignoreCase = true) || cleanCritique.contains("sari", ignoreCase = true) || cleanCritique.contains("all", ignoreCase = true) ->
                "Incomplete email breakdown. Assistant summarized or missed key campus notices instead of listing them individually."
            cleanCritique.contains("mutabiq", ignoreCase = true) || cleanCritique.contains("asked", ignoreCase = true) || cleanCritique.contains("pucha", ignoreCase = true) ->
                "Intent misalignment. User's specific question constraint was overlooked in favor of generic summary text."
            else ->
                "Factual or detail discrepancy reported by student. Query was: '$prompt', Critique was: '$cleanCritique'."
        }
    }

    private fun formulateCritiqueFix(prompt: String, critique: String, rootCause: String): String {
        val cleanCritique = critique.trim()
        return when {
            cleanCritique.contains("date", ignoreCase = true) || cleanCritique.contains("deadline", ignoreCase = true) ->
                "ALWAYS cross-reference assignment/quiz deadlines against UPCOMING DEADLINES and the newest email circular. Never cite an older announcement date when an extension exists."
            cleanCritique.contains("fee", ignoreCase = true) || cleanCritique.contains("challan", ignoreCase = true) || cleanCritique.contains("voucher", ignoreCase = true) ->
                "Verify exact fee due dates and check for fee installment or extension circulars before quoting financial deadlines."
            cleanCritique.contains("puran", ignoreCase = true) || cleanCritique.contains("old", ignoreCase = true) || cleanCritique.contains("outdated", ignoreCase = true) ->
                "Prioritize today's announcements and recent notices over earlier circulars. If an email is older, clearly state its sent date."
            cleanCritique.contains("miss", ignoreCase = true) || cleanCritique.contains("sari", ignoreCase = true) || cleanCritique.contains("all", ignoreCase = true) ->
                "Break down every genuine campus announcement clearly without omitting important deadlines or academic notices (still skip routine login alerts and ads)."
            cleanCritique.contains("mutabiq", ignoreCase = true) || cleanCritique.contains("asked", ignoreCase = true) || cleanCritique.contains("pucha", ignoreCase = true) ->
                "Strictly isolate the student's exact query constraint instead of generating broad generic summaries."
            else ->
                "Silently respect this preference on similar queries: \"$cleanCritique\"."
        }
    }

    suspend fun getActivePromptRules(limit: Int = 8): List<String> = withContext(Dispatchers.IO) {
        defectDao.getRecentLogsByType("ALIGNMENT_CORRECTION", limit)
            .mapNotNull { it.suggestedFix.takeIf { fix -> fix.isNotBlank() } }
            .distinct()
    }

    suspend fun exportPromptPatch(): String = withContext(Dispatchers.IO) {
        val alignmentLogs = defectDao.getRecentLogsByType("ALIGNMENT_CORRECTION", 20)
        if (alignmentLogs.isEmpty()) {
            return@withContext "# No active alignment patches needed yet. Khabri is currently operating with clean alignment."
        }

        val builder = StringBuilder()
        builder.appendLine("### ⚡ READY-TO-PASTE KHABRI SYSTEM PROMPT PATCH")
        builder.appendLine("> Copy and paste these rules directly under CORE INSTRUCTIONS in Khabar's AI instructions:")
        builder.appendLine()
        builder.appendLine("```markdown")
        builder.appendLine("LEARNED CAMPUS ALIGNMENT RULES:")
        alignmentLogs.mapNotNull { it.suggestedFix.takeIf { f -> f.isNotBlank() } }.distinct().forEachIndexed { i, rule ->
            builder.appendLine("${i + 1}. $rule")
        }
        builder.appendLine("```")
        builder.toString()
    }

    suspend fun deleteLog(id: Long) = withContext(Dispatchers.IO) {
        defectDao.deleteLogById(id)
    }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        defectDao.clearAllLogs()
    }

    suspend fun exportMarkdownReport(): String = withContext(Dispatchers.IO) {
        val logs = defectDao.getAllLogs()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
        val nowStr = SimpleDateFormat("EEEE, d MMMM yyyy 'at' h:mm a", Locale.ENGLISH).format(Date())

        val builder = StringBuilder()
        builder.appendLine("# 🧠 KHABRI AI QUALITY & ALIGNMENT IMPROVEMENT LOG")
        builder.appendLine("> Generated from Khabar on: $nowStr")
        builder.appendLine("> Total Recorded Issues: ${logs.size}")
        builder.appendLine()

        // 1. Prompt Patch (Ready for 1-Tap Copy & Paste to Antigravity/Gemini)
        builder.appendLine(exportPromptPatch())
        builder.appendLine()

        val alignmentLogs = logs.filter { it.type == "ALIGNMENT_CORRECTION" }
        val technicalLogs = logs.filter { it.type == "TECHNICAL_BUG" }

        builder.appendLine("## 1. User Corrections & Alignment Deficiencies (${alignmentLogs.size})")
        if (alignmentLogs.isEmpty()) {
            builder.appendLine("*No alignment defects reported by the student yet. Khabri responses are on target!*")
            builder.appendLine()
        } else {
            for ((index, log) in alignmentLogs.withIndex()) {
                val dateStr = dateFormat.format(Date(log.timestamp))
                builder.appendLine("### [Correction #${index + 1}] ${log.title}")
                builder.appendLine("- **Timestamp**: $dateStr")
                if (!log.provider.isNullOrBlank()) {
                    builder.appendLine("- **Active AI Engine**: ${log.provider} (${log.model ?: "default"})")
                }
                builder.appendLine("- **Original Student Prompt**: \"${log.userPrompt}\"")
                log.aiResponse?.let {
                    builder.appendLine("- **What Khabri Answered**: \"$it\"")
                }
                builder.appendLine("- **Student's Correction**: \"${log.userCritique}\"")
                builder.appendLine("- **Autonomous Root-Cause Diagnosis**: ${log.rootCauseAnalysis}")
                builder.appendLine("- **Recommended System Prompt / Heuristic Fix**:")
                builder.appendLine("  ```text")
                builder.appendLine("  ${log.suggestedFix}")
                builder.appendLine("  ```")
                builder.appendLine()
            }
        }

        builder.appendLine("## 2. Autonomous Technical Bug Telemetry (${technicalLogs.size})")
        if (technicalLogs.isEmpty()) {
            builder.appendLine("*Zero technical runtime bugs logged. All API connections are operating cleanly.*")
            builder.appendLine()
        } else {
            for ((index, log) in technicalLogs.withIndex()) {
                val dateStr = dateFormat.format(Date(log.timestamp))
                builder.appendLine("### [Bug #${index + 1}] ${log.title}")
                builder.appendLine("- **Timestamp**: $dateStr")
                builder.appendLine("- **Provider / Model**: ${log.provider} / ${log.model}")
                builder.appendLine("- **Failing Query**: \"${log.userPrompt}\"")
                builder.appendLine("- **Error Diagnostic**: `${log.rootCauseAnalysis}`")
                builder.appendLine("- **Self-Healing Recommendation**: ${log.suggestedFix}")
                builder.appendLine()
            }
        }

        builder.appendLine("---")
        builder.appendLine("*Use this log to update Khabar's system prompt, adjust rules, or fine-tune models.*")
        builder.toString()
    }
}
