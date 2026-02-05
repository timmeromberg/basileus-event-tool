package com.basileus.eventtool.model

data class GraphFilter(
    val yearRange: IntRange = 1025..1100,
    val eventTypes: Set<EventType> = EventType.values().toSet(),
    val tiers: Set<EventTier> = EventTier.values().toSet(),
    val historicityRange: IntRange = 0..100,
    val requiredOutcomes: Set<String> = emptySet(),
    val searchQuery: String = ""
) {
    fun matches(event: EventNode): Boolean {
        // Year filter - check if event's year range OVERLAPS with filter range
        val eventMinYear = event.minYear ?: event.maxYear ?: 1025
        val eventMaxYear = event.maxYear ?: event.minYear ?: 1100
        // Overlap: event starts before filter ends AND event ends after filter starts
        if (eventMaxYear < yearRange.first || eventMinYear > yearRange.last) return false

        // Type filter
        if (event.type !in eventTypes) return false

        // Tier filter
        if (event.tier !in tiers) return false

        // Historicity filter
        if (event.historicityScore !in historicityRange) return false

        // Required outcomes filter (timeline preset)
        if (requiredOutcomes.isNotEmpty()) {
            val eventRequires = event.requiredOutcomes.toSet() +
                event.requiredOutcomesAny.flatten().toSet()
            if (eventRequires.intersect(requiredOutcomes).isEmpty() &&
                !requiredOutcomes.any { it in event.producedOutcomes.values.flatten() }) {
                return false
            }
        }

        // Search filter
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            if (!event.id.lowercase().contains(query) &&
                !event.title.lowercase().contains(query)) {
                return false
            }
        }

        return true
    }
}
