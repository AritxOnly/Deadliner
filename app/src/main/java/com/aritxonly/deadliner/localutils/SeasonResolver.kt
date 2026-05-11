package com.aritxonly.deadliner.localutils

import com.aritxonly.deadliner.model.Season
import java.time.LocalDate

object SeasonResolver {
    private data class MonthDay(val month: Int, val day: Int)

    private data class SeasonalBoundary(
        val lichun: MonthDay,
        val lixia: MonthDay,
        val liqiu: MonthDay,
        val lidong: MonthDay,
    )

    private val boundaryTable = mapOf(
        2020 to SeasonalBoundary(MonthDay(2, 4), MonthDay(5, 5), MonthDay(8, 7), MonthDay(11, 7)),
        2021 to SeasonalBoundary(MonthDay(2, 3), MonthDay(5, 5), MonthDay(8, 7), MonthDay(11, 7)),
        2022 to SeasonalBoundary(MonthDay(2, 4), MonthDay(5, 5), MonthDay(8, 7), MonthDay(11, 7)),
        2023 to SeasonalBoundary(MonthDay(2, 4), MonthDay(5, 6), MonthDay(8, 8), MonthDay(11, 8)),
        2024 to SeasonalBoundary(MonthDay(2, 4), MonthDay(5, 5), MonthDay(8, 7), MonthDay(11, 7)),
        2025 to SeasonalBoundary(MonthDay(2, 3), MonthDay(5, 5), MonthDay(8, 7), MonthDay(11, 7)),
        2026 to SeasonalBoundary(MonthDay(2, 4), MonthDay(5, 5), MonthDay(8, 8), MonthDay(11, 7)),
        2027 to SeasonalBoundary(MonthDay(2, 4), MonthDay(5, 5), MonthDay(8, 8), MonthDay(11, 7)),
        2028 to SeasonalBoundary(MonthDay(2, 4), MonthDay(5, 5), MonthDay(8, 7), MonthDay(11, 7)),
        2029 to SeasonalBoundary(MonthDay(2, 3), MonthDay(5, 5), MonthDay(8, 7), MonthDay(11, 7)),
        2030 to SeasonalBoundary(MonthDay(2, 4), MonthDay(5, 5), MonthDay(8, 8), MonthDay(11, 7)),
        2031 to SeasonalBoundary(MonthDay(2, 3), MonthDay(5, 5), MonthDay(8, 7), MonthDay(11, 7)),
        2032 to SeasonalBoundary(MonthDay(2, 4), MonthDay(5, 5), MonthDay(8, 7), MonthDay(11, 7)),
        2033 to SeasonalBoundary(MonthDay(2, 3), MonthDay(5, 5), MonthDay(8, 7), MonthDay(11, 7)),
        2034 to SeasonalBoundary(MonthDay(2, 4), MonthDay(5, 5), MonthDay(8, 7), MonthDay(11, 7)),
        2035 to SeasonalBoundary(MonthDay(2, 4), MonthDay(5, 5), MonthDay(8, 7), MonthDay(11, 7)),
    )

    fun resolve(date: LocalDate = LocalDate.now()): Season {
        val boundary = boundaryTable[date.year] ?: return fallbackByMonth(date)
        val lichun = LocalDate.of(date.year, boundary.lichun.month, boundary.lichun.day)
        val lixia = LocalDate.of(date.year, boundary.lixia.month, boundary.lixia.day)
        val liqiu = LocalDate.of(date.year, boundary.liqiu.month, boundary.liqiu.day)
        val lidong = LocalDate.of(date.year, boundary.lidong.month, boundary.lidong.day)

        return when {
            !date.isBefore(lichun) && date.isBefore(lixia) -> Season.SPRING
            !date.isBefore(lixia) && date.isBefore(liqiu) -> Season.SUMMER
            !date.isBefore(liqiu) && date.isBefore(lidong) -> Season.AUTUMN
            else -> Season.WINTER
        }
    }

    private fun fallbackByMonth(date: LocalDate): Season {
        return when (date.monthValue) {
            in 3..5 -> Season.SPRING
            in 6..8 -> Season.SUMMER
            in 9..11 -> Season.AUTUMN
            else -> Season.WINTER
        }
    }
}
