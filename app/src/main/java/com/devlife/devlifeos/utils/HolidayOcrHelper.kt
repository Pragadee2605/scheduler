package com.devlife.devlifeos.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.devlife.devlifeos.data.local.entity.Holiday
import com.devlife.devlifeos.data.local.entity.HolidayType
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.*

/**
 * Helper class to extract holiday dates from calendar images using ML Kit OCR
 * Enhanced with comprehensive logging and multiple date pattern support
 */
class HolidayOcrHelper(private val context: Context) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val TAG = "HolidayOcrHelper"

    /**
     * Extract holidays from calendar image
     */
    suspend fun extractHolidaysFromImage(imageUri: Uri, year: Int = Calendar.getInstance().get(Calendar.YEAR)): List<Holiday> {
        return try {
            Log.d(TAG, "==================== OCR EXTRACTION START ====================")
            Log.d(TAG, "Year: $year")
            Log.d(TAG, "Image URI: $imageUri")

            // Load image from URI
            val image = InputImage.fromFilePath(context, imageUri)
            Log.d(TAG, "Image loaded: ${image.width}x${image.height} pixels")

            // Recognize text
            val visionText = textRecognizer.process(image).await()
            val extractedText = visionText.text

            Log.d(TAG, "==================== EXTRACTED TEXT ====================")
            Log.d(TAG, extractedText)
            Log.d(TAG, "========================================================")
            Log.d(TAG, "Text stats: ${extractedText.length} chars, ${extractedText.split("\n").size} lines")

            if (extractedText.isBlank()) {
                Log.e(TAG, "❌ NO TEXT EXTRACTED FROM IMAGE!")
                return emptyList()
            }

            // Parse holidays
            val holidays = parseHolidaysFromText(extractedText, year)
            Log.d(TAG, "✅ Successfully extracted ${holidays.size} holidays")
            holidays.forEachIndexed { index, holiday ->
                Log.d(TAG, "  ${index + 1}. ${holiday.name} → ${holiday.date}")
            }

            Log.d(TAG, "==================== OCR EXTRACTION END ====================")
            holidays
        } catch (e: IOException) {
            Log.e(TAG, "❌ IOException loading image", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error extracting holidays", e)
            emptyList()
        }
    }

    /**
     * Parse holidays from extracted text
     * Handles both inline format and table/column-based calendar layouts
     */
    private fun parseHolidaysFromText(text: String, defaultYear: Int): List<Holiday> {
        val holidays = mutableListOf<Holiday>()
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        Log.d(TAG, "Parsing ${lines.size} non-empty lines...")

        // Month mapping
        val monthMap = mapOf(
            "jan" to 1, "january" to 1,
            "feb" to 2, "february" to 2,
            "mar" to 3, "march" to 3,
            "apr" to 4, "april" to 4,
            "may" to 5,
            "jun" to 6, "june" to 6,
            "jul" to 7, "july" to 7,
            "aug" to 8, "august" to 8,
            "sep" to 9, "sept" to 9, "september" to 9,
            "oct" to 10, "october" to 10,
            "nov" to 11, "november" to 11,
            "dec" to 12, "december" to 12
        )

        // Day of week names (to filter out)
        val dayNames = setOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
                             "mon", "tue", "wed", "thu", "fri", "sat", "sun")

        // First pass: collect all dates with indices
        val datesWithIndices = mutableListOf<Pair<Int, Triple<Int, Int, Int>>>()
        for ((index, line) in lines.withIndex()) {
            val dateInfo = extractDateFromLine(line, defaultYear, monthMap)
            if (dateInfo != null) {
                datesWithIndices.add(index to dateInfo)
                Log.d(TAG, "Line $index: '$line' → DATE FOUND")
            } else {
                Log.d(TAG, "Line $index: '$line'")
            }
        }

        // Second pass: collect potential holiday names (lines without dates)
        val potentialNames = mutableListOf<Pair<Int, String>>()
        for ((index, line) in lines.withIndex()) {
            if (!containsDate(line, monthMap) && line.length >= 3) {
                val lower = line.lowercase().trim()

                // Skip headers, footers, and day names
                if (lower in listOf("holiday", "date", "day", "week", "day of the week", "month", "year",
                                    "s.no", "sr.no", "no.", "#", "chennai", "bangalore", "mumbai", "delhi")) {
                    continue
                }

                // Skip if it's just a day name
                if (lower in dayNames) {
                    continue
                }

                val cleaned = cleanHolidayName(line)
                if (cleaned.length >= 3 && cleaned.lowercase() !in dayNames) {
                    potentialNames.add(index to cleaned)
                    Log.d(TAG, "  → Potential name: '$cleaned'")
                }
            }
        }

        // Third pass: match names to dates using smart algorithm
        for ((dateIndex, dateInfo) in datesWithIndices) {
            val (day, month, year) = dateInfo
            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
            val dateLine = lines[dateIndex]

            // Strategy 1: Extract name from same line if date has extra text
            var bestName = extractHolidayName(dateLine, monthMap)
            if (bestName.length >= 5 && bestName.lowercase() !in dayNames) {
                Log.d(TAG, "  ✓ Inline: '$bestName' → $dateStr")
                holidays.add(Holiday(
                    name = bestName,
                    date = dateStr,
                    year = year,
                    month = month,
                    day = day,
                    type = HolidayType.COMPANY
                ))
                continue
            }

            // Strategy 2: Find closest meaningful name (skip day names)
            var selectedName: String? = null
            var selectedDistance = Int.MAX_VALUE

            for ((nameIndex, name) in potentialNames) {
                val distance = kotlin.math.abs(nameIndex - dateIndex)

                // Skip day of week names
                if (name.lowercase() in dayNames) {
                    continue
                }

                // Prefer names BEFORE dates (typical calendar layout)
                // Allow small distance for column-based layouts
                if (distance <= 5) {
                    // If equal distance, prefer the one before the date
                    if (distance < selectedDistance ||
                        (distance == selectedDistance && nameIndex < dateIndex)) {
                        selectedName = name
                        selectedDistance = distance
                    }
                }
            }

            if (selectedName != null) {
                Log.d(TAG, "  ✓ Matched: '$selectedName' → $dateStr (distance: $selectedDistance)")
                holidays.add(Holiday(
                    name = selectedName,
                    date = dateStr,
                    year = year,
                    month = month,
                    day = day,
                    type = HolidayType.COMPANY
                ))
                // Remove used name to avoid duplicates
                potentialNames.removeIf { it.second == selectedName }
            } else {
                // Last resort: use generic name
                val name = "Holiday on ${String.format(Locale.US, "%02d/%02d", day, month)}"
                Log.d(TAG, "  ⚠ No name found, using: $name")
                holidays.add(Holiday(
                    name = name,
                    date = dateStr,
                    year = year,
                    month = month,
                    day = day,
                    type = HolidayType.COMPANY
                ))
            }
        }

        return holidays.distinctBy { it.date }.sortedBy { it.date }
    }

    /**
     * Extract date from text line
     */
    private fun extractDateFromLine(line: String, defaultYear: Int, monthMap: Map<String, Int>): Triple<Int, Int, Int>? {
        // Pattern 1: DD/MM/YYYY, DD-MM-YYYY, DD.MM.YYYY
        Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})""").find(line)?.let {
            try {
                var day = it.groupValues[1].toInt()
                var month = it.groupValues[2].toInt()
                var year = it.groupValues[3].toInt()

                // Handle DD/MM vs MM/DD ambiguity - assume DD/MM for Indian format
                if (day > 12 && month <= 12) {
                    // day is definitely day
                } else if (month > 12 && day <= 12) {
                    // swap them
                    val temp = day
                    day = month
                    month = temp
                }

                if (year < 100) year += 2000

                if (isValidDate(day, month, year)) {
                    Log.d(TAG, "    → DD/MM/YYYY: $day/$month/$year")
                    return Triple(day, month, year)
                }
            } catch (_: Exception) {}
        }

        // Pattern 2: DD MMM YYYY or DD MMM
        Regex("""(\d{1,2})\s+([A-Za-z]{3,})\s*(\d{4})?""").find(line)?.let {
            try {
                val day = it.groupValues[1].toInt()
                val monthStr = it.groupValues[2].lowercase()
                val year = it.groupValues[3].takeIf { s -> s.isNotEmpty() }?.toInt() ?: defaultYear

                monthMap[monthStr]?.let { month ->
                    if (isValidDate(day, month, year)) {
                        Log.d(TAG, "    → DD MMM YYYY: $day $monthStr $year")
                        return Triple(day, month, year)
                    }
                }
            } catch (_: Exception) {}
        }

        // Pattern 3: Month DD, YYYY or Month DD
        Regex("""([A-Za-z]{3,})\s+(\d{1,2}),?\s*(\d{4})?""").find(line)?.let {
            try {
                val monthStr = it.groupValues[1].lowercase()
                val day = it.groupValues[2].toInt()
                val year = it.groupValues[3].takeIf { s -> s.isNotEmpty() }?.toInt() ?: defaultYear

                monthMap[monthStr]?.let { month ->
                    if (isValidDate(day, month, year)) {
                        Log.d(TAG, "    → Month DD YYYY: $monthStr $day $year")
                        return Triple(day, month, year)
                    }
                }
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * Extract holiday name from line
     */
    private fun extractHolidayName(line: String, monthMap: Map<String, Int>): String {
        var name = line

        // Remove date patterns
        name = name.replace(Regex("""\d{1,2}[/\-.\s]+\d{1,2}[/\-.\s]+\d{2,4}"""), "")
        name = name.replace(Regex("""\d{1,2}\s+[A-Za-z]{3,}\s*\d{0,4}"""), "")
        name = name.replace(Regex("""[A-Za-z]{3,}\s+\d{1,2},?\s*\d{0,4}"""), "")
        name = name.replace(Regex("""\b20\d{2}\b"""), "")

        // Remove day names
        val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "mon", "tue", "wed", "thu", "fri", "sat", "sun")
        days.forEach { day ->
            name = name.replace(Regex("""\b$day\b""", RegexOption.IGNORE_CASE), "")
        }

        return cleanHolidayName(name)
    }

    /**
     * Clean holiday name - remove special characters, numbers, symbols
     */
    private fun cleanHolidayName(name: String): String {
        return name
            // Remove special symbols often found in calendars (*, #, ©, ®, etc.)
            .replace(Regex("""[*#©®™@$%^&()+=\[\]{}|\\/<>~`]"""), " ")
            // Remove numbers
            .replace(Regex("""\d+"""), " ")
            // Keep only letters, spaces, hyphens, apostrophes
            .replace(Regex("""[^\w\s'-]"""), " ")
            // Normalize whitespace
            .replace(Regex("""\s+"""), " ")
            .trim()
            // Filter out single character words
            .split(" ")
            .filter { it.length > 1 }
            // Capitalize properly
            .joinToString(" ") {
                word -> word.lowercase().replaceFirstChar { c -> c.uppercase() }
            }
    }

    /**
     * Check if line contains date
     */
    private fun containsDate(line: String, monthMap: Map<String, Int>): Boolean {
        if (Regex("""\d{1,2}[/\-.]""").containsMatchIn(line)) return true
        return monthMap.keys.any { line.lowercase().contains(it) }
    }

    /**
     * Validate date
     */
    private fun isValidDate(day: Int, month: Int, year: Int): Boolean {
        if (day !in 1..31 || month !in 1..12 || year !in 2020..2100) return false

        val daysInMonth = when (month) {
            2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        return day <= daysInMonth
    }

    fun close() {
        textRecognizer.close()
    }
}
