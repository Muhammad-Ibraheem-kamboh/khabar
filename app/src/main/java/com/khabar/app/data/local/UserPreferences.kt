package com.khabar.app.data.local

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

    object Keys {
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
        val LMS_URL = stringPreferencesKey("lms_url")
        val CUSTOM_PORTAL_URL = stringPreferencesKey("custom_portal_url")
        val AI_DISCLOSURE_SHOWN = booleanPreferencesKey("ai_disclosure_shown")
        val FOCUS_IBA_ONLY = booleanPreferencesKey("focus_iba_only")
        val NEXT_CLASS_WIDGET_ENABLED = booleanPreferencesKey("next_class_widget_enabled")
        val ABSENCE_TRACKER_ENABLED = booleanPreferencesKey("absence_tracker_enabled")
        val MORNING_BRIEFING_ENABLED = booleanPreferencesKey("morning_briefing_enabled")
        val DEADLINE_ALARM_ENABLED = booleanPreferencesKey("deadline_alarm_enabled")
        val OFFLINE_AI_ENABLED = booleanPreferencesKey("offline_ai_enabled")
        val CAMPUS_ID = stringPreferencesKey("campus_id")
        val MORNING_BRIEFING_TIME = stringPreferencesKey("morning_briefing_time")
        val DEADLINE_ALARM_LEAD_MINUTES = intPreferencesKey("deadline_alarm_lead_minutes")
    }

    companion object {
        const val DEFAULT_UMS_URL = "https://ums.iba.edu.pk/"
        const val DEFAULT_LMS_URL = "https://lms.iba.edu.pk/"
        const val DEFAULT_CUSTOM_PORTAL_URL = "https://iba.edu.pk/"
        const val OBSOLETE_PORTAL_HOST = "portal.iba.edu.pk"

        fun isInvalidOrObsoletePortalUrl(url: String?): Boolean {
            if (url.isNullOrBlank()) return true
            val trimmed = url.trim()
            if (trimmed.contains(OBSOLETE_PORTAL_HOST, ignoreCase = true)) return true
            if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) return true
            val withoutScheme = if (trimmed.startsWith("https://", ignoreCase = true)) {
                trimmed.substring(8).trim()
            } else {
                trimmed.substring(7).trim()
            }
            val hostPart = withoutScheme.substringBefore("/").substringBefore("?").substringBefore("#").trim()
            if (hostPart.isBlank() || hostPart.contains(" ") || !hostPart.any { it.isLetterOrDigit() }) return true
            return false
        }

        fun normalizePortalUrl(url: String?, defaultUrl: String = DEFAULT_UMS_URL): String {
            if (url.isNullOrBlank()) return defaultUrl
            var trimmed = url.trim()
            if (trimmed.contains(OBSOLETE_PORTAL_HOST, ignoreCase = true)) {
                return defaultUrl
            }
            if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                trimmed = "https://$trimmed"
            }
            val withoutScheme = if (trimmed.startsWith("https://", ignoreCase = true)) {
                trimmed.substring(8).trim()
            } else {
                trimmed.substring(7).trim()
            }
            val hostPart = withoutScheme.substringBefore("/").substringBefore("?").substringBefore("#").trim()
            if (hostPart.isBlank() || hostPart.contains(" ") || !hostPart.any { it.isLetterOrDigit() }) {
                return defaultUrl
            }
            return trimmed
        }
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
    val portalUrlFlow: Flow<String> = context.dataStore.data.map {
        val saved = it[Keys.PORTAL_URL]
        if (isInvalidOrObsoletePortalUrl(saved)) {
            DEFAULT_UMS_URL
        } else {
            saved!!
        }
    }
    val lmsUrlFlow: Flow<String> = context.dataStore.data.map {
        val saved = it[Keys.LMS_URL]
        if (isInvalidOrObsoletePortalUrl(saved)) {
            DEFAULT_LMS_URL
        } else {
            saved!!
        }
    }
    val customPortalUrlFlow: Flow<String> = context.dataStore.data.map {
        val saved = it[Keys.CUSTOM_PORTAL_URL]
        if (isInvalidOrObsoletePortalUrl(saved)) {
            DEFAULT_CUSTOM_PORTAL_URL
        } else {
            saved!!
        }
    }
    val aiDisclosureShownFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.AI_DISCLOSURE_SHOWN] ?: false }
    val focusIbaOnlyFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.FOCUS_IBA_ONLY] ?: false }
    val nextClassWidgetEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.NEXT_CLASS_WIDGET_ENABLED] ?: true }
    val absenceTrackerEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.ABSENCE_TRACKER_ENABLED] ?: true }
    val morningBriefingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.MORNING_BRIEFING_ENABLED] ?: true }
    val deadlineAlarmEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.DEADLINE_ALARM_ENABLED] ?: true }
    val offlineAiEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.OFFLINE_AI_ENABLED] ?: true }
    val campusIdFlow: Flow<String> = context.dataStore.data.map { it[Keys.CAMPUS_ID] ?: com.khabar.app.config.CampusConfig.DEFAULT_CAMPUS_ID }
    val morningBriefingTimeFlow: Flow<String> = context.dataStore.data.map { it[Keys.MORNING_BRIEFING_TIME] ?: "07:30" }
    val deadlineAlarmLeadMinutesFlow: Flow<Int> = context.dataStore.data.map { it[Keys.DEADLINE_ALARM_LEAD_MINUTES] ?: 120 }

    suspend fun setNextClassWidgetEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NEXT_CLASS_WIDGET_ENABLED] = enabled }
    }

    suspend fun setAbsenceTrackerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ABSENCE_TRACKER_ENABLED] = enabled }
    }

    suspend fun setMorningBriefingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MORNING_BRIEFING_ENABLED] = enabled }
    }

    suspend fun setMorningBriefingTime(timeStr: String) {
        context.dataStore.edit { it[Keys.MORNING_BRIEFING_TIME] = timeStr }
    }

    suspend fun setDeadlineAlarmEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEADLINE_ALARM_ENABLED] = enabled }
    }

    suspend fun setDeadlineAlarmLeadMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEADLINE_ALARM_LEAD_MINUTES] = minutes }
    }

    suspend fun setOfflineAiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_AI_ENABLED] = enabled }
    }

    suspend fun setCampusId(campusId: String) {
        context.dataStore.edit { it[Keys.CAMPUS_ID] = campusId }
    }

    suspend fun setFocusIbaOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FOCUS_IBA_ONLY] = enabled }
    }

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
        val clean = normalizePortalUrl(url, DEFAULT_UMS_URL)
        context.dataStore.edit { it[Keys.PORTAL_URL] = clean }
    }

    suspend fun setLmsUrl(url: String) {
        val clean = normalizePortalUrl(url, DEFAULT_LMS_URL)
        context.dataStore.edit { it[Keys.LMS_URL] = clean }
    }

    suspend fun setCustomPortalUrl(url: String) {
        val clean = normalizePortalUrl(url, DEFAULT_CUSTOM_PORTAL_URL)
        context.dataStore.edit { it[Keys.CUSTOM_PORTAL_URL] = clean }
    }

    suspend fun migrateInvalidPortalUrls() {
        context.dataStore.edit { prefs ->
            val portal = prefs[Keys.PORTAL_URL]
            if (portal != null && isInvalidOrObsoletePortalUrl(portal)) {
                prefs[Keys.PORTAL_URL] = DEFAULT_UMS_URL
            }
            val lms = prefs[Keys.LMS_URL]
            if (lms != null && isInvalidOrObsoletePortalUrl(lms)) {
                prefs[Keys.LMS_URL] = DEFAULT_LMS_URL
            }
            val custom = prefs[Keys.CUSTOM_PORTAL_URL]
            if (custom != null && isInvalidOrObsoletePortalUrl(custom)) {
                prefs[Keys.CUSTOM_PORTAL_URL] = DEFAULT_CUSTOM_PORTAL_URL
            }
        }
    }

    suspend fun setAiDisclosureShown(shown: Boolean) {
        context.dataStore.edit { it[Keys.AI_DISCLOSURE_SHOWN] = shown }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
