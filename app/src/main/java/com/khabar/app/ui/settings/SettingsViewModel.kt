package com.khabar.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khabar.app.ai.KeystoreSecretManager
import com.khabar.app.config.AppConfig
import com.khabar.app.data.local.UserPreferences
import com.khabar.app.data.local.entities.CourseEntity
import com.khabar.app.data.local.entities.InterestEntity
import com.khabar.app.data.local.entities.ProfileEntity
import com.khabar.app.data.repository.MailRepository
import com.khabar.app.data.repository.ProfileRepository
import com.khabar.app.domain.model.SummaryLanguage
import com.khabar.app.util.SampleMasker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: ProfileEntity? = null,
    val summaryLanguage: String = "en",
    val instantModeEnabled: Boolean = false,
    val fastModeEnabled: Boolean = false,
    val quietDuringClass: Boolean = false,
    val dailyDigestEnabled: Boolean = true,
    val todayBoardEnabled: Boolean = false,
    val focusIbaOnly: Boolean = false,
    val nextClassWidgetEnabled: Boolean = true,
    val absenceTrackerEnabled: Boolean = true,
    val morningBriefingEnabled: Boolean = true,
    val morningBriefingTime: String = "07:30",
    val deadlineAlarmEnabled: Boolean = true,
    val deadlineAlarmLeadMinutes: Int = 120,
    val offlineAiEnabled: Boolean = true,
    val campusId: String = com.khabar.app.config.CampusConfig.DEFAULT_CAMPUS_ID,
    val courses: List<CourseEntity> = emptyList(),
    val interests: List<InterestEntity> = emptyList(),
    val isOutlookConfigured: Boolean = false,
    val isGmailConfigured: Boolean = false,
    val isAiConfigured: Boolean = false,
    val maskedDataJson: String? = null
)

