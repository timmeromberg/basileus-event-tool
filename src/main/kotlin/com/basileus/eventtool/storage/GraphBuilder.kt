package com.basileus.eventtool.storage

import com.basileus.eventtool.model.*

class GraphBuilder {

    fun buildEdges(events: List<EventNode>, outcomes: List<OutcomeNode>): List<EventEdge> {
        val edges = mutableListOf<EventEdge>()

        // Map: outcome ID -> events that produce it
        val producersByOutcome = mutableMapOf<String, MutableList<String>>()
        events.forEach { event ->
            event.producedOutcomes.values.flatten().forEach { outcomeId ->
                producersByOutcome.getOrPut(outcomeId) { mutableListOf() }.add(event.id)
            }
        }

        // For each event, find edges based on its required/forbidden outcomes
        events.forEach { consumer ->
            // Required outcomes (AND logic)
            consumer.requiredOutcomes.forEach { outcomeId ->
                producersByOutcome[outcomeId]?.forEach { producerId ->
                    if (producerId != consumer.id) {
                        edges.add(EventEdge(
                            fromEventId = producerId,
                            toEventId = consumer.id,
                            outcomeId = outcomeId,
                            type = EdgeType.REQUIRED
                        ))
                    }
                }
            }

            // Required outcomes any (OR logic)
            consumer.requiredOutcomesAny.forEach { group ->
                group.forEach { outcomeId ->
                    producersByOutcome[outcomeId]?.forEach { producerId ->
                        if (producerId != consumer.id) {
                            edges.add(EventEdge(
                                fromEventId = producerId,
                                toEventId = consumer.id,
                                outcomeId = outcomeId,
                                type = EdgeType.REQUIRED_ANY
                            ))
                        }
                    }
                }
            }

            // Forbidden outcomes
            consumer.forbiddenOutcomes.forEach { outcomeId ->
                producersByOutcome[outcomeId]?.forEach { producerId ->
                    if (producerId != consumer.id) {
                        edges.add(EventEdge(
                            fromEventId = producerId,
                            toEventId = consumer.id,
                            outcomeId = outcomeId,
                            type = EdgeType.FORBIDDEN
                        ))
                    }
                }
            }
        }

        println("Built ${edges.size} edges")
        return edges
    }

    fun getProducersOf(outcomeId: String, events: List<EventNode>): List<EventNode> {
        return events.filter { event ->
            event.producedOutcomes.values.flatten().contains(outcomeId)
        }
    }

    fun getConsumersOf(outcomeId: String, events: List<EventNode>): List<EventNode> {
        return events.filter { event ->
            event.requiredOutcomes.contains(outcomeId) ||
            event.requiredOutcomesAny.any { group -> group.contains(outcomeId) }
        }
    }

    fun getBlockedBy(outcomeId: String, events: List<EventNode>): List<EventNode> {
        return events.filter { event ->
            event.forbiddenOutcomes.contains(outcomeId)
        }
    }
}
