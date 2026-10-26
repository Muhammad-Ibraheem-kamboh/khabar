package com.khabar.app.data.repository

import com.khabar.app.ai.OfflineSummarizer
import com.khabar.app.data.local.dao.FeedbackDao
import com.khabar.app.data.local.dao.MailDao
import com.khabar.app.data.local.entities.MailEntity
import com.khabar.app.data.local.entities.SenderOverrideEntity
import com.khabar.app.data.local.entities.UnknownCoursePromptEntity
import com.khabar.app.domain.model.MailBucket
import com.khabar.app.domain.model.RawMail
import com.khabar.app.domain.model.SummaryLanguage
import com.khabar.app.notifications.KhabarNotificationManager
import com.khabar.app.rules.RulesEngine
import com.khabar.app.sources.MailSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId

class MailRepository(
    private val mailDao: MailDao,
    private val feedbackDao: FeedbackDao,
    private val profileRepository: ProfileRepository,
    private val rulesEngine: RulesEngine,
    private val offlineSummarizer: OfflineSummarizer,
    private val notificationManager: KhabarNotificationManager,
    private val mailSources: List<MailSource>
) {
    private val json = Json { ignoreUnknownKeys = true }

    val allMailsFlow: Flow<List<MailEntity>> = mailDao.getAllMailsFlow()
    val topImportantUnreadFlow: Flow<List<MailEntity>> = mailDao.getTopImportantUnreadFlow()
    val unreadImportantCountFlow: Flow<Int> = mailDao.getUnreadImportantCountFlow()
    val unreadCountFlow: Flow<Int> get() = unreadImportantCountFlow
    val pendingCoursePromptFlow: Flow<UnknownCoursePromptEntity?> = feedbackDao.getPendingUnknownCoursePromptFlow()

    fun getMailsByBucket(bucket: MailBucket): Flow<List<MailEntity>> {
        return mailDao.getMailsByBucketFlow(bucket.name)
    }

    fun getMailByIdFlow(id: String): Flow<MailEntity?> {
        return mailDao.getMailByIdFlow(id)
    }

    suspend fun getMailById(id: String): MailEntity? {
        return mailDao.getMailById(id)
    }

    fun getTodayStatsFlow(): Triple<Flow<Int>, Flow<Int>, Flow<Int>> {
        val todayStartMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return Triple(
            mailDao.getTodayReceivedCountFlow(todayStartMs),
            mailDao.getTodayForYouCountFlow(todayStartMs),
            mailDao.getTodayHiddenCountFlow(todayStartMs)
        )
    }

    suspend fun markAsRead(mailId: String) {
        mailDao.markAsRead(mailId)
    }

    suspend fun syncAll(): Int {
        val allNewMails = mutableListOf<RawMail>()
        for (source in mailSources) {
            if (source.isConfigured && source.isConnected) {
                try {
                    val mails = source.fetchNew()
                    allNewMails.addAll(mails)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        var processedCount = 0
        for (mail in allNewMails) {
            val existing = mailDao.getMailById(mail.id)
            if (existing == null) {
                processAndSaveIncoming(mail)
                processedCount++
            }
        }
        return processedCount
    }

    suspend fun processAndSaveIncoming(rawMail: RawMail): MailEntity {
        val profileRulesView = profileRepository.getUserProfileRulesView()
        val profile = profileRepository.getProfile()
        val language = SummaryLanguage.fromCode(profile?.summaryLanguage ?: "en")

        // 1. Check feedback learning override
        val override = feedbackDao.getOverride(rawMail.sender, "")
        val overrideBucket = override?.let { MailBucket.fromString(it.bucket) }

        // 2. Rules engine process
        val result = rulesEngine.process(
            mail = rawMail,
            profile = profileRulesView,
            senderOverrideBucket = overrideBucket
        )

        // 3. Offline summary
        val summary = offlineSummarizer.summarize(
            category = result.category,
            subject = rawMail.subject,
            bodyText = rawMail.bodyText,
            entities = result.entities,
            language = language
        )

        // 4. Save unknown course prompt if detected
        if (result.unknownCoursePrompt != null) {
            val existingPrompt = feedbackDao.getPromptForCourse(result.unknownCoursePrompt)
            if (existingPrompt == null) {
                feedbackDao.insertPrompt(
                    UnknownCoursePromptEntity(
                        code = result.unknownCoursePrompt,
                        mailId = rawMail.id
                    )
                )
            }
        }

        val entity = MailEntity(
            id = rawMail.id,
            sourceId = rawMail.sourceId,
            sender = rawMail.sender,
            senderEmail = rawMail.senderEmail,
            subject = rawMail.subject,
            bodyText = rawMail.bodyText,
            receivedAt = rawMail.receivedAt,
            isPreviewOnly = rawMail.isPreviewOnly,
            hasAttachments = rawMail.hasAttachments,
            attachmentNames = json.encodeToString(rawMail.attachmentNames),
            link = result.entities.link,
            category = result.category.idString,
            secondaryCategory = result.secondaryCategory?.idString,
            bucket = result.bucket.name,
            reasonsJson = json.encodeToString(result.reasons),
            headline = summary.headline,
            keyPointsJson = json.encodeToString(summary.bullets),
            factsJson = json.encodeToString(summary.facts),
            eventStart = result.entities.eventStart,
            eventEnd = result.entities.eventEnd,
            allDay = result.entities.allDay,
            room = result.entities.room,
            courseCode = result.entities.courseCode,
            read = false,
            summarySource = "OFFLINE",
            originalIntentKey = rawMail.originalIntentKey
        )

        mailDao.insertOrUpdate(entity)

        // 5. Trigger notification if needed
        notificationManager.postMailNotification(
            mail = entity,
            notifyGoodToKnow = profile?.notifyGoodToKnow ?: false,
            quietDuringClass = profile?.quietDuringClass ?: false,
            isInsideClassSlot = false
        )

        return entity
    }

    suspend fun reprocessSummariesForLanguage(language: SummaryLanguage) {
        val mails = mailDao.getAllMailsFlow().first()
        for (mail in mails) {
            val category = com.khabar.app.domain.model.MailCategory.fromString(mail.category)
            val entities = com.khabar.app.domain.model.ExtractedEntities(
                eventStart = mail.eventStart,
                eventEnd = mail.eventEnd,
                allDay = mail.allDay,
                dateText = null,
                room = mail.room,
                courseCode = mail.courseCode,
                link = mail.link
            )
            val summary = offlineSummarizer.summarize(
                category = category,
                subject = mail.subject,
                bodyText = mail.bodyText,
                entities = entities,
                language = language
            )
            val updated = mail.copy(
                headline = summary.headline,
                keyPointsJson = json.encodeToString(summary.bullets),
                factsJson = json.encodeToString(summary.facts)
            )
            mailDao.update(updated)
        }
    }

    suspend fun applyFeedbackOverride(mail: MailEntity, targetBucket: MailBucket) {
        // Save override in DB
        feedbackDao.setOverride(
            SenderOverrideEntity(
                sender = mail.sender,
                category = mail.category,
                bucket = targetBucket.name
            )
        )
        mailDao.updateBucket(mail.id, targetBucket.name)
    }

    suspend fun answerCoursePrompt(courseCode: String, isUserCourse: Boolean) {
        feedbackDao.markPromptAnswered(courseCode)
        if (isUserCourse) {
            profileRepository.addCourse(code = courseCode, section = "A")
        }
    }

    suspend fun updateAiSummary(mailId: String, headline: String, bullets: List<String>, facts: Map<String, String>) {
        val mail = mailDao.getMailById(mailId) ?: return
        val updated = mail.copy(
            headline = headline,
            keyPointsJson = json.encodeToString(bullets),
            factsJson = json.encodeToString(facts),
            summarySource = "AI"
        )
        mailDao.update(updated)
    }

    suspend fun getAllMailsDirect(): List<MailEntity> = mailDao.getAllMailsFlow().first()

    suspend fun moveMailBucket(mailId: String, bucket: MailBucket) {
        val mail = mailDao.getMailById(mailId) ?: return
        applyFeedbackOverride(mail, bucket)
    }

    suspend fun updateMail(mail: MailEntity) {
        mailDao.update(mail)
    }

    suspend fun deleteAllData() {
        clearAll()
    }

    suspend fun processIncomingMail(rawMail: RawMail): MailEntity {
        return processAndSaveIncoming(rawMail)
    }

    suspend fun loadSampleMails(): Int {
        val demoSource = mailSources.filterIsInstance<com.khabar.app.sources.DemoMailSource>().firstOrNull()
        if (demoSource != null) {
            val mails = demoSource.getSampleMails()
            for (m in mails) {
                processAndSaveIncoming(m)
            }
            return mails.size
        }
        return 0
    }

    suspend fun clearSampleMails() {
        mailDao.deleteDemoMails()
    }

    suspend fun clearAll() {
        mailDao.clear()
        feedbackDao.clearOverrides()
        feedbackDao.clearPrompts()
    }
}
