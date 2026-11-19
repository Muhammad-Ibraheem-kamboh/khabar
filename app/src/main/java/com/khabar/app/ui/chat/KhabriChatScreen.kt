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
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhabriChatScreen(
    viewModel: KhabriChatViewModel,
    bottomPadding: Dp = 92.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isListening by viewModel.voiceAssistantManager.isListening.collectAsState()
    val autoVoice by viewModel.autoVoiceEnabled.collectAsState()
    val activeProvider by viewModel.activeProvider.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val showKeyDialog by viewModel.showKeyDialog.collectAsState()
    val keyDialogProvider by viewModel.keyDialogProvider.collectAsState()
    val showHistoryDrawer by viewModel.showHistoryDrawer.collectAsState()
    val chatLanguage by viewModel.chatLanguage.collectAsState()

    val attachedBitmap by viewModel.attachedBitmap.collectAsState()
    val attachedOcrText by viewModel.attachedOcrText.collectAsState()

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

    // Photo picker launcher for attaching campus documents, notices, fee slips, timetables
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachImage(context, uri)
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
                            Triple(KeystoreSecretManager.PROVIDER_GEMINI, "Gemini (Free & Fast)", "gemini-2.5-flash"),
                            Triple(KeystoreSecretManager.PROVIDER_GROQ, "Groq (Lightning Fast)", "llama-3.3-70b-versatile"),
                            Triple(KeystoreSecretManager.PROVIDER_OPENROUTER, "OpenRouter (Multi-Model)", "meta-llama/llama-3.3-70b-instruct"),
                            Triple(KeystoreSecretManager.PROVIDER_CLAUDE, "Claude (Anthropic)", "claude-3-5-sonnet-20241022"),
                            Triple(KeystoreSecretManager.PROVIDER_OLLAMA, "Ollama (Local / PC)", "llama3.2")
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

        // Error / Status banner if any
        statusMessage?.let { err ->
            Surface(
                color = IosRed.copy(alpha = 0.12f),
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
                        text = err,
                        color = IosRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearStatusMessage() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = IosRed,
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
                // Empty State / Welcome Card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(KhabarBlue.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = null,
                            tint = KhabarBlue,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Hi! I am Khabri (خبری)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Your 24/7 personal campus AI assistant. Ask me about your emails, fee deadlines, quiz results, or course announcements. You can also attach screenshots and documents!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IosSecondaryText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                            onSpeakClick = {
                                viewModel.voiceAssistantManager.speak(msg.content, chatLanguage)
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

        // QUICK SUGGESTION CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(
                text = "📢 Today's updates",
                onClick = { viewModel.sendMessage("Hi Khabri, what are today's email updates?") }
            )
            SuggestionChip(
                text = "📧 Summarize today's mails",
                onClick = { viewModel.sendMessage("Please summarize all mails received today. Give a clear bullet point breakdown.") }
            )
            SuggestionChip(
                text = "⏰ Any deadlines soon?",
                onClick = { viewModel.sendMessage("Do I have any upcoming assignment or fee deadlines?") }
            )
            SuggestionChip(
                text = "🎓 Campus notices only",
                onClick = { viewModel.sendMessage("Show me official university administration and department notices only.") }
            )
            SuggestionChip(
                text = "💰 Fee & Scholarship",
                onClick = { viewModel.sendMessage("Are there any fee due dates, financial aid, or scholarship notices?") }
            )
        }

        // ATTACHED IMAGE PREVIEW (IF ATTACHED)
        attachedBitmap?.let { bmp ->
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
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Attached thumbnail",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Campus Document / Screenshot Attached",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (attachedOcrText != null) "✓ Text extracted (${attachedOcrText?.length ?: 0} characters)" else "Scanning text with on-device OCR...",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (attachedOcrText != null) IosGreen else IosSecondaryText
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
                // Photo Picker Button
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
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

                // Send Button
                val canSend = (inputText.isNotBlank() || attachedBitmap != null) && !isGenerating
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (canSend) KhabarBlue else MaterialTheme.colorScheme.outlineVariant)
                        .clickable(enabled = canSend) {
                            viewModel.sendMessage()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
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
}

/**
 * Modern Chat Bubble with Text Selection, Copy Action, and Document Thumbnail.
 */
@Composable
private fun ChatBubble(
    message: ChatMessageEntity,
    onSpeakClick: () -> Unit
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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
                // If user attached an image, render preview
                if (message.imageUri != null) {
                    AsyncImage(
                        model = message.imageUri,
                        contentDescription = "Attached Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Text wrapped in SelectionContainer so user can highlight and copy!
                SelectionContainer {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

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
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onSpeakClick,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = "Listen",
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
    val sheetState = rememberModalBottomSheetState()

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

            Spacer(modifier = Modifier.height(16.dp))

            // Current Active Session Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = KhabarBlue.copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, KhabarBlue),
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(IosGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Current Chat Session",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = KhabarBlue
                            )
                            Text(
                                text = "Active now • Session ID: ${currentSessionId.take(12)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = IosSecondaryText
                            )
                        }
                    }
                    Text(
                        text = "Active ●",
                        color = IosGreen,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
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
                            text = "No previous archived chats yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "When you talk with Khabri or start new chats using the (+) button, they will appear listed here.",
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
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                            text = formatTimestamp(session.lastTimestamp),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = IosSecondaryText
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteSession(session.sessionId) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = IosRed.copy(alpha = 0.7f),
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
