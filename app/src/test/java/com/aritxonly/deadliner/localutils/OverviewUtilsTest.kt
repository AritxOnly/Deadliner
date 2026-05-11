package com.aritxonly.deadliner.localutils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aritxonly.deadliner.R
import com.aritxonly.deadliner.model.DDLItem
import com.aritxonly.deadliner.model.DDLState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class OverviewUtilsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        GlobalUtils.init(context)
    }

    @Test
    fun parseDateTime_supportsUtcIsoInstant() {
        val parsed = GlobalUtils.parseDateTime("2026-03-31T16:59:00.407Z")
        val expected = java.time.Instant.parse("2026-03-31T16:59:00.407Z")
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        assertEquals(expected, parsed)
    }

    @Test
    fun buildSnapshot_usesDueMonthForMonthlyTrendAndDueDayForOverdueTrend() {
        val now = LocalDateTime.of(2026, 5, 11, 10, 0)
        val items = listOf(
            task(
                name = "cross month task",
                endTime = "2026-04-05 18:00",
                isCompleted = true,
                completeTime = "2026-05-01 08:00",
            ),
            task(
                name = "overdue on may 10",
                endTime = "2026-05-10 12:00",
            ),
        )

        val snapshot = OverviewUtils.buildSnapshot(context, items, now)

        val april = snapshot.monthlyStats.first { it.month == "4月" }
        val may = snapshot.monthlyStats.first { it.month == "5月" }
        assertEquals(1, april.totalCount)
        assertEquals(1, april.completedCount)
        assertEquals(1, april.overdueCompletedCount)
        assertEquals(1, may.totalCount)
        assertEquals(0, may.completedCount)

        val may10 = snapshot.dailyStats.first { it.label == "05-10" }
        assertEquals(1, may10.overdueCount)
    }

    @Test
    fun buildSnapshot_alignsTodayAndHistoryStatsWithIosOverviewRules() {
        val now = LocalDateTime.of(2026, 5, 11, 10, 0)
        val items = listOf(
            task(
                name = "done today",
                endTime = "2026-05-11 09:00",
                isCompleted = true,
                completeTime = "2026-05-11 09:30",
            ),
            task(
                name = "pending future",
                endTime = "2026-05-12 09:00",
            ),
            task(
                name = "overdue today",
                endTime = "2026-05-11 08:00",
            ),
            task(
                name = "abandoned active",
                endTime = "2026-05-11 07:00",
                state = DDLState.ABANDONED,
            ),
            task(
                name = "abandoned archived",
                endTime = "2026-04-20 07:00",
                state = DDLState.ABANDONED_ARCHIVED,
                isArchived = true,
            ),
        )

        val snapshot = OverviewUtils.buildSnapshot(context, items, now)

        assertEquals(1, snapshot.activeStats.getValue(context.getString(R.string.today_completed)))
        assertEquals(1, snapshot.activeStats.getValue(context.getString(R.string.pending_tasks)))
        assertEquals(1, snapshot.activeStats.getValue(context.getString(R.string.today_overdue)))
        assertEquals(1, snapshot.activeStats.getValue(context.getString(R.string.abandoned)))

        assertEquals(1, snapshot.historyStats.getValue(context.getString(R.string.cumulative_completed)))
        assertEquals(2, snapshot.historyStats.getValue(context.getString(R.string.current_pending)))
        assertEquals(2, snapshot.historyStats.getValue(context.getString(R.string.cumulative_abandoned)))
        assertEquals(1, snapshot.historyStats.getValue(context.getString(R.string.cumulative_overdue)))
    }

    @Test
    fun buildSnapshot_countsUnfinishedAbandonedTasksInLastMonthOverdueMetric() {
        val now = LocalDateTime.of(2026, 5, 11, 10, 0)
        val items = listOf(
            task(
                name = "active unfinished",
                endTime = "2026-04-10 09:00",
            ),
            task(
                name = "abandoned unfinished",
                endTime = "2026-04-11 09:00",
                state = DDLState.ABANDONED,
            ),
            task(
                name = "completed",
                endTime = "2026-04-12 09:00",
                isCompleted = true,
                completeTime = "2026-04-12 10:00",
            ),
        )

        val snapshot = OverviewUtils.buildSnapshot(context, items, now)

        val overdueMetric = snapshot.metrics.first {
            it.label == context.getString(R.string.metric_last_month_overdue)
        }
        assertEquals("2", overdueMetric.value)
    }

    private fun task(
        name: String,
        startTime: String = "2026-04-01 09:00",
        endTime: String,
        isCompleted: Boolean = false,
        completeTime: String = "",
        state: DDLState = if (isCompleted) DDLState.COMPLETED else DDLState.ACTIVE,
        isArchived: Boolean = state.isArchivedFamily(),
    ): DDLItem {
        return DDLItem(
            id = name.hashCode().toLong(),
            name = name,
            startTime = startTime,
            endTime = endTime,
            isCompleted = isCompleted,
            state = state,
            completeTime = completeTime,
            note = "",
            isArchived = isArchived,
        )
    }
}
