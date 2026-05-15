package com.tsugiba.nav.data.repository

import com.tsugiba.nav.BuildConfig
import com.tsugiba.nav.data.api.DirectionsApiService
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
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double,
        mode: ActivityMode
    ): Flow<Result<List<Route>>> = flow {
        runCatching {
            val origin = "$originLat,$originLng"
            val destination = "$destinationLat,$destinationLng"
            val response = directionsApi.getDirections(
                origin = origin,
                destination = destination,
                mode = mode.googleMode,
                apiKey = BuildConfig.MAPS_API_KEY
            )
            if (response.status != "OK") error("Directions API error: ${response.status}")
            RouteMapper.map(response, mode)
        }.also { emit(it) }
    }

    fun buildSuggestions(routes: List<Route>): List<TrafficSuggestion> {
        val suggestions = mutableListOf<TrafficSuggestion>()
        val primary = routes.firstOrNull() ?: return emptyList()

        when (primary.trafficLevel) {
            TrafficLevel.HEAVY -> suggestions.add(
                TrafficSuggestion(
                    id = "congestion_primary",
                    type = SuggestionType.CONGESTION_AHEAD,
                    title = "Heavy foot traffic ahead",
                    description = "Your current route has significant congestion."
                )
            )
            TrafficLevel.MODERATE -> suggestions.add(
                TrafficSuggestion(
                    id = "congestion_moderate",
                    type = SuggestionType.CONGESTION_AHEAD,
                    title = "Moderate traffic on route",
                    description = "Some congestion detected along your path."
                )
            )
            else -> {}
        }

        routes.drop(1).forEachIndexed { index, alt ->
            val saving = (primary.durationInTrafficSeconds ?: primary.durationSeconds) -
                    (alt.durationInTrafficSeconds ?: alt.durationSeconds)
            if (saving > 30) {
                suggestions.add(
                    TrafficSuggestion(
                        id = "alt_route_$index",
                        type = SuggestionType.FASTER_PATH,
                        title = "Faster route via ${alt.summary}",
                        description = "Save ~${saving / 60} min with ${alt.trafficLevel.displayName.lowercase()}.",
                        altRoute = alt,
                        timeSavingSeconds = saving
                    )
                )
            } else if (alt.trafficLevel == TrafficLevel.CLEAR &&
                primary.trafficLevel != TrafficLevel.CLEAR) {
                suggestions.add(
                    TrafficSuggestion(
                        id = "quiet_route_$index",
                        type = SuggestionType.QUIETER_STREET,
                        title = "Quieter route via ${alt.summary}",
                        description = "Less crowded path for a more comfortable walk.",
                        altRoute = alt,
                        timeSavingSeconds = saving
                    )
                )
            }
        }
        return suggestions
    }
}
