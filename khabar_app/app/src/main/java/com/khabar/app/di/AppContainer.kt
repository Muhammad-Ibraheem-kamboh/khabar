package com.khabar.app.di

import android.content.Context
import com.khabar.app.ai.AiSummarizer
import com.khabar.app.ai.KeystoreSecretManager
import com.khabar.app.ai.OfflineSummarizer
import com.khabar.app.ai.OpenAiCompatibleClient
import com.khabar.app.data.local.KhabarDatabase
import com.khabar.app.data.local.UserPreferences
import com.khabar.app.data.repository.MailRepository
import com.khabar.app.data.repository.ProfileRepository
import com.khabar.app.data.repository.ReminderRepository
import com.khabar.app.notifications.KhabarNotificationManager
import com.khabar.app.notifications.NotificationChannelsManager
import com.khabar.app.rules.RulesEngine
import com.khabar.app.sources.DemoMailSource
import com.khabar.app.sources.GmailMailSource
import com.khabar.app.sources.InstantMailSource
import com.khabar.app.sources.MailSource
import com.khabar.app.sources.OutlookMailSource

class AppContainer(val context: Context) {

    val database: KhabarDatabase by lazy { KhabarDatabase.getInstance(context) }
    val userPreferences: UserPreferences by lazy { UserPreferences(context) }

    val keystoreSecretManager: KeystoreSecretManager by lazy { KeystoreSecretManager(context) }
    val secretManager: KeystoreSecretManager get() = keystoreSecretManager
    val openAiClient: OpenAiCompatibleClient by lazy { OpenAiCompatibleClient() }
    val aiSummarizer: AiSummarizer by lazy { AiSummarizer(openAiClient, keystoreSecretManager) }
    val offlineSummarizer: OfflineSummarizer by lazy { OfflineSummarizer(context) }

    val rulesEngine: RulesEngine by lazy { RulesEngine() }

    val channelsManager: NotificationChannelsManager by lazy {
        NotificationChannelsManager(context, database.feedbackDao())
    }

    val notificationManager: KhabarNotificationManager by lazy {
        KhabarNotificationManager(context, channelsManager)
    }

    val demoMailSource: DemoMailSource by lazy { DemoMailSource(context) }
    val instantMailSource: InstantMailSource by lazy { InstantMailSource() }
    val outlookMailSource: OutlookMailSource by lazy { OutlookMailSource(context) }
    val gmailMailSource: GmailMailSource by lazy { GmailMailSource(context) }

    val mailSources: List<MailSource> by lazy {
        listOf(demoMailSource, instantMailSource, outlookMailSource, gmailMailSource)
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(
            profileDao = database.profileDao(),
            courseDao = database.courseDao(),
            interestDao = database.interestDao()
        )
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(reminderDao = database.reminderDao())
    }

    val mailRepository: MailRepository by lazy {
        MailRepository(
            mailDao = database.mailDao(),
            feedbackDao = database.feedbackDao(),
            profileRepository = profileRepository,
            rulesEngine = rulesEngine,
            offlineSummarizer = offlineSummarizer,
            notificationManager = notificationManager,
            mailSources = mailSources
        )
    }
}
