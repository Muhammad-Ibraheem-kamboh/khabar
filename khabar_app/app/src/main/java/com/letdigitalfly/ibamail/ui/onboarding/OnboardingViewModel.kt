package com.letdigitalfly.ibamail.ui.onboarding

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.letdigitalfly.ibamail.data.local.UserPreferences
import com.letdigitalfly.ibamail.data.local.entities.CourseEntity
import com.letdigitalfly.ibamail.data.local.entities.InterestEntity
import com.letdigitalfly.ibamail.data.local.entities.ProfileEntity
import com.letdigitalfly.ibamail.data.repository.MailRepository
import com.letdigitalfly.ibamail.data.repository.ProfileRepository
import com.letdigitalfly.ibamail.domain.model.ProgramGroup
import com.letdigitalfly.ibamail.domain.model.ProgramItem
import com.letdigitalfly.ibamail.domain.model.SummaryLanguage
import com.letdigitalfly.ibamail.util.SemesterCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate

data class OnboardingUiState(
    val currentStep: Int = 1,
    val summaryLanguage: SummaryLanguage = SummaryLanguage.ENGLISH,
    val programGroups: List<ProgramGroup> = emptyList(),
    val selectedProgram: ProgramItem? = null,
    val customProgramName: String = "",
    val joinTerm: String = "Fall",
    val joinYear: Int = 2024,
    val semesterIndex: Int = 5,
    val standing: String = "Junior",
    val semesterOverridden: Boolean = false,
    val campus: String = "Both",
    val mbaShift: String = "Morning",
    val mbaExecBatch: String = "",
    val major: String = "Not decided yet",
    val courses: List<CourseEntity> = emptyList(),
    val interests: List<InterestEntity> = emptyList(),
    val isOcrScanning: Boolean = false,
    val ocrScannedCourses: List<CourseEntity> = emptyList(),
    val isReadyToFinish: Boolean = false
)

