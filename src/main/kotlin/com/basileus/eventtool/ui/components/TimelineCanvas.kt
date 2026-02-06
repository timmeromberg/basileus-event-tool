package com.basileus.eventtool.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.basileus.eventtool.model.*

data class EventPosition(
    val event: EventNode,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

@Composable
fun TimelineCanvas(
    events: List<EventNode>,
    edges: List<EventEdge>,
    filter: GraphFilter,
    selectedEventId: String?,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val textMeasurer = rememberTextMeasurer()

    val filteredEvents = remember(events, filter) {
        events.filter { filter.matches(it) }
    }

    val eventPositions = remember(filteredEvents, filter.yearRange, scale, offset) {
        calculateEventPositions(filteredEvents, filter.yearRange.first, scale, offset)
    }

    val relevantEdges = remember(edges, filteredEvents, selectedEventId) {
        val eventIds = filteredEvents.map { it.id }.toSet()
        val visibleEdges = edges.filter { it.fromEventId in eventIds && it.toEventId in eventIds }

        // If an event is selected, only show edges connected to it
        if (selectedEventId != null) {
            visibleEdges.filter { it.fromEventId == selectedEventId || it.toEventId == selectedEventId }
        } else {
            visibleEdges
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(EventToolTheme.canvasBackground)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.3f, 3f)
                    offset += pan
                }
            }
            .pointerInput(eventPositions) {
                detectTapGestures { tapOffset ->
                    eventPositions.find { pos ->
                        tapOffset.x >= pos.x && tapOffset.x <= pos.x + pos.width &&
                        tapOffset.y >= pos.y && tapOffset.y <= pos.y + pos.height
                    }?.let { pos ->
                        onEventClick(pos.event.id)
                    }
                }
            }
    ) {
        // Draw year axis with headers
        drawYearAxis(filter.yearRange.first, filter.yearRange.last, scale, offset, textMeasurer)

        // Draw lane labels
        drawLaneLabels(scale, offset, textMeasurer)

        // Draw edges (behind events)
        relevantEdges.forEach { edge ->
            drawEdge(edge, eventPositions)
        }

        // Draw events
        eventPositions.forEach { pos ->
            drawEventNode(pos, selectedEventId == pos.event.id, textMeasurer)
        }
    }
}

private fun calculateEventPositions(
    events: List<EventNode>,
    minYear: Int,
    scale: Float,
    offset: Offset
): List<EventPosition> {
    val positions = mutableListOf<EventPosition>()
    val width = EventToolTheme.nodeWidth * scale
    val height = EventToolTheme.nodeHeight * scale

    // Group events by type for lane assignment
    val eventsByType = events.groupBy { it.type }
    val laneOrder = listOf(EventType.CRISIS, EventType.SITUATION, EventType.OPPORTUNITY, EventType.NARRATIVE, EventType.RETIREMENT)

    laneOrder.forEachIndexed { laneIndex, eventType ->
        val laneEvents = eventsByType[eventType] ?: emptyList()

        // Sort by year within lane (use minYear for positioning)
        val sortedEvents = laneEvents.sortedBy { it.minYear ?: it.maxYear ?: 1025 }

        // Track vertical stack count per year to stack events vertically
        val stackCountByYear = mutableMapOf<Int, Int>()

        sortedEvents.forEach { event ->
            // Position at minYear (start of valid range), centered in year column
            val eventStartYear = event.minYear ?: event.maxYear ?: 1025
            val yearColumnX = EventToolTheme.timelineMarginLeft + (eventStartYear - minYear) * EventToolTheme.yearWidth * scale + offset.x
            val centeredX = yearColumnX + (EventToolTheme.yearWidth * scale - width) / 2  // Center in year column

            // Stack vertically within the same year with more spacing
            val stackIndex = stackCountByYear.getOrDefault(eventStartYear, 0)
            stackCountByYear[eventStartYear] = stackIndex + 1

            val baseY = EventToolTheme.timelineMarginTop + laneIndex * EventToolTheme.laneHeight * scale + offset.y
            val verticalSpacing = height + EventToolTheme.eventSpacingY  // Vertical gap between stacked events
            val y = baseY + stackIndex * verticalSpacing

            positions.add(EventPosition(event, centeredX, y, width, height))
        }
    }

    return positions
}

