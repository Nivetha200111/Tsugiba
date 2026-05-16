package com.tsugiba.nav.data.model

data class HealthSnapshot(
    val stepsToday: Long,
    val sleepLastNightMinutes: Long,
    val latestHeartRateBpm: Int,
    val activeCaloriesToday: Double
)
