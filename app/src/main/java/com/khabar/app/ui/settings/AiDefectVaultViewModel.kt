package com.khabar.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khabar.app.ai.AiDefectManager
import com.khabar.app.data.local.entities.AiDefectLogEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AiDefectVaultViewModel(
    private val defectManager: AiDefectManager
) : ViewModel() {

    val logsFlow: StateFlow<List<AiDefectLogEntity>> = defectManager.allLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFilter = MutableStateFlow("ALL") // "ALL", "ALIGNMENT", "BUG"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            defectManager.deleteLog(id)
            refreshPromptPatch()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            defectManager.clearAllLogs()
            refreshPromptPatch()
            _toastMessage.value = "All logs cleared"
        }
    }

    private val _promptPatch = MutableStateFlow("")
    val promptPatch: StateFlow<String> = _promptPatch.asStateFlow()

    init {
        refreshPromptPatch()
    }

    fun refreshPromptPatch() {
        viewModelScope.launch {
            _promptPatch.value = defectManager.exportPromptPatch()
        }
    }

    fun copyPromptPatch(context: Context) {
        viewModelScope.launch {
            val patch = defectManager.exportPromptPatch()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Khabri System Prompt Patch", patch)
            clipboard.setPrimaryClip(clip)
            _toastMessage.value = "⚡ Copied System Prompt Patch to clipboard!"
        }
    }

    fun copyMarkdownToClipboard(context: Context) {
        viewModelScope.launch {
            val markdown = defectManager.exportMarkdownReport()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Khabri AI Improvement Log", markdown)
            clipboard.setPrimaryClip(clip)
            _toastMessage.value = "📋 Copied full Markdown to clipboard!"
        }
    }

    fun shareMarkdown(context: Context) {
        viewModelScope.launch {
            val markdown = defectManager.exportMarkdownReport()
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Khabri AI Improvement & Bug Log")
                putExtra(Intent.EXTRA_TEXT, markdown)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Khabri Improvement Log"))
        }
    }
}
