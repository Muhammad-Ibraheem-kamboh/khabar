package com.khabar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.khabar.app.data.local.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

data class ChatSessionInfo(
    val sessionId: String,
    val sessionTitle: String,
    val lastTimestamp: Long
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<ChatMessageEntity>>

    @Query("SELECT sessionId, sessionTitle, MAX(timestamp) as lastTimestamp FROM chat_messages GROUP BY sessionId ORDER BY lastTimestamp DESC")
    fun getAllSessionsFlow(): Flow<List<ChatSessionInfo>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(sessionId: String, limit: Int = 30): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesLegacy(limit: Int = 30): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET sessionTitle = :newTitle WHERE sessionId = :sessionId")
    suspend fun renameSession(sessionId: String, newTitle: String)

    @Query("SELECT sessionTitle FROM chat_messages WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionTitle(sessionId: String): String?

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}
