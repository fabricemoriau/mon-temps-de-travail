package com.example.un.utils

import java.util.*

object HolidayHelper {

    fun isHoliday(calendar: Calendar): Boolean {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1 // 1-12
        val year = calendar.get(Calendar.YEAR)

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
        val easterMonday = easter.clone() as Calendar
        easterMonday.add(Calendar.DAY_OF_YEAR, 1)
        if (isSameDay(calendar, easterMonday)) return true

        // Ascension (Pâques + 39 jours)
        val ascension = easter.clone() as Calendar
        ascension.add(Calendar.DAY_OF_YEAR, 39)
        if (isSameDay(calendar, ascension)) return true

        // Lundi de Pentecôte (Pâques + 50 jours)
        val pentecostMonday = easter.clone() as Calendar
        pentecostMonday.add(Calendar.DAY_OF_YEAR, 50)
        if (isSameDay(calendar, pentecostMonday)) return true

        return false
    }

    private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getEaster(year: Int): Calendar {
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
        
        return Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
