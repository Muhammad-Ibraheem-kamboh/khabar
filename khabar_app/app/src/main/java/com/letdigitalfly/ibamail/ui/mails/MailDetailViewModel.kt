package com.letdigitalfly.ibamail.ui.mails

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letdigitalfly.ibamail.ai.AiSummarizer
import com.letdigitalfly.ibamail.ai.KeystoreSecretManager
import com.letdigitalfly.ibamail.config.AppConfig
import com.letdigitalfly.ibamail.data.local.UserPreferences
import com.letdigitalfly.ibamail.data.local.dao.ReminderDao
import com.letdigitalfly.ibamail.data.local.entities.MailEntity
import com.letdigitalfly.ibamail.data.local.entities.ReminderEntity
import com.letdigitalfly.ibamail.data.repository.MailRepository
import com.letdigitalfly.ibamail.domain.model.MailBucket
import com.letdigitalfly.ibamail.domain.model.SummaryLanguage
import com.letdigitalfly.ibamail.notifications.AlarmReminderReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MailDetailUiState(
    val mail: MailEntity? = null,
    val selectedTab: Int = 0, // 0 = Summary, 1 = Original
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
    val showAiConfigHelp: Boolean = false,
    val isReminderAdded: Boolean = false
)

class MailDetailViewModel(
    private val mailId: String,
    private val mailRepository: MailRepository,
    private val userPreferences: UserPreferences,
    private val reminderDao: ReminderDao,
    private val aiSummarizer: AiSummarizer,
    private val secretManager: KeystoreSecretManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MailDetailUiState())
    val uiState: StateFlow<MailDetailUiState> = _uiState.asStateFlow()

    init {
        loadMail()
    }

    private fun loadMail() {
        viewModelScope.launch {
            val mail = mailRepository.getMailById(mailId)
            if (mail != null) {
                mailRepository.markAsRead(mailId)
                _uiState.value = _uiState.value.copy(mail = mail)
            }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun requestAiSummary() {
        val currentMail = _uiState.value.mail ?: return
        viewModelScope.launch {
            val hasApiKey = secretManager.getSecret(AppConfig.PREF_KEY_AI_KEY)?.isNotBlank() == true ||
                    AppConfig.AI_API_KEY.isNotBlank()

            if (!hasApiKey) {
                _uiState.value = _uiState.value.copy(showAiConfigHelp = true)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isAiLoading = true, aiError = null)
            val langCode = userPreferences.summaryLanguageFlow.first()
            val lang = SummaryLanguage.fromCode(langCode)
            val result = aiSummarizer.summarize(
                subject = currentMail.subject,
                body = currentMail.bodyText,
                language = lang
            )

            if (result != null) {
                mailRepository.updateAiSummary(
                    mailId = currentMail.id,
                    headline = result.headline,
                    bullets = result.bulletPoints,
                    facts = result.facts
                )
                val updated = mailRepository.getMailById(mailId)
                _uiState.value = _uiState.value.copy(
                    mail = updated,
                    isAiLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isAiLoading = false,
                    aiError = "Could not generate AI summary. Using offline summary."
                )
            }
        }
    }

    fun dismissAiHelp() {
        _uiState.value = _uiState.value.copy(showAiConfigHelp = false)
    }

    fun moveToHidden() {
        viewModelScope.launch {
            mailRepository.moveMailBucket(mailId, MailBucket.HIDDEN)
            val mail = mailRepository.getMailById(mailId)
            _uiState.value = _uiState.value.copy(mail = mail)
        }
    }

    fun moveToImportant() {
        viewModelScope.launch {
            mailRepository.moveMailBucket(mailId, MailBucket.IMPORTANT)
            val mail = mailRepository.getMailById(mailId)
            _uiState.value = _uiState.value.copy(mail = mail)
        }
    }

    fun addReminder(context: Context, offsetHours: Long = 2) {
        val mail = _uiState.value.mail ?: return
        val dueAt = mail.extractedDateEpoch ?: (System.currentTimeMillis() + 86400_000L)
        val triggerAt = (dueAt - (offsetHours * 3600_000L)).coerceAtLeast(System.currentTimeMillis() + 60_000L)

        viewModelScope.launch {
            val reminder = ReminderEntity(
                mailId = mail.id,
                title = mail.headline.ifBlank { mail.subject },
                courseCode = mail.courseCode,
                dueAt = dueAt,
                remindAt = triggerAt,
                done = false
            )
            val id = reminderDao.insert(reminder)

            // Set system alarm
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(context, AlarmReminderReceiver::class.java).apply {
                putExtra("reminder_title", reminder.title)
                putExtra("course_code", reminder.courseCode)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )

            _uiState.value = _uiState.value.copy(isReminderAdded = true)
        }
    }
}
