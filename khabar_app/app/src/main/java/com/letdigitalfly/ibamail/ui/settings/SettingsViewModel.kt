package com.letdigitalfly.ibamail.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letdigitalfly.ibamail.ai.KeystoreSecretManager
import com.letdigitalfly.ibamail.config.AppConfig
import com.letdigitalfly.ibamail.data.local.UserPreferences
import com.letdigitalfly.ibamail.data.local.entities.CourseEntity
import com.letdigitalfly.ibamail.data.local.entities.InterestEntity
import com.letdigitalfly.ibamail.data.local.entities.ProfileEntity
import com.letdigitalfly.ibamail.data.repository.MailRepository
import com.letdigitalfly.ibamail.data.repository.ProfileRepository
import com.letdigitalfly.ibamail.domain.model.SummaryLanguage
import com.letdigitalfly.ibamail.util.SampleMasker
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
        profileRepository.profileFlow,
        userPreferences.summaryLanguageFlow,
        userPreferences.instantModeFlow,
        userPreferences.fastModeFlow,
        userPreferences.quietDuringClassFlow,
        userPreferences.todayBoardNotificationFlow,
        profileRepository.coursesFlow,
        profileRepository.interestsFlow
    ) { values ->
        val profile = values[0] as? ProfileEntity
        val summaryLanguage = values[1] as? String ?: "en"
        val instantMode = values[2] as? Boolean ?: false
        val fastMode = values[3] as? Boolean ?: false
        val quietDuringClass = values[4] as? Boolean ?: false
        val todayBoard = values[5] as? Boolean ?: false
        val courses = values[6] as? List<CourseEntity> ?: emptyList()
        val interests = values[7] as? List<InterestEntity> ?: emptyList()

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
            dailyDigestEnabled = true,
            todayBoardEnabled = todayBoard,
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
        // Daily digest setting
    }

    fun setTodayBoard(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setTodayBoardNotification(enabled)
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
