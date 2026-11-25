package com.khabar.app.ui.portal

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.khabar.app.data.local.UserPreferences
import com.khabar.app.data.local.entities.CourseEntity
import com.khabar.app.data.repository.MailRepository
import com.khabar.app.data.repository.ProfileRepository
import com.khabar.app.domain.model.PortalCourseDetector
import com.khabar.app.domain.model.RawMail
import com.khabar.app.ui.theme.IosGreen
import com.khabar.app.ui.theme.IosSecondaryText
import com.khabar.app.ui.theme.KhabarBlue
import com.khabar.app.util.ClassScheduleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class PortalTab(val title: String, val defaultUrl: String) {
    UMS("IBA UMS", UserPreferences.DEFAULT_UMS_URL),
    LMS("IBA LMS", UserPreferences.DEFAULT_LMS_URL),
    CUSTOM("Custom Portal", UserPreferences.DEFAULT_CUSTOM_PORTAL_URL)
}

data class PortalErrorState(
    val errorCode: Int,
    val description: String,
    val failingUrl: String,
    val isDnsFailure: Boolean
)

data class PortalDiscrepancyReport(
    val detectedName: String? = null,
    val currentName: String = "",
    val detectedProgram: String? = null,
    val currentProgram: String = "",
    val detectedErp: String? = null,
    val newCourses: List<CourseEntity> = emptyList(),
    val updatedCourses: List<CourseEntity> = emptyList(),
    val scheduleClashes: List<ClassScheduleHelper.ScheduleClash> = emptyList(),
    val isLmsAuthoritative: Boolean = false,
    val sourcePortalName: String = ""
)

object PortalSecurityHelper {
    fun extractHost(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val normalized = if (!url.contains("://")) "https://$url" else url
            val uri = java.net.URI(normalized)
            val h = uri.host?.lowercase()?.trim()
            if (!h.isNullOrBlank()) {
                h
            } else {
                extractHostFallback(url)
            }
        } catch (_: Exception) {
            extractHostFallback(url)
        }
    }

    private fun extractHostFallback(url: String): String? {
        val withoutScheme = if (url.contains("://")) url.substringAfter("://") else url
        val withoutUserInfo = if (withoutScheme.contains("@")) withoutScheme.substringAfter("@") else withoutScheme
        val hostOnly = withoutUserInfo.substringBefore("/").substringBefore("?").substringBefore("#").substringBefore(":")
        return hostOnly.lowercase().trim().ifBlank { null }
    }

    fun isTrustedSslHost(host: String?, activeTargetUrl: String? = null): Boolean {
        if (host.isNullOrBlank()) return false
        val lowerHost = host.lowercase().trim()
        val targetHost = extractHost(activeTargetUrl)

        if (targetHost != null && (lowerHost == targetHost || lowerHost.endsWith(".$targetHost"))) return true

        return lowerHost == "iba.edu.pk" ||
                lowerHost.endsWith(".iba.edu.pk") ||
                lowerHost == "microsoft.com" ||
                lowerHost.endsWith(".microsoft.com") ||
                lowerHost == "microsoftonline.com" ||
                lowerHost.endsWith(".microsoftonline.com") ||
                lowerHost == "live.com" ||
                lowerHost.endsWith(".live.com") ||
                lowerHost == "office.com" ||
                lowerHost.endsWith(".office.com") ||
                lowerHost == "office365.com" ||
                lowerHost.endsWith(".office365.com") ||
                lowerHost == "windows.net" ||
                lowerHost.endsWith(".windows.net") ||
                lowerHost == "msauth.net" ||
                lowerHost.endsWith(".msauth.net")
    }

    fun shouldKeepInWebView(scheme: String?): Boolean {
        if (scheme.isNullOrBlank()) return false
        val s = scheme.lowercase().trim()
        return s == "http" ||
                s == "https" ||
                s == "javascript" ||
                s == "about" ||
                s == "data" ||
                s == "blob"
    }
}

object PortalContentParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseExtractedContent(jsonResult: String?): Pair<String, String> {
        if (jsonResult.isNullOrBlank() || jsonResult == "null") {
            return Pair("", "")
        }
        return try {
            val trimmed = jsonResult.trim()
            val contentString = if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                val element = json.parseToJsonElement(trimmed)
                element.jsonPrimitive.contentOrNull ?: trimmed
            } else {
                trimmed
            }

            val root = json.parseToJsonElement(contentString).jsonObject
            val title = root["title"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
            val text = root["text"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
            Pair(title, text)
        } catch (_: Exception) {
            try {
                val trimmed = jsonResult.trim()
                val titleMatch = Regex("\"title\"\\s*:\\s*\"([^\"]*)\"").find(trimmed)
                val textMatch = Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").find(trimmed)
                val fallbackTitle = titleMatch?.groupValues?.getOrNull(1) ?: ""
                val fallbackText = textMatch?.groupValues?.getOrNull(1) ?: ""
                Pair(fallbackTitle, fallbackText)
            } catch (_: Exception) {
                Pair("", "")
            }
        }
    }
}

class PortalBridge(
    private val onPageContentExtracted: (title: String, body: String, url: String) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun onPageContent(title: String, body: String, url: String) {
        onPageContentExtracted(title, body, url)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PortalWebScreen(
    userPreferences: UserPreferences,
    mailRepository: MailRepository,
    profileRepository: ProfileRepository? = null,
    onAskKhabri: (title: String, extractedText: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Automatically migrate and clean any obsolete portal URLs on load
    LaunchedEffect(Unit) {
        userPreferences.migrateInvalidPortalUrls()
    }

    val savedPortalUrl by userPreferences.portalUrlFlow.collectAsState(initial = UserPreferences.DEFAULT_UMS_URL)
    val savedLmsUrl by userPreferences.lmsUrlFlow.collectAsState(initial = UserPreferences.DEFAULT_LMS_URL)
    val savedCustomUrl by userPreferences.customPortalUrlFlow.collectAsState(initial = UserPreferences.DEFAULT_CUSTOM_PORTAL_URL)

    var currentTab by remember { mutableStateOf(PortalTab.UMS) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var currentUrl by remember { mutableStateOf("") }
    var currentTitle by remember { mutableStateOf("") }
    var isExtracting by remember { mutableStateOf(false) }
    var portalError by remember { mutableStateOf<PortalErrorState?>(null) }

    var showEditUrlDialog by remember { mutableStateOf(false) }
    var editUrlInput by remember { mutableStateOf("") }

    var pendingDiscrepancyReport by remember { mutableStateOf<PortalDiscrepancyReport?>(null) }

    var autoSyncBadgeText by remember { mutableStateOf<String?>(null) }
    var lastAutoScrapedUrl by remember { mutableStateOf("") }

    LaunchedEffect(autoSyncBadgeText) {
        if (autoSyncBadgeText != null) {
            delay(3500)
            autoSyncBadgeText = null
        }
    }

    val handlePortalExtraction: (String, String, String, Boolean) -> Unit = { extractedTitle, rawText, sourceUrl, isManual ->
        if (rawText.length >= 20) {
            scope.launch {
                try {
                    // 1. Auto-save page announcements / bulletins to mailDao
                    val pageHash = (sourceUrl.hashCode().toString() + "_" + rawText.take(120).hashCode().toString()).replace("-", "n")
                    val rawMail = RawMail(
                        id = "portal_${currentTab.name}_$pageHash",
                        sourceId = "PORTAL_${currentTab.name}",
                        sender = currentTab.title,
                        senderEmail = "portal@iba.edu.pk",
                        subject = extractedTitle.ifBlank { "Portal Notice: ${currentTab.title}" },
                        bodyText = rawText.take(8000),
                        receivedAt = System.currentTimeMillis()
                    )
                    mailRepository.processAndSaveIncoming(rawMail)

                    // 2. Autonomous course, schedule, and profile detection
                    if (profileRepository != null) {
                        val isLms = currentTab == PortalTab.LMS
                        val detectedProfile = PortalCourseDetector.detectStudentProfile(rawText)
                        val detectedCourses = PortalCourseDetector.detectCourses(rawText)
                        val currentProfile = profileRepository.profileFlow.first()
                        val existingCourses = profileRepository.coursesFlow.first()

                        val existingByCode = existingCourses.associateBy { PortalCourseDetector.normalizeCourseCode(it.code) }

                        val newCourses = mutableListOf<CourseEntity>()
                        val updatedCourses = mutableListOf<CourseEntity>()

                        for (detected in detectedCourses) {
                            val normCode = PortalCourseDetector.normalizeCourseCode(detected.code)
                            val matched = existingByCode[normCode]
                            if (matched == null) {
                                newCourses.add(detected)
                            } else {
                                var changed = false
                                var betterSection = matched.section
                                var betterSlots = matched.slotsJson
                                var betterName = matched.name
                                var betterTeacher = matched.teacherName

                                if (isLms) {
                                    // LMS is authoritative source of truth
                                    if (detected.section.isNotBlank() && detected.section != matched.section) {
                                        betterSection = detected.section
                                        changed = true
                                    }
                                    if (detected.slotsJson.isNotBlank() && detected.slotsJson != "[]" && detected.slotsJson != matched.slotsJson) {
                                        betterSlots = detected.slotsJson
                                        changed = true
                                    }
                                    if (detected.name.isNotBlank() && !detected.name.startsWith("University Course") && detected.name != matched.name) {
                                        betterName = detected.name
                                        changed = true
                                    }
                                    if (detected.teacherName.isNotBlank() && detected.teacherName != matched.teacherName) {
                                        betterTeacher = detected.teacherName
                                        changed = true
                                    }
                                } else {
                                    if ((matched.section.isBlank() || matched.section == "A") && detected.section.isNotBlank() && detected.section != "A") {
                                        betterSection = detected.section
                                        changed = true
                                    }
                                    if ((matched.slotsJson.isBlank() || matched.slotsJson == "[]") && detected.slotsJson.isNotBlank() && detected.slotsJson != "[]") {
                                        betterSlots = detected.slotsJson
                                        changed = true
                                    }
                                    if ((matched.name.isBlank() || matched.name.startsWith("University Course") || matched.name.contains("Course (")) && detected.name.isNotBlank() && !detected.name.startsWith("University Course")) {
                                        betterName = detected.name
                                        changed = true
                                    }
                                    if (matched.teacherName.isBlank() && detected.teacherName.isNotBlank()) {
                                        betterTeacher = detected.teacherName
                                        changed = true
                                    }
                                }

                                if (changed) {
                                    updatedCourses.add(
                                        matched.copy(
                                            section = betterSection,
                                            slotsJson = betterSlots,
                                            name = betterName,
                                            teacherName = betterTeacher
                                        )
                                    )
                                }
                            }
                        }

                        // Check schedule clashes across existing + updated + new
                        val mergedForClashCheck = existingCourses.map { existing ->
                            val updated = updatedCourses.find { PortalCourseDetector.normalizeCourseCode(it.code) == PortalCourseDetector.normalizeCourseCode(existing.code) }
                            updated ?: existing
                        } + newCourses
                        val clashes = ClassScheduleHelper.detectScheduleClashes(mergedForClashCheck)

                        // Check student profile name / program differences
                        val nameDiff = detectedProfile?.fullName?.takeIf { it.isNotBlank() && !it.equals(currentProfile?.firstName, ignoreCase = true) }
                        val progDiff = detectedProfile?.programName?.takeIf { it.isNotBlank() && !it.equals(currentProfile?.programName, ignoreCase = true) }
                        val erpDetected = detectedProfile?.erpId

                        val hasDiscrepancy = nameDiff != null || progDiff != null || newCourses.isNotEmpty() || updatedCourses.isNotEmpty() || clashes.isNotEmpty()

                        if (hasDiscrepancy) {
                            val report = PortalDiscrepancyReport(
                                detectedName = nameDiff,
                                currentName = currentProfile?.firstName ?: "",
                                detectedProgram = progDiff,
                                currentProgram = currentProfile?.programName ?: "",
                                detectedErp = erpDetected,
                                newCourses = newCourses,
                                updatedCourses = updatedCourses,
                                scheduleClashes = clashes,
                                isLmsAuthoritative = isLms,
                                sourcePortalName = currentTab.title
                            )

                            if (isManual || isLms || clashes.isNotEmpty()) {
                                pendingDiscrepancyReport = report
                            } else {
                                val toSave = newCourses + updatedCourses
                                if (toSave.isNotEmpty()) {
                                    profileRepository.saveCourses(toSave, isAuthoritativeLms = isLms)
                                    autoSyncBadgeText = "✓ Synced ${toSave.size} course(s) from ${currentTab.title}"
                                }
                                if (nameDiff != null || progDiff != null) {
                                    profileRepository.updateProfileFromPortal(nameDiff, progDiff)
                                }
                            }
                        } else {
                            if (isManual) {
                                Toast.makeText(context, "📥 Synced to Khabri! Announcements & schedule are up-to-date.", Toast.LENGTH_SHORT).show()
                            } else {
                                autoSyncBadgeText = "✓ Synced portal bulletins to memory"
                            }
                        }
                    } else {
                        autoSyncBadgeText = "✓ Synced portal notices to memory"
                    }
                } catch (e: Exception) {
                    if (isManual) {
                        Toast.makeText(context, "Failed to sync: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else if (isManual) {
            Toast.makeText(context, "Page is still loading or empty. Please wait a moment.", Toast.LENGTH_SHORT).show()
        }
    }

    val portalBridge = remember {
        PortalBridge { title, body, pageUrl ->
            scope.launch(Dispatchers.Main) {
                handlePortalExtraction(title, body, pageUrl, false)
            }
        }
    }

    val activeTargetUrl = when (currentTab) {
        PortalTab.UMS -> if (UserPreferences.isInvalidOrObsoletePortalUrl(savedPortalUrl)) UserPreferences.DEFAULT_UMS_URL else savedPortalUrl
        PortalTab.LMS -> if (UserPreferences.isInvalidOrObsoletePortalUrl(savedLmsUrl)) UserPreferences.DEFAULT_LMS_URL else savedLmsUrl
        PortalTab.CUSTOM -> if (UserPreferences.isInvalidOrObsoletePortalUrl(savedCustomUrl)) UserPreferences.DEFAULT_CUSTOM_PORTAL_URL else savedCustomUrl
    }

    BackHandler {
        if (portalError != null) {
            portalError = null
            onBack()
        } else if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onBack()
        }
    }

    LaunchedEffect(currentTab, activeTargetUrl) {
        portalError = null
        webViewInstance?.loadUrl(activeTargetUrl)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Secure",
                                    tint = IosGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentTitle.isNotBlank()) currentTitle.take(24) else currentTab.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = currentUrl.ifBlank { activeTargetUrl }.take(42),
                                style = MaterialTheme.typography.labelSmall,
                                color = IosSecondaryText,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (portalError != null) {
                                portalError = null
                                onBack()
                            } else if (webViewInstance?.canGoBack() == true) {
                                webViewInstance?.goBack()
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            editUrlInput = activeTargetUrl
                            showEditUrlDialog = true
                        }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit Portal Link", tint = KhabarBlue)
                        }
                        IconButton(onClick = {
                            portalError = null
                            val reloadTarget = if (UserPreferences.isInvalidOrObsoletePortalUrl(currentUrl)) activeTargetUrl else currentUrl.ifBlank { activeTargetUrl }
                            webViewInstance?.loadUrl(reloadTarget)
                        }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reload", tint = KhabarBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                if (loadingProgress in 0.01f..0.99f && portalError == null) {
                    LinearProgressIndicator(
                        progress = { loadingProgress },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = KhabarBlue
                    )
                }

                // Portal Tabs Switcher (IBA UMS, IBA LMS, Custom Portal)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PortalTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (currentTab != tab) {
                                    currentTab = tab
                                    portalError = null
                                }
                            },
                            label = { Text(tab.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KhabarBlue.copy(alpha = 0.15f),
                                selectedLabelColor = KhabarBlue
                            )
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (portalError == null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Extract and Save to Memory Button
                        Button(
                            onClick = {
                                val web = webViewInstance ?: return@Button
                                isExtracting = true
                                val jsExtract = """
                                    (function() {
                                        var title = document.title || "";
                                        var body = document.body ? (document.body.innerText || document.body.textContent || "") : "";
                                        return JSON.stringify({ title: title, text: body });
                                    })();
                                """.trimIndent()

                                web.evaluateJavascript(jsExtract) { jsonResult ->
                                    isExtracting = false
                                    val (extractedTitle, rawText) = PortalContentParser.parseExtractedContent(jsonResult)
                                    handlePortalExtraction(extractedTitle, rawText, currentUrl.ifBlank { activeTargetUrl }, true)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            enabled = !isExtracting
                        ) {
                            if (isExtracting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.Download, contentDescription = null, tint = KhabarBlue, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Memory", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Ask Khabri About This Page Button
                        Button(
                            onClick = {
                                val web = webViewInstance ?: return@Button
                                val jsExtract = """
                                    (function() {
                                        var title = document.title || "";
                                        var body = document.body ? (document.body.innerText || document.body.textContent || "") : "";
                                        return JSON.stringify({ title: title, text: body });
                                    })();
                                """.trimIndent()

                                web.evaluateJavascript(jsExtract) { jsonResult ->
                                    val (extractedTitle, rawText) = PortalContentParser.parseExtractedContent(jsonResult)
                                    if (rawText.isNotBlank()) {
                                        onAskKhabri(extractedTitle.ifBlank { currentTab.title }, rawText.take(5000))
                                    } else {
                                        onAskKhabri(currentTab.title, "Portal content from ${currentTab.title} ($activeTargetUrl)")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KhabarBlue)
                        ) {
                            Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ask Khabri", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        @Suppress("DEPRECATION")
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            allowContentAccess = true
                            allowFileAccess = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(false)

                            val defaultUa = userAgentString ?: ""
                            val cleanUa = if (defaultUa.contains("Version/4.0") || defaultUa.contains("; wv")) {
                                defaultUa.replace("Version/4.0 ", "").replace("; wv", "")
                            } else if (defaultUa.isNotBlank()) {
                                defaultUa
                            } else {
                                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Mobile Safari/537.36"
                            }
                            userAgentString = cleanUa
                        }

                        // Preserves Microsoft Authenticator and Azure AD SSO cookies & sessions
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        addJavascriptInterface(portalBridge, "PortalBridge")

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress / 100f
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrBlank()) {
                                    currentTitle = title
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                currentUrl = url ?: ""
                                portalError = null
                                isExtracting = false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                currentUrl = url ?: ""
                                loadingProgress = 1f
                                CookieManager.getInstance().flush()

                                val pageUrl = url ?: ""
                                val isLoginOrSso = pageUrl.contains("login", ignoreCase = true) ||
                                        pageUrl.contains("signin", ignoreCase = true) ||
                                        pageUrl.contains("oauth", ignoreCase = true) ||
                                        pageUrl.contains("saml", ignoreCase = true) ||
                                        pageUrl.contains("microsoftonline", ignoreCase = true) ||
                                        pageUrl.contains("msauth", ignoreCase = true) ||
                                        pageUrl.contains("sts/", ignoreCase = true)

                                if (!isLoginOrSso && pageUrl.isNotBlank() && pageUrl != lastAutoScrapedUrl) {
                                    lastAutoScrapedUrl = pageUrl
                                    val jsExtract = """
                                        (function() {
                                            try {
                                                var title = document.title || "";
                                                var body = document.body ? (document.body.innerText || document.body.textContent || "") : "";
                                                if (window.PortalBridge && typeof window.PortalBridge.onPageContent === 'function') {
                                                    window.PortalBridge.onPageContent(title, body, window.location.href);
                                                }
                                                return JSON.stringify({ title: title, text: body });
                                            } catch (e) {
                                                return JSON.stringify({ title: "", text: "" });
                                            }
                                        })();
                                    """.trimIndent()

                                    view?.postDelayed({
                                        view.evaluateJavascript(jsExtract) { jsonResult ->
                                            val (extractedTitle, rawText) = PortalContentParser.parseExtractedContent(jsonResult)
                                            handlePortalExtraction(extractedTitle, rawText, pageUrl, false)
                                        }
                                    }, 600)
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                val sslUrl = error?.url?.ifBlank { null } ?: view?.url ?: activeTargetUrl
                                val host = PortalSecurityHelper.extractHost(sslUrl)
                                if (PortalSecurityHelper.isTrustedSslHost(host, activeTargetUrl)) {
                                    // IBA Karachi portals and Microsoft Azure AD login may encounter
                                    // intermediate SSL certificate trust chain nuances on mobile.
                                    // Proceeding for trusted institutional & SSO domains prevents certificate exceptions.
                                    handler?.proceed()
                                } else {
                                    handler?.cancel()
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    val code = error?.errorCode ?: ERROR_UNKNOWN
                                    val desc = error?.description?.toString() ?: "Network error"
                                    val failingUrl = request.url?.toString() ?: currentUrl
                                    val isDns = code == ERROR_HOST_LOOKUP ||
                                            desc.contains("ERR_NAME_NOT_RESOLVED", ignoreCase = true) ||
                                            desc.contains("HOST_LOOKUP", ignoreCase = true)
                                    portalError = PortalErrorState(
                                        errorCode = code,
                                        description = desc,
                                        failingUrl = failingUrl,
                                        isDnsFailure = isDns
                                    )
                                }
                            }

                            @Suppress("DEPRECATION")
                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                val desc = description ?: "Network error"
                                val url = failingUrl ?: currentUrl
                                val isDns = errorCode == ERROR_HOST_LOOKUP ||
                                        desc.contains("ERR_NAME_NOT_RESOLVED", ignoreCase = true) ||
                                        desc.contains("HOST_LOOKUP", ignoreCase = true)
                                portalError = PortalErrorState(
                                    errorCode = errorCode,
                                    description = desc,
                                    failingUrl = url,
                                    isDnsFailure = isDns
                                )
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val uri = request?.url ?: return false
                                val scheme = uri.scheme?.lowercase() ?: ""
                                if (PortalSecurityHelper.shouldKeepInWebView(scheme)) {
                                    // Keep all Azure AD SSO redirects, SAML, OAuth, and inline JS inside WebView
                                    return false
                                }
                                return handleExternalUri(ctx, view, uri.toString())
                            }

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url.isNullOrBlank()) return false
                                val uri = Uri.parse(url)
                                val scheme = uri.scheme?.lowercase() ?: ""
                                if (PortalSecurityHelper.shouldKeepInWebView(scheme)) {
                                    return false
                                }
                                return handleExternalUri(ctx, view, url)
                            }
                        }

                        loadUrl(activeTargetUrl)
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Autonomous sync notification badge
            AnimatedVisibility(
                visible = autoSyncBadgeText != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .zIndex(10f)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.95f),
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = IosGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = autoSyncBadgeText ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // In-app error UI overlay on network or DNS failure (e.g. net::ERR_NAME_NOT_RESOLVED)
            portalError?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WifiOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (error.isDnsFailure) "Portal Not Found (DNS Error)" else "Failed to Load Portal",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (error.failingUrl.contains("portal.iba.edu.pk")) {
                                            "The old portal address 'portal.iba.edu.pk' is no longer active. IBA Karachi now uses UMS (ums.iba.edu.pk) and LMS (lms.iba.edu.pk)."
                                        } else if (error.isDnsFailure) {
                                            "The web address could not be resolved (net::ERR_NAME_NOT_RESOLVED). Please check your internet connection or update the portal URL."
                                        } else {
                                            "An error occurred while loading '${error.failingUrl.take(45)}': ${error.description}"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = IosSecondaryText
                                    )
                                }

                                // Failing URL container
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = error.failingUrl.ifBlank { activeTargetUrl },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        maxLines = 2,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                // Actions: Edit URL and Retry
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            editUrlInput = error.failingUrl.ifBlank { activeTargetUrl }
                                            showEditUrlDialog = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit URL")
                                    }

                                    Button(
                                        onClick = {
                                            val retryTarget = if (UserPreferences.isInvalidOrObsoletePortalUrl(error.failingUrl)) {
                                                activeTargetUrl
                                            } else {
                                                error.failingUrl.ifBlank { activeTargetUrl }
                                            }
                                            portalError = null
                                            webViewInstance?.loadUrl(retryTarget)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = KhabarBlue)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry")
                                    }
                                }

                                // Quick reset option when obsolete URL is encountered
                                if (error.failingUrl.contains("portal.iba.edu.pk") || activeTargetUrl.contains("portal.iba.edu.pk")) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                userPreferences.setPortalUrl(UserPreferences.DEFAULT_UMS_URL)
                                                currentTab = PortalTab.UMS
                                                portalError = null
                                                webViewInstance?.loadUrl(UserPreferences.DEFAULT_UMS_URL)
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = "Switch to IBA UMS (${UserPreferences.DEFAULT_UMS_URL})",
                                            color = KhabarBlue,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditUrlDialog) {
        AlertDialog(
            onDismissRequest = { showEditUrlDialog = false },
            title = {
                Text(
                    text = "Configure ${currentTab.title} Link",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Customize the web address for this portal (e.g. if your campus uses a custom ERP or LMS host):",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryText
                    )
                    OutlinedTextField(
                        value = editUrlInput,
                        onValueChange = { editUrlInput = it },
                        label = { Text("Web Address (URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rawInput = editUrlInput.trim()
                        if (rawInput.isNotBlank()) {
                            val normalized = UserPreferences.normalizePortalUrl(rawInput, currentTab.defaultUrl)
                            scope.launch {
                                when (currentTab) {
                                    PortalTab.UMS -> userPreferences.setPortalUrl(normalized)
                                    PortalTab.LMS -> userPreferences.setLmsUrl(normalized)
                                    PortalTab.CUSTOM -> userPreferences.setCustomPortalUrl(normalized)
                                }
                                portalError = null
                                webViewInstance?.loadUrl(normalized)
                                Toast.makeText(context, "Portal link saved! ✓", Toast.LENGTH_SHORT).show()
                                showEditUrlDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KhabarBlue)
                ) {
                    Text("Save Link")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val defaultUrl = currentTab.defaultUrl
                            when (currentTab) {
                                PortalTab.UMS -> userPreferences.setPortalUrl(defaultUrl)
                                PortalTab.LMS -> userPreferences.setLmsUrl(defaultUrl)
                                PortalTab.CUSTOM -> userPreferences.setCustomPortalUrl(defaultUrl)
                            }
                            portalError = null
                            webViewInstance?.loadUrl(defaultUrl)
                            showEditUrlDialog = false
                        }
                    }
                ) {
                    Text("Reset to Default", color = IosSecondaryText)
                }
            }
        )
    }

    pendingDiscrepancyReport?.let { report ->
        AlertDialog(
            onDismissRequest = { pendingDiscrepancyReport = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (report.scheduleClashes.isNotEmpty()) Icons.Rounded.Warning else Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = if (report.scheduleClashes.isNotEmpty()) MaterialTheme.colorScheme.error else KhabarBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (report.isLmsAuthoritative) "LMS Verification & Sync" else "Portal Verification",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "We analyzed this page from ${report.sourcePortalName}${if (report.isLmsAuthoritative) " (Official Authoritative Source)" else ""}. Review detected updates and sync into Khabar:",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryText
                    )

                    // 1. Student Profile Updates
                    if (report.detectedName != null || report.detectedProgram != null || report.detectedErp != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.Person, contentDescription = null, tint = KhabarBlue, modifier = Modifier.size(16.dp))
                                    Text("Student Profile Discrepancy", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = KhabarBlue)
                                }
                                report.detectedName?.let {
                                    Text("• Name: $it (Current: ${report.currentName.ifBlank { "Not set" }})", fontSize = 12.sp)
                                }
                                report.detectedProgram?.let {
                                    Text("• Program: $it (Current: ${report.currentProgram.ifBlank { "Not set" }})", fontSize = 12.sp)
                                }
                                report.detectedErp?.let {
                                    Text("• Student ID / ERP: $it", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // 2. Schedule Clashes / Overlaps Warning
                    if (report.scheduleClashes.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Text("Timetable Clashes / Overlaps (${report.scheduleClashes.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                                report.scheduleClashes.forEach { clash ->
                                    Text(
                                        "• ${clash.courseACode} and ${clash.courseBCode} clash on ${clash.day} (${clash.timeSlotA} vs ${clash.timeSlotB})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // 3. New Enrolled Courses
                    if (report.newCourses.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.School, contentDescription = null, tint = KhabarBlue, modifier = Modifier.size(16.dp))
                                    Text("New Courses Detected (${report.newCourses.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = KhabarBlue)
                                }
                                report.newCourses.forEach { c ->
                                    val nameSuffix = if (c.name.isNotBlank() && !c.name.startsWith("University Course")) " (${c.name})" else ""
                                    Text("• ${c.code}$nameSuffix - Sec: ${c.section}", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // 4. Updated Courses Details
                    if (report.updatedCourses.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = KhabarBlue, modifier = Modifier.size(16.dp))
                                    Text("Course Details to Update (${report.updatedCourses.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = KhabarBlue)
                                }
                                report.updatedCourses.forEach { c ->
                                    Text("• ${c.code}: Sec ${c.section}, ${c.name}", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val toSave = report.newCourses + report.updatedCourses
                            if (toSave.isNotEmpty()) {
                                profileRepository?.saveCourses(toSave, isAuthoritativeLms = report.isLmsAuthoritative)
                            }
                            if (report.detectedName != null || report.detectedProgram != null) {
                                profileRepository?.updateProfileFromPortal(report.detectedName, report.detectedProgram, forceUpdate = true)
                            }
                            profileRepository?.deduplicateCourses()
                            Toast.makeText(context, "✓ Profile & courses synced with ${report.sourcePortalName}!", Toast.LENGTH_SHORT).show()
                            pendingDiscrepancyReport = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KhabarBlue)
                ) {
                    Text(if (report.isLmsAuthoritative) "Sync All (LMS Authoritative)" else "Sync All")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDiscrepancyReport = null }
                ) {
                    Text("Keep Current", color = IosSecondaryText)
                }
            }
        )
    }
}

/**
 * Handles external and custom app schemes (such as Microsoft Authenticator intent:// or msauth://).
 */
private fun handleExternalUri(context: Context, view: WebView?, uriString: String): Boolean {
    return try {
        if (uriString.startsWith("intent:", ignoreCase = true)) {
            val intent = Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                try {
                    context.startActivity(intent)
                    return true
                } catch (_: Exception) {}

                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (!fallbackUrl.isNullOrBlank()) {
                    view?.loadUrl(fallbackUrl)
                    return true
                }
            }
        } else {
            val uri = Uri.parse(uriString)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return true
            } catch (_: android.content.ActivityNotFoundException) {
                if (uriString.startsWith("msauth", ignoreCase = true) || uriString.contains("authenticator", ignoreCase = true)) {
                    Toast.makeText(context, "Microsoft Authenticator not installed. Please verify with code or install the app.", Toast.LENGTH_LONG).show()
                }
                return true
            }
        }
        true
    } catch (_: Exception) {
        true
    }
}
