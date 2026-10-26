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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.data.local.entities.InterestEntity
import com.khabar.app.ui.components.HairlineDivider
import com.khabar.app.ui.components.PrimaryButton
import com.khabar.app.ui.components.ToggleRow
import com.khabar.app.ui.theme.SquircleCardShape
import com.khabar.app.ui.theme.SquircleIconShape
import com.khabar.app.ui.theme.SystemBlue

@Composable
fun Screen6Interests(
    interests: List<InterestEntity>,
    onToggleInterest: (String, Boolean) -> Unit,
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
    ) {
        // Top Bar & Progress Dots (Step 6 active)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
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

            // Step 6 active
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) {
                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    Spacer(modifier = Modifier.size(6.dp))
                }
                Box(modifier = Modifier.size(20.dp, 6.dp).background(SystemBlue, RoundedCornerShape(3.dp)))
                Spacer(modifier = Modifier.size(6.dp))
                repeat(2) {
                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    Spacer(modifier = Modifier.size(6.dp))
                }
            }

            Spacer(modifier = Modifier.size(40.dp))
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            item {
                Text(
                    text = "Which topics do you want to see?",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Categories that are turned off will go into your Hidden tab so nothing is lost.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = SquircleCardShape)
                        .background(MaterialTheme.colorScheme.surface, SquircleCardShape)
                ) {
                    Column {
                        interests.forEachIndexed { index, interest ->
                            val (title, icon, iconColor) = getInterestMeta(interest.key)

                            ToggleRow(
                                title = title,
                                checked = interest.enabled,
                                onCheckedChange = { onToggleInterest(interest.key, it) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(iconColor, SquircleIconShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            )

                            if (index < interests.size - 1) {
                                HairlineDivider(startIndent = 68.dp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You can change these later in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Bottom Action Dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            PrimaryButton(text = "Continue", onClick = onContinue)
        }
    }
}

private fun getInterestMeta(key: String): Triple<String, ImageVector, Color> {
    return when (key) {
        "INTERNSHIPS" -> Triple("Internships and jobs", Icons.Rounded.Work, Color(0xFF5856D6))
        "EXCHANGE" -> Triple("Exchange and study abroad", Icons.Rounded.Flight, Color(0xFFFF2D55))
        "SCHOLARSHIPS" -> Triple("Scholarships and fee help", Icons.Rounded.Payments, Color(0xFF30B0C7))
        "HOSTEL" -> Triple("Hostel", Icons.Rounded.Hotel, Color(0xFFFF9500))
        "TRANSPORT" -> Triple("Transport", Icons.Rounded.DirectionsBus, Color(0xFF0070EB))
        "SOCIETIES" -> Triple("Societies and events", Icons.Rounded.Diversity3, Color(0xFFAF52DE))
        "ALUMNI" -> Triple("Alumni", Icons.Rounded.AccountBalance, Color(0xFF34C759))
        "SPORTS" -> Triple("Sports", Icons.Rounded.SportsSoccer, Color(0xFFFF3B30))
        else -> Triple(key, Icons.Rounded.Work, Color(0xFF8E8E93))
    }
}
