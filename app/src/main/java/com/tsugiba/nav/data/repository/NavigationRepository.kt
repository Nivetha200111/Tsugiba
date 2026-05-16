package com.tsugiba.nav.data.repository

import com.tsugiba.nav.BuildConfig
import com.tsugiba.nav.data.api.DirectionsApiService
import com.tsugiba.nav.data.api.LandmarkHintEngine
import com.tsugiba.nav.data.api.RouteMapper
import com.tsugiba.nav.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationRepository @Inject constructor(
    private val directionsApi: DirectionsApiService
) {
    fun getRoutes(
        originLat: Double, originLng: Double,
        destinationLat: Double, destinationLng: Double,
        mode: ActivityMode
    ): Flow<Result<List<Route>>> = flow {
        runCatching {
            val response = directionsApi.getDirections(
                origin = "$originLat,$originLng",
                destination = "$destinationLat,$destinationLng",
                mode = mode.googleMode,
                apiKey = BuildConfig.MAPS_API_KEY
            )
            if (response.status != "OK") error("Directions API: ${response.status}")
            val routes = RouteMapper.map(response, mode)
            if (routes.isEmpty()) error("No routes found for this destination")
            routes
        }.also { emit(it) }
    }

    fun buildSuggestions(routes: List<Route>): List<TrafficSuggestion> {
        val suggestions = mutableListOf<TrafficSuggestion>()
        val primary = routes.firstOrNull() ?: return emptyList()

        if (primary.trafficLevel == TrafficLevel.HEAVY || primary.trafficLevel == TrafficLevel.MODERATE) {
            suggestions.add(TrafficSuggestion(
                id = "congestion_primary",
                type = SuggestionType.CONGESTION_AHEAD,
                title = if (primary.trafficLevel == TrafficLevel.HEAVY) "Jammed ahead" else "Getting crowded",
                description = LandmarkHintEngine.buildCongestionMessage(primary)
            ))
        }

        routes.drop(1).forEachIndexed { index, alt ->
            val saving = (primary.durationInTrafficSeconds ?: primary.durationSeconds) -
                    (alt.durationInTrafficSeconds ?: alt.durationSeconds)
            val isQuieter = alt.trafficLevel.ordinal < primary.trafficLevel.ordinal
            if (saving > 30 || isQuieter) {
                val type = if (saving > 30) SuggestionType.FASTER_PATH else SuggestionType.QUIETER_STREET
                suggestions.add(TrafficSuggestion(
                    id = "alt_route_$index",
                    type = type,
                    title = if (saving > 30) "Faster route found" else "Quieter path available",
                    description = LandmarkHintEngine.buildTrafficMessage(primary, alt),
                    altRoute = alt,
                    timeSavingSeconds = maxOf(saving, 0)
                ))
            }
        }
        return suggestions
    }
}
