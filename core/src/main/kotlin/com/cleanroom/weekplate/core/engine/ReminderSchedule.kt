package com.cleanroom.weekplate.core.engine

import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

object ReminderSchedule {
    const val DEFAULT_MINUTES_AFTER_MIDNIGHT = 10 * 60

    fun next(
        now: ZonedDateTime,
        day: DayOfWeek = DayOfWeek.SUNDAY,
        minutesAfterMidnight: Int = DEFAULT_MINUTES_AFTER_MIDNIGHT
    ): ZonedDateTime {
        require(minutesAfterMidnight in 0 until 24 * 60)
        val hour = minutesAfterMidnight / 60
        val minute = minutesAfterMidnight % 60
        var candidate = now.toLocalDate()
            .with(TemporalAdjusters.nextOrSame(day))
            .atTime(hour, minute)
            .atZone(now.zone)
        if (!candidate.isAfter(now)) candidate = candidate.plusWeeks(1)
        return candidate
    }
}
