package com.khabar.app.data.local.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val firstName: String = "",
    val summaryLanguage: String = "en",
    val programId: String = "bba",
    val programName: String = "BBA",
    val level: String = "Undergraduate",
    val joinTerm: String = "Fall",
    val joinYear: Int = 2024,
    val semesterIndex: Int = 5,
    val semesterOverride: Boolean = false,
    val standing: String = "Junior",
    val campus: String = "Main",
    val shift: String? = null,
    val emBatch: String? = null,
    val major: String? = null,
    val portalUrl: String = "",
    val digestEnabled: Boolean = true,
    val digestTime: String = "07:30",
    val quietDuringClass: Boolean = false,
    val notifyGoodToKnow: Boolean = false,
    val useAiForUnclear: Boolean = false,
    val fastMode: Boolean = false,
    val instantMode: Boolean = false,
    val onboardingDone: Boolean = false
) {
    @get:Ignore
    val programCode: String get() = programName

    @get:Ignore
    val lastName: String get() = ""
}
