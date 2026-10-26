package com.letdigitalfly.ibamail.ui.onboarding

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.letdigitalfly.ibamail.domain.model.ProgramItem
import com.letdigitalfly.ibamail.ui.components.HairlineDivider
import com.letdigitalfly.ibamail.ui.components.PrimaryButton
import com.letdigitalfly.ibamail.ui.theme.SquircleButtonShape
import com.letdigitalfly.ibamail.ui.theme.SquircleCardShape
import com.letdigitalfly.ibamail.ui.theme.SystemBlue
import com.letdigitalfly.ibamail.ui.theme.SystemBlueContainer
import java.time.LocalDate

@Composable
fun Screen3bJoinDate(
    selectedProgram: ProgramItem?,
    joinTerm: String,
    joinYear: Int,
    semesterIndex: Int,
    standing: String,
    onJoinTermAndYearChanged: (String, Int) -> Unit,
    onSemesterOverridden: (Int) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFixDialog by remember { mutableStateOf(false) }
    val currentYear = LocalDate.now().year
    val years = (currentYear - 8..currentYear).toList().reversed()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar & Progress Dots
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

            // Progress Dots (Step 4 active)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                Spacer(modifier = Modifier.size(6.dp))
                Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                Spacer(modifier = Modifier.size(6.dp))
                Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                Spacer(modifier = Modifier.size(6.dp))
                Box(modifier = Modifier.size(20.dp, 6.dp).background(SystemBlue, RoundedCornerShape(3.dp)))
                Spacer(modifier = Modifier.size(6.dp))
                repeat(4) {
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
                    text = "When did you join?",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This allows Khabar to identify your exact semester, batch, and standing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Intake Term Card
                Text(
                    text = "INTAKE SEMESTER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = SquircleCardShape)
                        .background(MaterialTheme.colorScheme.surface, SquircleCardShape)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Fall", "Spring", "Summer").forEach { term ->
                            val isTermSelected = term.equals(joinTerm, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .padding(horizontal = 4.dp)
                                    .background(
                                        color = if (isTermSelected) SystemBlue else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onJoinTermAndYearChanged(term, joinYear) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = term,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isTermSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isTermSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Join Year Card
                Text(
                    text = "JOIN YEAR",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = SquircleCardShape)
                        .background(MaterialTheme.colorScheme.surface, SquircleCardShape)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        years.chunked(3).forEach { rowYears ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowYears.forEach { yr ->
                                    val isYrSelected = yr == joinYear
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .padding(horizontal = 4.dp)
                                            .background(
                                                color = if (isYrSelected) SystemBlue else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onJoinTermAndYearChanged(joinTerm, yr) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = yr.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isYrSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isYrSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Calculated Standing Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = SquircleCardShape)
                        .background(SystemBlueContainer.copy(alpha = 0.5f), SquircleCardShape)
                        .border(1.dp, SystemBlue.copy(alpha = 0.2f), SquircleCardShape)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = SystemBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Calculated Academic Standing",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SystemBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Semester $semesterIndex · $standing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Based on $joinTerm $joinYear join date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Fix it",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SystemBlue,
                                modifier = Modifier.clickable { showFixDialog = true }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Bottom Action Dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            PrimaryButton(
                text = "Continue",
                onClick = onContinue
            )
        }
    }

    if (showFixDialog) {
        var tempSem by remember { mutableStateOf(semesterIndex.toString()) }
        AlertDialog(
            onDismissRequest = { showFixDialog = false },
            title = { Text("Override Current Semester", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "If you froze a semester, took leaves, or transferred credits, specify your exact current semester index (1 - 12):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempSem,
                        onValueChange = { tempSem = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Semester Index") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = tempSem.toIntOrNull() ?: semesterIndex
                        onSemesterOverridden(parsed.coerceIn(1, 12))
                        showFixDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemBlue)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFixDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
