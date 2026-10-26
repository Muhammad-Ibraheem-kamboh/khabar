package com.letdigitalfly.ibamail.di

import android.content.Context
import com.letdigitalfly.ibamail.ai.AiSummarizer
import com.letdigitalfly.ibamail.ai.KeystoreSecretManager
import com.letdigitalfly.ibamail.ai.OfflineSummarizer
import com.letdigitalfly.ibamail.ai.OpenAiCompatibleClient
import com.letdigitalfly.ibamail.data.local.KhabarDatabase
import com.letdigitalfly.ibamail.data.local.UserPreferences
import com.letdigitalfly.ibamail.data.repository.MailRepository
import com.letdigitalfly.ibamail.data.repository.ProfileRepository
import com.letdigitalfly.ibamail.data.repository.ReminderRepository
import com.letdigitalfly.ibamail.notifications.KhabarNotificationManager
import com.letdigitalfly.ibamail.notifications.NotificationChannelsManager
import com.letdigitalfly.ibamail.rules.RulesEngine
import com.letdigitalfly.ibamail.sources.DemoMailSource
import com.letdigitalfly.ibamail.sources.GmailMailSource
import com.letdigitalfly.ibamail.sources.InstantMailSource
import com.letdigitalfly.ibamail.sources.MailSource
import com.letdigitalfly.ibamail.sources.OutlookMailSource

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
