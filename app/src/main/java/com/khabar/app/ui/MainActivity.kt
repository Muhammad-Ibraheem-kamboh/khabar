package com.khabar.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.khabar.app.KhabarApp
import com.khabar.app.ui.navigation.NavGraph
import com.khabar.app.ui.navigation.Screen
import com.khabar.app.ui.theme.KhabarBlue
import com.khabar.app.ui.theme.KhabarTheme

class MainActivity : ComponentActivity() {

    private var deepLinkMailId by mutableStateOf<String?>(null)

    private val devAiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.khabar.app.SET_AI_KEY") {
                val appContainer = (application as KhabarApp).appContainer
                val key = intent.getStringExtra("key") ?: return
                val explicitProvider = intent.getStringExtra("provider")
                val provider = if (!explicitProvider.isNullOrBlank()) {
                    com.khabar.app.ai.KeystoreSecretManager.canonicalProvider(explicitProvider)
                } else {
                    com.khabar.app.ai.KeystoreSecretManager.detectProvider(key)
                }
                val baseUrl = intent.getStringExtra("baseUrl") ?: com.khabar.app.ai.KeystoreSecretManager.getDefaultBaseUrl(provider)
                val model = intent.getStringExtra("model") ?: com.khabar.app.ai.KeystoreSecretManager.getDefaultModel(provider)

                appContainer.keystoreSecretManager.saveApiKeyForProvider(provider, key)
                appContainer.keystoreSecretManager.saveAiApiKey(key)
                appContainer.keystoreSecretManager.saveAiSettings(provider, baseUrl, model)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = IntentFilter("com.khabar.app.SET_AI_KEY")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(devAiReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(devAiReceiver, filter)
        }

        val appContainer = (application as KhabarApp).appContainer
        handleIntent(intent)

        setContent {
            val isDark = isSystemInDarkTheme()
            val onboardingDonePref by appContainer.userPreferences.onboardingDoneFlow.collectAsState(initial = null)
            val profile by appContainer.profileRepository.profileFlow.collectAsState(initial = null)

            KhabarTheme(darkTheme = isDark) {
                if (onboardingDonePref == null && profile == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = KhabarBlue)
                    }
                } else {
                    val isCompleted = (onboardingDonePref == true) || (profile?.onboardingDone == true)
                    val startDestination = if (isCompleted) {
                        Screen.Main.route
                    } else {
                        Screen.OnboardingWelcome.route
                    }

                    NavGraph(
                        appContainer = appContainer,
                        startDestination = startDestination,
                        deepLinkMailId = deepLinkMailId,
                        onDeepLinkHandled = { deepLinkMailId = null }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(devAiReceiver)
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "khabar" && data.host == "mail") {
            deepLinkMailId = data.lastPathSegment
        }
    }
}
