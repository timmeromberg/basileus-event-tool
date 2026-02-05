package com.basileus.eventtool.ui.components

import androidx.compose.ui.graphics.Color
import com.basileus.eventtool.model.EventTier
import com.basileus.eventtool.model.EventType
import com.basileus.eventtool.model.EdgeType

object EventToolTheme {
    // Background colors
    val canvasBackground = Color(0xFF1E1E2E)
    val panelBackground = Color(0xFF2D2D3D)
    val selectedBackground = Color(0xFF3D3D5C)

    // Event type colors
    fun eventTypeColor(type: EventType): Color = when (type) {
        EventType.CRISIS -> Color(0xFFE06C75)        // Red
        EventType.SITUATION -> Color(0xFF61AFEF)     // Blue
        EventType.OPPORTUNITY -> Color(0xFF98C379)   // Green
        EventType.NARRATIVE -> Color(0xFFD19A66)     // Orange
        EventType.RETIREMENT -> Color(0xFFABB2BF)    // Gray
    }

    // Event tier colors (border/accent)
    fun tierColor(tier: EventTier): Color = when (tier) {
        EventTier.MINOR -> Color(0xFF5C6370)         // Gray
        EventTier.MAJOR -> Color(0xFFC678DD)         // Purple
        EventTier.GREAT -> Color(0xFFE5C07B)         // Gold
    }

    // Edge type colors
    fun edgeColor(type: EdgeType): Color = when (type) {
        EdgeType.REQUIRED -> Color(0xFF61AFEF)       // Blue (AND dependency)
        EdgeType.REQUIRED_ANY -> Color(0xFF56B6C2)   // Cyan (OR dependency)
        EdgeType.FORBIDDEN -> Color(0xFFE06C75)      // Red (blocker)
        EdgeType.PRODUCES -> Color(0xFF98C379)       // Green (production)
    }

    // Historicity gradient (0 = most ahistorical, 100 = historical)
    fun historicityColor(score: Int): Color {
        val alpha = (score / 100f).coerceIn(0f, 1f)
        // Blend from purple (ahistorical) to white (historical)
        return Color(
            red = 0.76f + (1f - 0.76f) * alpha,
            green = 0.47f + (1f - 0.47f) * alpha,
            blue = 0.87f + (1f - 0.87f) * alpha,
            alpha = 1f
        )
    }

    // Text colors
    val textPrimary = Color(0xFFABB2BF)
    val textSecondary = Color(0xFF5C6370)
    val textHighlight = Color(0xFFE5C07B)

    // Timeline colors
    val timelineAxis = Color(0xFF3E4452)
    val timelineYearMark = Color(0xFF5C6370)

    // Node dimensions
    const val nodeWidth = 180f
    const val nodeHeight = 60f
    const val nodeCornerRadius = 8f
    const val nodePadding = 8f

    // Timeline layout
    const val yearWidth = 300f       // Pixels per year (wide horizontal space)
    const val laneHeight = 200f      // Height per event type lane (room for stacking)
    const val timelineMarginTop = 70f  // Space for year headers
    const val timelineMarginLeft = 120f
    const val eventSpacingY = 10f    // Vertical gap between stacked events
}
