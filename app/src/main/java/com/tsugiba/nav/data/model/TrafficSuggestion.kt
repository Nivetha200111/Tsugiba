package com.tsugiba.nav.data.model

data class TrafficSuggestion(
    val id: String,
    val type: SuggestionType,
    val title: String,
    val description: String,
    val altRoute: Route? = null,
    val timeSavingSeconds: Int = 0
)

enum class SuggestionType {
    ALTERNATE_ROUTE,
    CONGESTION_AHEAD,
    INCIDENT_AHEAD,
    FASTER_PATH,
    QUIETER_STREET
}
