package com.tsugiba.nav

import com.tsugiba.nav.data.model.ActivityMode
import com.tsugiba.nav.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test fun `meters under 1000 shows m`() = assertEquals("500 m", FormatUtils.formatDistance(500))
    @Test fun `meters over 1000 shows km`() = assertEquals("1.5 km", FormatUtils.formatDistance(1500))
    @Test fun `duration in minutes`() = assertEquals("5 min", FormatUtils.formatDuration(300))
    @Test fun `duration with hours`() = assertEquals("1h 30m", FormatUtils.formatDuration(5400))
    @Test fun `calories for walk`() {
        val cal = FormatUtils.estimatedCalories(5000, ActivityMode.WALK)
        assert(cal in 150..350) { "Unexpected: $cal" }
    }
}
