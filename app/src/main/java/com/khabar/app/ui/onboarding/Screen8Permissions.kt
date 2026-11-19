package com.khabar.app.ui.onboarding

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.khabar.app.ui.components.HairlineDivider
import com.khabar.app.ui.components.PrimaryButton
import com.khabar.app.ui.theme.SquircleCardShape
import com.khabar.app.ui.theme.SquircleIconShape
import com.khabar.app.ui.theme.SystemBlue
import com.khabar.app.ui.theme.SystemBlueContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen8Permissions(
    onFinished: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasNotificationListenerAccess by remember { mutableStateOf(false) }
    var isBatteryIgnoringOptimizations by remember { mutableStateOf(false) }
    var canScheduleExactAlarms by remember { mutableStateOf(false) }

    var showRestrictedSettingsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    fun checkPermissions() {
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else true

        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        hasNotificationListenerAccess = enabledListeners.contains(context.packageName)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        isBatteryIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        checkPermissions()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        checkPermissions()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Top Bar & Progress Dots (Step 8 active)
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

                // Progress Dots (Step 8 active)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(7) {
                        Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                        Spacer(modifier = Modifier.size(6.dp))
                    }
                    Box(modifier = Modifier.size(20.dp, 6.dp).background(SystemBlue, RoundedCornerShape(3.dp)))
                }

                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Just one final step",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant these permissions so you never miss an urgent university notice.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Inset Grouped Permissions Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 1.dp, shape = SquircleCardShape)
                    .background(MaterialTheme.colorScheme.surface, SquircleCardShape)
            ) {
                Column {
                    // Row 1: Notifications
                    PermissionRow(
                        icon = Icons.Rounded.Notifications,
                        iconBg = SystemBlue,
                        title = "Allow notifications",
                        subtitle = "For urgent quiz and class cancellation alerts",
                        isGranted = hasNotificationPermission,
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                    HairlineDivider(startIndent = 68.dp)

                    // Row 2: Notification Listener for Instant mode
                    PermissionRow(
                        icon = Icons.Rounded.NotificationsActive,
                        iconBg = Color(0xFFFF9500),
                        title = "Notification access (Instant mode)",
                        subtitle = "To read incoming emails and summarize immediately",
                        isGranted = hasNotificationListenerAccess,
                        howAction = { showRestrictedSettingsSheet = true },
                        onAction = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                    HairlineDivider(startIndent = 68.dp)

                    // Row 3: Battery Unrestricted
                    PermissionRow(
                        icon = Icons.Rounded.BatteryChargingFull,
                        iconBg = Color(0xFF34C759),
                        title = "Battery: Unrestricted",
                        subtitle = "Prevents Android from stopping background checks",
                        isGranted = isBatteryIgnoringOptimizations,
                        helpLink = "dontkillmyapp.com",
                        onHelpLinkClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com"))
                            context.startActivity(intent)
                        },
                        onAction = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    )
                    HairlineDivider(startIndent = 68.dp)

                    // Row 4: Exact Alarms (optional)
                    PermissionRow(
                        icon = Icons.Rounded.Alarm,
                        iconBg = Color(0xFFAF52DE),
                        title = "Exact reminders",
                        subtitle = "Fires deadline reminders at the precise minute",
                        isGranted = canScheduleExactAlarms,
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Celebration Card (Adaptive Apple-style container with proper light/dark contrast)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SystemBlue.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .border(1.dp, SystemBlue.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(SystemBlue.copy(alpha = 0.22f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Rounded.Check, contentDescription = null, tint = SystemBlue)
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = "All set! Khabar is ready to organize your academic life.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Bottom Action Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            PrimaryButton(
                text = "I am ready",
                onClick = onFinished
            )
        }
    }

    // "How?" Bottom Sheet explaining Android Restricted Settings for APK installs
    if (showRestrictedSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRestrictedSettingsSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Allowing Restricted Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Because Khabar was installed via APK (outside Google Play), Android security may show \"Restricted setting\".\n\n" +
                    "To enable notification access:\n" +
                    "1. Open your phone's Settings > Apps > Khabar.\n" +
                    "2. Tap the three dots (⋮) in the top-right corner.\n" +
                    "3. Tap \"Allow restricted settings\".\n" +
                    "4. Return here and tap \"Turn on\" to grant access.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        showRestrictedSettingsSheet = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlueContainer)
                ) {
                    Text("Open Khabar App Info", color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onAction: () -> Unit,
    howAction: (() -> Unit)? = null,
    helpLink: String? = null,
    onHelpLinkClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, SquircleIconShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (howAction != null && !isGranted) {
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "How?",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(onClick = howAction)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (helpLink != null && onHelpLinkClick != null && !isGranted) {
                    Text(
                        text = "Xiaomi, Oppo, Vivo settings: $helpLink",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = SystemBlue,
                        modifier = Modifier.clickable(onClick = onHelpLinkClick)
                    )
                }
            }
        }

        Box(modifier = Modifier.padding(start = 8.dp)) {
            if (isGranted) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF34C759).copy(alpha = 0.18f), RoundedCornerShape(9999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34C759)
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(9999.dp),
                    border = BorderStroke(1.dp, SystemBlue.copy(alpha = 0.6f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Turn on", style = MaterialTheme.typography.labelSmall, color = SystemBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
