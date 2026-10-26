package com.khabar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.khabar.app.data.local.entities.NotificationChannelVersionEntity
import com.khabar.app.data.local.entities.SenderOverrideEntity
import com.khabar.app.data.local.entities.UnknownCoursePromptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {

    // Overrides
    @Query("SELECT * FROM sender_overrides WHERE sender = :sender AND category = :category LIMIT 1")
    suspend fun getOverride(sender: String, category: String): SenderOverrideEntity?

    @Query("SELECT * FROM sender_overrides")
    suspend fun getAllOverrides(): List<SenderOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setOverride(override: SenderOverrideEntity)

    // Unknown Course Prompts
    @Query("SELECT * FROM unknown_course_prompts WHERE answered = 0 LIMIT 1")
    fun getPendingUnknownCoursePromptFlow(): Flow<UnknownCoursePromptEntity?>

    @Query("SELECT * FROM unknown_course_prompts WHERE code = :code LIMIT 1")
    suspend fun getPromptForCourse(code: String): UnknownCoursePromptEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPrompt(prompt: UnknownCoursePromptEntity)

    @Query("UPDATE unknown_course_prompts SET answered = 1 WHERE code = :code")
    suspend fun markPromptAnswered(code: String)

    // Channel Versions
    @Query("SELECT * FROM notification_channel_versions WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getChannelVersion(categoryId: String): NotificationChannelVersionEntity?

    @Query("SELECT * FROM notification_channel_versions")
    suspend fun getAllChannelVersions(): List<NotificationChannelVersionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChannelVersion(entity: NotificationChannelVersionEntity)

    @Query("DELETE FROM sender_overrides")
    suspend fun clearOverrides()

    @Query("DELETE FROM unknown_course_prompts")
    suspend fun clearPrompts()

    @Query("DELETE FROM notification_channel_versions")
    suspend fun clearChannelVersions()
}
