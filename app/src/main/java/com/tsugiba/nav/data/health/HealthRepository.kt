package com.tsugiba.nav.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.tsugiba.nav.data.model.HealthSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        )
    }

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    suspend fun hasPermissions(): Boolean = runCatching {
        client().permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
    }.getOrDefault(false)

    suspend fun getSnapshot(): Result<HealthSnapshot> = runCatching {
        val c = client()
        val now = Instant.now()
        val startOfDay = ZonedDateTime.now(ZoneId.systemDefault())
            .toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val sleepWindowStart = now.minus(18, ChronoUnit.HOURS)

        val steps = c.readRecords(
            ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(startOfDay, now))
        ).records.sumOf { it.count }

        val latestHr = c.readRecords(
            ReadRecordsRequest(HeartRateRecord::class,
                TimeRangeFilter.between(now.minus(2, ChronoUnit.HOURS), now))
        ).records.lastOrNull()?.samples?.lastOrNull()?.beatsPerMinute?.toInt() ?: 0

        val sleepMinutes = c.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class,
                TimeRangeFilter.between(sleepWindowStart, now))
        ).records.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }

        val calories = c.readRecords(
            ReadRecordsRequest(ActiveCaloriesBurnedRecord::class,
                TimeRangeFilter.between(startOfDay, now))
        ).records.sumOf { it.energy.inKilocalories }

        HealthSnapshot(steps, sleepMinutes, latestHr, calories)
    }
}
