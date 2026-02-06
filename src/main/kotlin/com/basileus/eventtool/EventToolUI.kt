package com.basileus.eventtool

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.core.Koin
import com.basileus.eventtool.model.*
import com.basileus.eventtool.ui.components.*

@Composable
fun EventToolUI(koin: Koin) {
    val viewModel: EventToolViewModel = koin.get()
    val graphState by viewModel.graphState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(EventToolTheme.canvasBackground)) {
        // Top bar
        TopBar(
            viewMode = graphState.viewMode,
            onViewModeChange = { viewModel.setViewMode(it) },
            onRefresh = { viewModel.refresh() }
        )

        // Main content
        Row(modifier = Modifier.weight(1f)) {
            // Left sidebar - Filters
            FilterPanel(
                filter = graphState.filter,
                onFilterChange = { viewModel.updateFilter(it) },
                modifier = Modifier.width(200.dp)
            )

            // Main canvas
            Box(modifier = Modifier.weight(1f)) {
                when (graphState.viewMode) {
                    ViewMode.TIMELINE -> TimelineCanvas(
                        events = graphState.events.values.toList(),
                        edges = graphState.edges,
                        filter = graphState.filter,
                        selectedEventId = graphState.selectedEventId,
                        onEventClick = { viewModel.selectEvent(it) }
                    )
                    ViewMode.GRAPH -> GraphCanvas(
                        events = graphState.events.values.toList(),
                        edges = graphState.edges,
                        filter = graphState.filter,
                        selectedEventId = graphState.selectedEventId,
                        onEventClick = { viewModel.selectEvent(it) }
                    )
                }
            }

            // Right sidebar - Detail panel
            if (graphState.selectedEventId != null) {
                DetailPanel(
                    event = graphState.events[graphState.selectedEventId],
                    events = graphState.events,
                    edges = graphState.edges,
                    onClose = { viewModel.selectEvent(null) },
                    onUpdateYears = { eventId, minYear, maxYear ->
                        viewModel.updateEventYears(eventId, minYear, maxYear)
                    },
                    modifier = Modifier.width(280.dp)
                )
            }
        }

        // Status bar
        StatusBar(
            totalEvents = graphState.eventCount,
            visibleEvents = graphState.events.values.count { graphState.filter.matches(it) },
            selectedEvent = graphState.selectedEventId,
            edgeCount = graphState.edgeCount
        )
    }
}

@Composable
private fun TopBar(
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EventToolTheme.panelBackground)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Basileus Event Tool",
            color = EventToolTheme.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ViewModeButton("Refresh", false) { onRefresh() }
            ViewModeButton("Timeline", viewMode == ViewMode.TIMELINE) {
                onViewModeChange(ViewMode.TIMELINE)
            }
            ViewModeButton("Graph", viewMode == ViewMode.GRAPH) {
                onViewModeChange(ViewMode.GRAPH)
            }
        }
    }
}

