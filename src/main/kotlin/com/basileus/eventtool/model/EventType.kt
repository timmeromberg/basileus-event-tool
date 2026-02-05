package com.basileus.eventtool.model

enum class EventType {
    CRISIS,
    SITUATION,
    OPPORTUNITY,
    NARRATIVE,
    RETIREMENT;

    companion object {
        fun fromString(value: String): EventType? = when (value.lowercase()) {
            "crisis" -> CRISIS
            "situation" -> SITUATION
            "opportunity" -> OPPORTUNITY
            "narrative" -> NARRATIVE
            "retirement" -> RETIREMENT
            else -> null
        }
    }
}
