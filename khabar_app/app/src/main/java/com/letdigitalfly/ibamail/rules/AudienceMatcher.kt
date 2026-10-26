package com.letdigitalfly.ibamail.rules

import com.letdigitalfly.ibamail.domain.model.ExtractedEntities
import com.letdigitalfly.ibamail.domain.model.MailBucket
import com.letdigitalfly.ibamail.domain.model.MailCategory

data class UserCourseInfo(
    val code: String,
    val section: String,
    val name: String? = null,
    val teacherName: String? = null,
    val teacherEmail: String? = null
)

data class UserProfileRulesView(
    val programId: String,
    val programName: String,
    val joinTerm: String,
    val joinYear: Int,
    val semesterIndex: Int,
    val standing: String, // Freshman, Sophomore, Junior, Senior
    val campus: String,   // Main, City, Both
    val major: String? = null,
    val shift: String? = null, // Morning, Evening
    val courses: List<UserCourseInfo>,
    val interests: Map<String, Boolean>
)

data class AudienceDecision(
    val bucket: MailBucket,
    val reasons: List<String>,
    val isTeacherMatch: Boolean = false,
    val isCourseMatch: Boolean = false,
    val unknownCoursePrompt: String? = null
)

object AudienceMatcher {

    fun match(
        category: MailCategory,
        entities: ExtractedEntities,
        sender: String,
        senderEmail: String?,
        subject: String,
        body: String,
        profile: UserProfileRulesView,
        receivedAtEpochMs: Long,
        senderOverrideBucket: MailBucket? = null
    ): AudienceDecision {
        val reasons = mutableListOf<String>()

        // 1. Feedback Learning Override wins first if present
        if (senderOverrideBucket != null) {
            reasons.add("You moved this sender before")
            return AudienceDecision(
                bucket = senderOverrideBucket,
                reasons = reasons
            )
        }

        // 2. Check Teacher Match
        val matchedTeacher = profile.courses.firstOrNull { course ->
            (!course.teacherEmail.isNullOrBlank() && senderEmail?.contains(course.teacherEmail, ignoreCase = true) == true) ||
            (!course.teacherName.isNullOrBlank() && sender.contains(course.teacherName, ignoreCase = true)) ||
            (!course.teacherName.isNullOrBlank() && (subject.contains(course.teacherName, ignoreCase = true) || body.contains(course.teacherName, ignoreCase = true)))
        }

        if (matchedTeacher != null) {
            reasons.add("From your teacher: ${matchedTeacher.teacherName ?: matchedTeacher.code}")
            return AudienceDecision(
                bucket = MailBucket.IMPORTANT,
                reasons = reasons,
                isTeacherMatch = true,
                isCourseMatch = true
            )
        }

        // 3. Course Match check
        var courseMatched = false
        var unknownCourse: String? = null

        if (entities.courseCode != null) {
            val userCourse = profile.courses.firstOrNull { it.code.equals(entities.courseCode, ignoreCase = true) }
            if (userCourse != null) {
                // Check section if specified in mail
                if (entities.section != null && userCourse.section.isNotBlank()) {
                    if (entities.section.equals(userCourse.section, ignoreCase = true) || entities.section.equals("ALL", ignoreCase = true)) {
                        courseMatched = true
                        reasons.add("Your course: ${userCourse.code}-${userCourse.section}")
                    } else {
                        // Section mismatch!
                        reasons.add("For Section ${entities.section} only (You are Section ${userCourse.section})")
                        return AudienceDecision(
                            bucket = MailBucket.HIDDEN,
                            reasons = reasons
                        )
                    }
                } else {
                    courseMatched = true
                    reasons.add("Your course: ${userCourse.code}")
                }
            } else {
                // Course is mentioned in mail, but user is not enrolled
                unknownCourse = entities.courseCode
            }
        }

        // Course match wins over other mismatches!
        if (courseMatched) {
            return AudienceDecision(
                bucket = MailBucket.IMPORTANT,
                reasons = reasons,
                isCourseMatch = true
            )
        }

        // 4. Dimension checks: Program, Batch, Standing, Campus
        var hasDimension = false
        var hasDimensionMismatch = false

        // A) Program
        if (entities.mentionedPrograms.isNotEmpty()) {
            hasDimension = true
            val matchesProgram = entities.mentionedPrograms.any {
                it.equals(profile.programId, ignoreCase = true) ||
                it.replace("_", "").equals(profile.programId.replace("_", ""), ignoreCase = true) ||
                it.equals(profile.programName, ignoreCase = true)
            }
            if (matchesProgram) {
                reasons.add("Your program: ${profile.programName}")
            } else {
                hasDimensionMismatch = true
                val otherProg = entities.mentionedPrograms.first()
                reasons.add("For $otherProg only")
            }
        }

        // B) Standing
        if (entities.mentionedStandings.isNotEmpty()) {
            hasDimension = true
            val matchesStanding = entities.mentionedStandings.any { it.equals(profile.standing, ignoreCase = true) }
            if (matchesStanding) {
                reasons.add(profile.standing)
            } else {
                hasDimensionMismatch = true
                reasons.add("For ${entities.mentionedStandings.joinToString()} only")
            }
        }

        // C) Batches
        if (entities.mentionedBatches.isNotEmpty()) {
            hasDimension = true
            val userBatch = "${profile.joinTerm} ${profile.joinYear}"
            val userYearBatch = "${profile.joinYear} batch"
            val matchesBatch = entities.mentionedBatches.any {
                it.contains(profile.joinYear.toString()) || it.contains(userBatch, ignoreCase = true)
            }
            if (matchesBatch) {
                reasons.add("Your batch: $userBatch")
            } else {
                hasDimensionMismatch = true
                reasons.add("For other batch")
            }
        }

        // D) Campus
        if (entities.campus != null && profile.campus != "Both") {
            hasDimension = true
            if (entities.campus.equals(profile.campus, ignoreCase = true) || entities.campus.equals("Both", ignoreCase = true)) {
                reasons.add("${profile.campus} Campus")
            } else {
                hasDimensionMismatch = true
                reasons.add("For ${entities.campus} Campus only")
            }
        }

        // E) Shift (for MBA)
        if (profile.shift != null) {
            val lowerText = "$subject $body".lowercase()
            if (profile.shift == "Morning" && lowerText.contains("mba evening")) {
                hasDimensionMismatch = true
                reasons.add("For MBA Evening only")
            } else if (profile.shift == "Evening" && lowerText.contains("mba morning")) {
                hasDimensionMismatch = true
                reasons.add("For MBA Morning only")
            }
        }

        // If any dimension mismatches and user didn't have course match -> HIDDEN
        if (hasDimensionMismatch) {
            return AudienceDecision(
                bucket = MailBucket.HIDDEN,
                reasons = reasons,
                unknownCoursePrompt = unknownCourse
            )
        }

        // 5. Interest-based Filter Check
        val lowerText = "$subject $body".lowercase()
        val interestChecks = listOf(
            Triple(listOf("hostel", "mess", "dining hall"), "HOSTEL", "Hostel"),
            Triple(listOf("transport", "point bus", "shuttle"), "TRANSPORT", "Transport"),
            Triple(listOf("scholarship", "financial aid", "zakat", "fee help"), "SCHOLARSHIPS", "Scholarships"),
            Triple(listOf("alumni", "reunion", "gala dinner"), "ALUMNI", "Alumni"),
            Triple(listOf("sports", "olympiad", "cricket", "football", "badminton"), "SPORTS", "Sports"),
            Triple(listOf("internship", "career fair", "recruitment", "job vacancy"), "INTERNSHIPS", "Internships"),
            Triple(listOf("exchange", "study abroad", "erasmus"), "EXCHANGE", "Exchange"),
            Triple(listOf("society", "societies", "club", "guest lecture", "seminar"), "SOCIETIES", "Societies and events")
        )

        for ((keywords, interestKey, label) in interestChecks) {
            if (keywords.any { lowerText.contains(it) }) {
                val isInterestOn = profile.interests[interestKey] ?: false
                if (!isInterestOn && !courseMatched) {
                    reasons.add("$label interest is off")
                    return AudienceDecision(
                        bucket = MailBucket.HIDDEN,
                        reasons = reasons,
                        unknownCoursePrompt = unknownCourse
                    )
                } else if (isInterestOn) {
                    reasons.add("$label are on")
                }
            }
        }

        // 6. Bucket Assignment Logic
        // Important categories
        val importantCategories = setOf(
            MailCategory.CLASS_CHANGE,
            MailCategory.EXAM,
            MailCategory.DEADLINE,
            MailCategory.ATTENDANCE,
            MailCategory.REGISTRATION,
            MailCategory.RESULT,
            MailCategory.FEE
        )

        val daysUntilEvent = if (entities.eventStart != null) {
            val diffMs = entities.eventStart - receivedAtEpochMs
            (diffMs / (1000 * 60 * 60 * 24)).toInt()
        } else null

        val isTargetedMatch = hasDimension && !hasDimensionMismatch

        // A) Important:
        // - TARGETED match with category in {CLASS_CHANGE, EXAM, DEADLINE, ATTENDANCE, REGISTRATION, RESULT, FEE}
        // - BROADCAST with an urgent category and a date within 3 days
        // - Urgent deadline or career closing within 3 days
        if (isTargetedMatch && importantCategories.contains(category)) {
            if (reasons.isEmpty()) reasons.add("For your program")
            return AudienceDecision(
                bucket = MailBucket.IMPORTANT,
                reasons = reasons,
                unknownCoursePrompt = unknownCourse
            )
        }

        if (entities.isBroadcast && (category in importantCategories || (category == MailCategory.CAREER && daysUntilEvent != null && daysUntilEvent in 0..3))) {
            if (daysUntilEvent != null && daysUntilEvent in 0..3) {
                reasons.add("Action needed within 3 days")
                return AudienceDecision(
                    bucket = MailBucket.IMPORTANT,
                    reasons = reasons,
                    unknownCoursePrompt = unknownCourse
                )
            }
            if (category in importantCategories) {
                reasons.add("For all students")
                return AudienceDecision(
                    bucket = MailBucket.IMPORTANT,
                    reasons = reasons,
                    unknownCoursePrompt = unknownCourse
                )
            }
        }

        // B) Good to know:
        // - BROADCAST general or interest-based with interest ON
        // - TARGETED matches of other categories
        if (isTargetedMatch || entities.isBroadcast) {
            if (reasons.isEmpty()) reasons.add("General campus announcement")
            return AudienceDecision(
                bucket = MailBucket.GOOD_TO_KNOW,
                reasons = reasons,
                unknownCoursePrompt = unknownCourse
            )
        }

        // C) Hidden: default
        reasons.add("Not matching your profile")
        return AudienceDecision(
            bucket = MailBucket.HIDDEN,
            reasons = reasons,
            unknownCoursePrompt = unknownCourse
        )
    }
}
