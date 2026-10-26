package com.khabar.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.config.AppConfig
import com.khabar.app.ui.theme.SquircleButtonShape
import com.khabar.app.ui.theme.SquircleCardShape
import com.khabar.app.ui.theme.SquircleIconShape
import com.khabar.app.ui.theme.SystemBlue
import com.khabar.app.ui.theme.SystemBlueContainer

@Composable
fun Screen7ConnectMail(
    onContinueToPermissions: () -> Unit,
    onLoadSampleMailsAndFinish: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSlotHelpDialog by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        // Top Bar & Progress Dots (Step 7 active)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = SystemBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Step 7 active
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(6) {
                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    Spacer(modifier = Modifier.size(6.dp))
                }
                Box(modifier = Modifier.size(20.dp, 6.dp).background(SystemBlue, RoundedCornerShape(3.dp)))
                Spacer(modifier = Modifier.size(6.dp))
                Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
            }

            Spacer(modifier = Modifier.size(40.dp))
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                Text(
                    text = "Connect your mail",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Khabar will process and summarize your university notices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Card 1: Outlook / Microsoft 365
            item {
                ProviderCard(
                    title = "Outlook / Microsoft 365",
                    tag = "FASTEST, FULL MAIL",
                    description = "For official student university email accounts",
                    icon = Icons.Rounded.Mail,
                    iconBgColor = Color(0xFF0078D4),
                    buttonText = if (AppConfig.buildMsClientId.isNotBlank()) "Connect" else "Not set up yet",
                    isConfigured = AppConfig.buildMsClientId.isNotBlank(),
                    onActionClick = {
                        if (AppConfig.buildMsClientId.isNotBlank()) {
                            onContinueToPermissions()
                        } else {
                            showSlotHelpDialog = "Microsoft Graph (Outlook)"
                        }
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Card 2: Gmail
            item {
                ProviderCard(
                    title = "Gmail",
                    tag = null,
                    description = "Personal or GSuite university account",
                    icon = Icons.Rounded.AlternateEmail,
                    iconBgColor = Color(0xFFEA4335),
                    buttonText = if (AppConfig.buildGmailEnabled) "Connect" else "Not set up yet",
                    isConfigured = AppConfig.buildGmailEnabled,
                    onActionClick = {
                        if (AppConfig.buildGmailEnabled) {
                            onContinueToPermissions()
                        } else {
                            showSlotHelpDialog = "Google Play Services (Gmail)"
                        }
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Card 3: Instant Mode
            item {
                ProviderCard(
                    title = "Instant mode",
                    tag = "ZERO SETUP REQUIRED",
                    description = "Uses your Outlook or Gmail app notifications. No login needed.",
                    icon = Icons.Rounded.Bolt,
                    iconBgColor = Color(0xFFFF9500),
                    buttonText = "Turn on",
                    isConfigured = true,
                    onActionClick = onContinueToPermissions
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Card 4: Try with sample mails
            item {
                ProviderCard(
                    title = "Try with sample mails",
                    tag = "DEMO READY",
                    description = "Test Khabar immediately with 30 realistic IBA emails.",
                    icon = Icons.Rounded.Science,
                    iconBgColor = Color(0xFF34C759),
                    buttonText = "Load demo mails",
                    isConfigured = true,
                    onActionClick = onLoadSampleMailsAndFinish
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Privacy Callout
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = "Mails are processed on your phone. No email data, passwords, or personal credentials are ever sent to an external server.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Help Dialog for unconfigured slots
    showSlotHelpDialog?.let { slotName ->
        AlertDialog(
            onDismissRequest = { showSlotHelpDialog = null },
            title = { Text("$slotName Setup") },
            text = {
                Text(
                    "To enable direct $slotName sync, add your OAuth Client ID in local.properties:\n\n" +
                    "For Outlook: MS_CLIENT_ID and MS_SIGNATURE_HASH\n" +
                    "For Gmail: GMAIL_ENABLED=true\n\n" +
                    "Meanwhile, you can use Instant mode (notification listener) or Demo mode right away with zero setup!"
                )
            },
            confirmButton = {
                TextButton(onClick = { showSlotHelpDialog = null }) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
private fun ProviderCard(
    title: String,
    tag: String?,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    buttonText: String,
    isConfigured: Boolean,
    onActionClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = SquircleCardShape)
            .background(MaterialTheme.colorScheme.surface, SquircleCardShape)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(iconBgColor, SquircleIconShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!tag.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(SystemBlue.copy(alpha = 0.1f), RoundedCornerShape(9999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = SystemBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isConfigured) {
                Button(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = SquircleButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlueContainer)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(buttonText, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Spacer(modifier = Modifier.size(4.dp))
                        Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = SquircleButtonShape
                ) {
                    Text(buttonText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