class OnboardingViewModel(
    private val context: Context,
    private val profileRepository: ProfileRepository,
    private val mailRepository: MailRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    init {
        loadProgramsFromJson()
        loadExistingState()
    }

    private fun loadProgramsFromJson() {
        try {
            val content = context.assets.open("programs.json").bufferedReader().use { it.readText() }
            val root = json.parseToJsonElement(content).jsonObject
            val groupsArray = root["groups"]?.jsonArray ?: return

            val groups = groupsArray.map { groupElem ->
                val groupObj = groupElem.jsonObject
                val groupName = groupObj["name"]?.jsonPrimitive?.content ?: ""
                val progArray = groupObj["programs"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())

                val items = progArray.map { progElem ->
                    val progObj = progElem.jsonObject
                    ProgramItem(
                        id = progObj["id"]?.jsonPrimitive?.content ?: "",
                        name = progObj["name"]?.jsonPrimitive?.content ?: "",
                        level = progObj["level"]?.jsonPrimitive?.content ?: groupName,
                        aliases = progObj["aliases"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    )
                }
                ProgramGroup(name = groupName, programs = items)
            }

            _uiState.value = _uiState.value.copy(
                programGroups = groups,
                selectedProgram = groups.firstOrNull()?.programs?.firstOrNull()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadExistingState() {
        viewModelScope.launch {
            val profile = profileRepository.getProfile()
            val existingCourses = profileRepository.coursesFlow.first()
            val existingInterests = profileRepository.interestsFlow.first()

            if (profile != null) {
                val prog = _uiState.value.programGroups.flatMap { it.programs }
                    .firstOrNull { it.id == profile.programId }
                    ?: ProgramItem(id = profile.programId, name = profile.programName, level = profile.level, aliases = emptyList())

                _uiState.value = _uiState.value.copy(
                    summaryLanguage = SummaryLanguage.fromCode(profile.summaryLanguage),
                    selectedProgram = prog,
                    joinTerm = profile.joinTerm,
                    joinYear = profile.joinYear,
                    semesterIndex = profile.semesterIndex,
                    standing = profile.standing,
                    campus = profile.campus,
                    major = profile.major ?: "Not decided yet",
                    courses = existingCourses,
                    interests = existingInterests
                )
            } else {
                recalculateSemester()
                profileRepository.initializeDefaultInterests(_uiState.value.semesterIndex, false)
                _uiState.value = _uiState.value.copy(
                    interests = profileRepository.interestsFlow.first()
                )
            }
        }
    }

    fun setStep(step: Int) {
        _uiState.value = _uiState.value.copy(currentStep = step)
    }

    fun setSummaryLanguage(lang: SummaryLanguage) {
        _uiState.value = _uiState.value.copy(summaryLanguage = lang)
        saveCurrentProfile()
    }

    fun selectProgram(program: ProgramItem) {
        _uiState.value = _uiState.value.copy(selectedProgram = program)
        recalculateSemester()
        saveCurrentProfile()
    }

    fun setCustomProgram(name: String) {
        _uiState.value = _uiState.value.copy(customProgramName = name)
    }

    fun setJoinTermAndYear(term: String, year: Int) {
        _uiState.value = _uiState.value.copy(joinTerm = term, joinYear = year, semesterOverridden = false)
        recalculateSemester()
        saveCurrentProfile()
    }

    fun overrideSemester(index: Int) {
        val isGrad = _uiState.value.selectedProgram?.level?.equals("Graduate", ignoreCase = true) == true
        val newStanding = SemesterCalculator.getStanding(index, isGrad)
        _uiState.value = _uiState.value.copy(
            semesterIndex = index,
            standing = newStanding,
            semesterOverridden = true
        )
        saveCurrentProfile()
    }

    private fun recalculateSemester() {
        if (_uiState.value.semesterOverridden) return
        val isGrad = _uiState.value.selectedProgram?.level?.equals("Graduate", ignoreCase = true) == true
        val result = SemesterCalculator.calculate(
            joinTerm = _uiState.value.joinTerm,
            joinYear = _uiState.value.joinYear,
            isGraduate = isGrad,
            referenceDate = LocalDate.now()
        )
        _uiState.value = _uiState.value.copy(
            semesterIndex = result.semesterIndex,
            standing = result.standing
        )
    }

    fun setCampus(campus: String) {
        _uiState.value = _uiState.value.copy(campus = campus)
        saveCurrentProfile()
    }

    fun setMbaShift(shift: String) {
        _uiState.value = _uiState.value.copy(mbaShift = shift)
        saveCurrentProfile()
    }

    fun setMbaExecBatch(batch: String) {
        _uiState.value = _uiState.value.copy(mbaExecBatch = batch)
        saveCurrentProfile()
    }

    fun setMajor(major: String) {
        _uiState.value = _uiState.value.copy(major = major)
        saveCurrentProfile()
    }

    fun addCourse(code: String, section: String, name: String = "", teacherName: String = "", teacherEmail: String = "") {
        viewModelScope.launch {
            profileRepository.addCourse(code, section, name, teacherName, teacherEmail)
            _uiState.value = _uiState.value.copy(
                courses = profileRepository.coursesFlow.first()
            )
        }
    }

    fun removeCourse(courseId: Long) {
        viewModelScope.launch {
            profileRepository.removeCourse(courseId)
            _uiState.value = _uiState.value.copy(
                courses = profileRepository.coursesFlow.first()
            )
        }
    }

    fun toggleInterest(key: String, enabled: Boolean) {
        viewModelScope.launch {
            profileRepository.setInterestEnabled(key, enabled)
            _uiState.value = _uiState.value.copy(
                interests = profileRepository.interestsFlow.first()
            )
        }
    }

    fun scanScreenshot(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(isOcrScanning = true)
        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val scanned = parseOcrTimetable(visionText.text)
                _uiState.value = _uiState.value.copy(
                    isOcrScanning = false,
                    ocrScannedCourses = scanned
                )
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(isOcrScanning = false)
            }
    }

    private fun parseOcrTimetable(text: String): List<CourseEntity> {
        val coursePattern = Regex("\\b([A-Z]{3})\\s?-?(\\d{3})\\b", RegexOption.IGNORE_CASE)
        val results = mutableListOf<CourseEntity>()

        coursePattern.findAll(text).forEach { match ->
            val code = "${match.groupValues[1].uppercase()}${match.groupValues[2]}"
            if (results.none { it.code == code }) {
                results.add(
                    CourseEntity(
                        code = code,
                        section = "A",
                        name = ""
                    )
                )
            }
        }
        return results
    }

    fun applyScannedCourses(courses: List<CourseEntity>) {
        viewModelScope.launch {
            profileRepository.saveCourses(courses)
            _uiState.value = _uiState.value.copy(
                courses = profileRepository.coursesFlow.first(),
                ocrScannedCourses = emptyList()
            )
        }
    }

    fun loadSampleDataAndProceed(onDone: () -> Unit) {
        viewModelScope.launch {
            userPreferences.setOnboardingDone(true)
            profileRepository.loadSampleProfile()
            val sampleMails = mailRepository.loadSampleMails()
            saveCurrentProfile(onboardingDone = true)
            onDone()
        }
    }

    fun finishOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            userPreferences.setOnboardingDone(true)
            saveCurrentProfile(onboardingDone = true)
            onDone()
        }
    }

    private fun saveCurrentProfile(onboardingDone: Boolean = false) {
        viewModelScope.launch {
            val state = _uiState.value
            val prog = state.selectedProgram
            val profile = ProfileEntity(
                id = 1,
                firstName = "Ali",
                summaryLanguage = state.summaryLanguage.code,
                programId = prog?.id ?: "bba",
                programName = prog?.name ?: state.customProgramName.ifBlank { "BBA" },
                level = prog?.level ?: "Undergraduate",
                joinTerm = state.joinTerm,
                joinYear = state.joinYear,
                semesterIndex = state.semesterIndex,
                semesterOverride = state.semesterOverridden,
                standing = state.standing,
                campus = state.campus,
                shift = if (prog?.name?.contains("MBA", ignoreCase = true) == true) state.mbaShift else null,
                emBatch = if (prog?.name?.contains("Executive", ignoreCase = true) == true) state.mbaExecBatch else null,
                major = state.major,
                onboardingDone = onboardingDone
            )
            profileRepository.saveProfile(profile)
        }
    }
}
