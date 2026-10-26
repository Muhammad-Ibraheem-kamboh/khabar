package com.letdigitalfly.ibamail.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letdigitalfly.ibamail.data.local.entities.MailEntity
import com.letdigitalfly.ibamail.data.local.entities.ProfileEntity
import com.letdigitalfly.ibamail.data.local.entities.ReminderEntity
import com.letdigitalfly.ibamail.data.repository.MailRepository
import com.letdigitalfly.ibamail.data.repository.ProfileRepository
import com.letdigitalfly.ibamail.data.repository.ReminderRepository
import com.letdigitalfly.ibamail.domain.model.SummaryLanguage
import com.letdigitalfly.ibamail.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TodayUiState(
    val greeting: String = "Hello",
    val todayDateFormatted: String = "",
    val upNextReminder: ReminderEntity? = null,
    val upNextCountdown: String = "",
    val upNextProgress: Float = 0.5f,
    val topImportantMails: List<MailEntity> = emptyList(),
    val mailsReceivedToday: Int = 0,
    val mailsForYouToday: Int = 0,
    val mailsHiddenToday: Int = 0,
    val isRefreshing: Boolean = false,
    val hasMailSourceConnected: Boolean = true,
    val unknownCoursePromptCode: String? = null
)

class TodayViewModel(
    private val mailRepository: MailRepository,
    private val profileRepository: ProfileRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _currentTimeMs = MutableStateFlow(System.currentTimeMillis())

    init {
        // Ticker to refresh live countdown every minute
        viewModelScope.launch {
            while (isActive) {
                _currentTimeMs.value = System.currentTimeMillis()
                delay(60000)
            }
        }
    }

    private val statsFlow = mailRepository.getTodayStatsFlow()

    val uiState: StateFlow<TodayUiState> = combine(
        profileRepository.profileFlow,
        reminderRepository.nearestUpcomingReminderFlow,
        mailRepository.topImportantUnreadFlow,
        statsFlow.first,
        statsFlow.second,
        statsFlow.third,
        _isRefreshing,
        _currentTimeMs,
        mailRepository.pendingCoursePromptFlow
    ) { values ->
        val profile = values[0] as? ProfileEntity
        val nearestReminder = values[1] as? ReminderEntity
        val topMails = values[2] as? List<MailEntity> ?: emptyList()
        val receivedCount = values[3] as? Int ?: 0
        val forYouCount = values[4] as? Int ?: 0
        val hiddenCount = values[5] as? Int ?: 0
        val refreshing = values[6] as? Boolean ?: false
        val nowMs = values[7] as? Long ?: System.currentTimeMillis()
        val prompt = values[8] as? com.letdigitalfly.ibamail.data.local.entities.UnknownCoursePromptEntity

        val name = profile?.firstName?.trim() ?: ""
        val greeting = if (name.isNotBlank()) "Hello, $name" else "Hello"
        val lang = SummaryLanguage.fromCode(profile?.summaryLanguage ?: "en")

        val (countdown, progress) = if (nearestReminder != null) {
            val cd = DateUtils.formatCountdown(nearestReminder.dueAt, lang, nowMs)
            val totalSpan = (nearestReminder.dueAt - nearestReminder.createdAt).coerceAtLeast(1)
            val elapsed = (nowMs - nearestReminder.createdAt).coerceAtLeast(0)
            val prog = (elapsed.toFloat() / totalSpan.toFloat()).coerceIn(0.1f, 1.0f)
            cd to prog
        } else {
            "" to 0f
        }

        TodayUiState(
            greeting = greeting,
            todayDateFormatted = DateUtils.formatTodayDate(nowMs),
            upNextReminder = nearestReminder,
            upNextCountdown = countdown,
            upNextProgress = progress,
            topImportantMails = topMails,
            mailsReceivedToday = receivedCount,
            mailsForYouToday = forYouCount,
            mailsHiddenToday = hiddenCount,
            isRefreshing = refreshing,
            hasMailSourceConnected = true,
            unknownCoursePromptCode = prompt?.code
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TodayUiState(todayDateFormatted = DateUtils.formatTodayDate())
    )

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            mailRepository.syncAll()
            _isRefreshing.value = false
        }
    }

    fun answerCoursePrompt(courseCode: String, isMyCourse: Boolean) {
        viewModelScope.launch {
            mailRepository.answerCoursePrompt(courseCode, isMyCourse)
        }
    }
}
