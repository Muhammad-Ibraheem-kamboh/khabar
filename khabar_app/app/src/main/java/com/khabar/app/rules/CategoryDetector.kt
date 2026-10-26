package com.khabar.app.rules

import com.khabar.app.domain.model.MailCategory

data class CategoryResult(
    val primary: MailCategory,
    val secondary: MailCategory? = null,
    val confidenceScore: Int = 0
)

object CategoryDetector {

    private val keywords = mapOf(
        MailCategory.CLASS_CHANGE to listOf(
            "cancelled", "canceled", "postponed", "rescheduled", "make-up class", "makeup class",
            "make up class", "class shifted", "room change", "venue change", "moved to", "no class",
            "class nahi hogi", "cancel ho gayi", "lecture cancel", "class cancel", "reschedule",
            "class nahi", "shifted from", "moved from"
        ),
        MailCategory.EXAM to listOf(
            "quiz", "midterm", "mid-term", "final exam", "datesheet", "date sheet", "exam schedule",
            "paper", "viva", "test", "examination", "examinations", "datesheets", "imtehan",
            "retake", "quizzes"
        ),
        MailCategory.DEADLINE to listOf(
            "assignment", "deadline", "due", "submit", "submission", "project report",
            "last date to submit", "jama karwaye", "turnitin", "cutoff", "case study submission",
            "extended", "deadline extended", "report submission"
        ),
        MailCategory.FEE to listOf(
            "fee", "challan", "installment", "dues", "payment", "fine", "scholarship",
            "financial aid", "financial assistance", "zakat", "qarz", "surcharge", "voucher",
            "tuition", "challans", "installments", "qist"
        ),
        MailCategory.REGISTRATION to listOf(
            "add/drop", "add drop", "registration", "course registration", "withdrawal",
            "withdraw", "semester freeze", "enrolment", "enrollment", "clearance", "degree audit",
            "graduation form", "advising", "pre-requisite", "dakhla"
        ),
        MailCategory.RESULT to listOf(
            "result", "results", "grade", "grades", "gpa", "cgpa", "transcript",
            "dean's list", "retotaling", "re-checking", "marks sheet", "nataij"
        ),
        MailCategory.ATTENDANCE to listOf(
            "attendance", "absences", "shortage", "short attendance", "warning notice",
            "deregistration", "ghair hazir", "hazri", "absent"
        ),
        MailCategory.CAREER to listOf(
            "internship", "intern", "job", "vacancy", "career fair", "recruitment", "hiring",
            "apply now", "management trainee", "mt program", "cdc", "placement", "full-time",
            "part-time", "opening", "vacancies"
        ),
        MailCategory.EVENT to listOf(
            "seminar", "workshop", "society", "competition", "festival", "guest lecture",
            "info session", "interactive session", "talk", "orientation", "club", "olympiad", "reunion", "gala", "cultural",
            "conclave", "hackathon", "conference", "alumni dinner", "annual dinner", "gala dinner", "sports",
            "exchange", "study abroad", "erasmus", "exchange program"
        ),
        MailCategory.GENERAL to listOf(
            "holiday", "closed", "notice", "announcement", "maintenance", "shuttle",
            "timetable", "news", "newsletter", "khabarnama", "circular", "advisory", "office hours",
            "timings", "timing", "revised", "mess", "hostel", "transport", "bus", "route", "library"
        )
    )

