package com.example.un

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.montempsdetravail.R
import com.example.un.data.local.DatabaseHelper
import com.example.un.data.local.WorkDayEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VacationPeriodActivity : AppCompatActivity() {

    private var startDate = Calendar.getInstance()
    private var endDate = Calendar.getInstance()
    private val sdfDisplay = SimpleDateFormat("dd MMMM yyyy", Locale.FRANCE)
    private val sdfId = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vacation_period)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Période de Congés"

        val btnStart = findViewById<Button>(R.id.btnStartDate)
        val btnEnd = findViewById<Button>(R.id.btnEndDate)
        val btnConfirm = findViewById<Button>(R.id.btnConfirmVacation)

        btnStart.setOnClickListener {
            showDatePicker { cal ->
                startDate = cal
                btnStart.text = sdfDisplay.format(cal.time)
            }
        }

        btnEnd.setOnClickListener {
            showDatePicker { cal ->
                endDate = cal
                btnEnd.text = sdfDisplay.format(cal.time)
            }
        }

        btnConfirm.setOnClickListener {
            if (endDate.before(startDate)) {
                Toast.makeText(this, "La fin doit être après le début", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveVacationPeriod()
        }
    }

    private fun showDatePicker(onDateSelected: (Calendar) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val result = Calendar.getInstance().apply { set(y, m, d) }
            onDateSelected(result)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveVacationPeriod() {
        lifecycleScope.launch {
            val current = startDate.clone() as Calendar
            val db = DatabaseHelper.getDatabase(this@VacationPeriodActivity)
            
            while (!current.after(endDate)) {
                val dateId = sdfId.format(current.time)
                val wd = WorkDayEntity(
                    dateId = dateId,
                    timestamp = current.timeInMillis,
                    isVacation = true,
                    isRTT = false,
                    vehiculeType = "",
                    vehiculeNum = "",
                    amplitudeMillis = 0,
                    effectiveMillis = 0,
                    nightMillis = 0,
                    supMillis = 0
                )
                db.workDayDao().insert(wd)
                current.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            Toast.makeText(this@VacationPeriodActivity, "Période de vacances enregistrée !", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
