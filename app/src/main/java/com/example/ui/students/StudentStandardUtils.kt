package com.example.ui.students

/**
 * Standard / Class and Section utilities for Student Registration and Filtering (Phase C).
 */
object StudentStandardUtils {

    val STANDARDS = listOf(
        "1st",
        "2nd",
        "3rd",
        "4th",
        "5th",
        "6th",
        "7th",
        "8th",
        "9th",
        "10th"
    )

    val SECTIONS = listOf(
        "A",
        "B",
        "C",
        "D",
        "E",
        "F"
    )

    val FILTER_STANDARDS = listOf(
        "सर्व",
        "1st",
        "2nd",
        "3rd",
        "4th",
        "5th",
        "6th",
        "7th",
        "8th",
        "9th",
        "10th"
    )

    fun getStandardLabel(std: String): String = when (std) {
        "1st" -> "1st (इयत्ता १ ली)"
        "2nd" -> "2nd (इयत्ता २ री)"
        "3rd" -> "3rd (इयत्ता ३ री)"
        "4th" -> "4th (इयत्ता ४ थी)"
        "5th" -> "5th (इयत्ता ५ वी)"
        "6th" -> "6th (इयत्ता ६ वी)"
        "7th" -> "7th (इयत्ता ७ वी)"
        "8th" -> "8th (इयत्ता ८ वी)"
        "9th" -> "9th (इयत्ता ९ वी)"
        "10th" -> "10th (इयत्ता १० वी)"
        "सर्व", "All" -> "सर्व (All)"
        else -> std
    }

    fun getFilterChipLabel(std: String): String = when (std) {
        "1st" -> "1st (१ ली)"
        "2nd" -> "2nd (२ री)"
        "3rd" -> "3rd (३ री)"
        "4th" -> "4th (४ थी)"
        "5th" -> "5th (५ वी)"
        "6th" -> "6th (६ वी)"
        "7th" -> "7th (७ वी)"
        "8th" -> "8th (८ वी)"
        "9th" -> "9th (९ वी)"
        "10th" -> "10th (१० वी)"
        "सर्व", "All" -> "सर्व (All)"
        else -> std
    }

    fun getSectionLabel(section: String): String = when (section.trim().uppercase()) {
        "A" -> "तुकडी A (Section A)"
        "B" -> "तुकडी B (Section B)"
        "C" -> "तुकडी C (Section C)"
        "D" -> "तुकडी D (Section D)"
        "E" -> "तुकडी E (Section E)"
        "F" -> "तुकडी F (Section F)"
        else -> "तुकडी $section"
    }

    /**
     * Combines Standard and Section into clean, canonical stored format.
     * Example: standard="10th", section="A" -> "10th - A"
     */
    fun formatStoredStandard(standard: String, section: String?): String {
        val cleanStd = standard.trim()
        val cleanSec = section?.trim()?.uppercase()
        return if (!cleanSec.isNullOrBlank() && cleanSec != "NONE") {
            "$cleanStd - $cleanSec"
        } else {
            cleanStd
        }
    }

    /**
     * Parses the Standard (1st..10th) from any raw string format.
     */
    fun parseStandard(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val lower = raw.lowercase().trim()

        return when {
            lower.contains("10th") || lower.contains("१०") || lower.contains("class 10") || lower.contains("10-") || lower.contains("10 ") || lower == "10" -> "10th"
            lower.contains("9th") || lower.contains("९") || lower.contains("class 9") || lower.contains("9-") || lower.contains("9 ") || lower == "9" -> "9th"
            lower.contains("8th") || lower.contains("८") || lower.contains("class 8") || lower.contains("8-") || lower.contains("8 ") || lower == "8" -> "8th"
            lower.contains("7th") || lower.contains("७") || lower.contains("class 7") || lower.contains("7-") || lower.contains("7 ") || lower == "7" -> "7th"
            lower.contains("6th") || lower.contains("६") || lower.contains("class 6") || lower.contains("6-") || lower.contains("6 ") || lower == "6" -> "6th"
            lower.contains("5th") || lower.contains("५") || lower.contains("class 5") || lower.contains("5-") || lower.contains("5 ") || lower == "5" -> "5th"
            lower.contains("4th") || lower.contains("४") || lower.contains("class 4") || lower.contains("4-") || lower.contains("4 ") || lower == "4" -> "4th"
            lower.contains("3rd") || lower.contains("३") || lower.contains("class 3") || lower.contains("3-") || lower.contains("3 ") || lower == "3" -> "3rd"
            lower.contains("2nd") || lower.contains("२") || lower.contains("class 2") || lower.contains("2-") || lower.contains("2 ") || lower == "2" -> "2nd"
            lower.contains("1st") || lower.contains("१") || lower.contains("class 1") || lower.contains("1-") || lower.contains("1 ") || lower == "1" -> "1st"
            else -> raw.trim()
        }
    }

    /**
     * Parses Section (A..F) from any raw string format.
     */
    fun parseSection(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val upper = raw.uppercase().trim()

        // 1. Look for patterns like "- A", " 10-A", " 10 A", "(Class 10-A)", "/ A"
        val regex = Regex("""(?:-|–|/|\(|\s)([A-F])(?:\)|-|\s|$)""")
        val match = regex.find(upper)
        if (match != null) {
            val candidate = match.groupValues[1]
            // Guard against match with "1ST", "2ND", "3RD", "TH"
            if (!upper.contains("${candidate}ST") && !upper.contains("${candidate}ND") && !upper.contains("${candidate}RD") && !upper.contains("${candidate}TH")) {
                return candidate
            }
        }

        // 2. Direct single letter
        if (upper.length == 1 && upper in listOf("A", "B", "C", "D", "E", "F")) {
            return upper
        }

        // 3. Marathi letters (अ->A, ब->B, क->C, ड->D, इ->E, फ->F)
        return when {
            raw.contains("अ") -> "A"
            raw.contains("ब") -> "B"
            raw.contains("क") -> "C"
            raw.contains("ड") -> "D"
            raw.contains("इ") -> "E"
            raw.contains("फ") -> "F"
            else -> null
        }
    }
}
