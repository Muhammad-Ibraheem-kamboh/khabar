package com.khabar.app.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.data.local.entities.AiDefectLogEntity
import com.khabar.app.ui.components.Pill
import com.khabar.app.ui.theme.IosGreen
import com.khabar.app.ui.theme.IosRed
import com.khabar.app.ui.theme.IosSecondaryText
import com.khabar.app.ui.theme.KhabarBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDefectVaultScreen(
    viewModel: AiDefectVaultViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allLogs by viewModel.logsFlow.collectAsState()
    val filter by viewModel.selectedFilter.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    val filteredLogs = when (filter) {
        "ALIGNMENT" -> allLogs.filter { it.type == "ALIGNMENT_CORRECTION" }
        "BUG" -> allLogs.filter { it.type == "TECHNICAL_BUG" }
        else -> allLogs
    }

    val alignmentCount = allLogs.count { it.type == "ALIGNMENT_CORRECTION" }
    val bugCount = allLogs.count { it.type == "TECHNICAL_BUG" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Bug & Quality Vault",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Self-Reflection & Feedback Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = IosSecondaryText
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.copyMarkdownToClipboard(context) },
                        enabled = allLogs.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy Markdown",
                            tint = if (allLogs.isNotEmpty()) KhabarBlue else IosSecondaryText
                        )
                    }
                    IconButton(
                        onClick = { viewModel.shareMarkdown(context) },
                        enabled = allLogs.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = if (allLogs.isNotEmpty()) KhabarBlue else IosSecondaryText
                        )
                    }
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        enabled = allLogs.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Clear All",
                            tint = if (allLogs.isNotEmpty()) IosRed else IosSecondaryText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Stats Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${allLogs.size}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Total Logged",
                            style = MaterialTheme.typography.labelSmall,
                            color = IosSecondaryText
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$alignmentCount",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF9C27B0)
                        )
                        Text(
                            text = "User Corrections",
                            style = MaterialTheme.typography.labelSmall,
                            color = IosSecondaryText
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$bugCount",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = IosRed
                        )
                        Text(
                            text = "Runtime Bugs",
                            style = MaterialTheme.typography.labelSmall,
                            color = IosSecondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val promptPatch by viewModel.promptPatch.collectAsState()
            var showPatchModal by remember { mutableStateOf(false) }

            if (alignmentCount > 0) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, KhabarBlue.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPatchModal = !showPatchModal }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = KhabarBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Weekly Prompt Upgrade Patch",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = KhabarBlue
                                )
                            }
                            Pill(text = "Active Memory ✓", containerColor = IosGreen.copy(alpha = 0.15f), contentColor = IosGreen)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Khabri has synthesized your corrections into prompt rules. Tap to view or copy ready-to-paste instructions for your weekend update.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AnimatedVisibility(visible = showPatchModal) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = promptPatch.ifBlank { "Compiling active patches..." },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        ),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.copyPromptPatch(context) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Prompt Patch")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == "ALL",
                    onClick = { viewModel.setFilter("ALL") },
                    label = { Text("All (${allLogs.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KhabarBlue.copy(alpha = 0.15f),
                        selectedLabelColor = KhabarBlue
                    )
                )
                FilterChip(
                    selected = filter == "ALIGNMENT",
                    onClick = { viewModel.setFilter("ALIGNMENT") },
                    label = { Text("Corrections ($alignmentCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF9C27B0).copy(alpha = 0.15f),
                        selectedLabelColor = Color(0xFF9C27B0)
                    )
                )
                FilterChip(
                    selected = filter == "BUG",
                    onClick = { viewModel.setFilter("BUG") },
                    label = { Text("Bugs ($bugCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IosRed.copy(alpha = 0.15f),
                        selectedLabelColor = IosRed
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = IosGreen,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Defects Logged",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "When you correct Khabri or an API errors out,\nit will automatically analyze and log here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = IosSecondaryText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        DefectLogCard(
                            log = log,
                            onDelete = { viewModel.deleteLog(log.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Defect Logs?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all recorded user corrections and technical bug records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearAllLogs()
                    }
                ) {
                    Text("Clear All", color = IosRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DefectLogCard(
    log: AiDefectLogEntity,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isAlignment = log.type == "ALIGNMENT_CORRECTION"
    val accentColor = if (isAlignment) Color(0xFF9C27B0) else IosRed
    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.ENGLISH)
    val dateStr = dateFormat.format(Date(log.timestamp))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAlignment) Icons.Rounded.Psychology else Icons.Rounded.BugReport,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Pill(
                        text = if (isAlignment) "🧠 User Correction" else "🐛 Technical Bug",
                        containerColor = accentColor.copy(alpha = 0.12f),
                        contentColor = accentColor
                    )
                }
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = IosSecondaryText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = log.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!log.provider.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Provider: ${log.provider} (${log.model ?: "default"})",
                    style = MaterialTheme.typography.labelSmall,
                    color = IosSecondaryText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Preview
            Text(
                text = "Query: \"${log.userPrompt.take(80)}${if (log.userPrompt.length > 80) "..." else ""}\"",
                style = MaterialTheme.typography.bodySmall,
                color = IosSecondaryText
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    if (!log.aiResponse.isNullOrBlank()) {
                        Text(
                            text = "Khabri's Answer:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = KhabarBlue
                        )
                        Text(
                            text = log.aiResponse,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!log.userCritique.isNullOrBlank()) {
                        Text(
                            text = "Student's Feedback:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF9C27B0)
                        )
                        Text(
                            text = log.userCritique,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = "Root-Cause Diagnosis:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = IosRed
                    )
                    Text(
                        text = log.rootCauseAnalysis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Self-Healing Recommendation:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = IosGreen
                    )
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = log.suggestedFix,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, IosRed.copy(alpha = 0.4f))
                        ) {
                            Text("Delete Log", color = IosRed, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