private fun DrawScope.drawYearAxis(
    minYear: Int,
    maxYear: Int,
    scale: Float,
    offset: Offset,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val headerY = 30f + offset.y
    val canvasHeight = size.height

    // Draw year headers and vertical grid lines for each year
    for (year in minYear..maxYear) {
        val x = EventToolTheme.timelineMarginLeft + (year - minYear) * EventToolTheme.yearWidth * scale + offset.x

        // Skip if off screen
        if (x < -100f || x > size.width + 100f) continue

        // Vertical grid line (subtle for all years, stronger for decades)
        val isDecade = year % 10 == 0
        val lineAlpha = if (isDecade) 0.3f else 0.1f
        drawLine(
            color = EventToolTheme.timelineAxis.copy(alpha = lineAlpha),
            start = Offset(x, headerY + 25f),
            end = Offset(x, canvasHeight),
            strokeWidth = if (isDecade) 2f else 1f
        )

        // Year label - show every 5 years, or every year if zoomed in enough
        val showLabel = year % 5 == 0 || scale > 1.5f
        if (showLabel) {
            val yearText = year.toString()
            val textResult = textMeasurer.measure(
                text = yearText,
                style = TextStyle(
                    color = if (isDecade) EventToolTheme.textPrimary else EventToolTheme.textSecondary,
                    fontSize = if (isDecade) 13.sp else 11.sp
                )
            )
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(x - textResult.size.width / 2, headerY)
            )
        }
    }

    // Draw horizontal header line below year numbers
    drawLine(
        color = EventToolTheme.timelineAxis,
        start = Offset(EventToolTheme.timelineMarginLeft + offset.x, headerY + 22f),
        end = Offset(EventToolTheme.timelineMarginLeft + (maxYear - minYear) * EventToolTheme.yearWidth * scale + offset.x, headerY + 22f),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawLaneLabels(
    scale: Float,
    offset: Offset,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val laneLabels = listOf("Crisis", "Situation", "Opportunity", "Narrative", "Retirement")
    val laneColors = listOf(
        EventToolTheme.eventTypeColor(EventType.CRISIS),
        EventToolTheme.eventTypeColor(EventType.SITUATION),
        EventToolTheme.eventTypeColor(EventType.OPPORTUNITY),
        EventToolTheme.eventTypeColor(EventType.NARRATIVE),
        EventToolTheme.eventTypeColor(EventType.RETIREMENT)
    )

    laneLabels.forEachIndexed { index, label ->
        val y = EventToolTheme.timelineMarginTop + index * EventToolTheme.laneHeight * scale + offset.y + EventToolTheme.nodeHeight * scale / 2

        val textResult = textMeasurer.measure(
            text = label,
            style = TextStyle(color = laneColors[index], fontSize = 14.sp)
        )

        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(20f, y - textResult.size.height / 2)
        )
    }
}

private fun DrawScope.drawEdge(edge: EventEdge, positions: List<EventPosition>) {
    val fromPos = positions.find { it.event.id == edge.fromEventId } ?: return
    val toPos = positions.find { it.event.id == edge.toEventId } ?: return

    val startX = fromPos.x + fromPos.width
    val startY = fromPos.y + fromPos.height / 2
    val endX = toPos.x
    val endY = toPos.y + toPos.height / 2

    val color = EventToolTheme.edgeColor(edge.type)
    val alpha = if (edge.type == EdgeType.FORBIDDEN) 0.5f else 0.7f

    // Draw curved arrow
    val controlX = (startX + endX) / 2
    val path = Path().apply {
        moveTo(startX, startY)
        cubicTo(
            controlX, startY,
            controlX, endY,
            endX, endY
        )
    }

    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(width = if (edge.type == EdgeType.FORBIDDEN) 1.5f else 2f)
    )

    // Draw arrowhead
    val arrowSize = 8f
    drawLine(
        color = color.copy(alpha = alpha),
        start = Offset(endX - arrowSize, endY - arrowSize),
        end = Offset(endX, endY),
        strokeWidth = 2f
    )
    drawLine(
        color = color.copy(alpha = alpha),
        start = Offset(endX - arrowSize, endY + arrowSize),
        end = Offset(endX, endY),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawEventNode(
    pos: EventPosition,
    isSelected: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val event = pos.event

    // Background
    val bgColor = if (isSelected) EventToolTheme.selectedBackground else EventToolTheme.panelBackground
    drawRoundRect(
        color = bgColor,
        topLeft = Offset(pos.x, pos.y),
        size = Size(pos.width, pos.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(EventToolTheme.nodeCornerRadius)
    )

    // Border (color by type, thickness by tier)
    val borderColor = EventToolTheme.eventTypeColor(event.type)
    val borderWidth = when (event.tier) {
        EventTier.MINOR -> 1f
        EventTier.MAJOR -> 2f
        EventTier.GREAT -> 3f
    }
    drawRoundRect(
        color = borderColor,
        topLeft = Offset(pos.x, pos.y),
        size = Size(pos.width, pos.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(EventToolTheme.nodeCornerRadius),
        style = Stroke(width = borderWidth)
    )

    // Title text
    val titleText = if (event.title.length > 20) event.title.take(18) + "..." else event.title
    val titleResult = textMeasurer.measure(
        text = titleText,
        style = TextStyle(color = EventToolTheme.textPrimary, fontSize = 11.sp)
    )
    drawText(
        textLayoutResult = titleResult,
        topLeft = Offset(pos.x + 8f, pos.y + 8f)
    )

    // Year text (show range if different)
    val yearText = when {
        event.minYear != null && event.maxYear != null && event.minYear != event.maxYear ->
            "${event.minYear}-${event.maxYear}"
        event.minYear != null -> event.minYear.toString()
        event.maxYear != null -> event.maxYear.toString()
        else -> "?"
    }
    val yearResult = textMeasurer.measure(
        text = yearText,
        style = TextStyle(color = EventToolTheme.textSecondary, fontSize = 10.sp)
    )
    drawText(
        textLayoutResult = yearResult,
        topLeft = Offset(pos.x + 8f, pos.y + pos.height - yearResult.size.height - 8f)
    )

    // Historicity indicator (small dot)
    val histColor = EventToolTheme.historicityColor(event.historicityScore)
    drawCircle(
        color = histColor,
        radius = 4f,
        center = Offset(pos.x + pos.width - 12f, pos.y + pos.height - 12f)
    )
}
