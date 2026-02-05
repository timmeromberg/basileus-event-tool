package com.basileus.eventtool.model

data class OutcomeNode(
    val id: String,
    val name: String,
    val historicityScore: Int,
    val historicalImpactScore: Int,
    val category: String,
    val playerDescription: String?
)
