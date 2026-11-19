package com.khabar.app.util

import java.time.LocalDate

data class SemesterCalculationResult(
    val semesterIndex: Int,
    val standing: String,
    val currentTerm: String,
    val currentYear: Int
)

object SemesterCalculator {

    fun calculate(
        joinTerm: String, // "Fall" or "Spring"
        joinYear: Int,
        isGraduate: Boolean = false,
        referenceDate: LocalDate = LocalDate.now()
    ): SemesterCalculationResult {
        val currentYear = referenceDate.year
        val currentMonth = referenceDate.monthValue // 1 to 12

        // Terms: Fall (Aug to Dec), Spring (Jan to May). Jun/Jul count as post-Spring.
        val currentTerm = if (currentMonth in 8..12) "Fall" else "Spring"

        // Count Fall and Spring terms from join to current inclusive
        var count = 0
        var term = if (joinTerm.equals("Spring", ignoreCase = true)) "Spring" else "Fall"
        var year = joinYear

        val targetYear = currentYear
        val targetTerm = currentTerm

        // Term progression loop
        while (year < targetYear || (year == targetYear && (term == targetTerm || (term == "Spring" && targetTerm == "Fall")))) {
            count++
            if (year == targetYear && term == targetTerm) {
                break
            }
            if (term == "Spring") {
                term = "Fall"
            } else {
                term = "Spring"
                year++
            }
        }

        val semesterIndex = count.coerceIn(1, 12)
        val standing = getStanding(semesterIndex, isGraduate)

        return SemesterCalculationResult(
            semesterIndex = semesterIndex,
            standing = standing,
            currentTerm = currentTerm,
            currentYear = currentYear
        )
    }

    fun calculateStanding(
        joinTerm: String,
        joinYear: Int,
        currentYear: Int = 2024,
        currentMonth: Int = 10
    ): String {
        return calculate(
            joinTerm = joinTerm,
            joinYear = joinYear,
            isGraduate = false,
            referenceDate = LocalDate.of(currentYear, currentMonth, 1)
        ).standing
    }

    fun getStanding(semesterIndex: Int, isGraduate: Boolean = false): String {
        return if (!isGraduate) {
            when (semesterIndex) {
                1, 2 -> "Freshman"
                3, 4 -> "Sophomore"
                5, 6 -> "Junior"
                else -> "Senior"
            }
        } else {
            val yearNumber = ((semesterIndex - 1) / 2) + 1
            "Year $yearNumber"
        }
    }
}
