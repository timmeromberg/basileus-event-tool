package com.basileus.eventtool.model

enum class EdgeType {
    REQUIRED,       // Event B requires this outcome from Event A (AND logic)
    REQUIRED_ANY,   // Event B requires any of a group (OR logic)
    FORBIDDEN,      // Event B blocked by outcome from Event A
    PRODUCES        // Event A produces outcome (for outcome-centric view)
}

data class EventEdge(
    val fromEventId: String,
    val toEventId: String,
    val outcomeId: String,
    val type: EdgeType
)
