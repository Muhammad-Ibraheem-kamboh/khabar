package com.khabar.app.data.repository

import com.khabar.app.data.local.dao.ReminderDao
import com.khabar.app.data.local.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepository(
    private val reminderDao: ReminderDao
) {
    val allRemindersFlow: Flow<List<ReminderEntity>> = reminderDao.getAllRemindersFlow()
    val activeRemindersFlow: Flow<List<ReminderEntity>> = reminderDao.getAllRemindersFlow().map { list -> list.filter { !it.done } }
    val completedRemindersFlow: Flow<List<ReminderEntity>> = reminderDao.getAllRemindersFlow().map { list -> list.filter { it.done } }
    val nearestUpcomingReminderFlow: Flow<ReminderEntity?> = reminderDao.getNearestUpcomingReminderFlow(System.currentTimeMillis())

    suspend fun getReminderById(id: Long): ReminderEntity? = reminderDao.getReminderById(id)
    suspend fun getReminderByMailId(mailId: String): ReminderEntity? = reminderDao.getReminderByMailId(mailId)

    suspend fun insertReminder(reminder: ReminderEntity): Long {
        return reminderDao.insert(reminder)
    }

    suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.update(reminder)
    }

    suspend fun markReminderDone(id: Long, done: Boolean) {
        reminderDao.setDone(id, done)
    }

    suspend fun setReminderCompleted(id: Long, done: Boolean) {
        reminderDao.setDone(id, done)
    }

    suspend fun togglePinned(id: Long, currentPinned: Boolean) {
        reminderDao.setPinned(id, !currentPinned)
    }

    suspend fun deleteReminder(id: Long) {
        reminderDao.deleteById(id)
    }

    suspend fun deleteReminder(idString: String) {
        val id = idString.toLongOrNull() ?: return
        reminderDao.deleteById(id)
    }

    suspend fun clearAll() {
        reminderDao.clear()
    }
}
