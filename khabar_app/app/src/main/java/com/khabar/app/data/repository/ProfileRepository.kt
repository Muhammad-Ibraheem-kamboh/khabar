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

    suspend fun saveProfile(profile: ProfileEntity) {
        profileDao.insertOrUpdate(profile)
    }

    suspend fun saveCourses(courses: List<CourseEntity>) {
        courseDao.insertAll(courses)
    }

    suspend fun addCourse(code: String, section: String, name: String = "", teacherName: String = "", teacherEmail: String = "") {
        courseDao.insert(
            CourseEntity(
                code = code.uppercase().trim(),
                section = section.uppercase().trim(),
                name = name.trim(),
                teacherName = teacherName.trim(),
                teacherEmail = teacherEmail.trim()
            )
        )
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
            portalUrl = "https://portal.iba.edu.pk",
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
        courseDao.clear()
        courseDao.insertAll(courses)
    }

    suspend fun updateSummaryLanguage(lang: com.khabar.app.domain.model.SummaryLanguage) {
        val current = getProfile() ?: ProfileEntity()
        saveProfile(current.copy(summaryLanguage = lang.code))
    }

    suspend fun updateInterest(key: String, enabled: Boolean) {
        interestDao.setEnabled(key, enabled)
    }
}
