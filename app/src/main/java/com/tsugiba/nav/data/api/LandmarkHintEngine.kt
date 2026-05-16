package com.tsugiba.nav.data.api

import com.tsugiba.nav.data.model.*

object LandmarkHintEngine {

    fun buildHints(route: Route): List<RouteHint> =
        route.legs.flatMap { it.steps }.take(8).map { buildHint(it) }

    private fun buildHint(step: RouteStep): RouteHint {
        val instr = step.instruction
        val type = when {
            instr.contains("left", ignoreCase = true) -> HintType.TURN_LEFT
            instr.contains("right", ignoreCase = true) -> HintType.TURN_RIGHT
            instr.contains("roundabout", ignoreCase = true) ||
            instr.contains("circle", ignoreCase = true) -> HintType.ROUNDABOUT
            instr.contains("arrive", ignoreCase = true) ||
            instr.contains("destination", ignoreCase = true) -> HintType.ARRIVE
            else -> HintType.STRAIGHT
        }
        val distLabel = when {
            step.distanceMeters < 100 -> "just ahead"
            step.distanceMeters < 1000 -> "in ${step.distanceMeters}m"
            else -> "in ${"%.1f".format(step.distanceMeters / 1000.0)}km"
        }
        return RouteHint(humanify(instr, type), distLabel, type)
    }

    private fun humanify(raw: String, type: HintType): String {
        val clean = raw
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("^(Head|Continue|Walk)\\s+", RegexOption.IGNORE_CASE), "")
            .trimStart()
            .replaceFirstChar { it.uppercaseChar() }
        return when (type) {
            HintType.TURN_LEFT -> "Go left — $clean"
            HintType.TURN_RIGHT -> "Go right — $clean"
            HintType.ARRIVE -> "🏁 You're here! $clean"
            HintType.ROUNDABOUT -> "Take the roundabout — $clean"
            HintType.STRAIGHT -> clean
        }
    }

    fun buildTrafficMessage(primary: Route, alt: Route): String {
        val savingMin = ((primary.durationInTrafficSeconds ?: primary.durationSeconds) -
                (alt.durationInTrafficSeconds ?: alt.durationSeconds)) / 60
        val mainRoad = primary.summary.ifBlank { "main road" }
        val sideRoad = alt.summary.ifBlank { "this route" }
        return when (primary.trafficLevel) {
            TrafficLevel.HEAVY -> when {
                savingMin > 5 -> "$mainRoad is packed right now — cut through $sideRoad instead, saves ~${savingMin} min"
                else -> "$mainRoad is jammed — $sideRoad is way clearer"
            }
            TrafficLevel.MODERATE -> "Bit of a crowd on $mainRoad — $sideRoad is moving better"
            else -> "Quieter path via $sideRoad — skip the busier $mainRoad"
        }
    }

    fun buildCongestionMessage(route: Route): String {
        val road = route.summary.ifBlank { "your route" }
        return when (route.trafficLevel) {
            TrafficLevel.HEAVY -> "$road is pretty jammed ahead — long-press the map to check a side path"
            TrafficLevel.MODERATE -> "Getting a bit crowded on $road — manageable but slow"
            else -> "Clear ahead on $road — you're good to go"
        }
    }
}
