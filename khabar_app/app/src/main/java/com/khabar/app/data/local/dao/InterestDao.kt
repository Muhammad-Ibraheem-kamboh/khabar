package com.khabar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.khabar.app.data.local.entities.InterestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterestDao {
    @Query("SELECT * FROM interests")
    fun getAllInterestsFlow(): Flow<List<InterestEntity>>

    @Query("SELECT * FROM interests")
    suspend fun getAllInterests(): List<InterestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(interest: InterestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(interests: List<InterestEntity>)

    @Query("UPDATE interests SET enabled = :enabled WHERE `key` = :key")
    suspend fun setEnabled(key: String, enabled: Boolean)

    @Query("DELETE FROM interests")
    suspend fun clear()
}
