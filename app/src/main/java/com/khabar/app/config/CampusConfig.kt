package com.khabar.app.config

/**
 * Generic multi-campus configuration model.
 * Provides default configuration for IBA Karachi while allowing seamless expansion
 * to other universities (FAST, NUST, LUMS, KU, etc.) without code fragmentation.
 */
data class CampusProfile(
    val id: String,
    val name: String,
    val shortName: String,
    val portalName: String,
    val defaultPortalUrl: String,
    val defaultLmsUrl: String,
    val maxAbsencesAllowed: Int = 5
)

object CampusConfig {
    const val DEFAULT_CAMPUS_ID = "iba"

    val SUPPORTED_CAMPUSES = listOf(
        CampusProfile(
            id = "iba",
            name = "IBA Karachi",
            shortName = "IBA",
            portalName = "UMS / Sakai LMS",
            defaultPortalUrl = "https://ums.iba.edu.pk/",
            defaultLmsUrl = "https://lms.iba.edu.pk/",
            maxAbsencesAllowed = 5
        ),
        CampusProfile(
            id = "fast",
            name = "FAST NUCES",
            shortName = "FAST",
            portalName = "Flex / Slate LMS",
            defaultPortalUrl = "https://flex.nu.edu.pk/",
            defaultLmsUrl = "https://slate.nu.edu.pk/",
            maxAbsencesAllowed = 6
        ),
        CampusProfile(
            id = "nust",
            name = "NUST Islamabad",
            shortName = "NUST",
            portalName = "Qalam Portal / LMS",
            defaultPortalUrl = "https://qalam.nust.edu.pk/",
            defaultLmsUrl = "https://lms.nust.edu.pk/",
            maxAbsencesAllowed = 6
        ),
        CampusProfile(
            id = "lums",
            name = "LUMS Lahore",
            shortName = "LUMS",
            portalName = "Zambeel / Canvas LMS",
            defaultPortalUrl = "https://zambeel.lums.edu.pk/",
            defaultLmsUrl = "https://canvas.lums.edu.pk/",
            maxAbsencesAllowed = 5
        ),
        CampusProfile(
            id = "generic",
            name = "General Campus",
            shortName = "Campus",
            portalName = "Student Portal / LMS",
            defaultPortalUrl = "https://portal.university.edu/",
            defaultLmsUrl = "https://lms.university.edu/",
            maxAbsencesAllowed = 5
        )
    )

    fun getCampusById(id: String?): CampusProfile {
        return SUPPORTED_CAMPUSES.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: SUPPORTED_CAMPUSES.firstOrNull { it.name.contains(id ?: "", ignoreCase = true) }
            ?: SUPPORTED_CAMPUSES.first()
    }
}
