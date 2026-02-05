package com.basileus.eventtool.utils

import androidx.compose.ui.geometry.Offset
import com.basileus.eventtool.model.EventEdge
import com.basileus.eventtool.model.EventNode
import kotlin.math.sqrt
import kotlin.random.Random

data class NodePosition(
    val id: String,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f
)

class ForceDirectedLayout(
    private val width: Float = 1200f,
    private val height: Float = 800f
) {
    private val repulsionStrength = 5000f
    private val attractionStrength = 0.01f
    private val damping = 0.85f
    private val minDistance = 100f

    fun layout(
        events: List<EventNode>,
        edges: List<EventEdge>,
        iterations: Int = 100
    ): Map<String, Offset> {
        if (events.isEmpty()) return emptyMap()

        // Initialize positions randomly
        val positions = events.map { event ->
            NodePosition(
                id = event.id,
                x = Random.nextFloat() * width,
                y = Random.nextFloat() * height
            )
        }
        val positionMap = positions.associateBy { it.id }

        // Run simulation
        repeat(iterations) { iteration ->
            val temperature = 1f - (iteration.toFloat() / iterations)

            // Apply repulsion between all pairs
            for (i in positions.indices) {
                for (j in i + 1 until positions.size) {
                    applyRepulsion(positions[i], positions[j], temperature)
                }
            }

            // Apply attraction along edges
            edges.forEach { edge ->
                val from = positionMap[edge.fromEventId]
                val to = positionMap[edge.toEventId]
                if (from != null && to != null) {
                    applyAttraction(from, to, temperature)
                }
            }

            // Update positions
            positions.forEach { pos ->
                pos.x += pos.vx
                pos.y += pos.vy
                pos.vx *= damping
                pos.vy *= damping

                // Keep within bounds
                pos.x = pos.x.coerceIn(50f, width - 50f)
                pos.y = pos.y.coerceIn(50f, height - 50f)
            }
        }

        return positions.associate { it.id to Offset(it.x, it.y) }
    }

    private fun applyRepulsion(a: NodePosition, b: NodePosition, temperature: Float) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

        if (distance < minDistance * 3) {
            val force = repulsionStrength * temperature / (distance * distance)
            val fx = (dx / distance) * force
            val fy = (dy / distance) * force

            a.vx -= fx
            a.vy -= fy
            b.vx += fx
            b.vy += fy
        }
    }

    private fun applyAttraction(a: NodePosition, b: NodePosition, temperature: Float) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

        val force = attractionStrength * distance * temperature
        val fx = (dx / distance) * force
        val fy = (dy / distance) * force

        a.vx += fx
        a.vy += fy
        b.vx -= fx
        b.vy -= fy
    }
}
