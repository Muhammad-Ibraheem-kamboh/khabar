package com.khabar.app.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.ai.KeystoreSecretManager
import com.khabar.app.ai.OpenAiCompatibleClient
import com.khabar.app.config.AppConfig
import com.khabar.app.data.repository.ProfileRepository
import com.khabar.app.ui.components.PrimaryButton
import com.khabar.app.ui.components.SecondaryButton
import com.khabar.app.ui.theme.IosGreen
import com.khabar.app.ui.theme.IosRed
import com.khabar.app.ui.theme.IosSecondaryText
import com.khabar.app.ui.theme.KhabarBlue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    profileRepository: ProfileRepository,
    secretManager: KeystoreSecretManager,
    openAiClient: OpenAiCompatibleClient,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf(AppConfig.AI_BASE_URL.ifBlank { "https://api.groq.com/openai/v1" }) }
    var model by remember { mutableStateOf(AppConfig.AI_MODEL.ifBlank { "llama-3.3-70b-versatile" }) }
    var apiKey by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val savedUrl = secretManager.getAiBaseUrl()
        val savedModel = secretManager.getAiModel()
        val savedKey = secretManager.getAiApiKey()
        if (savedUrl.isNotBlank()) baseUrl = savedUrl
        if (savedModel.isNotBlank()) model = savedModel
        if (savedKey.isNotBlank()) apiKey = savedKey
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Summaries", fontWeight = FontWeight.Bold) },
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

            Text(
                text = "Use an OpenAI-compatible API to generate smart bullet points and explain complicated university notifications.",
                style = MaterialTheme.typography.bodyMedium,
                color = IosSecondaryText
            )

            // Presets
            Text(
                text = "PRESETS",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = IosSecondaryText
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetChip(
                    label = "Groq",
                    selected = baseUrl.contains("groq"),
                    onClick = {
                        baseUrl = "https://api.groq.com/openai/v1"
                        model = "llama-3.3-70b-versatile"
                    },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = "Gemini",
                    selected = baseUrl.contains("generativelanguage"),
                    onClick = {
                        baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai"
                        model = "gemini-1.5-flash"
                    },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = "OpenRouter",
                    selected = baseUrl.contains("openrouter"),
                    onClick = {
                        baseUrl = "https://openrouter.ai/api/v1"
                        model = "meta-llama/llama-3.3-70b-instruct"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Inputs Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it.trim() },
                        label = { Text("Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it.trim() },
                        label = { Text("Model Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it.trim() },
                        label = { Text("API Key") },
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showApiKey) "Hide key" else "Show key"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Test connection result
            testStatus?.let { status ->
                val isSuccess = status.startsWith("Success")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = if (isSuccess) IosGreen else IosRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSuccess) IosGreen else IosRed
                    )
                }
            }

            // Buttons
            SecondaryButton(
                text = if (isTesting) "Testing connection..." else "Test Connection",
                enabled = !isTesting && apiKey.isNotBlank(),
                onClick = {
                    coroutineScope.launch {
                        isTesting = true
                        testStatus = null
                        val success = openAiClient.testConnection(baseUrl, model, apiKey)
                        isTesting = false
                        testStatus = if (success) "Success! AI model responded." else "Failed. Check URL, model, and key."
                    }
                }
            )

            PrimaryButton(
                text = if (isSaved) "Saved!" else "Save AI Settings",
                onClick = {
                    coroutineScope.launch {
                        secretManager.saveAiApiKey(apiKey)
                        secretManager.saveAiSettings(provider = "Custom", baseUrl = baseUrl, model = model)
                        isSaved = true
                    }
                }
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) KhabarBlue else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}
