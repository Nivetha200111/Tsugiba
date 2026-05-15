package com.tsugiba.nav.util

object FormatUtils {
    fun formatDistance(meters: Int): String = when {
        meters >= 1000 -> "%.1f km".format(meters / 1000.0)
        else -> "$meters m"
    }

    fun formatDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "%dh %02dm".format(h, m)
            m > 0 -> "%d min".format(m)
            else -> "$s sec"
        }
    }

    fun estimatedCalories(distanceMeters: Int, mode: com.tsugiba.nav.data.model.ActivityMode): Int {
        val metPerMode = when (mode) {
            com.tsugiba.nav.data.model.ActivityMode.WALK -> 3.5
            com.tsugiba.nav.data.model.ActivityMode.JOG  -> 7.0
            com.tsugiba.nav.data.model.ActivityMode.RUN  -> 10.0
        }
        val weightKg = 70.0
        val hours = (distanceMeters / 1000.0) / mode.avgSpeedKmh
        return (metPerMode * weightKg * hours).toInt()
    }
}
