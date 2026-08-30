package com.example.un.utils

import kotlin.math.roundToInt

object SalaryCalculator {

    data class SalaryResult(
        val totalHours: Double,
        val basePay: Double,
        val sup15Hours: Double = 0.0,
        val paySup15: Double = 0.0,
        val sup25Hours: Double,
        val paySup25: Double,
        val sup50Hours: Double,
        val paySup50: Double,
        val nightHours: Double,
        val payNight: Double,
        val sundayHolidayHours: Double = 0.0,
        val paySundayHoliday: Double = 0.0,
        val sundayHolidayPremium: Double = 0.0,
        val gardePremium: Double,
        val mealAllowance: Double,
        val totalNet: Double,
        val details: String
    )

    fun calculate(
        isAmbulancier: Boolean,
        isTaxi: Boolean,
        totalEffMillis: Long,
        totalSupMillis: Long,
        totalNightMillis: Long,
        sundayHolidayEffMillis: Long,
        tauxHoraire: Double,
        panierCount: Int,
        tauxPanier: Double,
        gardeCount: Int,
        has5PercentComp: Boolean = false,
        sundayHolidayCount: Int = 0,
        primeGardeSamu: Double = 30.0,
        baseHeures: Double = 151.67,
        tauxMajorNuit: Double = 25.0,
        primeDimanche: Double = 26.30,
        isCalcMensuel: Boolean = false
    ): SalaryResult {
        val realRate = if (has5PercentComp) tauxHoraire * 1.05 else tauxHoraire
        
        val totalHours = totalEffMillis / (1000.0 * 3600.0)
        val nightHours = totalNightMillis / (1000.0 * 3600.0)
        val sunHolHours = sundayHolidayEffMillis / (1000.0 * 3600.0)

        var supHours = 0.0
        if (isCalcMensuel) {
            if (totalHours > baseHeures) {
                supHours = totalHours - baseHeures
            }
        } else {
            supHours = totalSupMillis / (1000.0 * 3600.0)
        }

        val basePay = totalHours * realRate
        val paySup25 = supHours * realRate * 0.25
        val paySup50 = 0.0 
        
        var paySunHol = 0.0
        var sunHolPremium = 0.0

        if (isAmbulancier) {
            sunHolPremium = sundayHolidayCount * primeDimanche
        }
        
        if (isTaxi) {
            paySunHol += sunHolHours * realRate * 0.15
        }

        val payNight = nightHours * realRate * (tauxMajorNuit / 100.0)
        val gardePremium = gardeCount * primeGardeSamu
        val totalMeal = panierCount * tauxPanier

        val totalNet = basePay + paySup25 + paySup50 + payNight + paySunHol + sunHolPremium + gardePremium + totalMeal

        val sb = StringBuilder()
        sb.append("Temps de Travail (Amplitude 100%) : ${formatDuration(totalEffMillis)} (${round(totalHours)} h)\n")
        
        if (supHours > 0) {
            val label = if (isCalcMensuel) "Mensuel > ${round(baseHeures)}h" else "Cumul Journalier > 8h30"
            sb.append("Heures Sup 25% ($label) : ${round(supHours)} h\n")
        }
        
        sb.append("----------------------------\n")
        sb.append("Salaire de Base NET${if (has5PercentComp) " (+5%)" else ""} : ${round(basePay)} €\n")
        
        if (paySup25 > 0) sb.append("Majoration Heures Sup : ${round(paySup25)} €\n")
        
        if (nightHours > 0) sb.append("Prime Nuit (+${tauxMajorNuit.roundToInt()}%) : ${round(payNight)} €\n")
        
        if (sunHolPremium > 0) sb.append("Indemnités Dim/Férié ($sundayHolidayCount x ${round(primeDimanche)}€) : ${round(sunHolPremium)} €\n")
        if (paySunHol > 0) sb.append("Majoration Taxi Dim/Férié : ${round(paySunHol)} €\n")
        
        if (gardePremium > 0) sb.append("Primes Gardes ($gardeCount x ${round(primeGardeSamu)}€) : ${round(gardePremium)} €\n")
        if (totalMeal > 0) sb.append("Paniers repas ($panierCount x ${round(tauxPanier)}€) : ${round(totalMeal)} €")

        return SalaryResult(
            totalHours = totalHours,
            basePay = basePay,
            sup25Hours = supHours,
            paySup25 = paySup25,
            sup50Hours = 0.0,
            paySup50 = 0.0,
            nightHours = nightHours,
            payNight = payNight,
            sundayHolidayHours = sunHolHours,
            paySundayHoliday = paySunHol,
            sundayHolidayPremium = sunHolPremium,
            gardePremium = gardePremium,
            mealAllowance = totalMeal,
            totalNet = totalNet,
            details = sb.toString()
        )
    }

    private fun formatDuration(millis: Long): String {
        val h = millis / (1000 * 3600)
        val m = (millis / (1000 * 60)) % 60
        val mStr = if (m < 10) "0$m" else "$m"
        return "${h}h$mStr"
    }

    private fun round(value: Double): String {
        return ((value * 100.0).roundToInt() / 100.0).toString()
    }
}
