package com.tsugiba.nav.data.api

import android.text.Html
import com.tsugiba.nav.data.api.response.DirectionsResponse
import com.tsugiba.nav.data.api.response.LegDto
import com.tsugiba.nav.data.api.response.RouteDto
import com.tsugiba.nav.data.model.*
import java.util.UUID

object RouteMapper {

    fun map(response: DirectionsResponse, mode: ActivityMode): List<Route> =
        response.routes.mapIndexed { index, dto -> mapRoute(dto, index, mode) }

    private fun mapRoute(dto: RouteDto, index: Int, mode: ActivityMode): Route {
        val legs = dto.legs.map { mapLeg(it) }
        val totalDistance = legs.sumOf { it.distanceMeters }
        val totalDuration = legs.sumOf { it.durationSeconds }
        val trafficDuration = dto.legs.sumOf { it.durationInTraffic?.value ?: it.duration.value }
        val trafficLevel = computeTrafficLevel(totalDuration, trafficDuration)
        return Route(
            id = UUID.randomUUID().toString(),
            summary = dto.summary.ifBlank { "Route ${index + 1}" },
            legs = legs,
            overviewPolyline = dto.overviewPolyline.points,
            distanceMeters = totalDistance,
            durationSeconds = totalDuration,
            durationInTrafficSeconds = trafficDuration.takeIf { dto.legs.any { l -> l.durationInTraffic != null } },
            activityMode = mode,
            trafficLevel = trafficLevel
        )
    }

    private fun mapLeg(dto: LegDto) = RouteLeg(
        steps = dto.steps.map { step ->
            RouteStep(
                instruction = Html.fromHtml(step.htmlInstructions, Html.FROM_HTML_MODE_COMPACT).toString(),
                distanceMeters = step.distance.value,
                durationSeconds = step.duration.value,
                startLocation = LatLng(step.startLocation.lat, step.startLocation.lng),
                endLocation = LatLng(step.endLocation.lat, step.endLocation.lng),
                polyline = step.polyline.points
            )
        },
        distanceMeters = dto.distance.value,
        durationSeconds = dto.duration.value
    )

    private fun computeTrafficLevel(baseDuration: Int, trafficDuration: Int): TrafficLevel {
        if (baseDuration == 0) return TrafficLevel.UNKNOWN
        val ratio = trafficDuration.toFloat() / baseDuration
        return when {
            ratio < 1.05f -> TrafficLevel.CLEAR
            ratio < 1.15f -> TrafficLevel.LIGHT
            ratio < 1.30f -> TrafficLevel.MODERATE
            else -> TrafficLevel.HEAVY
        }
    }
}
