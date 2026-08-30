package com.example.un.utils

import java.util.Calendar

fun HolidayHelper.isHoliday(calendar: Calendar): Boolean {
    return isHoliday(kotlinx.datetime.LocalDate(
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    ))
}
