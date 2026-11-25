package com.khabar.app.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.khabar.app.ai.KeystoreSecretManager
import com.khabar.app.data.local.entities.ChatMessageEntity
import com.khabar.app.ui.theme.IosGreen
import com.khabar.app.ui.theme.IosRed
import com.khabar.app.ui.theme.IosSecondaryText
import com.khabar.app.ui.theme.KhabarBlue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates dynamic, campus-relevant psychological greeting pairs (Title to Subtitle)
 * based on time of day, student profile name, and language preference.
 */
fun getPsychologicalGreeting(
    name: String,
    language: String,
    hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
): Pair<String, String> {
    val studentName = name.ifBlank { "Ibraheem" }
    return when (language) {
        "ur" -> {
            when (hour) {
                in 5..11 -> "صبح بخیر، $studentName! ☕" to "آج کی صبح کیا نمٹانا ہے؟ صبح کے لیکچرز، کوئزز، یا فیس کی تفصیلات؟"
                in 12..16 -> "دن کا جائزہ، $studentName ⚡" to "آج کے سرکلرز، پورٹل نوٹسز، یا اسائنمنٹ ڈیڈلائنز کی تفصیل درکار ہے؟"
                else -> "خوش آمدید، $studentName! 🌙" to "آج رات کیا نمٹانا ہے؟ کل کا شیڈول، کورس کے اعلانات، یا اہم ای میلز؟"
            }
        }
        "roman_urdu" -> {
            when (hour) {
                in 5..11 -> "Morning hustle, $studentName! ☕" to "Aaj subah kis cheez ko tackle karna hai — morning lectures, quizzes, ya fee updates?"
                in 12..16 -> "Midday check-in, $studentName ⚡" to "Aaj ke circulars, portal notices, ya assignment deadlines ka quick summary chahiye?"
                else -> "Late night focus, $studentName 🌙" to "Kal ka schedule, course announcements, ya unread emails dekhni hain?"
            }
        }
        else -> { // Default English
            when (hour) {
                in 5..11 -> "Morning hustle, $studentName! ☕" to "What are we tackling today? Morning lectures, upcoming quizzes, or fee updates?"
                in 12..16 -> "Midday check-in, $studentName ⚡" to "Let's clear the clutter. Need a breakdown of today's circulars, portal notices, or assignment deadlines?"
                else -> "Late night focus, $studentName 🌙" to "What are we conquering tonight? Tomorrow's schedule, course announcements, or mail summaries?"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhabriChatScreen(
    viewModel: KhabriChatViewModel,
    bottomPadding: Dp = 92.dp,
    onNavigateToAiDefectVault: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isListening by viewModel.voiceAssistantManager.isListening.collectAsState()
    val isSpeaking by viewModel.voiceAssistantManager.isSpeaking.collectAsState()
    val autoVoice by viewModel.autoVoiceEnabled.collectAsState()
    val activeProvider by viewModel.activeProvider.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val lastFailedCanRetry by viewModel.lastFailedCanRetry.collectAsState()
    val showKeyDialog by viewModel.showKeyDialog.collectAsState()
    val keyDialogProvider by viewModel.keyDialogProvider.collectAsState()
    val showHistoryDrawer by viewModel.showHistoryDrawer.collectAsState()
    val chatLanguage by viewModel.chatLanguage.collectAsState()

    val attachedBitmap by viewModel.attachedBitmap.collectAsState()
    val attachedOcrText by viewModel.attachedOcrText.collectAsState()
    val attachedIsPdf by viewModel.attachedIsPdf.collectAsState()
    val isAttachingDocument by viewModel.isAttachingDocument.collectAsState()

    val profileState by remember(context) {
        com.khabar.app.data.local.KhabarDatabase.getInstance(context).profileDao().getProfileFlow()
    }.collectAsState(initial = null)
    val studentName = profileState?.firstName?.ifBlank { "Ibraheem" } ?: "Ibraheem"
    val (greetingTitle, greetingSubtitle) = remember(studentName, chatLanguage) {
        getPsychologicalGreeting(studentName, chatLanguage)
    }

    var showModelMenu by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Activity result launcher for audio recording permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceInput()
        }
    }

    var showAttachmentChooser by remember { mutableStateOf(false) }

    // Photo picker launcher for attaching campus documents, notices, fee slips, timetables
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachDocumentOrImage(context, uri)
        }
    }

    // Document/PDF picker launcher for attaching campus circulars & fee vouchers
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.attachDocumentOrImage(context, uri)
        }
    }

    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Key Setup Modal Dialog
    if (showKeyDialog) {
        AiApiKeySetupDialog(
            initialProvider = keyDialogProvider,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissKeyDialog() }
        )
    }

    // Chat History Bottom Sheet
    if (showHistoryDrawer) {
        ChatHistoryBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissHistoryDrawer() }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // TOP APP BAR (Clean, spacious, no wrapping)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Assistant Avatar + Title + Model Selector Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { showModelMenu = true }
                        .weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(KhabarBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Khabri AI",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Model selection subtitle with dropdown arrow
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Text(
                                text = if (activeProvider == KeystoreSecretManager.PROVIDER_AUTO) "Auto (Smart Fallback)" else "$activeProvider • $activeModel",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                color = KhabarBlue,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "▾",
                                color = KhabarBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Model switcher dropdown anchored to the title
                    DropdownMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✨ Auto (Smart Fallback)",
                                            fontWeight = if (activeProvider == KeystoreSecretManager.PROVIDER_AUTO) FontWeight.Bold else FontWeight.SemiBold,
                                            color = KhabarBlue
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "⚡ Best",
                                            color = IosGreen,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Text(
                                        text = "Prioritizes fast free models & auto-switches on limits",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = IosSecondaryText
                                    )
                                }
                            },
                            onClick = {
                                showModelMenu = false
                                viewModel.switchAiModel(KeystoreSecretManager.PROVIDER_AUTO)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        val providers = listOf(
                            Triple(KeystoreSecretManager.PROVIDER_GEMINI, "Gemini (Free & Vision)", KeystoreSecretManager.getDefaultModel(KeystoreSecretManager.PROVIDER_GEMINI)),
                            Triple(KeystoreSecretManager.PROVIDER_GROQ, "Groq (Ultra-Fast)", KeystoreSecretManager.getDefaultModel(KeystoreSecretManager.PROVIDER_GROQ)),
                            Triple(KeystoreSecretManager.PROVIDER_OPENROUTER, "OpenRouter (Multi-Model)", KeystoreSecretManager.getDefaultModel(KeystoreSecretManager.PROVIDER_OPENROUTER)),
                            Triple(KeystoreSecretManager.PROVIDER_CLAUDE, "Claude (Anthropic)", KeystoreSecretManager.getDefaultModel(KeystoreSecretManager.PROVIDER_CLAUDE)),
                            Triple(KeystoreSecretManager.PROVIDER_OLLAMA, "Ollama (Local / PC)", KeystoreSecretManager.getDefaultModel(KeystoreSecretManager.PROVIDER_OLLAMA))
                        )

                        providers.forEach { (provider, label, model) ->
                            val hasKey = viewModel.hasKeyForProvider(provider)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = if (activeProvider == provider) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = if (hasKey) "✓ Ready" else "⚙️ Set Key",
                                            color = if (hasKey) IosGreen else IosRed,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                },
                                onClick = {
                                    showModelMenu = false
                                    viewModel.switchAiModel(
                                        provider = provider,
                                        baseUrl = KeystoreSecretManager.getDefaultBaseUrl(provider),
                                        model = model
                                    )
                                }
                            )
                        }
                    }
                }

                // Right: Clean 4 Action Items (Language, History, New Chat, Overflow Menu)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 1. Language Toggle Chip
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.clickable { showLanguageMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Language,
                                    contentDescription = "Language",
                                    tint = KhabarBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = when (chatLanguage) {
                                        "ur" -> "اردو"
                                        "roman_urdu" -> "Roman"
                                        else -> "EN"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🇬🇧 English (Pure English)") },
                                onClick = {
                                    viewModel.setChatLanguage("en")
                                    showLanguageMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🇵🇰 اردو (Urdu Nastaliq Script)") },
                                onClick = {
                                    viewModel.setChatLanguage("ur")
                                    showLanguageMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("💬 Roman Urdu (Campus Style)") },
                                onClick = {
                                    viewModel.setChatLanguage("roman_urdu")
                                    showLanguageMenu = false
                                }
                            )
                        }
                    }

                    // 2. Chat History Button
                    IconButton(
                        onClick = { viewModel.toggleHistoryDrawer() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "Chat History",
                            tint = KhabarBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 3. New Chat Button (+)
                    IconButton(
                        onClick = { viewModel.startNewChat() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "New Chat",
                            tint = KhabarBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 4. Overflow Menu (⋮) for Key Setup, Voice, and Clear
                    var showOverflowMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(
                                text = "⋮",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Key,
                                            contentDescription = null,
                                            tint = if (viewModel.hasKeyForProvider(activeProvider)) IosGreen else IosRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Set API Keys")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.openKeyDialog(activeProvider)
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (autoVoice) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeMute,
                                            contentDescription = null,
                                            tint = if (autoVoice) IosGreen else IosSecondaryText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(if (autoVoice) "Voice Reply: ON" else "Voice Reply: OFF")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleAutoVoice()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.BugReport,
                                            contentDescription = null,
                                            tint = KhabarBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("AI Bug & Quality Vault")
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToAiDefectVault()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = IosRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Clear Chat History", color = IosRed)
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.clearChat()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Error / Status banner with Retry recovery action
        statusMessage?.let { msg ->
            val isInfoMsg = msg.contains("Vault", ignoreCase = true) || msg.startsWith("✓")
            val bannerBg = if (isInfoMsg) IosGreen.copy(alpha = 0.12f) else IosRed.copy(alpha = 0.12f)
            val bannerTint = if (isInfoMsg) IosGreen else IosRed

            Surface(
                color = bannerBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = msg,
                        color = bannerTint,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (lastFailedCanRetry && !isGenerating) {
                        TextButton(
                            onClick = { viewModel.retryLastMessage() },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Retry",
                                color = KhabarBlue,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.clearStatusMessage() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = bannerTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Warning banner if active provider has no API key
        if (activeProvider != KeystoreSecretManager.PROVIDER_AUTO &&
            !viewModel.hasKeyForProvider(activeProvider) &&
            !activeProvider.equals(KeystoreSecretManager.PROVIDER_OLLAMA, ignoreCase = true)
        ) {
            Surface(
                color = Color(0xFFFFF3CD),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openKeyDialog(activeProvider) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "⚠️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No key configured for $activeProvider. Tap to set up your key!",
                            color = Color(0xFF856404),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Text(
                        text = "Set Key",
                        color = KhabarBlue,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // CHAT MESSAGE LIST
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                // Empty State / Dynamic Psychological Welcome Greeting
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(KhabarBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = null,
                            tint = KhabarBlue,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = greetingTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = greetingSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IosSecondaryText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3 Interactive Quick-Query Action Cards
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.sendMessage("Summarize all campus emails and notices received today with key actions.")
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Summarize Today's Updates",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Campus emails, notices & urgent actions",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IosSecondaryText
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.sendMessage("What are my upcoming assignment deadlines, quizzes, or fee due dates?")
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📅",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Upcoming Quizzes & Deadlines",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Due dates, upcoming quizzes & fee vouchers",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IosSecondaryText
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.sendMessage("Show me the latest announcements and room schedules for my enrolled courses.")
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📚",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Course Notices & Schedule",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Faculty announcements & room numbers",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = IosSecondaryText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { viewModel.openKeyDialog(activeProvider) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = KhabarBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Engine: $activeProvider (${if (viewModel.hasKeyForProvider(activeProvider)) "Ready ✓" else "Tap to Set Key ⚙️"})",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = KhabarBlue
                        )
                    }
                }
            } else {
                val lastAssistantId = remember(messages) {
                    messages.lastOrNull { it.role == "assistant" }?.id
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(
                            message = msg,
                            isSpeaking = isSpeaking,
                            isLastAssistant = msg.id == lastAssistantId && !isGenerating,
                            onSpeakClick = {
                                viewModel.voiceAssistantManager.toggleSpeak(msg.content, chatLanguage)
                            },
                            onRegenerateClick = {
                                viewModel.regenerateLastResponse()
                            },
                            onReportMistake = { reason ->
                                viewModel.reportMessageMistake(msg, reason)
                            }
                        )
                    }

                    if (isGenerating) {
                        item {
                            GeneratingBubble()
                        }
                    }
                }
            }
        }

        // QUICK SUGGESTION CHIPS (shown as quick follow-ups once chat starts; empty state already displays 3 large cards)
        if (messages.isNotEmpty() && !isGenerating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    text = "📢 Today's updates",
                    onClick = { viewModel.sendMessage("Summarize today's key campus updates and action items.") }
                )
                SuggestionChip(
                    text = "⏰ Deadlines",
                    onClick = { viewModel.sendMessage("What are my upcoming assignment, quiz, or fee deadlines?") }
                )
                SuggestionChip(
                    text = "🎓 Campus notices",
                    onClick = { viewModel.sendMessage("Show official university administration and department notices only.") }
                )
                SuggestionChip(
                    text = "💰 Fee & Aid",
                    onClick = { viewModel.sendMessage("Are there any fee due dates, financial aid, or scholarship notices?") }
                )
            }
        }

        // ATTACHING DOCUMENT LOADING INDICATOR
        if (isAttachingDocument) {
            Surface(
                color = KhabarBlue.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = KhabarBlue,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Processing attachment...",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = KhabarBlue
                    )
                }
            }
        }

        // ATTACHED IMAGE / PDF PREVIEW (IF ATTACHED)
        if (attachedBitmap != null || attachedOcrText != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, KhabarBlue.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (attachedBitmap != null && !attachedIsPdf) {
                        Image(
                            bitmap = attachedBitmap!!.asImageBitmap(),
                            contentDescription = "Attached thumbnail",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KhabarBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = "Document",
                                tint = if (attachedIsPdf) Color(0xFFE53935) else KhabarBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (attachedIsPdf) "PDF Document Attached" else if (attachedBitmap != null) "Screenshot / Image Attached" else "Portal Context Attached",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (attachedOcrText != null) "✓ Ready (${attachedOcrText?.length ?: 0} chars extracted)" else "✓ Ready for visual AI analysis",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = IosGreen
                        )
                    }
                    IconButton(
                        onClick = { viewModel.removeAttachedImage() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ACTIVE SPEECH PLAYER BANNER (1-tap stop from anywhere!)
        AnimatedVisibility(visible = isSpeaking) {
            Surface(
                color = IosRed.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, IosRed.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = IosRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Khabri is speaking...",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = IosRed
                        )
                    }
                    TextButton(
                        onClick = { viewModel.stopSpeaking() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Stop, contentDescription = null, tint = IosRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop Reading", color = IosRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // BOTTOM INPUT BAR (Responsive above FloatingTabBar and soft keyboard)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (bottomPadding > 0.dp) Modifier.navigationBarsPadding() else Modifier)
                .padding(bottom = bottomPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo/Document Picker Button
                IconButton(
                    onClick = {
                        showAttachmentChooser = true
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = "Attach Document / Screenshot",
                        tint = if (attachedBitmap != null) KhabarBlue else IosSecondaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Mic Button with Animated Pulse when listening
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isListening) IosRed else MaterialTheme.colorScheme.surfaceVariant)
                        .scale(if (isListening) pulseScale else 1.0f)
                        .clickable {
                            if (isListening) {
                                viewModel.stopVoiceInput()
                            } else {
                                val hasRecordPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasRecordPermission) {
                                    viewModel.startVoiceInput()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                        contentDescription = "Voice",
                        tint = if (isListening) Color.White else KhabarBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text Input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.onInputTextChanged(it) },
                    placeholder = {
                        Text(
                            text = if (isListening) "Listening to voice..." else if (attachedBitmap != null) "Ask about this document..." else "Ask Khabri anything...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IosSecondaryText
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KhabarBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send / Stop Generation Button
                val canSend = (inputText.isNotBlank() || attachedBitmap != null || attachedOcrText != null) && !isGenerating
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isGenerating -> IosRed
                                canSend -> KhabarBlue
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                        .clickable(enabled = isGenerating || canSend) {
                            if (isGenerating) {
                                viewModel.stopGeneration()
                            } else {
                                viewModel.sendMessage()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isGenerating) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = "Stop generating",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAttachmentChooser) {
        AlertDialog(
            onDismissRequest = { showAttachmentChooser = false },
            title = { Text("Attach to Khabri AI", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Attach an image or PDF circular/timetable for Khabri to analyze on-device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentChooser = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, tint = KhabarBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("🖼️ Image / Screenshot", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAttachmentChooser = false
                                docPickerLauncher.launch("application/pdf")
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = Color(0xFFE53935))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("📄 PDF Circular / Fee Voucher", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showAttachmentChooser = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Modern Chat Bubble with Text Selection, Copy Action, and Document Thumbnail.
 */
@Composable
private fun ChatBubble(
    message: ChatMessageEntity,
    isSpeaking: Boolean = false,
    isLastAssistant: Boolean = false,
    onSpeakClick: () -> Unit,
    onRegenerateClick: () -> Unit = {},
    onReportMistake: (String) -> Unit = {}
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showReportDialog by remember { mutableStateOf(false) }
    var showFullscreenImage by remember { mutableStateOf(false) }
    var customCritique by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<String?>(null) }

    if (showFullscreenImage && message.imageUri != null) {
        Dialog(
            onDismissRequest = { showFullscreenImage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { showFullscreenImage = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = message.imageUri,
                    contentDescription = "Fullscreen Image Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showFullscreenImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close preview",
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (showReportDialog) {
        val presets = listOf(
            "Intent mismatch (Did not answer what I asked)",
            "Incorrect date or deadline mentioned",
            "Wrong fee / voucher information",
            "Factual error / Hallucination"
        )
        AlertDialog(
            onDismissRequest = {
                showReportDialog = false
                customCritique = ""
                selectedPreset = null
            },
            title = {
                Text(
                    text = "Report AI Mistake / Ghalti",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Tell Khabri what went wrong so it logs this mistake in your AI Vault and improves:",
                        style = MaterialTheme.typography.bodySmall,
                        color = IosSecondaryText
                    )
                    presets.forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) KhabarBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.dp, KhabarBlue) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPreset = if (isSelected) null else preset
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isSelected) "●" else "○",
                                    color = if (isSelected) KhabarBlue else IosSecondaryText,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = customCritique,
                        onValueChange = { customCritique = it },
                        placeholder = { Text("Or type details in your own words...", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = customCritique.trim().ifBlank { selectedPreset ?: "Student reported mistake" }
                        onReportMistake(reason)
                        showReportDialog = false
                        customCritique = ""
                        selectedPreset = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KhabarBlue)
                ) {
                    Text("Submit to Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReportDialog = false
                    customCritique = ""
                    selectedPreset = null
                }) {
                    Text("Cancel", color = IosSecondaryText)
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(KhabarBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SmartToy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) KhabarBlue else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // If user attached an image, render tappable preview
                if (message.imageUri != null) {
                    AsyncImage(
                        model = message.imageUri,
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showFullscreenImage = true },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Formatted rich markdown typography for both user and assistant
                FormattedChatMessage(
                    content = message.content,
                    isUser = isUser,
                    modifier = Modifier.fillMaxWidth()
                )

                // Action Row (Copy and Speak)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy button
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.content))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy message",
                            tint = if (isUser) Color.White.copy(alpha = 0.75f) else IosSecondaryText,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (!isUser) {
                        if (isLastAssistant) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onRegenerateClick,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Regenerate response",
                                    tint = IosSecondaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onSpeakClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                contentDescription = if (isSpeaking) "Stop Speaking" else "Listen",
                                tint = if (isSpeaking) IosRed else IosSecondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BugReport,
                                contentDescription = "Report Mistake / Ghalti",
                                tint = IosSecondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated Loading Indicator for AI Reply Generation.
 */
@Composable
private fun GeneratingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(KhabarBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.SmartToy,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = KhabarBlue,
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Khabri is reviewing campus updates...",
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondaryText
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = KhabarBlue,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Chat History Bottom Sheet to switch sessions or start fresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryBottomSheet(
    viewModel: KhabriChatViewModel,
    onDismiss: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    var renamingSession by remember { mutableStateOf<com.khabar.app.data.local.dao.ChatSessionInfo?>(null) }
    var renameText by remember { mutableStateOf("") }

    if (renamingSession != null) {
        val target = renamingSession!!
        AlertDialog(
            onDismissRequest = { renamingSession = null },
            title = { Text("Rename Chat", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Chat Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameSession(target.sessionId, renameText)
                        }
                        renamingSession = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KhabarBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingSession = null }) {
                    Text("Cancel", color = IosSecondaryText)
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chat History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        viewModel.startNewChat()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KhabarBlue),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Chat", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Show clean Active Conversation banner only when the current canvas is a fresh unsaved chat
            val currentSavedSession = sessions.find { it.sessionId == currentSessionId }
            if (currentSavedSession == null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KhabarBlue.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, KhabarBlue.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(IosGreen)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "New Conversation",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = KhabarBlue
                                )
                                Text(
                                    text = if (messages.isEmpty()) "Ready for your question" else "${messages.size} messages",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = IosSecondaryText
                                )
                            }
                        }
                        Text(
                            text = "Active",
                            color = IosGreen,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            if (sessions.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No saved conversations yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your previous chats will appear here so you can reopen, rename, or delete them anytime.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = IosSecondaryText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.sessionId }) { session ->
                        val isSelected = session.sessionId == currentSessionId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) KhabarBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.5.dp, KhabarBlue) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.switchSession(session.sessionId)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = if (isSelected) KhabarBlue else IosSecondaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = session.sessionTitle.ifBlank { "Campus Chat" },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) KhabarBlue else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = (if (isSelected) "Active • " else "") + formatTimestamp(session.lastTimestamp),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (isSelected) IosGreen else IosSecondaryText
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            renameText = session.sessionTitle.ifBlank { "Campus Chat" }
                                            renamingSession = session
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Rename chat",
                                            tint = KhabarBlue.copy(alpha = 0.85f),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteSession(session.sessionId) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete chat",
                                            tint = IosRed.copy(alpha = 0.75f),
                                            modifier = Modifier.size(18.dp)
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
}

/**
 * Modern In-App Dialog for configuring AI API Keys with dynamic provider loading,
 * direct browser link to obtain free keys, and live test connection before saving.
 */
@Composable
fun AiApiKeySetupDialog(
    initialProvider: String,
    viewModel: KhabriChatViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val providers = listOf(
        KeystoreSecretManager.PROVIDER_GEMINI,
        KeystoreSecretManager.PROVIDER_GROQ,
        KeystoreSecretManager.PROVIDER_OPENROUTER,
        KeystoreSecretManager.PROVIDER_CLAUDE,
        KeystoreSecretManager.PROVIDER_OLLAMA
    )

    val validInitial = if (initialProvider == KeystoreSecretManager.PROVIDER_AUTO) KeystoreSecretManager.PROVIDER_GEMINI else initialProvider
    var selectedProvider by remember { mutableStateOf(validInitial) }

    // Dynamic inputs that automatically load the saved values for selected provider
    var keyInput by remember(selectedProvider) {
        mutableStateOf(viewModel.getKeyForProvider(selectedProvider))
    }
    var baseUrlInput by remember(selectedProvider) {
        mutableStateOf(viewModel.getBaseUrlForProvider(selectedProvider))
    }
    var modelInput by remember(selectedProvider) {
        mutableStateOf(viewModel.getModelForProvider(selectedProvider))
    }

    var showPassword by remember { mutableStateOf(false) }

    // Live test connection state
    var isTesting by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var testIsSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Configure AI Provider",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Provider Selector Chips
                Text(
                    text = "Select AI Engine to Configure:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    providers.forEach { p ->
                        val isSelected = selectedProvider.equals(p, ignoreCase = true)
                        val hasKey = viewModel.hasKeyForProvider(p)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) KhabarBlue else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                selectedProvider = p
                                testResultText = null
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = p,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                )
                                if (hasKey) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "✓",
                                        color = if (isSelected) Color.White else IosGreen,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                // Short, clear guidance & direct Get Key link per provider
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        when (selectedProvider) {
                            KeystoreSecretManager.PROVIDER_GEMINI -> {
                                Text(
                                    text = "🌟 Google Gemini (Recommended & 100% Free)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "1. Visit aistudio.google.com\n2. Click 'Get API key' → 'Create key'\n3. 1M tokens/min free tier. No credit card required!",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = IosSecondaryText
                                )
                            }
                            KeystoreSecretManager.PROVIDER_GROQ -> {
                                Text(
                                    text = "⚡ Groq (Ultra-Fast & Free)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "1. Visit console.groq.com/keys\n2. Create API key starting with 'gsk_...'\n3. 500 tokens/second lightning speed!",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = IosSecondaryText
                                )
                            }
                            KeystoreSecretManager.PROVIDER_OPENROUTER -> {
                                Text(
                                    text = "🌐 OpenRouter (Multi-Model Hub)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "1. Visit openrouter.ai/keys\n2. Create key starting with 'sk-or-v1-...'\n3. Access Llama 3.3, Mistral, Gemma & 100+ models.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = IosSecondaryText
                                )
                            }
                            KeystoreSecretManager.PROVIDER_CLAUDE -> {
                                Text(
                                    text = "🧠 Claude 3.5 Sonnet (Anthropic)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "1. Visit console.anthropic.com\n2. Generate key starting with 'sk-ant-...'\n3. Unrivaled reasoning for essays and coding.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = IosSecondaryText
                                )
                            }
                            KeystoreSecretManager.PROVIDER_OLLAMA -> {
                                Text(
                                    text = "💻 Ollama (Local AI on PC)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Runs on your laptop/PC (ollama run llama3.2). On your physical phone, enter your PC's Wi-Fi IP address below (e.g. http://192.168.1.50:11434/v1).",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = IosSecondaryText
                                )
                            }
                        }

                        // Direct Browser Link to Get Key
                        val websiteUrl = KeystoreSecretManager.getProviderWebsiteUrl(selectedProvider)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Get Free $selectedProvider Key ↗",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                if (keyInput.isBlank() && selectedProvider != KeystoreSecretManager.PROVIDER_OLLAMA) {
                    Surface(
                        color = Color(0xFFFFF3CD),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ No API key set for $selectedProvider. Tap the link above to get your key in Chrome, then paste it below:",
                            color = Color(0xFF856404),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // API Key field (for non-Ollama)
                if (selectedProvider != KeystoreSecretManager.PROVIDER_OLLAMA) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = {
                            keyInput = it.trim()
                            testResultText = null
                        },
                        label = { Text("$selectedProvider API Key") },
                        placeholder = { Text("Paste your API key here...") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Base URL field (editable for Ollama / Custom)
                if (selectedProvider == KeystoreSecretManager.PROVIDER_OLLAMA) {
                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = {
                            baseUrlInput = it.trim()
                            testResultText = null
                        },
                        label = { Text("PC / Host Endpoint URL") },
                        placeholder = { Text("http://192.168.1.X:11434/v1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Test Connection Button & Live Feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isTesting = true
                                testResultText = null
                                val result = viewModel.testProviderConfig(
                                    provider = selectedProvider,
                                    apiKey = keyInput,
                                    baseUrl = baseUrlInput.ifBlank { KeystoreSecretManager.getDefaultBaseUrl(selectedProvider) },
                                    model = modelInput.ifBlank { KeystoreSecretManager.getDefaultModel(selectedProvider) }
                                )
                                isTesting = false
                                result.onSuccess { reply ->
                                    testIsSuccess = true
                                    testResultText = "✓ Connection Verified! Ready to use."
                                }.onFailure { err ->
                                    testIsSuccess = false
                                    testResultText = "✗ Failed: ${err.message ?: "Invalid key or endpoint"}"
                                }
                            }
                        },
                        enabled = !isTesting && (keyInput.isNotBlank() || selectedProvider == KeystoreSecretManager.PROVIDER_OLLAMA),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Connection...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (testIsSuccess) IosGreen else KhabarBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⚡ Test Key & Connection")
                        }
                    }
                }

                // Inline Test Status Card
                testResultText?.let { feedback ->
                    Surface(
                        color = if (testIsSuccess) IosGreen.copy(alpha = 0.12f) else IosRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedback,
                            color = if (testIsSuccess) IosGreen else IosRed,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Security Assurance Note
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Secured",
                        tint = IosGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🔒 Hardware Keystore Protected (AES-256-GCM Encryption)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = IosSecondaryText
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveProviderConfig(
                        provider = selectedProvider,
                        apiKey = keyInput,
                        customBaseUrl = baseUrlInput.ifBlank { null },
                        customModel = modelInput.ifBlank { null }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = KhabarBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save & Connect")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }
}
