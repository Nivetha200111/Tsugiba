package com.tsugiba.nav.data.health

import com.tsugiba.nav.data.model.HealthSnapshot

object HealthInsightEngine {

    data class Insight(val summary: String, val suggestion: String)

    fun buildInsight(snapshot: HealthSnapshot): Insight {
        val sleepH = snapshot.sleepLastNightMinutes / 60
        val steps = snapshot.stepsToday
        val hr = snapshot.latestHeartRateBpm

        val suggestion = when {
            sleepH in 1..4 ->
                "😴 You barely slept — your body needs rest. Light walk only today."
            hr > 100 ->
                "💓 Heart rate's elevated — ease into it with a gentle stroll."
            sleepH >= 7 && steps < 2000 && (hr == 0 || hr < 85) ->
                "🔥 Well rested and barely moved — perfect day to push it. Go run!"
            sleepH >= 6 && steps < 5000 ->
                "🏃 Good sleep, decent energy — a brisk jog would set you up nicely."
            steps > 12000 ->
                "✅ You've smashed 12k steps already! A cool-down walk is plenty."
            steps in 7000..12000 ->
                "💟 Solid step count — short jog to round it off?"
            sleepH >= 5 && steps < 3000 ->
                "🚶 Haven't moved much yet — great time to get out!"
            else ->
                "👍 Looks good — pick your pace and go!"
        }

        return Insight(buildSummary(snapshot), suggestion)
    }

    private fun buildSummary(s: HealthSnapshot): String = buildList {
        if (s.sleepLastNightMinutes > 0) {
            val h = s.sleepLastNightMinutes / 60
            val m = s.sleepLastNightMinutes % 60
            add("💤 ${h}h${if (m > 0) " ${m}m" else ""}")
        }
        add("🦶 ${formatSteps(s.stepsToday)}")
        if (s.latestHeartRateBpm > 0) add("❤️ ${s.latestHeartRateBpm}bpm")
        if (s.activeCaloriesToday > 0) add("🔥 ${s.activeCaloriesToday.toInt()}kcal")
    }.joinToString("  ")

    private fun formatSteps(steps: Long): String =
        if (steps >= 1000) "${steps / 1000}.${(steps % 1000) / 100}k" else "$steps"
}
