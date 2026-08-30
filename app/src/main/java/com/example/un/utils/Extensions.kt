package com.example.un.utils

import java.util.Calendar
import kotlinx.datetime.LocalDate

fun Calendar.toLocalDate(): LocalDate = LocalDate(
    get(Calendar.YEAR),
    get(Calendar.MONTH) + 1,
    get(Calendar.DAY_OF_MONTH)
)
