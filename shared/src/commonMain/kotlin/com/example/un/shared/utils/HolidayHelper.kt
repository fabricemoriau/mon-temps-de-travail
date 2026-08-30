package com.example.un.utils

import kotlinx.datetime.*

object HolidayHelper {

    fun isHoliday(date: LocalDate): Boolean {
        val day = date.dayOfMonth
        val month = date.monthNumber
        val year = date.year

        // Fixes
        if (day == 1 && month == 1) return true // Jour de l'an
        if (day == 1 && month == 5) return true // Fête du travail
        if (day == 8 && month == 5) return true // Victoire 1945
        if (day == 14 && month == 7) return true // Fête nationale
        if (day == 15 && month == 8) return true // Assomption
        if (day == 1 && month == 11) return true // Toussaint
        if (day == 11 && month == 11) return true // Armistice 1918
        if (day == 25 && month == 12) return true // Noël

        // Mobiles (Calcul de Pâques)
        val easter = getEaster(year)
        
        // Lundi de Pâques (Pâques + 1 jour)
        val easterMonday = easter.plus(1, DateTimeUnit.DAY)
        if (date == easterMonday) return true

        // Ascension (Pâques + 39 jours)
        val ascension = easter.plus(39, DateTimeUnit.DAY)
        if (date == ascension) return true

        // Lundi de Pentecôte (Pâques + 50 jours)
        val pentecostMonday = easter.plus(50, DateTimeUnit.DAY)
        if (date == pentecostMonday) return true

        return false
    }

    private fun getEaster(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        
        return LocalDate(year, month, day)
    }
}
