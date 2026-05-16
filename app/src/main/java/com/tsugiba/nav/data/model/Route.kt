package com.tsugiba.nav.data.model

data class Route(
    val id: String,
    val summary: String,
    val legs: List<RouteLeg>,
    val overviewPolyline: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val durationInTrafficSeconds: Int?,
    val activityMode: ActivityMode,
    val trafficLevel: TrafficLevel
) {
    val hasTrafficDelay: Boolean
        get() = durationInTrafficSeconds != null &&
                durationInTrafficSeconds > durationSeconds * 1.15
}

data class RouteLeg(
    val steps: List<RouteStep>,
    val distanceMeters: Int,
    val durationSeconds: Int
)

data class RouteStep(
    val instruction: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val startLocation: LatLng,
    val endLocation: LatLng,
    val polyline: String
)

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

enum class ActivityMode(val label: String, val googleMode: String, val avgSpeedKmh: Float) {
    WALK("Walk", "walking", 5f),
    JOG("Jog", "walking", 8f),
    RUN("Run", "walking", 12f)
}

enum class TrafficLevel(val displayName: String) {
    CLEAR("Clear"),
    LIGHT("Light traffic"),
    MODERATE("Moderate traffic"),
    HEAVY("Heavy traffic"),
    UNKNOWN("Unknown")
}
