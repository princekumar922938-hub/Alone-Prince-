package com.example.util

import com.example.model.DateRange
import com.example.model.DateRangeOption
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object DateUtils {

    val KOLKATA_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")
    private val INDIAN_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val MONTH_YEAR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

    fun today(): LocalDate = LocalDate.now(KOLKATA_ZONE)

    fun todayEpochDay(): Long = today().toEpochDay()

    fun formatDate(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(INDIAN_DATE_FORMATTER)
    }

    fun formatMonthYear(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(MONTH_YEAR_FORMATTER)
    }

    fun parseDate(dateStr: String): Long? {
        return try {
            LocalDate.parse(dateStr, INDIAN_DATE_FORMATTER).toEpochDay()
        } catch (_: Exception) {
            null
        }
    }

    fun toIsoMonth(date: LocalDate): String {
        return String.format("%04d-%02d", date.year, date.monthValue)
    }

    fun currentMonthKey(): String {
        return toIsoMonth(today())
    }

    fun currentTimeString(): String {
        val now = java.time.LocalTime.now(KOLKATA_ZONE)
        return now.format(TIME_FORMATTER)
    }

    fun getDateRange(
        option: DateRangeOption,
        customStart: Long? = null,
        customEnd: Long? = null
    ): DateRange {
        val now = today()
        return when (option) {
            DateRangeOption.TODAY -> {
                val epoch = now.toEpochDay()
                DateRange(epoch, epoch, "Today (${formatDate(epoch)})")
            }
            DateRangeOption.YESTERDAY -> {
                val yEpoch = now.minusDays(1).toEpochDay()
                DateRange(yEpoch, yEpoch, "Yesterday (${formatDate(yEpoch)})")
            }
            DateRangeOption.LAST_7_DAYS -> {
                val start = now.minusDays(6).toEpochDay()
                val end = now.toEpochDay()
                DateRange(start, end, "Last 7 Days")
            }
            DateRangeOption.THIS_MONTH -> {
                val start = now.withDayOfMonth(1).toEpochDay()
                val end = now.with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
                DateRange(start, end, "This Month (${now.format(MONTH_YEAR_FORMATTER)})")
            }
            DateRangeOption.LAST_MONTH -> {
                val prevMonth = now.minusMonths(1)
                val start = prevMonth.withDayOfMonth(1).toEpochDay()
                val end = prevMonth.with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
                DateRange(start, end, "Last Month (${prevMonth.format(MONTH_YEAR_FORMATTER)})")
            }
            DateRangeOption.THIS_QUARTER -> {
                val quarter = (now.monthValue - 1) / 3 + 1
                val startMonth = (quarter - 1) * 3 + 1
                val start = LocalDate.of(now.year, startMonth, 1).toEpochDay()
                val end = LocalDate.of(now.year, startMonth + 2, 1)
                    .with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
                DateRange(start, end, "Q$quarter ${now.year}")
            }
            DateRangeOption.LAST_QUARTER -> {
                var quarter = (now.monthValue - 1) / 3
                var year = now.year
                if (quarter == 0) {
                    quarter = 4
                    year -= 1
                }
                val startMonth = (quarter - 1) * 3 + 1
                val start = LocalDate.of(year, startMonth, 1).toEpochDay()
                val end = LocalDate.of(year, startMonth + 2, 1)
                    .with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
                DateRange(start, end, "Q$quarter $year")
            }
            DateRangeOption.THIS_YEAR -> {
                val start = LocalDate.of(now.year, 1, 1).toEpochDay()
                val end = LocalDate.of(now.year, 12, 31).toEpochDay()
                DateRange(start, end, "Year ${now.year}")
            }
            DateRangeOption.LAST_YEAR -> {
                val lastYear = now.year - 1
                val start = LocalDate.of(lastYear, 1, 1).toEpochDay()
                val end = LocalDate.of(lastYear, 12, 31).toEpochDay()
                DateRange(start, end, "Year $lastYear")
            }
            DateRangeOption.CUSTOM -> {
                val s = customStart ?: now.minusDays(30).toEpochDay()
                val e = customEnd ?: now.toEpochDay()
                DateRange(minOf(s, e), maxOf(s, e), "${formatDate(minOf(s, e))} - ${formatDate(maxOf(s, e))}")
            }
            DateRangeOption.ALL_TIME -> {
                DateRange(0L, now.plusYears(10).toEpochDay(), "All Time")
            }
        }
    }

    fun getQuarterRange(year: Int, quarter: Int): DateRange {
        val startMonth = (quarter - 1) * 3 + 1
        val start = LocalDate.of(year, startMonth, 1).toEpochDay()
        val end = LocalDate.of(year, startMonth + 2, 1)
            .with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
        return DateRange(start, end, "Q$quarter $year")
    }

    fun getMonthRange(year: Int, month: Int): DateRange {
        val start = LocalDate.of(year, month, 1).toEpochDay()
        val end = LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth()).toEpochDay()
        val label = LocalDate.of(year, month, 1).format(MONTH_YEAR_FORMATTER)
        return DateRange(start, end, label)
    }

    fun getYearRange(year: Int): DateRange {
        val start = LocalDate.of(year, 1, 1).toEpochDay()
        val end = LocalDate.of(year, 12, 31).toEpochDay()
        return DateRange(start, end, "Year $year")
    }
}
