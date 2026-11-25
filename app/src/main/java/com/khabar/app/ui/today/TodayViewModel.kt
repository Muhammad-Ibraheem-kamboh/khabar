package com.khabar.app.ui.today

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khabar.app.KhabarApp
import com.khabar.app.data.local.entities.MailEntity
import com.khabar.app.data.local.entities.ProfileEntity
import com.khabar.app.data.local.entities.ReminderEntity
import com.khabar.app.data.repository.MailRepository
import com.khabar.app.data.repository.ProfileRepository
import com.khabar.app.data.repository.ReminderRepository
import com.khabar.app.domain.model.SummaryLanguage
import com.khabar.app.util.DateUtils
import com.khabar.app.util.DocumentScannerHelper
import com.khabar.app.util.ScannedDocumentResult
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
    val unknownCoursePromptCode: String? = null,
    val nextClassStatus: com.khabar.app.util.NextClassStatus = com.khabar.app.util.NextClassStatus(),
    val showNextClassWidget: Boolean = true,
    val showAbsenceTracker: Boolean = true
)

class TodayViewModel(
    private val mailRepository: MailRepository,
    private val profileRepository: ProfileRepository,
    private val reminderRepository: ReminderRepository,
    private val userPreferences: com.khabar.app.data.local.UserPreferences? = null
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

        // Real-time notification-to-dashboard pipeline listener
        viewModelScope.launch {
            mailRepository.mailReceivedEvents.collect {
                _currentTimeMs.value = System.currentTimeMillis()
            }
        }
    }

    private val statsFlow = mailRepository.getTodayStatsFlow()

    private val prefWidgetFlow = userPreferences?.nextClassWidgetEnabledFlow ?: MutableStateFlow(true)
    private val prefAbsenceFlow = userPreferences?.absenceTrackerEnabledFlow ?: MutableStateFlow(true)

    val uiState: StateFlow<TodayUiState> = combine(
        combine(
            profileRepository.profileFlow,
            reminderRepository.nearestUpcomingReminderFlow,
            mailRepository.topImportantUnreadFlow,
            statsFlow.first,
            statsFlow.second
        ) { profile, reminder, mails, rec, forYou ->
            arrayOf(profile, reminder, mails, rec, forYou)
        },
        combine(
            statsFlow.third,
            _isRefreshing,
            _currentTimeMs,
            mailRepository.pendingCoursePromptFlow,
            profileRepository.coursesFlow
        ) { hidden, refreshing, nowMs, prompt, courses ->
            arrayOf(hidden, refreshing, nowMs, prompt, courses)
        },
        combine(
            prefWidgetFlow,
            prefAbsenceFlow
        ) { showWidget, showAbsence ->
            arrayOf(showWidget, showAbsence)
        }
    ) { g1, g2, g3 ->
        val profile = g1[0] as? ProfileEntity
        val nearestReminder = g1[1] as? ReminderEntity
        val topMails = g1[2] as? List<MailEntity> ?: emptyList()
        val receivedCount = g1[3] as? Int ?: 0
        val forYouCount = g1[4] as? Int ?: 0

        val hiddenCount = g2[0] as? Int ?: 0
        val refreshing = g2[1] as? Boolean ?: false
        val nowMs = g2[2] as? Long ?: System.currentTimeMillis()
        val prompt = g2[3] as? com.khabar.app.data.local.entities.UnknownCoursePromptEntity
        val courses = g2[4] as? List<com.khabar.app.data.local.entities.CourseEntity> ?: emptyList()

        val showWidget = g3[0] as? Boolean ?: true
        val showAbsence = g3[1] as? Boolean ?: true

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

        val nextClassStatus = com.khabar.app.util.ClassScheduleHelper.findNextClassStatus(courses, nowMs)

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
            unknownCoursePromptCode = prompt?.code,
            nextClassStatus = nextClassStatus,
            showNextClassWidget = showWidget,
            showAbsenceTracker = showAbsence
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TodayUiState(todayDateFormatted = DateUtils.formatTodayDate())
    )

    fun updateCourseAbsence(courseId: Long, newAbsences: Int) {
        viewModelScope.launch {
            profileRepository.updateCourseAbsences(courseId, newAbsences)
        }
    }

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

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedNoticeDialog = MutableStateFlow<ScannedDocumentResult?>(null)
    val scannedNoticeDialog: StateFlow<ScannedDocumentResult?> = _scannedNoticeDialog.asStateFlow()

    fun scanBitmap(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val result = DocumentScannerHelper.processBitmap(bitmap)
                _scannedNoticeDialog.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun scanUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val result = DocumentScannerHelper.processDocumentUri(context, uri)
                _scannedNoticeDialog.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun saveScannedNotice(context: Context, result: ScannedDocumentResult, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val app = context.applicationContext as KhabarApp
                DocumentScannerHelper.saveScannedDocumentToKhabar(context, app.container, result)
                _scannedNoticeDialog.value = null
                onDone()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun dismissScannedDialog() {
        _scannedNoticeDialog.value = null
    }
}
