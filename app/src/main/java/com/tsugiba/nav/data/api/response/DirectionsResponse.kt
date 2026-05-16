package com.tsugiba.nav.data.api.response

import com.google.gson.annotations.SerializedName

data class DirectionsResponse(
    val status: String,
    val routes: List<RouteDto>
)

data class RouteDto(
    val summary: String,
    @SerializedName("overview_polyline") val overviewPolyline: PolylineDto,
    val legs: List<LegDto>,
    val warnings: List<String>
)

data class LegDto(
    val distance: ValueDto,
    val duration: ValueDto,
    @SerializedName("duration_in_traffic") val durationInTraffic: ValueDto?,
    val steps: List<StepDto>
)

data class StepDto(
    @SerializedName("html_instructions") val htmlInstructions: String,
    val distance: ValueDto,
    val duration: ValueDto,
    @SerializedName("start_location") val startLocation: LocationDto,
    @SerializedName("end_location") val endLocation: LocationDto,
    val polyline: PolylineDto
)

data class ValueDto(val value: Int, val text: String)
data class LocationDto(val lat: Double, val lng: Double)
data class PolylineDto(val points: String)
