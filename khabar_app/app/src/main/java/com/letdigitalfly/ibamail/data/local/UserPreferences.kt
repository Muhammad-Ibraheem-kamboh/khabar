package com.letdigitalfly.ibamail.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "khabar_prefs")

class UserPreferences(private val context: Context) {

    private object Keys {
        val ONBOARDING_STEP = intPreferencesKey("onboarding_step")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val SUMMARY_LANGUAGE = stringPreferencesKey("summary_language")
        val FAST_MODE = booleanPreferencesKey("fast_mode")
        val INSTANT_MODE = booleanPreferencesKey("instant_mode")
        val DISMISS_HIDDEN_NOTIFICATIONS = booleanPreferencesKey("dismiss_hidden_notifications")
        val NOTIFY_GOOD_TO_KNOW = booleanPreferencesKey("notify_good_to_know")
        val QUIET_DURING_CLASS = booleanPreferencesKey("quiet_during_class")
        val TODAY_BOARD_NOTIFICATION = booleanPreferencesKey("today_board_notification")
        val PORTAL_URL = stringPreferencesKey("portal_url")
        val AI_DISCLOSURE_SHOWN = booleanPreferencesKey("ai_disclosure_shown")
    }

    val onboardingStepFlow: Flow<Int> = context.dataStore.data.map { it[Keys.ONBOARDING_STEP] ?: 1 }
    val onboardingDoneFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val summaryLanguageFlow: Flow<String> = context.dataStore.data.map { it[Keys.SUMMARY_LANGUAGE] ?: "en" }
    val fastModeFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.FAST_MODE] ?: false }
    val instantModeFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.INSTANT_MODE] ?: false }
    val dismissHiddenNotificationsFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.DISMISS_HIDDEN_NOTIFICATIONS] ?: false }
    val notifyGoodToKnowFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFY_GOOD_TO_KNOW] ?: false }
    val quietDuringClassFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.QUIET_DURING_CLASS] ?: false }
    val todayBoardNotificationFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.TODAY_BOARD_NOTIFICATION] ?: false }
    val portalUrlFlow: Flow<String> = context.dataStore.data.map { it[Keys.PORTAL_URL] ?: "" }
    val aiDisclosureShownFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.AI_DISCLOSURE_SHOWN] ?: false }

    suspend fun setOnboardingStep(step: Int) {
        context.dataStore.edit { it[Keys.ONBOARDING_STEP] = step }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setSummaryLanguage(code: String) {
        context.dataStore.edit { it[Keys.SUMMARY_LANGUAGE] = code }
    }

    suspend fun setFastMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FAST_MODE] = enabled }
    }

    suspend fun setInstantMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INSTANT_MODE] = enabled }
    }

    suspend fun setDismissHiddenNotifications(dismiss: Boolean) {
        context.dataStore.edit { it[Keys.DISMISS_HIDDEN_NOTIFICATIONS] = dismiss }
    }

    suspend fun setNotifyGoodToKnow(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_GOOD_TO_KNOW] = enabled }
    }

    suspend fun setQuietDuringClass(enabled: Boolean) {
        context.dataStore.edit { it[Keys.QUIET_DURING_CLASS] = enabled }
    }

    suspend fun setTodayBoardNotification(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TODAY_BOARD_NOTIFICATION] = enabled }
    }

    suspend fun setPortalUrl(url: String) {
        context.dataStore.edit { it[Keys.PORTAL_URL] = url }
    }

    suspend fun setAiDisclosureShown(shown: Boolean) {
        context.dataStore.edit { it[Keys.AI_DISCLOSURE_SHOWN] = shown }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
