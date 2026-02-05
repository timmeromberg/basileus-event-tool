package com.basileus.eventtool.model

data class EventNode(
    val id: String,
    val title: String,
    val type: EventType,
    val tier: EventTier,
    val minYear: Int?,
    val maxYear: Int?,
    val historicityScore: Int,
    val location: String?,
    val requiredOutcomes: List<String>,
    val requiredOutcomesAny: List<List<String>>,
    val forbiddenOutcomes: List<String>,
    val producedOutcomes: Map<String, List<String>>  // resultLabel -> outcomes set
) {
    val yearRange: IntRange?
        get() = if (minYear != null && maxYear != null) minYear..maxYear
                else if (minYear != null) minYear..minYear
                else if (maxYear != null) maxYear..maxYear
                else null

    val displayYear: Int
        get() = minYear ?: maxYear ?: 1025
}
