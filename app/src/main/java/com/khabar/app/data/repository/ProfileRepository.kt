package com.khabar.app.data.repository

import com.khabar.app.config.AppConfig
import com.khabar.app.data.local.dao.CourseDao
import com.khabar.app.data.local.dao.InterestDao
import com.khabar.app.data.local.dao.ProfileDao
import com.khabar.app.data.local.entities.CourseEntity
import com.khabar.app.data.local.entities.InterestEntity
import com.khabar.app.data.local.entities.ProfileEntity
import com.khabar.app.rules.UserCourseInfo
import com.khabar.app.rules.UserProfileRulesView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val courseDao: CourseDao,
    private val interestDao: InterestDao
) {
    val profileFlow: Flow<ProfileEntity?> = profileDao.getProfileFlow()
    val coursesFlow: Flow<List<CourseEntity>> = courseDao.getAllCoursesFlow()
    val interestsFlow: Flow<List<InterestEntity>> = interestDao.getAllInterestsFlow()

    suspend fun getProfile(): ProfileEntity? = profileDao.getProfile()

    suspend fun getAllCourses(): List<CourseEntity> = courseDao.getAllCourses()

    suspend fun updateCourseAbsences(id: Long, absences: Int) {
        courseDao.updateAbsences(id, absences.coerceAtLeast(0))
    }

    suspend fun saveProfile(profile: ProfileEntity) {
        profileDao.insertOrUpdate(profile)
    }

    fun normalizeCourseCode(code: String): String {
        return code.uppercase().replace(Regex("""[^A-Z0-9]"""), "").trim()
    }

    suspend fun upsertCourse(
        course: CourseEntity,
        isAuthoritativeLms: Boolean = false
    ): CourseEntity {
        val norm = normalizeCourseCode(course.code)
        if (norm.isBlank()) return course

        val matches = courseDao.findByNormalizedCode(norm)
        if (matches.isEmpty()) {
            val toInsert = course.copy(
                code = norm,
                section = course.section.trim().ifBlank { "A" },
                name = course.name.trim().ifBlank { norm }
            )
            val newId = courseDao.insert(toInsert)
            return toInsert.copy(id = newId)
        } else {
            val primary = matches.first()
            if (matches.size > 1) {
                for (dup in matches.drop(1)) {
                    courseDao.deleteById(dup.id)
                }
            }

            val updatedSection = when {
                isAuthoritativeLms && course.section.isNotBlank() -> course.section.trim()
                primary.section.isBlank() || primary.section == "A" -> course.section.trim().ifBlank { primary.section }
                else -> primary.section
            }
            val updatedName = when {
                isAuthoritativeLms && course.name.isNotBlank() && !course.name.startsWith("University Course") -> course.name.trim()
                primary.name.isBlank() || primary.name.startsWith("University Course") || primary.name == primary.code -> course.name.trim().ifBlank { primary.name }
                else -> primary.name
            }
            val updatedTeacher = when {
                isAuthoritativeLms && course.teacherName.isNotBlank() -> course.teacherName.trim()
                primary.teacherName.isBlank() -> course.teacherName.trim().ifBlank { primary.teacherName }
                else -> primary.teacherName
            }
            val updatedSlots = when {
                isAuthoritativeLms && course.slotsJson.isNotBlank() && course.slotsJson != "[]" -> course.slotsJson
                primary.slotsJson.isBlank() || primary.slotsJson == "[]" -> course.slotsJson.ifBlank { primary.slotsJson }
                else -> primary.slotsJson
            }
            val currentAbsences = if (course.absences > 0) course.absences else primary.absences
            val currentMax = if (course.maxAbsences > 0) course.maxAbsences else primary.maxAbsences

            val merged = primary.copy(
                code = norm,
                section = updatedSection,
                name = updatedName,
                teacherName = updatedTeacher,
                teacherEmail = course.teacherEmail.ifBlank { primary.teacherEmail },
                slotsJson = updatedSlots,
                absences = currentAbsences,
                maxAbsences = currentMax
            )
            courseDao.update(merged)
            return merged
        }
    }

    suspend fun saveCourses(courses: List<CourseEntity>, isAuthoritativeLms: Boolean = false) {
        for (c in courses) {
            upsertCourse(c, isAuthoritativeLms = isAuthoritativeLms)
        }
    }

    suspend fun addCourse(
        code: String,
        section: String,
        name: String = "",
        teacherName: String = "",
        teacherEmail: String = ""
    ) {
        upsertCourse(
            CourseEntity(
                code = code,
                section = section,
                name = name,
                teacherName = teacherName,
                teacherEmail = teacherEmail
            ),
            isAuthoritativeLms = false
        )
    }

    suspend fun deduplicateCourses(): Int {
        val all = courseDao.getAllCourses()
        val grouped = all.groupBy { normalizeCourseCode(it.code) }
        var duplicatesRemoved = 0

        for ((_, list) in grouped) {
            if (list.size > 1) {
                val best = list.maxWithOrNull(
                    compareBy<CourseEntity> { if (it.slotsJson != "[]" && it.slotsJson.isNotBlank()) 1 else 0 }
                        .thenBy { if (!it.name.startsWith("University Course") && it.name != it.code) 1 else 0 }
                        .thenBy { if (it.section != "A" && it.section.isNotBlank()) 1 else 0 }
                        .thenBy { it.absences }
                ) ?: list.first()

                for (item in list) {
                    if (item.id != best.id) {
                        courseDao.deleteById(item.id)
                        duplicatesRemoved++
                    }
                }
            }
        }
        return duplicatesRemoved
    }

    suspend fun updateProfileFromPortal(fullName: String?, programName: String?, forceUpdate: Boolean = false): Boolean {
        val current = getProfile() ?: ProfileEntity()
        var changed = false
        var updatedName = current.firstName
        var updatedProgramId = current.programId
        var updatedProgramName = current.programName

        if (!fullName.isNullOrBlank()) {
            val trimmedName = fullName.trim()
            if (forceUpdate || current.firstName.isBlank() || current.firstName.equals("Student", ignoreCase = true) || !current.firstName.contains(" ") || !current.firstName.equals(trimmedName, ignoreCase = true)) {
                updatedName = trimmedName
                changed = true
            }
        }

        if (!programName.isNullOrBlank()) {
            val normProg = programName.trim()
            if (forceUpdate || current.programName.isBlank() || current.programName.equals("BBA", ignoreCase = true) || current.programName.equals("Undergraduate", ignoreCase = true) || !current.programName.equals(normProg, ignoreCase = true)) {
                updatedProgramName = normProg
                updatedProgramId = normProg.lowercase().replace(" ", "_")
                changed = true
            }
        }

        if (changed) {
            saveProfile(
                current.copy(
                    firstName = updatedName,
                    programId = updatedProgramId,
                    programName = updatedProgramName
                )
            )
        }
        return changed
    }

    suspend fun removeCourse(id: Long) {
        courseDao.deleteById(id)
    }

    suspend fun setInterestEnabled(key: String, enabled: Boolean) {
        interestDao.setEnabled(key, enabled)
    }

    suspend fun initializeDefaultInterests(semesterIndex: Int, isGraduate: Boolean) {
        val existing = interestDao.getAllInterests()
        if (existing.isNotEmpty()) return

        // Smart defaults:
        // Internships ON if semester >= 5 (or graduate)
        // Exchange ON if Sophomore (3-4) or Junior (5-6)
        // Societies ON; others OFF
        val internshipsOn = semesterIndex >= 5 || isGraduate
        val exchangeOn = semesterIndex in 3..6 && !isGraduate

        val defaultList = AppConfig.DEFAULT_INTERESTS.map { (key, _) ->
            val enabled = when (key) {
                "INTERNSHIPS" -> internshipsOn
                "EXCHANGE" -> exchangeOn
                "SOCIETIES" -> true
                else -> false
            }
            InterestEntity(key = key, enabled = enabled)
        }
        interestDao.insertAll(defaultList)
    }

    suspend fun loadSampleProfile() {
        val sampleProfile = ProfileEntity(
            id = 1,
            firstName = "Ali",
            summaryLanguage = "en",
            programId = "bba",
            programName = "BBA",
            level = "Undergraduate",
            joinTerm = "Fall",
            joinYear = 2024,
            semesterIndex = 5,
            standing = "Junior",
            campus = "Main",
            major = "Marketing",
            portalUrl = AppConfig.PORTAL_URL,
            digestEnabled = true,
            digestTime = "07:30",
            quietDuringClass = false,
            notifyGoodToKnow = false,
            useAiForUnclear = false,
            fastMode = false,
            instantMode = true,
            onboardingDone = true
        )
        saveProfile(sampleProfile)

        courseDao.clear()
        val sampleCourses = listOf(
            CourseEntity(code = "FIN201", section = "B", name = "Financial Management", teacherName = "Dr. Waqas", teacherEmail = "waqas@iba.edu.pk"),
            CourseEntity(code = "MKT201", section = "A", name = "Principles of Marketing", teacherName = "Sir Farhan", teacherEmail = "farhan@iba.edu.pk"),
            CourseEntity(code = "ECO103", section = "C", name = "Principles of Microeconomics", teacherName = "Dr. Tariq", teacherEmail = "tariq@iba.edu.pk")
        )
        courseDao.insertAll(sampleCourses)

        interestDao.clear()
        val sampleInterests = listOf(
            InterestEntity("INTERNSHIPS", true),
            InterestEntity("EXCHANGE", true),
            InterestEntity("SOCIETIES", true),
            InterestEntity("SCHOLARSHIPS", false),
            InterestEntity("HOSTEL", false),
            InterestEntity("TRANSPORT", false),
            InterestEntity("ALUMNI", false),
            InterestEntity("SPORTS", false)
        )
        interestDao.insertAll(sampleInterests)
    }

    suspend fun getUserProfileRulesView(): UserProfileRulesView {
        val profile = getProfile() ?: ProfileEntity()
        val courses = courseDao.getAllCourses().map {
            UserCourseInfo(
                code = it.code,
                section = it.section,
                name = it.name,
                teacherName = it.teacherName,
                teacherEmail = it.teacherEmail
            )
        }
        val interests = interestDao.getAllInterests().associate { it.key to it.enabled }

        return UserProfileRulesView(
            programId = profile.programId,
            programName = profile.programName,
            joinTerm = profile.joinTerm,
            joinYear = profile.joinYear,
            semesterIndex = profile.semesterIndex,
            standing = profile.standing,
            campus = profile.campus,
            major = profile.major,
            shift = profile.shift,
            courses = courses,
            interests = interests
        )
    }

    suspend fun clearAll() {
        profileDao.clear()
        courseDao.clear()
        interestDao.clear()
    }

    suspend fun saveAcademicProfile(
        firstName: String,
        lastName: String,
        programCode: String,
        joinTerm: String,
        campus: String,
        courses: List<CourseEntity>
    ) {
        val current = getProfile() ?: ProfileEntity()
        val updated = current.copy(
            firstName = "$firstName $lastName".trim(),
            programId = programCode.lowercase(),
            programName = programCode.uppercase(),
            joinTerm = joinTerm,
            campus = campus
        )
        saveProfile(updated)

        val existingCourses = courseDao.getAllCourses()
        val newNormalizedCodes = courses.map { normalizeCourseCode(it.code) }.toSet()

        // Delete courses explicitly removed by user
        for (old in existingCourses) {
            val norm = normalizeCourseCode(old.code)
            if (!newNormalizedCodes.contains(norm)) {
                courseDao.deleteById(old.id)
            }
        }

        // Upsert all specified courses (preserving existing slots/absences)
        for (c in courses) {
            upsertCourse(c, isAuthoritativeLms = false)
        }
    }

    suspend fun updateSummaryLanguage(lang: com.khabar.app.domain.model.SummaryLanguage) {
        val current = getProfile() ?: ProfileEntity()
        saveProfile(current.copy(summaryLanguage = lang.code))
    }

    suspend fun updateInterest(key: String, enabled: Boolean) {
        interestDao.setEnabled(key, enabled)
    }
}
