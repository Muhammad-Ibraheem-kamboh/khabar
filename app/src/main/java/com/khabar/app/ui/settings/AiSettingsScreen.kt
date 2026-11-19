package com.khabar.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khabar.app.ai.KeystoreSecretManager
import com.khabar.app.ai.OpenAiCompatibleClient
import com.khabar.app.data.repository.ProfileRepository
import com.khabar.app.ui.components.PrimaryButton
import com.khabar.app.ui.components.SecondaryButton
import com.khabar.app.ui.theme.IosGreen
import com.khabar.app.ui.theme.IosRed
import com.khabar.app.ui.theme.IosSecondaryText
import com.khabar.app.ui.theme.KhabarBlue
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedProvider by remember {
        mutableStateOf(
            secretManager.getAiProvider().ifBlank { KeystoreSecretManager.PROVIDER_GEMINI }
        )
    }

    var baseUrl by remember {
        mutableStateOf(secretManager.getBaseUrlForProvider(selectedProvider))
    }
    var model by remember {
        mutableStateOf(secretManager.getModelForProvider(selectedProvider))
    }
    var apiKey by remember {
        mutableStateOf(secretManager.getApiKeyForProvider(selectedProvider))
    }

    var showApiKey by remember { mutableStateOf(false) }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }

    val providers = listOf(
        KeystoreSecretManager.PROVIDER_GEMINI,
        KeystoreSecretManager.PROVIDER_GROQ,
        KeystoreSecretManager.PROVIDER_OPENROUTER,
        KeystoreSecretManager.PROVIDER_CLAUDE,
        KeystoreSecretManager.PROVIDER_OLLAMA
    )

    fun selectEngine(provider: String) {
        selectedProvider = provider
        baseUrl = KeystoreSecretManager.getDefaultBaseUrl(provider)
        model = KeystoreSecretManager.getDefaultModel(provider)
        // Dynamically load only this provider's key (clears if not set so user is prompted)
        apiKey = secretManager.getApiKeyForProvider(provider)
        testStatus = null
        isSaved = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Summaries & Khabri", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Configure API keys for Khabri AI and smart campus digests. Each engine retains its own independent key.",
                style = MaterialTheme.typography.bodyMedium,
                color = IosSecondaryText
            )

            // PRESETS SELECTOR
            Text(
                text = "SELECT AI ENGINE",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = IosSecondaryText
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                providers.forEach { p ->
                    val isSelected = selectedProvider.equals(p, ignoreCase = true)
                    val hasKey = secretManager.hasApiKeyForProvider(p)

                    PresetChip(
                        label = p,
                        selected = isSelected,
                        hasKey = hasKey,
                        onClick = { selectEngine(p) },
                        modifier = Modifier.width(105.dp)
                    )
                }
            }

            // Direct Browser Link to Get Key for Selected Engine
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (selectedProvider) {
                                KeystoreSecretManager.PROVIDER_GEMINI -> "🌟 Google Gemini (Free & Fast)"
                                KeystoreSecretManager.PROVIDER_GROQ -> "⚡ Groq (Ultra-Fast & Free)"
                                KeystoreSecretManager.PROVIDER_OPENROUTER -> "🌐 OpenRouter (Multi-Model Hub)"
                                KeystoreSecretManager.PROVIDER_CLAUDE -> "🧠 Anthropic Claude (Premium)"
                                else -> "💻 Ollama (Local on PC)"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = if (apiKey.isNotBlank()) "✓ Configured" else "⚙️ Key Missing",
                            color = if (apiKey.isNotBlank()) IosGreen else IosRed,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (apiKey.isBlank() && selectedProvider != KeystoreSecretManager.PROVIDER_OLLAMA)
                            "No API key configured for $selectedProvider. Tap the button below to get your key in Chrome, then paste it here:"
                        else
                            "Endpoint and model configured. You can test connection or update keys anytime:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = IosSecondaryText
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val websiteUrl = KeystoreSecretManager.getProviderWebsiteUrl(selectedProvider)
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Get Free $selectedProvider Key ↗",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            // Inputs Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = {
                            baseUrl = it.trim()
                            testStatus = null
                            isSaved = false
                        },
                        label = { Text("Base URL ($selectedProvider)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = model,
                        onValueChange = {
                            model = it.trim()
                            testStatus = null
                            isSaved = false
                        },
                        label = { Text("Model Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (selectedProvider != KeystoreSecretManager.PROVIDER_OLLAMA) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = {
                                apiKey = it.trim()
                                testStatus = null
                                isSaved = false
                            },
                            label = { Text("$selectedProvider API Key") },
                            placeholder = { Text("Paste $selectedProvider key here...") },
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
            }

            // Test connection result feedback
            testStatus?.let { status ->
                val isSuccess = status.startsWith("Success")
                Surface(
                    color = if (isSuccess) IosGreen.copy(alpha = 0.12f) else IosRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
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
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (isSuccess) IosGreen else IosRed
                        )
                    }
                }
            }

            // Test Connection Button
            SecondaryButton(
                text = if (isTesting) "Testing connection..." else "⚡ Test Connection ($selectedProvider)",
                enabled = !isTesting && (apiKey.isNotBlank() || selectedProvider == KeystoreSecretManager.PROVIDER_OLLAMA),
                onClick = {
                    coroutineScope.launch {
                        isTesting = true
                        testStatus = null
                        val result = openAiClient.testConnectionDetailed(baseUrl, model, apiKey)
                        isTesting = false
                        result.onSuccess {
                            testStatus = "Success! $selectedProvider responded cleanly."
                        }.onFailure { err ->
                            testStatus = "Failed: ${err.message ?: "Invalid key or endpoint"}"
                        }
                    }
                }
            )

            // Save Settings Button
            PrimaryButton(
                text = if (isSaved) "Saved for $selectedProvider! ✓" else "Save Settings for $selectedProvider",
                onClick = {
                    coroutineScope.launch {
                        secretManager.saveApiKeyForProvider(selectedProvider, apiKey)
                        secretManager.saveAiSettings(provider = selectedProvider, baseUrl = baseUrl, model = model)
                        secretManager.saveAiApiKey(apiKey)
                        isSaved = true
                        Toast.makeText(context, "$selectedProvider configuration saved!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Security Assurance Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Secured",
                        tint = IosGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hardware Encrypted: Your keys are secured in Android Keystore with AES-256-GCM. Never stored in plaintext, never logged, and protected against extraction.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = IosSecondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    hasKey: Boolean,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
            if (hasKey) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "✓",
                    color = if (selected) Color.White else IosGreen,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
