package com.khabar.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.config.AppConfig
import com.khabar.app.ui.components.HairlineDivider
import com.khabar.app.ui.components.PrimaryButton
import com.khabar.app.ui.theme.SquircleCardShape
import com.khabar.app.ui.theme.SquircleExtraLargeCardShape
import com.khabar.app.ui.theme.SquircleIconShape
import com.khabar.app.ui.theme.SystemBlue
import com.khabar.app.ui.theme.SystemBlueContainer

@Composable
fun Screen1Welcome(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Progress Dots (Step 1 active)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(20.dp, 6.dp).background(SystemBlue, RoundedCornerShape(3.dp)))
            Spacer(modifier = Modifier.size(6.dp))
            repeat(7) {
                Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp)))
                Spacer(modifier = Modifier.size(6.dp))
            }
        }

        // Center Branding & Features
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Icon Squircle
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .shadow(elevation = 16.dp, shape = SquircleExtraLargeCardShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(SystemBlue, SystemBlueContainer)
                        ),
                        shape = SquircleExtraLargeCardShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mail,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .background(Color.White, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = SystemBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = AppConfig.APP_NAME,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AppConfig.APP_TAGLINE,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3 Feature Rows in Inset Grouped Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 1.dp, shape = SquircleCardShape)
                    .background(MaterialTheme.colorScheme.surface, SquircleCardShape)
            ) {
                Column {
                    FeatureRow(
                        icon = Icons.Rounded.PersonSearch,
                        iconTint = SystemBlue,
                        title = "Only for you",
                        description = "Other students' emails are hidden automatically."
                    )
                    HairlineDivider(startIndent = 56.dp)
                    FeatureRow(
                        icon = Icons.Rounded.Translate,
                        iconTint = Color(0xFF5856D6),
                        title = "Explained simply",
                        description = "Clear summaries in English, Roman Urdu or Urdu."
                    )
                    HairlineDivider(startIndent = 56.dp)
                    FeatureRow(
                        icon = Icons.Rounded.NotificationsActive,
                        iconTint = Color(0xFF34C759),
                        title = "Notifications that act",
                        description = "One tap to add calendar, set alarms, or share."
                    )
                }
            }
        }

        // Bottom Action Dock
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            PrimaryButton(
                text = "Get started",
                onClick = onGetStarted
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = " Your mails stay on your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconTint.copy(alpha = 0.12f), SquircleIconShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
