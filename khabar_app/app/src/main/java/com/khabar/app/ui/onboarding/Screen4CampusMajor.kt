package com.khabar.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.domain.model.ProgramItem
import com.khabar.app.ui.components.PrimaryButton
import com.khabar.app.ui.theme.SquircleCardShape
import com.khabar.app.ui.theme.SystemBlue
import com.khabar.app.ui.theme.SystemBlueContainer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Screen4CampusMajor(
    selectedCampus: String,
    selectedProgram: ProgramItem?,
    mbaShift: String,
    mbaExecBatch: String,
    selectedMajor: String,
    onCampusSelected: (String) -> Unit,
    onMbaShiftSelected: (String) -> Unit,
    onMbaExecBatchChanged: (String) -> Unit,
    onMajorSelected: (String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBba = selectedProgram?.name?.equals("BBA", ignoreCase = true) == true
    val isMba = selectedProgram?.name?.equals("MBA", ignoreCase = true) == true
    val isMbaExec = selectedProgram?.name?.contains("Executive", ignoreCase = true) == true

    val majors = when {
        isBba -> listOf("Marketing", "Finance", "MIS", "HRM", "Entrepreneurship", "Not decided yet")
        isMba -> listOf("Marketing", "Finance", "HRM", "Supply Chain", "Not decided yet")
        else -> emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar & Progress Dots (Step 4 active)
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

            // Step 4 active
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(3) {
                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    Spacer(modifier = Modifier.size(6.dp))
                }
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
                    text = "Campus and major",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Khabar shows events and updates for your campus and field.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Campus Section
                Text(
                    text = "YOUR CAMPUS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Main" to "University Rd", "City" to "Garden Rd", "Both" to "Cross-campus").forEach { (camp, sub) ->
                        val isSelected = selectedCampus.equals(camp, ignoreCase = true)
                        CampusCard(
                            title = camp,
                            subtitle = sub,
                            isSelected = isSelected,
                            onClick = { onCampusSelected(camp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // MBA Shift / Executive batch
            if (isMba || isMbaExec) {
                item {
                    Text(
                        text = "WHICH SHIFT?",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Morning", "Evening").forEach { shift ->
                            val isShiftSelected = shift.equals(mbaShift, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(elevation = 1.dp, shape = SquircleCardShape)
                                    .background(MaterialTheme.colorScheme.surface, SquircleCardShape)
                                    .border(
                                        width = if (isShiftSelected) 2.dp else 1.dp,
                                        color = if (isShiftSelected) SystemBlue else MaterialTheme.colorScheme.outlineVariant,
                                        shape = SquircleCardShape
                                    )
                                    .clickable { onMbaShiftSelected(shift) }
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shift,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isShiftSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isShiftSelected) SystemBlue else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (isMbaExec) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "BATCH NUMBER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = mbaExecBatch,
                            onValueChange = onMbaExecBatchChanged,
                            placeholder = { Text("e.g. 24") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Major List
            if (majors.isNotEmpty()) {
                item {
                    Text(
                        text = "YOUR MAJOR",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        majors.forEach { majorItem ->
                            val isMajorSelected = majorItem.equals(selectedMajor, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isMajorSelected) SystemBlueContainer else MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(9999.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isMajorSelected) SystemBlueContainer else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(9999.dp)
                                    )
                                    .clickable { onMajorSelected(majorItem) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isMajorSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                    }
                                    Text(
                                        text = majorItem,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isMajorSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isMajorSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
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

@Composable
private fun CampusCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(elevation = if (isSelected) 2.dp else 1.dp, shape = SquircleCardShape)
            .background(
                if (isSelected) SystemBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                SquircleCardShape
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) SystemBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = SquircleCardShape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isSelected) SystemBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = if (isSelected) SystemBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) SystemBlue else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
