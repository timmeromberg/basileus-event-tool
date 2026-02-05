package com.basileus.eventtool.storage

import com.basileus.eventtool.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class EventLoader(private val config: EventToolStorageConfig) {

    suspend fun loadAllEvents(): List<EventNode> = withContext(Dispatchers.IO) {
        val events = mutableListOf<EventNode>()
        val eventsDir = config.eventsPath.toFile()

        if (!eventsDir.exists()) {
            println("Events directory not found: ${eventsDir.absolutePath}")
            return@withContext events
        }

        // Walk through all subdirectories (crisis, situation, opportunity, narrative)
        eventsDir.walkTopDown()
            .filter { it.isFile && it.extension == "toml" }
            .forEach { file ->
                try {
                    parseEventFile(file)?.let { events.add(it) }
                } catch (e: Exception) {
                    println("Error parsing ${file.name}: ${e.message}")
                }
            }

        println("Loaded ${events.size} events")
        events
    }

    private fun parseEventFile(file: File): EventNode? {
        val content = file.readText()
        val type = inferEventType(file)

        var id: String? = null
        var title: String? = null
        var tier: EventTier = EventTier.MINOR
        var minYear: Int? = null
        var maxYear: Int? = null
        var historicityScore = 100
        var location: String? = null
        val requiredOutcomes = mutableListOf<String>()
        val requiredOutcomesAny = mutableListOf<List<String>>()
        val forbiddenOutcomes = mutableListOf<String>()
        val producedOutcomes = mutableMapOf<String, MutableList<String>>()

        // Extract multi-line arrays using regex (only if array spans multiple lines)
        var requiredOutcomesAnyParsed = false
        val multiLineMatch = Regex("""required_outcomes_any\s*=\s*\[\s*\n([\s\S]*?)\]""").find(content)
        if (multiLineMatch != null) {
            // Multi-line format: parse the full content
            val fullMatch = Regex("""required_outcomes_any\s*=\s*(\[\s*\n[\s\S]*?\n\s*\])""").find(content)
            if (fullMatch != null) {
                val parsed = parseNestedArray(fullMatch.groupValues[1].replace("\n", "").replace(" ", ""))
                parsed.forEach { requiredOutcomesAny.add(it) }
                requiredOutcomesAnyParsed = true
            }
        }

        var currentSection = ""
        var currentResultLabel = ""

        content.lines().forEach { line ->
            val trimmed = line.trim()

            when {
                trimmed.startsWith("[event]") && !trimmed.startsWith("[event.") -> {
                    currentSection = "event"
                }
                trimmed.startsWith("[event.conditions]") -> {
                    currentSection = "conditions"
                }
                trimmed.startsWith("[[event.options.results]]") -> {
                    currentSection = "result"
                    currentResultLabel = ""
                }
                trimmed.startsWith("[[event.options]]") -> {
                    currentSection = "option"
                    currentResultLabel = "option_effect"  // Default label for direct option effects
                }
                trimmed.startsWith("[event.options.effects]") -> {
                    currentSection = "option_effects"
                }
                trimmed.contains("=") && currentSection.isNotEmpty() -> {
                    val eqIndex = trimmed.indexOf('=')
                    val key = trimmed.substring(0, eqIndex).trim()
                    val value = trimmed.substring(eqIndex + 1).trim()

                    when (currentSection) {
                        "event" -> when (key) {
                            "id" -> id = value.trim('"')
                            "title" -> title = value.trim('"')
                            "tier" -> tier = EventTier.fromString(value.trim('"')) ?: EventTier.MINOR
                            "historicity_score" -> historicityScore = value.toIntOrNull() ?: 100
                            "location" -> location = value.trim('"')
                            "year" -> {
                                // Narrative events use single 'year' field
                                val year = value.toIntOrNull()
                                if (year != null) {
                                    minYear = year
                                    maxYear = year
                                }
                            }
                        }
                        "conditions" -> when (key) {
                            "min_year" -> minYear = value.toIntOrNull()
                            "max_year" -> maxYear = value.toIntOrNull()
                            "required_outcomes" -> {
                                parseStringArray(value).forEach { requiredOutcomes.add(it) }
                            }
                            "forbidden_outcomes" -> {
                                parseStringArray(value).forEach { forbiddenOutcomes.add(it) }
                            }
                            "required_outcomes_any" -> {
                                // Handle single-line format only (multi-line handled above)
                                if (!requiredOutcomesAnyParsed && value.contains("]]")) {
                                    parseNestedArray(value).forEach { requiredOutcomesAny.add(it) }
                                }
                            }
                        }
                        "result" -> when (key) {
                            "label" -> currentResultLabel = value.trim('"')
                            "outcomes" -> {
                                // Parse outcomes = { set = [...], clear = [...] }
                                val setMatch = Regex("""set\s*=\s*\[(.*?)\]""").find(value)
                                if (setMatch != null) {
                                    val outcomes = parseStringArray("[${setMatch.groupValues[1]}]")
                                    if (currentResultLabel.isNotEmpty() && outcomes.isNotEmpty()) {
                                        producedOutcomes.getOrPut(currentResultLabel) { mutableListOf() }
                                            .addAll(outcomes)
                                    }
                                }
                            }
                        }
                        "option_effects" -> when (key) {
                            "outcomes" -> {
                                // Parse outcomes from [event.options.effects] (narrative/situation options)
                                val setMatch = Regex("""set\s*=\s*\[(.*?)\]""").find(value)
                                if (setMatch != null) {
                                    val outcomes = parseStringArray("[${setMatch.groupValues[1]}]")
                                    if (outcomes.isNotEmpty()) {
                                        producedOutcomes.getOrPut("option_effect") { mutableListOf() }
                                            .addAll(outcomes)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (id == null || title == null) return null

        return EventNode(
            id = id!!,
            title = title!!,
            type = type,
            tier = tier,
            minYear = minYear,
            maxYear = maxYear,
            historicityScore = historicityScore,
            location = location,
            requiredOutcomes = requiredOutcomes,
            requiredOutcomesAny = requiredOutcomesAny,
            forbiddenOutcomes = forbiddenOutcomes,
            producedOutcomes = producedOutcomes
        )
    }

    private fun inferEventType(file: File): EventType {
        val path = file.absolutePath.lowercase()
        return when {
            path.contains("/crisis/") || file.name.contains("CRISIS") -> EventType.CRISIS
            path.contains("/situation/") || file.name.contains("SITUATION") -> EventType.SITUATION
            path.contains("/opportunity/") || file.name.contains("OPPORTUNITY") -> EventType.OPPORTUNITY
            path.contains("/narrative/") || file.name.contains("NARRATIVE") -> EventType.NARRATIVE
            path.contains("/retirement/") || file.name.contains("RETIREMENT") -> EventType.RETIREMENT
            else -> EventType.SITUATION
        }
    }

    private fun parseStringArray(value: String): List<String> {
        // Parse ["item1", "item2"] format
        val content = value.trim().removeSurrounding("[", "]")
        if (content.isBlank()) return emptyList()

        return content.split(",")
            .map { it.trim().trim('"') }
            .filter { it.isNotEmpty() }
    }

    private fun parseNestedArray(value: String): List<List<String>> {
        // Parse [["a"], ["b", "c"]] format
        val results = mutableListOf<List<String>>()
        var depth = 0
        var current = StringBuilder()

        for (char in value) {
            when (char) {
                '[' -> {
                    depth++
                    // Don't include brackets in current - we only want the contents
                }
                ']' -> {
                    if (depth == 2 && current.isNotEmpty()) {
                        // Closing an inner array - parse what we have
                        results.add(parseStringArray("[${current}]"))
                        current = StringBuilder()
                    }
                    depth--
                }
                ',' -> {
                    if (depth == 1) {
                        // Skip comma between inner arrays
                    } else {
                        current.append(char)
                    }
                }
                else -> if (depth >= 2) current.append(char)
            }
        }

        return results
    }
}
