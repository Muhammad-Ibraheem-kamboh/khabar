package com.khabar.app.ui

import android.content.Intent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.khabar.app.KhabarApp
import com.khabar.app.ui.navigation.NavGraph
import com.khabar.app.ui.navigation.Screen
import com.khabar.app.ui.theme.KhabarBlue
import com.khabar.app.ui.theme.KhabarTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as KhabarApp).appContainer

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
                        startDestination = startDestination
                    )
                }
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "khabar" && data.host == "mail") {
            val mailId = data.lastPathSegment
            // Deep link handling if needed
        }
    }
}
