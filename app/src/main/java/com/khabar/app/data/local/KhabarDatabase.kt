package com.khabar.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.khabar.app.data.local.dao.CourseDao
import com.khabar.app.data.local.dao.FeedbackDao
import com.khabar.app.data.local.dao.InterestDao
import com.khabar.app.data.local.dao.MailDao
import com.khabar.app.data.local.dao.ProfileDao
import com.khabar.app.data.local.dao.ReminderDao
import com.khabar.app.data.local.entities.CourseEntity
import com.khabar.app.data.local.entities.InterestEntity
import com.khabar.app.data.local.entities.MailEntity
import com.khabar.app.data.local.entities.NotificationChannelVersionEntity
import com.khabar.app.data.local.entities.ProfileEntity
import com.khabar.app.data.local.entities.ReminderEntity
import com.khabar.app.data.local.entities.SenderOverrideEntity
import com.khabar.app.data.local.entities.UnknownCoursePromptEntity

@Database(
    entities = [
        ProfileEntity::class,
        CourseEntity::class,
        InterestEntity::class,
        MailEntity::class,
        ReminderEntity::class,
        SenderOverrideEntity::class,
        UnknownCoursePromptEntity::class,
        NotificationChannelVersionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KhabarDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun courseDao(): CourseDao
    abstract fun interestDao(): InterestDao
    abstract fun mailDao(): MailDao
    abstract fun reminderDao(): ReminderDao
    abstract fun feedbackDao(): FeedbackDao

    companion object {
        @Volatile
        private var INSTANCE: KhabarDatabase? = null

        fun getInstance(context: Context): KhabarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KhabarDatabase::class.java,
                    "khabar.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
