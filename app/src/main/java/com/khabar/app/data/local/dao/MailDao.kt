package com.khabar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.khabar.app.data.local.entities.MailEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MailDao {
    @Query("SELECT * FROM mails ORDER BY receivedAt DESC")
    fun getAllMailsFlow(): Flow<List<MailEntity>>

    @Query("SELECT * FROM mails WHERE bucket = :bucket ORDER BY receivedAt DESC")
    fun getMailsByBucketFlow(bucket: String): Flow<List<MailEntity>>

    @Query("SELECT * FROM mails WHERE (bucket = 'IMPORTANT' OR bucket = 'GOOD_TO_KNOW') AND read = 0 ORDER BY CASE WHEN bucket = 'IMPORTANT' THEN 1 ELSE 2 END, receivedAt DESC LIMIT 5")
    fun getTopImportantUnreadFlow(): Flow<List<MailEntity>>

    @Query("SELECT * FROM mails WHERE id = :id LIMIT 1")
    fun getMailByIdFlow(id: String): Flow<MailEntity?>

    @Query("SELECT * FROM mails WHERE id = :id LIMIT 1")
    suspend fun getMailById(id: String): MailEntity?

    @Query("SELECT * FROM mails WHERE receivedAt >= :sinceEpochMs ORDER BY receivedAt DESC")
    suspend fun getMailsSince(sinceEpochMs: Long): List<MailEntity>

    @Query("SELECT * FROM mails ORDER BY receivedAt DESC LIMIT :limit")
    suspend fun getRecentMailsDirect(limit: Int = 30): List<MailEntity>

    @Query("SELECT * FROM mails WHERE (senderEmail LIKE '%@iba.edu.pk' OR sender LIKE '%IBA%' OR subject LIKE '%IBA%') AND bucket = :bucket ORDER BY receivedAt DESC")
    fun getIbaMailsByBucketFlow(bucket: String): Flow<List<MailEntity>>

    @Query("SELECT * FROM mails WHERE (senderEmail LIKE '%@iba.edu.pk' OR sender LIKE '%IBA%' OR subject LIKE '%IBA%') ORDER BY receivedAt DESC")
    fun getIbaAllMailsFlow(): Flow<List<MailEntity>>

    @Query("SELECT COUNT(*) FROM mails WHERE receivedAt >= :sinceEpochMs")
    fun getTodayReceivedCountFlow(sinceEpochMs: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM mails WHERE receivedAt >= :sinceEpochMs AND bucket IN ('IMPORTANT', 'GOOD_TO_KNOW')")
    fun getTodayForYouCountFlow(sinceEpochMs: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM mails WHERE receivedAt >= :sinceEpochMs AND bucket = 'HIDDEN'")
    fun getTodayHiddenCountFlow(sinceEpochMs: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM mails WHERE bucket = 'IMPORTANT' AND read = 0")
    fun getUnreadImportantCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(mail: MailEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(mail: MailEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mails: List<MailEntity>)

    @Update
    suspend fun update(mail: MailEntity)

    @Query("UPDATE mails SET read = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE mails SET bucket = :bucket, overrideBucket = :bucket WHERE id = :id")
    suspend fun updateBucket(id: String, bucket: String)

    @Query("DELETE FROM mails WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM mails WHERE sourceId = 'demo_source' OR id LIKE 'demo_%' OR id LIKE 'mail_%'")
    suspend fun deleteDemoMails()

    @Query("DELETE FROM mails")
    suspend fun clear()
}
