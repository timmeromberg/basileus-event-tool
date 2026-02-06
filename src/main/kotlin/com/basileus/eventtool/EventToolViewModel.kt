package com.basileus.eventtool

import com.basileus.eventtool.model.*
import com.basileus.eventtool.storage.EventLoader
import com.basileus.eventtool.storage.OutcomeLoader
import com.basileus.eventtool.storage.GraphBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GraphState(
    val eventCount: Int = 0,
    val outcomeCount: Int = 0,
    val edgeCount: Int = 0,
    val events: Map<String, EventNode> = emptyMap(),
    val outcomes: Map<String, OutcomeNode> = emptyMap(),
    val edges: List<EventEdge> = emptyList(),
    val selectedEventId: String? = null,
    val selectedOutcomeId: String? = null,
    val viewMode: ViewMode = ViewMode.TIMELINE,
    val filter: GraphFilter = GraphFilter()
)

enum class ViewMode {
    TIMELINE,
    GRAPH
}

class EventToolViewModel(
    private val eventLoader: EventLoader,
    private val outcomeLoader: OutcomeLoader,
    private val graphBuilder: GraphBuilder
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _graphState = MutableStateFlow(GraphState())
    val graphState: StateFlow<GraphState> = _graphState.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        loadData()
    }

    private fun loadData() {
        scope.launch {
            val events = eventLoader.loadAllEvents()
            val outcomes = outcomeLoader.loadAllOutcomes()
            val edges = graphBuilder.buildEdges(events, outcomes)

            _graphState.value = GraphState(
                eventCount = events.size,
                outcomeCount = outcomes.size,
                edgeCount = edges.size,
                events = events.associateBy { it.id },
                outcomes = outcomes.associateBy { it.id },
                edges = edges
            )
        }
    }

    fun selectEvent(eventId: String?) {
        _graphState.value = _graphState.value.copy(selectedEventId = eventId)
    }

    fun selectOutcome(outcomeId: String?) {
        _graphState.value = _graphState.value.copy(selectedOutcomeId = outcomeId)
    }

    fun setViewMode(mode: ViewMode) {
        _graphState.value = _graphState.value.copy(viewMode = mode)
    }

    fun updateFilter(filter: GraphFilter) {
        _graphState.value = _graphState.value.copy(filter = filter)
    }
}