@Composable
private fun ViewModeButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) EventToolTheme.selectedBackground else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (isSelected) EventToolTheme.textHighlight else EventToolTheme.textSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun FilterPanel(
    filter: GraphFilter,
    onFilterChange: (GraphFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(EventToolTheme.panelBackground)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Filters", color = EventToolTheme.textPrimary, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        // Year range
        Text("Year Range", color = EventToolTheme.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var startText by remember(filter.yearRange.first) { mutableStateOf(filter.yearRange.first.toString()) }
            var endText by remember(filter.yearRange.last) { mutableStateOf(filter.yearRange.last.toString()) }

            OutlinedTextField(
                value = startText,
                onValueChange = { value ->
                    startText = value
                    val start = value.toIntOrNull() ?: return@OutlinedTextField
                    if (start <= filter.yearRange.last) {
                        onFilterChange(filter.copy(yearRange = start..filter.yearRange.last))
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Start", fontSize = 10.sp) },
                textStyle = LocalTextStyle.current.copy(
                    color = EventToolTheme.textPrimary,
                    fontSize = 12.sp
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = EventToolTheme.textPrimary,
                    cursorColor = EventToolTheme.textHighlight,
                    focusedBorderColor = EventToolTheme.textHighlight,
                    unfocusedBorderColor = EventToolTheme.textSecondary,
                    focusedLabelColor = EventToolTheme.textHighlight,
                    unfocusedLabelColor = EventToolTheme.textSecondary
                )
            )
            OutlinedTextField(
                value = endText,
                onValueChange = { value ->
                    endText = value
                    val end = value.toIntOrNull() ?: return@OutlinedTextField
                    if (end >= filter.yearRange.first) {
                        onFilterChange(filter.copy(yearRange = filter.yearRange.first..end))
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("End", fontSize = 10.sp) },
                textStyle = LocalTextStyle.current.copy(
                    color = EventToolTheme.textPrimary,
                    fontSize = 12.sp
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = EventToolTheme.textPrimary,
                    cursorColor = EventToolTheme.textHighlight,
                    focusedBorderColor = EventToolTheme.textHighlight,
                    unfocusedBorderColor = EventToolTheme.textSecondary,
                    focusedLabelColor = EventToolTheme.textHighlight,
                    unfocusedLabelColor = EventToolTheme.textSecondary
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Event types
        Text("Event Types", color = EventToolTheme.textSecondary, fontSize = 12.sp)
        EventType.values().forEach { type ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = type in filter.eventTypes,
                    onCheckedChange = { checked ->
                        val newTypes = if (checked) {
                            filter.eventTypes + type
                        } else {
                            filter.eventTypes - type
                        }
                        onFilterChange(filter.copy(eventTypes = newTypes))
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = EventToolTheme.eventTypeColor(type),
                        uncheckedColor = EventToolTheme.textSecondary
                    )
                )
                Text(type.name, color = EventToolTheme.eventTypeColor(type), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tiers
        Text("Tiers", color = EventToolTheme.textSecondary, fontSize = 12.sp)
        EventTier.values().forEach { tier ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = tier in filter.tiers,
                    onCheckedChange = { checked ->
                        val newTiers = if (checked) {
                            filter.tiers + tier
                        } else {
                            filter.tiers - tier
                        }
                        onFilterChange(filter.copy(tiers = newTiers))
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = EventToolTheme.tierColor(tier),
                        uncheckedColor = EventToolTheme.textSecondary
                    )
                )
                Text(tier.name, color = EventToolTheme.tierColor(tier), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search
        Text("Search", color = EventToolTheme.textSecondary, fontSize = 12.sp)
        OutlinedTextField(
            value = filter.searchQuery,
            onValueChange = { onFilterChange(filter.copy(searchQuery = it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                color = EventToolTheme.textPrimary,
                fontSize = 12.sp
            ),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = EventToolTheme.textPrimary,
                cursorColor = EventToolTheme.textHighlight,
                focusedBorderColor = EventToolTheme.textHighlight,
                unfocusedBorderColor = EventToolTheme.textSecondary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Timeline presets
        Text("Presets", color = EventToolTheme.textSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        PresetButton("All Events", filter.requiredOutcomes.isEmpty()) {
            onFilterChange(filter.copy(requiredOutcomes = emptySet()))
        }
        PresetButton("Maniakes Timeline", "maniakes_takes_throne" in filter.requiredOutcomes) {
            onFilterChange(filter.copy(requiredOutcomes = setOf("maniakes_takes_throne")))
        }
        PresetButton("Schism Averted", "schism_averted_1054" in filter.requiredOutcomes) {
            onFilterChange(filter.copy(requiredOutcomes = setOf("schism_averted_1054")))
        }
    }
}

@Composable
private fun PresetButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(
                if (isSelected) EventToolTheme.selectedBackground else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                if (isSelected) EventToolTheme.textHighlight else EventToolTheme.textSecondary,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(
            label,
            color = if (isSelected) EventToolTheme.textHighlight else EventToolTheme.textPrimary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun DetailPanel(
    event: EventNode?,
    events: Map<String, EventNode>,
    edges: List<EventEdge>,
    onClose: () -> Unit,
    onUpdateYears: (String, Int?, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (event == null) return

    val incomingEdges = edges.filter { it.toEventId == event.id }
    val outgoingEdges = edges.filter { it.fromEventId == event.id }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(EventToolTheme.panelBackground)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Event Details", color = EventToolTheme.textPrimary, fontWeight = FontWeight.Bold)
            Text(
                "X",
                color = EventToolTheme.textSecondary,
                modifier = Modifier.clickable(onClick = onClose)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Event info
        Text(event.title, color = EventToolTheme.textHighlight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(event.id, color = EventToolTheme.textSecondary, fontSize = 11.sp)

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge(event.type.name, EventToolTheme.eventTypeColor(event.type))
            Badge(event.tier.name, EventToolTheme.tierColor(event.tier))
            Badge("H:${event.historicityScore}", EventToolTheme.historicityColor(event.historicityScore))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Editable year fields
        Text("Year Range", color = EventToolTheme.textSecondary, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))

        var minYearText by remember(event.id, event.minYear) { mutableStateOf(event.minYear?.toString() ?: "") }
        var maxYearText by remember(event.id, event.maxYear) { mutableStateOf(event.maxYear?.toString() ?: "") }
        var yearsDirty by remember(event.id) { mutableStateOf(false) }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = minYearText,
                onValueChange = { minYearText = it; yearsDirty = true },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Min", fontSize = 9.sp) },
                textStyle = LocalTextStyle.current.copy(
                    color = EventToolTheme.textPrimary,
                    fontSize = 11.sp
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = EventToolTheme.textPrimary,
                    cursorColor = EventToolTheme.textHighlight,
                    focusedBorderColor = EventToolTheme.textHighlight,
                    unfocusedBorderColor = EventToolTheme.textSecondary,
                    focusedLabelColor = EventToolTheme.textHighlight,
                    unfocusedLabelColor = EventToolTheme.textSecondary
                )
            )
            OutlinedTextField(
                value = maxYearText,
                onValueChange = { maxYearText = it; yearsDirty = true },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Max", fontSize = 9.sp) },
                textStyle = LocalTextStyle.current.copy(
                    color = EventToolTheme.textPrimary,
                    fontSize = 11.sp
                ),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = EventToolTheme.textPrimary,
                    cursorColor = EventToolTheme.textHighlight,
                    focusedBorderColor = EventToolTheme.textHighlight,
                    unfocusedBorderColor = EventToolTheme.textSecondary,
                    focusedLabelColor = EventToolTheme.textHighlight,
                    unfocusedLabelColor = EventToolTheme.textSecondary
                )
            )
        }

        if (yearsDirty) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EventToolTheme.textHighlight, RoundedCornerShape(4.dp))
                    .clickable {
                        val newMin = minYearText.toIntOrNull()
                        val newMax = maxYearText.toIntOrNull()
                        onUpdateYears(event.id, newMin, newMax)
                        yearsDirty = false
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Save Years", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        event.location?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Location: $it", color = EventToolTheme.textPrimary, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Required outcomes
        if (event.requiredOutcomes.isNotEmpty()) {
            Text("Required (AND):", color = EventToolTheme.textSecondary, fontSize = 11.sp)
            event.requiredOutcomes.forEach { outcome ->
                Text("  - $outcome", color = EventToolTheme.edgeColor(EdgeType.REQUIRED), fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (event.requiredOutcomesAny.isNotEmpty()) {
            Text("Required (OR):", color = EventToolTheme.textSecondary, fontSize = 11.sp)
            event.requiredOutcomesAny.forEach { group ->
                Text("  - ${group.joinToString(" OR ")}", color = EventToolTheme.edgeColor(EdgeType.REQUIRED_ANY), fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (event.forbiddenOutcomes.isNotEmpty()) {
            Text("Forbidden:", color = EventToolTheme.textSecondary, fontSize = 11.sp)
            event.forbiddenOutcomes.forEach { outcome ->
                Text("  - $outcome", color = EventToolTheme.edgeColor(EdgeType.FORBIDDEN), fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Produced outcomes
        if (event.producedOutcomes.isNotEmpty()) {
            Text("Produces:", color = EventToolTheme.textSecondary, fontSize = 11.sp)
            event.producedOutcomes.forEach { (result, outcomes) ->
                Text("  $result:", color = EventToolTheme.textPrimary, fontSize = 11.sp)
                outcomes.forEach { outcome ->
                    Text("    - $outcome", color = EventToolTheme.edgeColor(EdgeType.PRODUCES), fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Connected events
        if (incomingEdges.isNotEmpty()) {
            Text("Depends on (${incomingEdges.size}):", color = EventToolTheme.textSecondary, fontSize = 11.sp)
            incomingEdges.take(5).forEach { edge ->
                val fromEvent = events[edge.fromEventId]
                Text(
                    "  <- ${fromEvent?.title ?: edge.fromEventId}",
                    color = EventToolTheme.edgeColor(edge.type),
                    fontSize = 11.sp
                )
            }
            if (incomingEdges.size > 5) {
                Text("  ... and ${incomingEdges.size - 5} more", color = EventToolTheme.textSecondary, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (outgoingEdges.isNotEmpty()) {
            Text("Leads to (${outgoingEdges.size}):", color = EventToolTheme.textSecondary, fontSize = 11.sp)
            outgoingEdges.take(5).forEach { edge ->
                val toEvent = events[edge.toEventId]
                Text(
                    "  -> ${toEvent?.title ?: edge.toEventId}",
                    color = EventToolTheme.edgeColor(edge.type),
                    fontSize = 11.sp
                )
            }
            if (outgoingEdges.size > 5) {
                Text("  ... and ${outgoingEdges.size - 5} more", color = EventToolTheme.textSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp)
    }
}

@Composable
private fun GraphPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EventToolTheme.canvasBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Graph View - Coming Soon",
            color = EventToolTheme.textSecondary,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun StatusBar(
    totalEvents: Int,
    visibleEvents: Int,
    selectedEvent: String?,
    edgeCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EventToolTheme.panelBackground)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "$visibleEvents / $totalEvents events",
            color = EventToolTheme.textSecondary,
            fontSize = 11.sp
        )
        Text(
            "$edgeCount edges",
            color = EventToolTheme.textSecondary,
            fontSize = 11.sp
        )
        selectedEvent?.let {
            Text(
                "Selected: $it",
                color = EventToolTheme.textHighlight,
                fontSize = 11.sp
            )
        }
    }
}
