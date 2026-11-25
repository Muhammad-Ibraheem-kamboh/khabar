package com.khabar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.khabar.app.data.local.entities.AiDefectLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiDefectLogDao {

    @Query("SELECT * FROM ai_defect_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<AiDefectLogEntity>>

    @Query("SELECT * FROM ai_defect_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<AiDefectLogEntity>

    @Query("SELECT * FROM ai_defect_logs WHERE type = :type ORDER BY timestamp DESC")
    fun getLogsByTypeFlow(type: String): Flow<List<AiDefectLogEntity>>

    @Query("SELECT * FROM ai_defect_logs WHERE type = :type ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogsByType(type: String, limit: Int): List<AiDefectLogEntity>

    @Query("SELECT COUNT(*) FROM ai_defect_logs")
    fun getLogCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AiDefectLogEntity): Long

    @Query("DELETE FROM ai_defect_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM ai_defect_logs")
    suspend fun clearAllLogs()
}
