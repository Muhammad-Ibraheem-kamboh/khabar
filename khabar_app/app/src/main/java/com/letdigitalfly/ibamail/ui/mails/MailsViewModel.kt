package com.letdigitalfly.ibamail.ui.mails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letdigitalfly.ibamail.data.local.entities.MailEntity
import com.letdigitalfly.ibamail.data.repository.MailRepository
import com.letdigitalfly.ibamail.domain.model.MailBucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MailsUiState(
    val selectedBucket: MailBucket = MailBucket.IMPORTANT,
    val searchQuery: String = "",
    val todayMails: List<MailEntity> = emptyList(),
    val yesterdayMails: List<MailEntity> = emptyList(),
    val earlierMails: List<MailEntity> = emptyList(),
    val importantCount: Int = 0,
    val goodToKnowCount: Int = 0,
    val hiddenCount: Int = 0,
    val isRefreshing: Boolean = false
)

class MailsViewModel(
    private val mailRepository: MailRepository
) : ViewModel() {

    private val _selectedBucket = MutableStateFlow(MailBucket.IMPORTANT)
    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<MailsUiState> = combine(
        mailRepository.allMailsFlow,
        _selectedBucket,
        _searchQuery,
        _isRefreshing
    ) { allMails, bucket, query, refreshing ->
        val importantCount = allMails.count { it.bucket == MailBucket.IMPORTANT.name }
        val goodToKnowCount = allMails.count { it.bucket == MailBucket.GOOD_TO_KNOW.name }
        val hiddenCount = allMails.count { it.bucket == MailBucket.HIDDEN.name }

        val filtered = allMails
            .filter { it.bucket == bucket.name }
            .filter { mail ->
                if (query.isBlank()) true
                else {
                    mail.subject.contains(query, ignoreCase = true) ||
                    mail.headline.contains(query, ignoreCase = true) ||
                    mail.senderName.contains(query, ignoreCase = true) ||
                    (mail.courseCode?.contains(query, ignoreCase = true) == true) ||
                    mail.bodyPreview.contains(query, ignoreCase = true)
                }
            }
            .sortedByDescending { it.receivedAt }

        val (today, yesterday, earlier) = partitionByDate(filtered)

        MailsUiState(
            selectedBucket = bucket,
            searchQuery = query,
            todayMails = today,
            yesterdayMails = yesterday,
            earlierMails = earlier,
            importantCount = importantCount,
            goodToKnowCount = goodToKnowCount,
            hiddenCount = hiddenCount,
            isRefreshing = refreshing
        )
    }
        // Offload filtering, search & date partitioning from Main thread to Background thread
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            MailsUiState()
        )

    fun selectBucket(bucket: MailBucket) {
        _selectedBucket.value = bucket
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun markAsRead(mailId: String) {
        viewModelScope.launch {
            mailRepository.markAsRead(mailId)
        }
    }

    fun moveToHidden(mailId: String) {
        viewModelScope.launch {
            mailRepository.moveMailBucket(mailId, MailBucket.HIDDEN)
        }
    }

    fun moveToImportant(mailId: String) {
        viewModelScope.launch {
            mailRepository.moveMailBucket(mailId, MailBucket.IMPORTANT)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            mailRepository.syncAll()
            _isRefreshing.value = false
        }
    }

    private fun partitionByDate(mails: List<MailEntity>): Triple<List<MailEntity>, List<MailEntity>, List<MailEntity>> {
        val today = mutableListOf<MailEntity>()
        val yesterday = mutableListOf<MailEntity>()
        val earlier = mutableListOf<MailEntity>()

        val now = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfYesterday = startOfToday - 86400_000L

        for (mail in mails) {
            when {
                mail.receivedAt >= startOfToday -> today.add(mail)
                mail.receivedAt >= startOfYesterday -> yesterday.add(mail)
                else -> earlier.add(mail)
            }
        }

        return Triple(today, yesterday, earlier)
    }
}
