package com.khabar.app.config

import com.khabar.app.BuildConfig

/**
 * Global application configuration.
 * Contains the constant APP_NAME and provides access to build-time or overridden slots.
 */
object AppConfig {
    const val APP_NAME = "Khabar"
    const val APP_TAGLINE = "Only the mails that matter to you."

    // Slots from BuildConfig (defaults to empty or false when unset)
    val buildMsClientId: String = BuildConfig.MS_CLIENT_ID
    val buildMsSignatureHash: String = BuildConfig.MS_SIGNATURE_HASH
    val buildGmailEnabled: Boolean = BuildConfig.GMAIL_ENABLED
    val buildGmailClientId: String = BuildConfig.GMAIL_CLIENT_ID
    val buildAiBaseUrl: String = BuildConfig.AI_BASE_URL
    val buildAiModel: String = BuildConfig.AI_MODEL
    val buildAiApiKey: String = BuildConfig.AI_API_KEY

    val MS_CLIENT_ID: String get() = buildMsClientId
    val MS_SIGNATURE_HASH: String get() = buildMsSignatureHash
    val GMAIL_ENABLED: Boolean get() = buildGmailEnabled
    val GMAIL_CLIENT_ID: String get() = buildGmailClientId
    val AI_BASE_URL: String get() = buildAiBaseUrl
    val AI_MODEL: String get() = buildAiModel
    val AI_API_KEY: String get() = buildAiApiKey

    const val PORTAL_URL = "https://ums.iba.edu.pk/"
    const val PREF_KEY_AI_KEY = "ai_api_key"

    const val DEFAULT_DIGEST_TIME = "07:30"
    const val SYNC_INTERVAL_MINUTES = 15L
    const val FAST_MODE_INTERVAL_SECONDS = 90L

    val DEFAULT_INTERESTS = listOf(
        "INTERNSHIPS" to "Internships and jobs",
        "EXCHANGE" to "Exchange and study abroad",
        "SCHOLARSHIPS" to "Scholarships and fee help",
        "HOSTEL" to "Hostel",
        "TRANSPORT" to "Transport",
        "SOCIETIES" to "Societies and events",
        "ALUMNI" to "Alumni",
        "SPORTS" to "Sports"
    )

    val DIRECT_ANSWER_PROTOCOL: String get() = KhabriPromptConfig.DIRECT_ANSWER_PROTOCOL
}

object KhabriPromptConfig {
    const val DIRECT_ANSWER_PROTOCOL = """CRITICAL DIRECT ANSWER & ANTI-FLUFF PROTOCOL (STRICT ENFORCEMENT):
1. ZERO PREAMBLES & NO PLEASANTRIES:
   - Begin your response IMMEDIATELY with the direct answer, factual summary, or first bullet point.
   - NEVER start with greetings, salutations, or student name (e.g. DO NOT SAY "Hello Muhammad Ibrahim!", "Hi Ibrahim,", "Sure!", "As requested,", "Here is what you asked for:", "Certainly!").
   - NEVER end with conversational pleasantries, fillers, or sign-offs (e.g. DO NOT SAY "I hope this helps!", "Let me know if you need anything else!", "Have a great day!").

2. STRICTLY ANSWER WHAT WAS ASKED:
   - Provide ONLY the specific information requested.
   - Do NOT provide unsolicited generic study advice, motivational speeches, or patronizing reminders.
   - If and only if there is a critical deadline or penalty occurring within 24 to 48 hours, you may include at most ONE single-line note at the very end: "💡 Urgent: [Short 1-line note]". Otherwise, end immediately after answering.

3. MOBILE TYPOGRAPHY & ZERO BROKEN TABLES:
   - ABSOLUTELY NEVER output raw markdown pipe tables (| col | col |). Mobile chat screens are narrow portrait displays; pipe tables break layout and leak raw syntax.
   - When presenting schedules, fee breakdowns, course comparisons, or tabular information, format exclusively as clean, structured key-value bullet cards:
     **[Course / Subject / Category Name]**
     • Detail: Value
     • When / Due: Date and time
     • Venue / Room: Location
     • Action: Required step
   - Use clean, standard bullets (- or •) with a single space. Never output triple asterisks (***) or nested (* **) formatting.
   - Use Markdown headers (### or ##) cleanly with an empty line before and after. Never use #### headers.
   - Keep emoji usage strictly functional (maximum 1 emoji per section/highlight). No emoji spam.

4. ZERO ROBOTIC TEMPLATES, NO CITATION SPAM, NO NOISE:
   - DO NOT repeat rigid mechanical templates like "• Core details: ... • Action: ... [📌 Source: ...]" on every item. Write natural, intelligent summaries.
   - DO NOT attach citation tags like "[📌 Source: ...]". The bold sender/department is already sufficient.
   - DO NOT explain well-known university portals (IBA, UMS, LMS). The user is already an IBA student.
   - DO NOT treat routine login alerts or sign-in OTP codes as university news.
   - DO NOT treat external third-party newsletters (like ChatGPT product announcements, commercial marketing) as university announcements.
   - SINGLE PASS ONLY: Provide your complete answer once. NEVER output duplicate lists or repeat the summary twice in one message."""
}
