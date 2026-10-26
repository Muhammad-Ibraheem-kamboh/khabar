package com.khabar.app.sources

import android.content.Context
import com.khabar.app.domain.model.MailCategory
import com.khabar.app.domain.model.RawMail
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DemoMailSource(private val context: Context) : MailSource {

    override val id: String = "demo_source"
    override val displayName: String = "Demo Sample Mails"
    override val isConfigured: Boolean = true
    override var isConnected: Boolean = false

    private val json = Json { ignoreUnknownKeys = true }
    private val _updatesFlow = MutableSharedFlow<RawMail>(extraBufferCapacity = 10)
    override val updatesFlow: SharedFlow<RawMail> = _updatesFlow.asSharedFlow()

    override suspend fun fetchNew(): List<RawMail> {
        return loadSampleMails()
    }

    fun getSampleMails(): List<RawMail> = loadSampleMails()

    fun loadSampleMails(): List<RawMail> {
        val jsonString = try {
            context.assets.open("sample_mails.json").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            return emptyList()
        }

        val array = json.parseToJsonElement(jsonString).jsonArray
        val now = System.currentTimeMillis()

        return array.mapIndexed { index, elem ->
            val obj = elem.jsonObject
            val mailId = obj["id"]?.jsonPrimitive?.content ?: "demo_$index"
            val sender = obj["sender"]?.jsonPrimitive?.content ?: "IBA Office"
            val senderEmail = obj["senderEmail"]?.jsonPrimitive?.content
            val subject = obj["subject"]?.jsonPrimitive?.content ?: ""
            val bodyText = obj["bodyText"]?.jsonPrimitive?.content ?: ""
            val relDays = obj["relativeDays"]?.jsonPrimitive?.intOrNull ?: 0
            val hasAttachments = obj["hasAttachments"]?.jsonPrimitive?.booleanOrNull ?: false
            val attachmentNames = obj["attachmentNames"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

            // Calculate timestamp relative to now
            val receivedAt = now - (relDays * 86400000L) - (index * 3600000L)

            RawMail(
                id = mailId,
                sourceId = id,
                sender = sender,
                senderEmail = senderEmail,
                subject = subject,
                bodyText = bodyText,
                receivedAt = receivedAt,
                isPreviewOnly = false,
                hasAttachments = hasAttachments,
                attachmentNames = attachmentNames,
                link = null
            )
        }
    }

    suspend fun simulateIncoming(category: MailCategory): RawMail {
        val now = System.currentTimeMillis()
        val randomSuffix = (1000..9999).random()
        val mailId = "sim_${category.idString.lowercase()}_$randomSuffix"

        val (sender, senderEmail, subject, body) = when (category) {
            MailCategory.CLASS_CHANGE -> Quad(
                "Program Office",
                "programoffice@iba.edu.pk",
                "Class cancelled: MKT201 (Section A) tomorrow 9:00 AM",
                "Dear Students, Please note that MKT201 Section A scheduled for tomorrow at 9:00 AM is cancelled as instructor is unwell. Makeup class schedule will be announced later."
            )
            MailCategory.EXAM -> Quad(
                "Examinations Office",
                "exams@iba.edu.pk",
                "Quiz 2 schedule - FIN201 Section B, Tuesday 11:00 AM, MCC-12",
                "Dear Students of FIN201, Quiz 2 will be conducted on Tuesday at 11:00 AM in MCC-12 Main Campus. Bring scientific calculators."
            )
            MailCategory.DEADLINE -> Quad(
                "Dr. Tariq (Instructor)",
                "tariq@iba.edu.pk",
                "Assignment 3 deadline extended - MKT201, Friday 11:59 PM",
                "Dear Students, Assignment 3 submission cutoff on LMS has been extended to Friday 11:59 PM. Please submit your project reports on https://lms.iba.edu.pk."
            )
            MailCategory.FEE -> Quad(
                "Accounts Office",
                "accounts@iba.edu.pk",
                "Fee challan Fall installment 2, last date without fine in 3 days",
                "Notice to all students: The last date to submit Fall installment 2 fee without late surcharge is in 3 days. Download challan from https://portal.iba.edu.pk."
            )
            MailCategory.REGISTRATION -> Quad(
                "Registrar Office",
                "registrar@iba.edu.pk",
                "Spring course registration for Junior students, 10:00 AM to 8:00 PM",
                "Dear Junior students, Course registration portal will open tomorrow at 10:00 AM. Clear all dues prior to enrollment."
            )
            MailCategory.RESULT -> Quad(
                "Examinations Office",
                "exams@iba.edu.pk",
                "Midterm results declared - BBA Fall 2024 batch",
                "Dear BBA Fall 2024 students, Midterm grades are now available on the ERP portal. Retotaling deadline is 3 days."
            )
            MailCategory.ATTENDANCE -> Quad(
                "Examinations Office",
                "exams@iba.edu.pk",
                "Attendance warning: FIN201, 3 absences recorded",
                "Student Warning: You have reached 3 absences in FIN201 Section B. Further shortage will lead to course deregistration."
            )
            MailCategory.CAREER -> Quad(
                "Career Development Center",
                "cdc@iba.edu.pk",
                "Internship: Marketing Intern, apply within 2 days via job portal",
                "Urgent opening: FMCG partner is looking for Marketing interns. Eligible BBA students can apply within 2 days on https://jobportal.iba.edu.pk."
            )
            MailCategory.EVENT -> Quad(
                "Student Societies Office",
                "societies@iba.edu.pk",
                "Seminar: Future of Fintech by Finance Club, Thursday 2:00 PM",
                "Finance Club invites all students to a guest lecture on Fintech innovation in G&T Auditorium this Thursday at 2:00 PM."
            )
            MailCategory.GENERAL -> Quad(
                "Registrar Office",
                "registrar@iba.edu.pk",
                "Campus closed on Monday due to public holiday",
                "All Main and City Campus offices and classes will remain closed this Monday on account of public holiday."
            )
        }

        val mail = RawMail(
            id = mailId,
            sourceId = id,
            sender = sender,
            senderEmail = senderEmail,
            subject = subject,
            bodyText = body,
            receivedAt = now,
            isPreviewOnly = false,
            hasAttachments = false,
            link = if (body.contains("http")) "https://portal.iba.edu.pk" else null
        )

        _updatesFlow.emit(mail)
        return mail
    }

    private data class Quad(val sender: String, val senderEmail: String, val subject: String, val body: String)
}