    fun detect(subject: String, body: String): CategoryResult {
        val lowerSubject = subject.lowercase()
        val lowerBody = body.lowercase()

        val scores = mutableMapOf<MailCategory, Int>()
        for (category in MailCategory.entries) {
            scores[category] = 0
        }

        // Special priority: if subject mentions class cancellation or room change
        if (lowerSubject.contains("class cancelled") || lowerSubject.contains("class cancel") ||
            lowerSubject.contains("cancel") && lowerSubject.contains("class") ||
            lowerSubject.contains("room change") || lowerSubject.contains("venue change") ||
            lowerSubject.contains("kal ki class nahi") || lowerSubject.contains("make-up class") ||
            lowerSubject.contains("makeup class")
        ) {
            scores[MailCategory.CLASS_CHANGE] = scores.getOrDefault(MailCategory.CLASS_CHANGE, 0) + 15
        }

        // Attendance check in subject
        if (lowerSubject.contains("attendance")) {
            scores[MailCategory.ATTENDANCE] = scores.getOrDefault(MailCategory.ATTENDANCE, 0) + 15
        }

        // Result / Grade check in subject (prioritize over exam if results are being announced)
        if (lowerSubject.contains("result") || lowerSubject.contains("results") || lowerSubject.contains("grade") || lowerSubject.contains("grades") || lowerSubject.contains("retotaling")) {
            scores[MailCategory.RESULT] = scores.getOrDefault(MailCategory.RESULT, 0) + 16
        }

        // Quiz / Exam check in subject
        if (lowerSubject.contains("quiz") || lowerSubject.contains("datesheet") || lowerSubject.contains("final exam") ||
            (lowerSubject.contains("midterm") && !lowerSubject.contains("result"))
        ) {
            scores[MailCategory.EXAM] = scores.getOrDefault(MailCategory.EXAM, 0) + 12
        }

        // Event / Exchange check in subject
        if (lowerSubject.contains("exchange") || lowerSubject.contains("seminar") || lowerSubject.contains("workshop") ||
            lowerSubject.contains("competition") || lowerSubject.contains("festival") || lowerSubject.contains("guest lecture") ||
            lowerSubject.contains("reunion") || lowerSubject.contains("olympiad") || lowerSubject.contains("alumni") ||
            lowerSubject.contains("dinner") || lowerSubject.contains("sports")
        ) {
            scores[MailCategory.EVENT] = scores.getOrDefault(MailCategory.EVENT, 0) + 15
        }

        // General announcement in subject
        if (lowerSubject.contains("hostel") || lowerSubject.contains("shuttle") || lowerSubject.contains("bus route") ||
            lowerSubject.contains("maintenance") || lowerSubject.contains("newsletter") || lowerSubject.contains("khabarnama") ||
            lowerSubject.contains("public holiday") || lowerSubject.contains("campus closed")
        ) {
            scores[MailCategory.GENERAL] = scores.getOrDefault(MailCategory.GENERAL, 0) + 12
        }

        // Registration / Graduation form check in subject (academic course/graduation registration only)
        val isEventRegistration = lowerSubject.contains("dinner") || lowerSubject.contains("reunion") ||
                lowerSubject.contains("olympiad") || lowerSubject.contains("sports") || lowerSubject.contains("alumni")
        if (!isEventRegistration && (
            lowerSubject.contains("registration") || lowerSubject.contains("graduation form") || lowerSubject.contains("add/drop") ||
            lowerSubject.contains("withdrawal") || lowerSubject.contains("enrolment") || lowerSubject.contains("enrollment") ||
            lowerSubject.contains("degree audit")
        )) {
            scores[MailCategory.REGISTRATION] = scores.getOrDefault(MailCategory.REGISTRATION, 0) + 16
        }

        // Fee check in subject
        if (lowerSubject.contains("fee") || lowerSubject.contains("challan") || lowerSubject.contains("scholarship") ||
            lowerSubject.contains("financial assistance") || lowerSubject.contains("zakat")
        ) {
            scores[MailCategory.FEE] = scores.getOrDefault(MailCategory.FEE, 0) + 15
        }

        // Career / Internship in subject
        if (lowerSubject.contains("internship") || lowerSubject.contains("recruitment") || lowerSubject.contains("management trainee") || lowerSubject.contains("job vacancy")) {
            scores[MailCategory.CAREER] = scores.getOrDefault(MailCategory.CAREER, 0) + 15
        }

        // Deadline in subject
        if ((lowerSubject.contains("deadline") && !lowerSubject.contains("withdrawal")) ||
            lowerSubject.contains("assignment") ||
            (lowerSubject.contains("submission") && !lowerSubject.contains("form"))
        ) {
            scores[MailCategory.DEADLINE] = scores.getOrDefault(MailCategory.DEADLINE, 0) + 12
        }

        for ((category, words) in keywords) {
            var catScore = scores.getOrDefault(category, 0)
            for (word in words) {
                if (lowerSubject.contains(word)) {
                    catScore += 3
                }
                if (lowerBody.contains(word)) {
                    catScore += 1
                }
            }
            scores[category] = catScore
        }

        val sorted = scores.entries.sortedByDescending { it.value }
        val top = sorted.firstOrNull() ?: return CategoryResult(MailCategory.GENERAL)

        if (top.value == 0) {
            return CategoryResult(MailCategory.GENERAL, null, 0)
        }

        val second = sorted.getOrNull(1)
        val secondary = if (second != null && second.value > 0 && (top.value - second.value) <= 2) {
            second.key
        } else null

        return CategoryResult(
            primary = top.key,
            secondary = secondary,
            confidenceScore = top.value
        )
    }
}
