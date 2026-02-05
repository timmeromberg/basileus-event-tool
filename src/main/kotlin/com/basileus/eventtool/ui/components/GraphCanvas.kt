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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.basileus.eventtool.model.*
import com.basileus.eventtool.utils.ForceDirectedLayout
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GraphCanvas(
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

    val relevantEdges = remember(edges, filteredEvents) {
        val eventIds = filteredEvents.map { it.id }.toSet()
        edges.filter { it.fromEventId in eventIds && it.toEventId in eventIds }
    }

    // Compute layout (memoized)
    val nodePositions = remember(filteredEvents, relevantEdges) {
        if (filteredEvents.isEmpty()) {
            emptyMap()
        } else {
            ForceDirectedLayout(1200f, 800f).layout(filteredEvents, relevantEdges, iterations = 150)
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
            .pointerInput(nodePositions, scale, offset) {
                detectTapGestures { tapOffset ->
                    val nodeWidth = EventToolTheme.nodeWidth * scale
                    val nodeHeight = EventToolTheme.nodeHeight * scale

                    filteredEvents.find { event ->
                        val pos = nodePositions[event.id] ?: return@find false
                        val screenX = pos.x * scale + offset.x
                        val screenY = pos.y * scale + offset.y

                        tapOffset.x >= screenX && tapOffset.x <= screenX + nodeWidth &&
                        tapOffset.y >= screenY && tapOffset.y <= screenY + nodeHeight
                    }?.let { event ->
                        onEventClick(event.id)
                    }
                }
            }
    ) {
        val nodeWidth = EventToolTheme.nodeWidth * scale
        val nodeHeight = EventToolTheme.nodeHeight * scale

        // Draw edges
        relevantEdges.forEach { edge ->
            val fromPos = nodePositions[edge.fromEventId] ?: return@forEach
            val toPos = nodePositions[edge.toEventId] ?: return@forEach

            drawGraphEdge(
                from = Offset(fromPos.x * scale + offset.x + nodeWidth, fromPos.y * scale + offset.y + nodeHeight / 2),
                to = Offset(toPos.x * scale + offset.x, toPos.y * scale + offset.y + nodeHeight / 2),
                type = edge.type,
                isHighlighted = edge.fromEventId == selectedEventId || edge.toEventId == selectedEventId
            )
        }

        // Draw nodes
        filteredEvents.forEach { event ->
            val pos = nodePositions[event.id] ?: return@forEach
            val screenPos = Offset(pos.x * scale + offset.x, pos.y * scale + offset.y)

            drawGraphNode(
                event = event,
                position = screenPos,
                size = Size(nodeWidth, nodeHeight),
                isSelected = event.id == selectedEventId,
                textMeasurer = textMeasurer
            )
        }
    }
}

private fun DrawScope.drawGraphEdge(
    from: Offset,
    to: Offset,
    type: EdgeType,
    isHighlighted: Boolean
) {
    val color = EventToolTheme.edgeColor(type)
    val alpha = when {
        isHighlighted -> 1f
        type == EdgeType.FORBIDDEN -> 0.3f
        else -> 0.5f
    }
    val strokeWidth = if (isHighlighted) 3f else 1.5f

    // Draw curved line
    val midX = (from.x + to.x) / 2
    val midY = (from.y + to.y) / 2
    val controlOffset = 30f

    val path = Path().apply {
        moveTo(from.x, from.y)
        quadraticTo(midX, midY - controlOffset, to.x, to.y)
    }

    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(width = strokeWidth)
    )

    // Draw arrowhead
    val angle = atan2(to.y - midY + controlOffset, to.x - midX)
    val arrowLength = 10f
    val arrowAngle = 0.5f

    drawLine(
        color = color.copy(alpha = alpha),
        start = to,
        end = Offset(
            to.x - arrowLength * cos(angle - arrowAngle),
            to.y - arrowLength * sin(angle - arrowAngle)
        ),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = color.copy(alpha = alpha),
        start = to,
        end = Offset(
            to.x - arrowLength * cos(angle + arrowAngle),
            to.y - arrowLength * sin(angle + arrowAngle)
        ),
        strokeWidth = strokeWidth
    )
}

private fun DrawScope.drawGraphNode(
    event: EventNode,
    position: Offset,
    size: Size,
    isSelected: Boolean,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    // Background
    val bgColor = if (isSelected) EventToolTheme.selectedBackground else EventToolTheme.panelBackground
    drawRoundRect(
        color = bgColor,
        topLeft = position,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(EventToolTheme.nodeCornerRadius)
    )

    // Border
    val borderColor = EventToolTheme.eventTypeColor(event.type)
    val borderWidth = when (event.tier) {
        EventTier.MINOR -> 1f
        EventTier.MAJOR -> 2f
        EventTier.GREAT -> 3f
    }
    drawRoundRect(
        color = borderColor,
        topLeft = position,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(EventToolTheme.nodeCornerRadius),
        style = Stroke(width = borderWidth)
    )

    // Title
    val titleText = if (event.title.length > 22) event.title.take(20) + "..." else event.title
    val titleResult = textMeasurer.measure(
        text = titleText,
        style = TextStyle(color = EventToolTheme.textPrimary, fontSize = 10.sp)
    )
    drawText(
        textLayoutResult = titleResult,
        topLeft = Offset(position.x + 6f, position.y + 6f)
    )

    // Year and type
    val infoText = "${event.displayYear} | ${event.type.name.take(3)}"
    val infoResult = textMeasurer.measure(
        text = infoText,
        style = TextStyle(color = EventToolTheme.textSecondary, fontSize = 9.sp)
    )
    drawText(
        textLayoutResult = infoResult,
        topLeft = Offset(position.x + 6f, position.y + size.height - infoResult.size.height - 6f)
    )

    // Selection indicator
    if (isSelected) {
        drawCircle(
            color = EventToolTheme.textHighlight,
            radius = 5f,
            center = Offset(position.x + size.width - 10f, position.y + 10f)
        )
    }
}
