package com.khabar.app.ui.onboarding

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
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.domain.model.SummaryLanguage
import com.khabar.app.ui.components.PrimaryButton
import com.khabar.app.ui.theme.SquircleCardShape
import com.khabar.app.ui.theme.SystemBlue
import com.khabar.app.ui.theme.UrduFontFamily

@Composable
fun Screen2Language(
    selectedLanguage: SummaryLanguage,
    onLanguageSelected: (SummaryLanguage) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            // Header with Back and Progress Dots
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

                // Step 2 active
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    Spacer(modifier = Modifier.size(6.dp))
                    Box(modifier = Modifier.size(20.dp, 6.dp).background(SystemBlue, RoundedCornerShape(3.dp)))
                    Spacer(modifier = Modifier.size(6.dp))
                    repeat(6) {
                        Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                        Spacer(modifier = Modifier.size(6.dp))
                    }
                }

                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "In which language should we explain your mails?",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Summaries and notifications will appear in this language. The app itself stays in English.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3 Language Selection Cards
            SummaryLanguage.entries.forEach { lang ->
                val isSelected = lang == selectedLanguage
                LanguageCard(
                    language = lang,
                    isSelected = isSelected,
                    onClick = { onLanguageSelected(lang) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Bottom Action Dock
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            PrimaryButton(
                text = "Continue",
                onClick = onContinue
            )
        }
    }
}

@Composable
private fun LanguageCard(
    language: SummaryLanguage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) SystemBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = if (isSelected) 2.dp else 1.dp, shape = SquircleCardShape)
            .background(MaterialTheme.colorScheme.surface, SquircleCardShape)
            .border(width = borderWidth, color = borderColor, shape = SquircleCardShape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = language.title,
                        style = if (language == SummaryLanguage.URDU) {
                            MaterialTheme.typography.headlineMedium.copy(fontFamily = UrduFontFamily, fontSize = 20.sp)
                        } else {
                            MaterialTheme.typography.headlineMedium
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = language.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (isSelected) SystemBlue else Color.Transparent,
                            CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = if (isSelected) SystemBlue else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sample sentence box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = "“${language.sampleSentence}”",
                    style = if (language == SummaryLanguage.URDU) {
                        MaterialTheme.typography.bodyMedium.copy(fontFamily = UrduFontFamily, fontSize = 15.sp, lineHeight = 24.sp)
                    } else {
                        MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
