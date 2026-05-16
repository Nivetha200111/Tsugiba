package com.tsugiba.nav.data.model

data class RouteHint(
    val instruction: String,
    val distanceLabel: String,
    val type: HintType
)

enum class HintType { TURN_LEFT, TURN_RIGHT, STRAIGHT, ROUNDABOUT, ARRIVE }
