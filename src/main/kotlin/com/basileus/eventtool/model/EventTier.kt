package com.basileus.eventtool.model

enum class EventTier {
    MINOR,
    MAJOR,
    GREAT;

    companion object {
        fun fromString(value: String): EventTier? = when (value.lowercase()) {
            "minor" -> MINOR
            "major" -> MAJOR
            "great" -> GREAT
            else -> null
        }
    }
}