class SettingsViewModel(
    private val profileRepository: ProfileRepository,
    private val mailRepository: MailRepository,
    private val userPreferences: UserPreferences,
    private val secretManager: KeystoreSecretManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            profileRepository.profileFlow,
            userPreferences.summaryLanguageFlow,
            userPreferences.instantModeFlow,
            userPreferences.fastModeFlow
        ) { profile, lang, instant, fast ->
            arrayOf(profile, lang, instant, fast)
        },
        combine(
            userPreferences.quietDuringClassFlow,
            userPreferences.todayBoardNotificationFlow,
            userPreferences.focusIbaOnlyFlow,
            userPreferences.nextClassWidgetEnabledFlow
        ) { quiet, todayBoard, focusIba, nextClass ->
            arrayOf(quiet, todayBoard, focusIba, nextClass)
        },
        combine(
            userPreferences.absenceTrackerEnabledFlow,
            userPreferences.morningBriefingEnabledFlow,
            userPreferences.morningBriefingTimeFlow,
            userPreferences.deadlineAlarmEnabledFlow
        ) { absence, morning, briefingTime, alarm ->
            arrayOf(absence, morning, briefingTime, alarm)
        },
        combine(
            userPreferences.deadlineAlarmLeadMinutesFlow,
            userPreferences.offlineAiEnabledFlow,
            userPreferences.campusIdFlow,
            profileRepository.coursesFlow
        ) { leadMins, offlineAi, campusId, courses ->
            arrayOf(leadMins, offlineAi, campusId, courses)
        },
        profileRepository.interestsFlow
    ) { g1, g2, g3, g4, interests ->
        val profile = g1[0] as? ProfileEntity
        val summaryLanguage = g1[1] as? String ?: "en"
        val instantMode = g1[2] as? Boolean ?: false
        val fastMode = g1[3] as? Boolean ?: false

        val quietDuringClass = g2[0] as? Boolean ?: false
        val todayBoard = g2[1] as? Boolean ?: false
        val focusIba = g2[2] as? Boolean ?: false
        val nextClass = g2[3] as? Boolean ?: true

        val absence = g3[0] as? Boolean ?: true
        val morning = g3[1] as? Boolean ?: true
        val briefingTime = g3[2] as? String ?: "07:30"
        val alarm = g3[3] as? Boolean ?: true

        val leadMins = g4[0] as? Int ?: 120
        val offlineAi = g4[1] as? Boolean ?: true
        val campusId = g4[2] as? String ?: com.khabar.app.config.CampusConfig.DEFAULT_CAMPUS_ID
        val courses = g4[3] as? List<CourseEntity> ?: emptyList()

        val hasOutlook = AppConfig.MS_CLIENT_ID.isNotBlank()
        val hasGmail = AppConfig.GMAIL_ENABLED
        val hasAi = (secretManager.getSecret(AppConfig.PREF_KEY_AI_KEY)?.isNotBlank() == true) ||
                AppConfig.AI_API_KEY.isNotBlank()

        SettingsUiState(
            profile = profile,
            summaryLanguage = summaryLanguage,
            instantModeEnabled = instantMode,
            fastModeEnabled = fastMode,
            quietDuringClass = quietDuringClass,
            dailyDigestEnabled = morning,
            todayBoardEnabled = todayBoard,
            focusIbaOnly = focusIba,
            nextClassWidgetEnabled = nextClass,
            absenceTrackerEnabled = absence,
            morningBriefingEnabled = morning,
            morningBriefingTime = briefingTime,
            deadlineAlarmEnabled = alarm,
            deadlineAlarmLeadMinutes = leadMins,
            offlineAiEnabled = offlineAi,
            campusId = campusId,
            courses = courses,
            interests = interests,
            isOutlookConfigured = hasOutlook,
            isGmailConfigured = hasGmail,
            isAiConfigured = hasAi
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState()
    )

    fun setSummaryLanguage(lang: SummaryLanguage) {
        viewModelScope.launch {
            userPreferences.setSummaryLanguage(lang.code)
            profileRepository.updateSummaryLanguage(lang)
            mailRepository.reprocessSummariesForLanguage(lang)
        }
    }

    fun setInstantMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setInstantMode(enabled)
        }
    }

    fun setFastMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setFastMode(enabled)
        }
    }

    fun setQuietDuringClass(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setQuietDuringClass(enabled)
        }
    }

    fun setDailyDigest(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setMorningBriefingEnabled(enabled)
        }
    }

    fun setNextClassWidget(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNextClassWidgetEnabled(enabled)
        }
    }

    fun setAbsenceTracker(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAbsenceTrackerEnabled(enabled)
        }
    }

    fun setMorningBriefing(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setMorningBriefingEnabled(enabled)
            val profile = profileRepository.getProfile()
            if (profile != null) {
                profileRepository.saveProfile(profile.copy(digestEnabled = enabled))
            }
            if (enabled) {
                com.khabar.app.notifications.BootCompletedReceiver.scheduleDailyDigest(
                    context,
                    profile?.digestTime ?: "07:30"
                )
            } else {
                com.khabar.app.notifications.BootCompletedReceiver.cancelDailyDigest(context)
            }
        }
    }

    fun setMorningBriefingTime(context: Context, timeStr: String) {
        viewModelScope.launch {
            userPreferences.setMorningBriefingTime(timeStr)
            val profile = profileRepository.getProfile()
            if (profile != null) {
                profileRepository.saveProfile(profile.copy(digestTime = timeStr))
            }
            if (uiState.value.morningBriefingEnabled) {
                com.khabar.app.notifications.BootCompletedReceiver.scheduleDailyDigest(context, timeStr)
            }
        }
    }

    fun setDeadlineAlarm(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDeadlineAlarmEnabled(enabled)
        }
    }

    fun setDeadlineAlarmLeadMinutes(minutes: Int) {
        viewModelScope.launch {
            userPreferences.setDeadlineAlarmLeadMinutes(minutes)
        }
    }

    fun setOfflineAi(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setOfflineAiEnabled(enabled)
        }
    }

    fun setCampus(campusId: String) {
        viewModelScope.launch {
            userPreferences.setCampusId(campusId)
        }
    }

    fun setTodayBoard(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setTodayBoardNotification(enabled)
        }
    }

    fun setFocusIbaOnly(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setFocusIbaOnly(enabled)
        }
    }

    fun exportMaskedSample() {
        viewModelScope.launch {
            val allMails = mailRepository.getAllMailsDirect()
            val maskedJson = SampleMasker.maskMailsToJson(allMails)
        }
    }

    fun deleteAllData(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            mailRepository.deleteAllData()
            profileRepository.clearAll()
            userPreferences.clear()
            secretManager.clearAll()
            onComplete()
        }
    }
}
