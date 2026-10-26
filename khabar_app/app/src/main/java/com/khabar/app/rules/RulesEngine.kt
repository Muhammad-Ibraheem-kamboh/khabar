package com.khabar.app.rules

import com.khabar.app.domain.model.ExtractedEntities
import com.khabar.app.domain.model.MailBucket
import com.khabar.app.domain.model.MailCategory
import com.khabar.app.domain.model.RawMail

data class ProcessedMailResult(
    val mail: RawMail,
    val category: MailCategory,
    val secondaryCategory: MailCategory?,
    val bucket: MailBucket,
    val reasons: List<String>,
    val entities: ExtractedEntities,
    val unknownCoursePrompt: String?
)

class RulesEngine(
    private val programAliases: Map<String, List<String>> = emptyMap()
) {

    fun process(
        mail: RawMail,
        profile: UserProfileRulesView,
        senderOverrideBucket: MailBucket? = null
    ): ProcessedMailResult {
        // Step A: Normalize
        val cleanBody = Normalizer.normalize(mail.bodyText)
        val normalizedMail = mail.copy(bodyText = cleanBody)

        // Step B: Category Detection
        val categoryResult = CategoryDetector.detect(normalizedMail.subject, cleanBody)

        // Step C: Entity Extraction
        val entities = EntityExtractor.extract(
            subject = normalizedMail.subject,
            body = cleanBody,
            receivedAtEpochMs = normalizedMail.receivedAt,
            programAliasesMap = programAliases
        )

        // Step D & E: Audience Matching and Bucketing
        val audienceDecision = AudienceMatcher.match(
            category = categoryResult.primary,
            entities = entities,
            sender = normalizedMail.sender,
            senderEmail = normalizedMail.senderEmail,
            subject = normalizedMail.subject,
            body = cleanBody,
            profile = profile,
            receivedAtEpochMs = normalizedMail.receivedAt,
            senderOverrideBucket = senderOverrideBucket
        )

        return ProcessedMailResult(
            mail = normalizedMail,
            category = categoryResult.primary,
            secondaryCategory = categoryResult.secondary,
            bucket = audienceDecision.bucket,
            reasons = audienceDecision.reasons,
            entities = entities,
            unknownCoursePrompt = audienceDecision.unknownCoursePrompt
        )
    }
}
