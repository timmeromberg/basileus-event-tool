package com.basileus.eventtool.storage

import com.basileus.eventtool.model.OutcomeNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class OutcomeLoader(private val config: EventToolStorageConfig) {

    suspend fun loadAllOutcomes(): List<OutcomeNode> = withContext(Dispatchers.IO) {
        val outcomes = mutableListOf<OutcomeNode>()
        val outcomesDir = config.outcomesPath.toFile()

        if (!outcomesDir.exists()) {
            println("Outcomes directory not found: ${outcomesDir.absolutePath}")
            return@withContext outcomes
        }

        // Walk through all subdirectories (deaths, emperors, military, etc.)
        outcomesDir.walkTopDown()
            .filter { it.isFile && it.extension == "toml" }
            .forEach { file ->
                try {
                    parseOutcomeFile(file)?.let { outcomes.add(it) }
                } catch (e: Exception) {
                    println("Error parsing ${file.name}: ${e.message}")
                }
            }

        println("Loaded ${outcomes.size} outcomes")
        outcomes
    }

    private fun parseOutcomeFile(file: File): OutcomeNode? {
        val content = file.readText()
        val category = file.parentFile?.name ?: "unknown"

        var id: String? = null
        var name: String? = null
        var historicityScore = 50
        var historicalImpactScore = 50
        var playerDescription: String? = null

        var inOutcomeSection = false

        content.lines().forEach { line ->
            val trimmed = line.trim()

            when {
                trimmed == "[outcome]" -> inOutcomeSection = true
                trimmed.startsWith("[") && trimmed != "[outcome]" -> inOutcomeSection = false
                trimmed.contains("=") && inOutcomeSection -> {
                    val eqIndex = trimmed.indexOf('=')
                    val key = trimmed.substring(0, eqIndex).trim()
                    val value = trimmed.substring(eqIndex + 1).trim()

                    when (key) {
                        "id" -> id = value.trim('"')
                        "name" -> name = value.trim('"')
                        "historicity_score" -> historicityScore = value.toIntOrNull() ?: 50
                        "historical_impact_score" -> historicalImpactScore = value.toIntOrNull() ?: 50
                        "player_description" -> playerDescription = parseMultilineString(value, content, trimmed)
                    }
                }
            }
        }

        if (id == null || name == null) return null

        return OutcomeNode(
            id = id!!,
            name = name!!,
            historicityScore = historicityScore,
            historicalImpactScore = historicalImpactScore,
            category = category,
            playerDescription = playerDescription
        )
    }

    private fun parseMultilineString(value: String, fullContent: String, currentLine: String): String {
        // Handle simple quoted strings
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.trim('"')
        }
        // For multiline ("""), just return the first line for now
        if (value.startsWith("\"\"\"")) {
            return value.removePrefix("\"\"\"").removeSuffix("\"\"\"").trim()
        }
        return value.trim('"')
    }
}
