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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import com.letdigitalfly.ibamail.domain.model.ProgramGroup
import com.letdigitalfly.ibamail.domain.model.ProgramItem
import com.letdigitalfly.ibamail.ui.components.HairlineDivider
import com.letdigitalfly.ibamail.ui.components.PrimaryButton
import com.letdigitalfly.ibamail.ui.theme.SquircleButtonShape
import com.letdigitalfly.ibamail.ui.theme.SquircleCardShape
import com.letdigitalfly.ibamail.ui.theme.SystemBlue
import com.letdigitalfly.ibamail.ui.theme.SystemBlueContainer
import java.time.LocalDate

@Composable
fun Screen3ProgramJoin(
    programGroups: List<ProgramGroup>,
    selectedProgram: ProgramItem?,
    joinTerm: String,
    joinYear: Int,
    semesterIndex: Int,
    standing: String,
    onProgramSelected: (ProgramItem) -> Unit,
    onJoinTermAndYearChanged: (String, Int) -> Unit,
    onSemesterOverridden: (Int) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
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

            // Step 3 active
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(2) {
                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    Spacer(modifier = Modifier.size(6.dp))
                }
                Box(modifier = Modifier.size(20.dp, 6.dp).background(SystemBlue, RoundedCornerShape(3.dp)))
                Spacer(modifier = Modifier.size(6.dp))
                repeat(5) {
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
                    text = "Which program are you in?",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "We filter batch and department emails specifically for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // iOS Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search program (e.g. BBA, CS)...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Grouped Programs
            val filteredGroups = programGroups.mapNotNull { group ->
                val filtered = group.programs.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.aliases.any { alias -> alias.contains(searchQuery, ignoreCase = true) }
                }
                if (filtered.isNotEmpty()) group.copy(programs = filtered) else null
            }

            filteredGroups.forEach { group ->
                item {
                    Text(
                        text = group.name.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 1.dp, shape = SquircleCardShape)
                            .background(MaterialTheme.colorScheme.surface, SquircleCardShape)
                    ) {
                        Column {
                            group.programs.forEachIndexed { index, prog ->
                                val isSelected = prog.id == selectedProgram?.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onProgramSelected(prog) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prog.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = SystemBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (index < group.programs.size - 1) {
                                    HairlineDivider(startIndent = 16.dp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Question B: When did you join?
            item {
                Text(
                    text = "When did you join?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 1.dp, shape = SquircleCardShape)
                        .background(MaterialTheme.colorScheme.surface, SquircleCardShape)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Term Selector
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Term",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Fall", "Spring").forEach { term ->
                                    val isTermSelected = term.equals(joinTerm, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                width = if (isTermSelected) 2.dp else 1.dp,
                                                color = if (isTermSelected) SystemBlue else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .background(
                                                if (isTermSelected) SystemBlue.copy(alpha = 0.1f) else Color.Transparent,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onJoinTermAndYearChanged(term, joinYear) }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = term,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isTermSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isTermSelected) SystemBlue else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Year Selector
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Year",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                years.take(3).forEach { year ->
                                    val isYearSelected = year == joinYear
                                    Box(
                                        modifier = Modifier
                                            .border(
                                                width = if (isYearSelected) 2.dp else 1.dp,
                                                color = if (isYearSelected) SystemBlue else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .background(
                                                if (isYearSelected) SystemBlue.copy(alpha = 0.1f) else Color.Transparent,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onJoinTermAndYearChanged(joinTerm, year) }
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = year.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isYearSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isYearSelected) SystemBlue else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Confirmation Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = SystemBlueContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = " You look like you are in semester $semesterIndex ($standing).",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onContinue,
                                shape = SquircleButtonShape,
                                colors = ButtonDefaults.buttonColors(containerColor = SystemBlueContainer)
                            ) {
                                Text("Looks right", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { showFixDialog = true },
                                shape = SquircleButtonShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, SquircleButtonShape)
                            ) {
                                Text("Fix it")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Bottom dock button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            PrimaryButton(text = "Continue", onClick = onContinue)
        }
    }

    // Fix Semester Number Dialog
    if (showFixDialog) {
        AlertDialog(
            onDismissRequest = { showFixDialog = false },
            title = { Text("Choose your semester") },
            text = {
                Column {
                    Text("Select your current active semester index (1 to 12):")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (1..6).forEach { sem ->
                            TextButton(onClick = {
                                onSemesterOverridden(sem)
                                showFixDialog = false
                            }) {
                                Text(sem.toString(), fontWeight = if (sem == semesterIndex) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (7..12).forEach { sem ->
                            TextButton(onClick = {
                                onSemesterOverridden(sem)
                                showFixDialog = false
                            }) {
                                Text(sem.toString(), fontWeight = if (sem == semesterIndex) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFixDialog = false }) { Text("Done") }
            }
        )
    }
}
