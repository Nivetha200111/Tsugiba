package com.tsugiba.nav

import com.tsugiba.nav.data.api.RouteMapper
import com.tsugiba.nav.data.api.response.*
import com.tsugiba.nav.data.model.ActivityMode
import com.tsugiba.nav.data.model.TrafficLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteMapperTest {

    private fun makeResponse(durationValue: Int, trafficValue: Int?) = DirectionsResponse(
        status = "OK",
        routes = listOf(
            RouteDto(
                summary = "Main St",
                overviewPolyline = PolylineDto("_p~iF~ps|U_ulLnnqC_mqNvxq`@"),
                warnings = emptyList(),
                legs = listOf(
                    LegDto(
                        distance = ValueDto(1000, "1 km"),
                        duration = ValueDto(durationValue, "${durationValue}s"),
                        durationInTraffic = trafficValue?.let { ValueDto(it, "${it}s") },
                        steps = listOf(
                            StepDto(
                                htmlInstructions = "Head north",
                                distance = ValueDto(1000, "1 km"),
                                duration = ValueDto(durationValue, "${durationValue}s"),
                                startLocation = LocationDto(37.7749, -122.4194),
                                endLocation = LocationDto(37.7849, -122.4194),
                                polyline = PolylineDto("_p~iF~ps|U_ulLnnqC")
                            )
                        )
                    )
                )
            )
        )
    )

    @Test
    fun `traffic level is CLEAR when no delay`() {
        val routes = RouteMapper.map(makeResponse(600, 610), ActivityMode.WALK)
        assertEquals(TrafficLevel.CLEAR, routes.first().trafficLevel)
    }

    @Test
    fun `traffic level is HEAVY when significant delay`() {
        val routes = RouteMapper.map(makeResponse(600, 900), ActivityMode.WALK)
        assertEquals(TrafficLevel.HEAVY, routes.first().trafficLevel)
    }

    @Test
    fun `hasTrafficDelay is true when delay exceeds 15 percent`() {
        val routes = RouteMapper.map(makeResponse(600, 720), ActivityMode.WALK)
        assertEquals(true, routes.first().hasTrafficDelay)
    }

    @Test
    fun `route summary falls back to Route 1 when blank`() {
        val response = makeResponse(600, null).copy(
            routes = makeResponse(600, null).routes.map { it.copy(summary = "") }
        )
        val routes = RouteMapper.map(response, ActivityMode.WALK)
        assertEquals("Route 1", routes.first().summary)
    }
}
