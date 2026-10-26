package com.letdigitalfly.ibamail.util

import com.letdigitalfly.ibamail.domain.model.SummaryLanguage
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    fun formatCountdown(
        targetEpochMs: Long,
        language: SummaryLanguage = SummaryLanguage.ENGLISH,
        nowEpochMs: Long = System.currentTimeMillis()
    ): String {
        val diffMs = targetEpochMs - nowEpochMs
        if (diffMs <= 0) {
            return when (language) {
                SummaryLanguage.ENGLISH -> "Due now"
                SummaryLanguage.ROMAN_URDU -> "Waqt khatam"
                SummaryLanguage.URDU -> "وقت ختم"
            }
        }

        val totalMinutes = diffMs / (1000 * 60)
        val totalHours = totalMinutes / 60
        val days = totalHours / 24
        val remainingHours = totalHours % 24
        val remainingMinutes = totalMinutes % 60

        return when (language) {
            SummaryLanguage.ENGLISH -> {
                when {
                    days > 0 -> "$days days ${remainingHours}h left"
                    totalHours > 0 -> "$totalHours hours ${remainingMinutes}m left"
                    else -> "$totalMinutes minutes left"
                }
            }
            SummaryLanguage.ROMAN_URDU -> {
                when {
                    days > 0 -> "$days din $remainingHours ghante baqi"
                    totalHours > 0 -> "$totalHours ghante baqi"
                    else -> "$totalMinutes minute baqi"
                }
            }
            SummaryLanguage.URDU -> {
                when {
                    days > 0 -> "$days دن $remainingHours گھنٹے باقی"
                    totalHours > 0 -> "$totalHours گھنٹے باقی"
                    else -> "$totalMinutes منٹ باقی"
                }
            }
        }
    }

    fun formatRelativeTime(
        epochMs: Long,
        nowEpochMs: Long = System.currentTimeMillis()
    ): String {
        val diffMs = nowEpochMs - epochMs
        val minutes = diffMs / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        val zoneId = ZoneId.systemDefault()
        val mailDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zoneId)
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

        return when {
            minutes < 2 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> mailDate.format(timeFormatter)
            days == 1L -> "Yesterday"
            days < 7 -> mailDate.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH))
            else -> mailDate.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
        }
    }

    fun formatTodayDate(nowEpochMs: Long = System.currentTimeMillis()): String {
        val date = LocalDate.ofInstant(Instant.ofEpochMilli(nowEpochMs), ZoneId.systemDefault())
        return date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))
    }

    fun formatRelative(epochMs: Long): String = formatRelativeTime(epochMs)

    fun formatFullDate(epochMs: Long): String {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault())
        return dateTime.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy 'at' h:mm a", Locale.ENGLISH))
    }
}
