package com.khabar.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.data.local.entities.CourseEntity
import com.khabar.app.data.local.entities.InterestEntity
import com.khabar.app.data.local.entities.ProfileEntity
import com.khabar.app.data.repository.ProfileRepository
import com.khabar.app.ui.components.PrimaryButton
import com.khabar.app.ui.components.ToggleRow
import com.khabar.app.ui.theme.IosDivider
import com.khabar.app.ui.theme.IosSecondaryText
import com.khabar.app.ui.theme.KhabarBlue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    profileRepository: ProfileRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var programCode by remember { mutableStateOf("") }
    var joinTerm by remember { mutableStateOf("") }
    var campus by remember { mutableStateOf("Main") }

    val coursesList = remember { mutableStateListOf<String>() }
    var newCourseCode by remember { mutableStateOf("") }

    val interestsList = remember { mutableStateListOf<InterestEntity>() }

    LaunchedEffect(Unit) {
        val p = profileRepository.profileFlow.first()
        if (p != null) {
            firstName = p.firstName
            lastName = p.lastName
            programCode = p.programCode
            joinTerm = p.joinTerm
            campus = p.campus
        }
        val c = profileRepository.coursesFlow.first()
        coursesList.clear()
        coursesList.addAll(c.map { it.courseCode })

        val ints = profileRepository.interestsFlow.first()
        interestsList.clear()
        interestsList.addAll(ints)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = KhabarBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Academic Info Card
            Text(
                text = "ACADEMIC INFO",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = IosSecondaryText
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First name") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last name") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = programCode,
                        onValueChange = { programCode = it.uppercase() },
                        label = { Text("Program (e.g. BSCS, BBA)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = joinTerm,
                            onValueChange = { joinTerm = it },
                            label = { Text("Join Term (e.g. Fall 23)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = campus,
                            onValueChange = { campus = it },
                            label = { Text("Campus") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Enrolled Courses
            Text(
                text = "ENROLLED COURSES",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = IosSecondaryText
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCourseCode,
                            onValueChange = { newCourseCode = it.uppercase() },
                            placeholder = { Text("Add code (e.g. CS201)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newCourseCode.isNotBlank() && !coursesList.contains(newCourseCode.trim())) {
                                    coursesList.add(newCourseCode.trim())
                                    newCourseCode = ""
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add", tint = KhabarBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    coursesList.forEachIndexed { index, code ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = code, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            IconButton(
                                onClick = { coursesList.remove(code) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Remove", tint = IosSecondaryText)
                            }
                        }
                        if (index < coursesList.size - 1) {
                            HorizontalDivider(color = IosDivider, thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Interests
            Text(
                text = "INTERESTS",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = IosSecondaryText
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    interestsList.forEachIndexed { index, interest ->
                        ToggleRow(
                            label = interest.name,
                            checked = interest.isEnabled,
                            onCheckedChange = { enabled ->
                                interestsList[index] = interest.copy(enabled = enabled)
                            }
                        )
                        if (index < interestsList.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = IosDivider, thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Save Button
            PrimaryButton(
                text = "Save Changes",
                onClick = {
                    coroutineScope.launch {
                        profileRepository.saveAcademicProfile(
                            firstName = firstName,
                            lastName = lastName,
                            programCode = programCode,
                            joinTerm = joinTerm,
                            campus = campus,
                            courses = coursesList.map { CourseEntity(code = it, section = "A", name = it) }
                        )
                        interestsList.forEach {
                            profileRepository.updateInterest(it.key, it.enabled)
                        }
                        onBack()
                    }
                }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
