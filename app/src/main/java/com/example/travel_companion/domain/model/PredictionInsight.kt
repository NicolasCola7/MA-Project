package com.example.travel_companion.domain.model

data class PredictionInsight(
    val type: InsightType,
    val message: String,
    val actionText: String? = null,
    val actionType: ActionType? = null
)

enum class InsightType {
    ACHIEVEMENT, WARNING, SUGGESTION, INFO
}

enum class ActionType {
    PLAN_TRIP, VIEW_SUGGESTIONS, VIEW_STATISTICS
}