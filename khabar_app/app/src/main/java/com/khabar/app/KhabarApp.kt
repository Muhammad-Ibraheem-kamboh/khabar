package com.khabar.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.khabar.app.config.AppConfig
import com.khabar.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class KhabarApp : Application() {

    lateinit var container: AppContainer
        private set

    val appContainer: AppContainer get() = container

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        CoroutineScope(Dispatchers.IO).launch {
            container.channelsManager.initializeChannels()
            container.mailRepository.clearSampleMails()
        }

        // Periodic WorkManager sync every 15 minutes
        val syncRequest = PeriodicWorkRequestBuilder<MailSyncWorker>(
            AppConfig.SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "khabar_periodic_mail_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    class MailSyncWorker(
        context: android.content.Context,
        workerParams: WorkerParameters
    ) : Worker(context, workerParams) {
        override fun doWork(): Result {
            val app = applicationContext as KhabarApp
            return try {
                kotlinx.coroutines.runBlocking {
                    app.container.mailRepository.syncAll()
                }
                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.retry()
            }
        }
    }
}
