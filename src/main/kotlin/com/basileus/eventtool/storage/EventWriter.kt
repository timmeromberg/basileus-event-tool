package com.basileus.eventtool.storage

import java.io.File

class EventWriter {

    fun updateEventYears(filePath: String, minYear: Int?, maxYear: Int?) {
        val file = File(filePath)
        if (!file.exists()) return

        val lines = file.readLines().toMutableList()
        var inConditions = false
        var inEvent = false
        var foundMinYear = false
        var foundMaxYear = false
        var conditionsStartIndex = -1

        for (i in lines.indices) {
            val trimmed = lines[i].trim()
            when {
                trimmed.startsWith("[event]") && !trimmed.startsWith("[event.") -> {
                    inEvent = true
                    inConditions = false
                }
                trimmed.startsWith("[event.conditions]") -> {
                    inConditions = true
                    inEvent = false
                    conditionsStartIndex = i
                }
                trimmed.startsWith("[") && !trimmed.startsWith("[event.conditions]") -> {
                    if (inConditions || inEvent) {
                        inConditions = false
                        inEvent = false
                    }
                }
                inConditions && trimmed.startsWith("min_year") -> {
                    if (minYear != null) {
                        lines[i] = "min_year = $minYear"
                    }
                    foundMinYear = true
                }
                inConditions && trimmed.startsWith("max_year") -> {
                    if (maxYear != null) {
                        lines[i] = "max_year = $maxYear"
                    }
                    foundMaxYear = true
                }
                inEvent && trimmed.startsWith("year") && trimmed.contains("=") -> {
                    // Narrative events with single year field
                    val key = trimmed.substringBefore("=").trim()
                    if (key == "year" && minYear != null) {
                        lines[i] = "year = $minYear"
                    }
                }
            }
        }

        // If min_year/max_year not found in conditions, insert them after [event.conditions]
        if (conditionsStartIndex >= 0) {
            if (!foundMaxYear && maxYear != null) {
                lines.add(conditionsStartIndex + 1, "max_year = $maxYear")
            }
            if (!foundMinYear && minYear != null) {
                lines.add(conditionsStartIndex + 1, "min_year = $minYear")
            }
        }

        file.writeText(lines.joinToString("\n") + "\n")
    }
}
