package com.example.un

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.montempsdetravail.R
import com.example.un.data.local.AppDatabase
import com.example.un.data.local.WorkDayEntity
import com.example.un.utils.ShareUtils
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AgendaActivity : AppCompatActivity() {

    private val times = mutableMapOf<Int, Calendar?>()
    private var selectedDate = Calendar.getInstance()
    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
    private val sdfDisplay = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE)
    
    private var lastLoadedWorkDay: WorkDayEntity? = null

    // Cache des vues pour éviter les findViewById répétitifs
    private lateinit var swIsRTT: SwitchMaterial
    private lateinit var llWorkContainer: View
    private lateinit var tvAmplitude: TextView
    private lateinit var tvTempsEffectif: TextView
    private lateinit var tvHeuresSup: TextView
    private lateinit var tvHeuresNuit: TextView
    private lateinit var etVehiculeType: EditText
    private lateinit var etVehiculeNum: EditText
    private lateinit var cbRepas: CheckBox
    private lateinit var cbGardeJour: CheckBox
    private lateinit var cbGardeNuit: CheckBox
    private lateinit var cbAllSup: CheckBox

    private val idToKey = mapOf(
        R.id.btnDebutTravail to "START",
        R.id.btnFinTravail to "END",
        R.id.btnPause1Debut to "P1S",
        R.id.btnPause1Fin to "P1E",
        R.id.btnRepasDebut to "RS",
        R.id.btnRepasFin to "RE",
        R.id.btnPause2Debut to "P2S",
        R.id.btnPause2Fin to "P2E",
        R.id.btnPauseNuitDebut to "PNS",
        R.id.btnPauseNuitFin to "PNE"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agenda)

        initViews()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Saisie Agenda"

        val incomingMillis = intent.getLongExtra("SELECTED_DATE_MILLIS", 0L)
        if (incomingMillis > 0) {
            selectedDate.timeInMillis = incomingMillis
        }

        idToKey.keys.forEach { setupTimePicker(it) }

        findViewById<ImageButton>(R.id.btnSelectDate).setOnClickListener { showDatePicker() }
        
        val checkBoxes = listOf(cbRepas, cbGardeJour, cbGardeNuit, cbAllSup)
        checkBoxes.forEach { it.setOnCheckedChangeListener { _, isChecked -> 
            if (isChecked) {
                if (it.id == R.id.cbGardeJour) {
                    cbGardeNuit.isChecked = false
                    autoFillGarde(8, 20)
                } else if (it.id == R.id.cbGardeNuit) {
                    cbGardeJour.isChecked = false
                    autoFillGarde(20, 8)
                }
            }
            calculateTimes() 
            autoSave()
        } }

        swIsRTT.setOnCheckedChangeListener { _, isChecked -> 
            toggleRTTMode(isChecked)
            autoSave()
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { autoSave() }
        }
        etVehiculeType.addTextChangedListener(textWatcher)
        etVehiculeNum.addTextChangedListener(textWatcher)

        findViewById<Button>(R.id.btnSaveWorkDay).setOnClickListener { saveToDatabase() }
        findViewById<Button>(R.id.btnDeleteWorkDay).setOnClickListener { confirmDelete() }
        findViewById<Button>(R.id.btnPrint).setOnClickListener { shareDay() }

        updateDateDisplay()
        loadWorkDayFromDb()
    }

    private fun confirmDelete() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Réinitialiser la journée")
            .setMessage("Voulez-vous supprimer toutes les saisies pour cette date ?")
            .setPositiveButton("Supprimer") { _, _ ->
                deleteWorkDay()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun deleteWorkDay() {
        val dateId = sdfDate.format(selectedDate.time)
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@AgendaActivity)
            val existing = db.workDayDao().getWorkDayById(dateId)
            if (existing != null) {
                db.workDayDao().delete(existing)
            }
            resetUiFields()
            swIsRTT.isChecked = false
            toggleRTTMode(false)
            Toast.makeText(this@AgendaActivity, "Journée réinitialisée", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun shareDay() {
        val wd = lastLoadedWorkDay
        if (wd == null) {
            Toast.makeText(this, "Enregistrez d'abord la journée", Toast.LENGTH_SHORT).show()
            return
        }
        val text = ShareUtils.generateDaySummary(wd)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Partager la journée"))
    }

    private fun initViews() {
        swIsRTT = findViewById(R.id.swIsRTT)
        llWorkContainer = findViewById(R.id.llWorkDetailsContainer)
        tvAmplitude = findViewById(R.id.tvAmplitude)
        tvTempsEffectif = findViewById(R.id.tvTempsEffectif)
        tvHeuresSup = findViewById(R.id.tvHeuresSup)
        tvHeuresNuit = findViewById(R.id.tvHeuresNuit)
        etVehiculeType = findViewById(R.id.etTypeVehicule)
        etVehiculeNum = findViewById(R.id.etNumVehicule)
        cbRepas = findViewById(R.id.cbRepasSup)
        cbGardeJour = findViewById(R.id.cbGardeJour)
        cbGardeNuit = findViewById(R.id.cbGardeNuit)
        cbAllSup = findViewById(R.id.cbAllSup)
    }

    private fun toggleRTTMode(isRTT: Boolean) {
        if (isRTT) {
            llWorkContainer.animate().alpha(0f).setDuration(300).withEndAction {
                llWorkContainer.visibility = View.GONE
                // On ne vide plus resetUiFields() ici pour permettre de revenir en arrière
                calculateTimes()
            }
        } else {
            llWorkContainer.alpha = 0f
            llWorkContainer.visibility = View.VISIBLE
            llWorkContainer.animate().alpha(1f).setDuration(300).start()
            calculateTimes()
        }
    }

    private fun autoFillGarde(startHour: Int, endHour: Int) {
        val startCal = (selectedDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val endCal = (selectedDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (endHour <= startHour) {
            endCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        times[R.id.btnDebutTravail] = startCal
        times[R.id.btnFinTravail] = endCal
        
        findViewById<Button>(R.id.btnDebutTravail).text = String.format("%02d:00", startHour)
        findViewById<Button>(R.id.btnFinTravail).text = String.format("%02d:00", endHour)
        
        // Reset pauses for garde SAMU (usually integrated)
        listOf(R.id.btnPause1Debut, R.id.btnPause1Fin, R.id.btnRepasDebut, R.id.btnRepasFin, 
               R.id.btnPause2Debut, R.id.btnPause2Fin, R.id.btnPauseNuitDebut, R.id.btnPauseNuitFin).forEach { id ->
            times[id] = null
            findViewById<Button>(id).text = "--:--"
        }
        checkMealAllowanceAuto()
        autoSave()
    }

    private fun updateDateDisplay() {
        val dateStr = sdfDisplay.format(selectedDate.time)
        findViewById<TextView>(R.id.tvCurrentDate).text = dateStr.replaceFirstChar { it.uppercase() }
        
        val tvRealToday = findViewById<TextView>(R.id.tvRealTodayDate)
        val todayCal = Calendar.getInstance()
        val sdfReal = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        tvRealToday.text = "Nous sommes le ${sdfReal.format(todayCal.time)}"

        val tvStatus = findViewById<TextView>(R.id.tvDateStatus)
        val todayCheck = Calendar.getInstance()
        val isToday = selectedDate.get(Calendar.YEAR) == todayCheck.get(Calendar.YEAR) &&
                      selectedDate.get(Calendar.DAY_OF_YEAR) == todayCheck.get(Calendar.DAY_OF_YEAR)
        
        if (isToday) {
            tvStatus.text = "AUJOURD'HUI"
            tvStatus.visibility = View.VISIBLE
        } else {
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val isYesterday = selectedDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                              selectedDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
            if (isYesterday) {
                tvStatus.text = "HIER"
                tvStatus.visibility = View.VISIBLE
            } else {
                tvStatus.visibility = View.GONE
            }
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(this, { _, y, m, d ->
            selectedDate.set(y, m, d)
            updateDateDisplay()
            loadWorkDayFromDb()
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadWorkDayFromDb() {
        val dateId = sdfDate.format(selectedDate.time)
        lifecycleScope.launch {
            val workDay = AppDatabase.getDatabase(this@AgendaActivity).workDayDao().getWorkDayById(dateId)
            lastLoadedWorkDay = workDay
            if (workDay != null) {
                if (workDay.isVacation) {
                    Toast.makeText(this@AgendaActivity, "Cette journée est marquée en VACANCES", Toast.LENGTH_SHORT).show()
                }
                applyWorkDayToUi(workDay)
            } else {
                resetUiFields()
                swIsRTT.isChecked = false
                toggleRTTMode(false)
            }
            calculateTimes()
        }
    }

    private fun applyWorkDayToUi(wd: WorkDayEntity) {
        swIsRTT.isChecked = wd.isRTT
        toggleRTTMode(wd.isRTT)

        if (!wd.isRTT) {
            etVehiculeType.setText(wd.vehiculeType)
            etVehiculeNum.setText(wd.vehiculeNum)
            cbRepas.isChecked = wd.hasExtraRepas
            cbGardeJour.isChecked = wd.isGardeJour
            cbGardeNuit.isChecked = wd.isGardeNuit
            cbAllSup.isChecked = wd.isAllSup

            idToKey.forEach { (id, _) ->
                val millis = when(id) {
                    R.id.btnDebutTravail -> wd.startMillis
                    R.id.btnFinTravail -> wd.endMillis
                    R.id.btnPause1Debut -> wd.pause1Start
                    R.id.btnPause1Fin -> wd.pause1End
                    R.id.btnRepasDebut -> wd.repasStart
                    R.id.btnRepasFin -> wd.repasEnd
                    R.id.btnPause2Debut -> wd.pause2Start
                    R.id.btnPause2Fin -> wd.pause2End
                    R.id.btnPauseNuitDebut -> wd.pauseNuitStart
                    R.id.btnPauseNuitFin -> wd.pauseNuitEnd
                    else -> 0L
                }
                if (millis > 0) {
                    val cal = Calendar.getInstance().apply { timeInMillis = millis }
                    times[id] = cal
                    findViewById<Button>(id).text = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                } else {
                    times[id] = null
                    findViewById<Button>(id).text = "--:--"
                }
            }
        }
    }

    private fun resetUiFields() {
        times.clear()
        idToKey.keys.forEach { id -> findViewById<Button>(id).text = "--:--" }
        cbRepas.isChecked = false
        cbGardeJour.isChecked = false
        cbGardeNuit.isChecked = false
        cbAllSup.isChecked = false
        etVehiculeType.setText("")
        etVehiculeNum.setText("")
    }

    private fun setupTimePicker(buttonId: Int) {
        val button = findViewById<Button>(buttonId)
        button.setOnClickListener {
            val initialCal = times[buttonId] ?: selectedDate
            TimePickerDialog(this, { _, hour, minute ->
                val cal = (selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                times[buttonId] = cal
                button.text = String.format("%02d:%02d", hour, minute)
                checkMealAllowanceAuto()
                calculateTimes()
                autoSave()
            }, initialCal.get(Calendar.HOUR_OF_DAY), initialCal.get(Calendar.MINUTE), true).show()
        }
        button.setOnLongClickListener {
            times[buttonId] = null
            button.text = "--:--"
            checkMealAllowanceAuto()
            calculateTimes()
            autoSave()
            true
        }
    }

    private fun calculateTimes() {
        if (swIsRTT.isChecked) {
            tvAmplitude.text = "Amplitude : 00h00"
            tvTempsEffectif.text = "Repos / RTT"
            tvHeuresSup.text = "Heures Supp : 00h00"
            tvHeuresNuit.text = "Heures Nuit : 00h00"
            return
        }

        val start = times[R.id.btnDebutTravail]
        val end = times[R.id.btnFinTravail]

        if (start == null || end == null) {
            tvAmplitude.text = "Amplitude : --:--"
            tvTempsEffectif.text = "Temps effectif : --:--"
            tvHeuresSup.text = "Heures Supp : --:--"
            tvHeuresNuit.text = "Heures Nuit : --:--"
            return
        }

        var amplitudeMillis = end.timeInMillis - start.timeInMillis
        if (amplitudeMillis < 0) amplitudeMillis += 24 * 60 * 60 * 1000

        if (cbGardeJour.isChecked || cbGardeNuit.isChecked) {
            amplitudeMillis = 12 * 60 * 60 * 1000L
        }

        var effectiveMillis = amplitudeMillis // Les pauses ne sont plus déduites selon la demande
        
        // Logic Panier Repas Auto supprimée d'ici pour permettre le décochage manuel
        // checkMealAllowanceAuto()

        val supMillis = if (cbAllSup.isChecked) {
            amplitudeMillis
        } else {
            val baseMillis = (8.5 * 60 * 60 * 1000).toLong() // 8h30
            if (amplitudeMillis > baseMillis) {
                val diff = amplitudeMillis - baseMillis
                val diffMinutes = diff / (1000 * 60)
                val roundedMinutes = (diffMinutes / 15) * 15 // Arrondi au quart d'heure
                roundedMinutes.toLong() * 60 * 1000
            } else 0L
        }

        val nightMillis = calculateNightMillis(start, end)

        tvAmplitude.text = "Amplitude : ${formatMillis(amplitudeMillis)}"
        tvTempsEffectif.text = "Temps effectif : ${formatMillis(effectiveMillis)}"
        tvHeuresSup.text = "Heures Supp : ${formatMillis(supMillis)}"
        tvHeuresNuit.text = "Heures Nuit : ${formatMillis(nightMillis)}"
    }

    private fun formatMillis(ms: Long): String {
        val h = ms / (1000 * 60 * 60)
        val m = (ms / (1000 * 60)) % 60
        return String.format(Locale.FRANCE, "%02dh%02d", h, m)
    }

    private fun getDuration(sId: Int, eId: Int): Long {
        val s = times[sId]
        val e = times[eId]
        if (s == null || e == null) return 0L
        var diff = e.timeInMillis - s.timeInMillis
        if (diff < 0) diff += 24 * 60 * 60 * 1000
        return diff
    }

    private fun checkMealAllowanceAuto() {
        // Ne pas auto-cocher en mode RTT
        if (swIsRTT.isChecked) return

        // Forcer le repas pour les Gardes de 12h (Jour ou Nuit)
        if (cbGardeJour.isChecked || cbGardeNuit.isChecked) {
            cbRepas.isChecked = true
            return
        }

        val rStart = times[R.id.btnRepasDebut]
        val rEnd = times[R.id.btnRepasFin]
        
        if (rStart == null || rEnd == null) {
            cbRepas.isChecked = true
            return
        }

        var duration = rEnd.timeInMillis - rStart.timeInMillis
        if (duration < 0) duration += 24 * 60 * 60 * 1000

        val startHour = rStart.get(Calendar.HOUR_OF_DAY)
        val endHour = rEnd.get(Calendar.HOUR_OF_DAY)
        val endMin = rEnd.get(Calendar.MINUTE)

        val isTooShort = duration < 45 * 60 * 1000
        val isOutOfRange = startHour < 11 || endHour > 14 || (endHour == 14 && endMin > 0)

        if (isTooShort || isOutOfRange) {
            cbRepas.isChecked = true
        } else {
            cbRepas.isChecked = false
        }
    }

    private fun calculateNightMillis(start: Calendar, end: Calendar): Long {
        var nightDuration = 0L
        val current = start.clone() as Calendar
        val endCal = end.clone() as Calendar
        if (endCal.before(current)) endCal.add(Calendar.DATE, 1)
        
        while (current.before(endCal)) {
            val hour = current.get(Calendar.HOUR_OF_DAY)
            if (hour >= 21 || hour < 6) nightDuration += 60 * 1000 
            current.add(Calendar.MINUTE, 1)
        }
        return nightDuration
    }

    /**
     * Sauvegarde automatique silencieuse de l'état actuel de la saisie.
     */
    private fun autoSave() {
        val dateId = sdfDate.format(selectedDate.time)
        val isRTT = swIsRTT.isChecked
        
        val start = times[R.id.btnDebutTravail]
        val end = times[R.id.btnFinTravail]
        
        val wd = if (isRTT) {
            WorkDayEntity(
                dateId = dateId,
                timestamp = selectedDate.timeInMillis,
                isRTT = true
            )
        } else {
            var amplitude = if (start != null && end != null) {
                var diff = end.timeInMillis - start.timeInMillis
                if (diff < 0) diff += 24 * 60 * 60 * 1000
                diff
            } else 0L

            var effective = amplitude // Pauses non déduites
            if (cbGardeJour.isChecked || cbGardeNuit.isChecked) {
                amplitude = 12 * 60 * 60 * 1000L
                effective = 12 * 60 * 60 * 1000L
            }

            val sup = if (cbAllSup.isChecked) {
                amplitude
            } else {
                val base = (8.5 * 60 * 60 * 1000).toLong()
                if (amplitude > base) {
                    val diffMin = (amplitude - base) / 60000
                    (diffMin / 15 * 15) * 60000
                } else 0L
            }
            
            val night = if (start != null && end != null) calculateNightMillis(start, end) else 0L

            WorkDayEntity(
                dateId = dateId,
                timestamp = selectedDate.timeInMillis,
                vehiculeType = etVehiculeType.text.toString(),
                vehiculeNum = etVehiculeNum.text.toString(),
                startMillis = start?.timeInMillis ?: 0,
                endMillis = end?.timeInMillis ?: 0,
                pause1Start = times[R.id.btnPause1Debut]?.timeInMillis ?: 0,
                pause1End = times[R.id.btnPause1Fin]?.timeInMillis ?: 0,
                repasStart = times[R.id.btnRepasDebut]?.timeInMillis ?: 0,
                repasEnd = times[R.id.btnRepasFin]?.timeInMillis ?: 0,
                pause2Start = times[R.id.btnPause2Debut]?.timeInMillis ?: 0,
                pause2End = times[R.id.btnPause2Fin]?.timeInMillis ?: 0,
                pauseNuitStart = times[R.id.btnPauseNuitDebut]?.timeInMillis ?: 0,
                pauseNuitEnd = times[R.id.btnPauseNuitFin]?.timeInMillis ?: 0,
                amplitudeMillis = amplitude,
                effectiveMillis = effective,
                nightMillis = night,
                supMillis = sup,
                hasExtraRepas = cbRepas.isChecked,
                isGardeJour = cbGardeJour.isChecked,
                isGardeNuit = cbGardeNuit.isChecked,
                isAllSup = cbAllSup.isChecked,
                isRTT = false
            )
        }

        lifecycleScope.launch {
            AppDatabase.getDatabase(this@AgendaActivity).workDayDao().insert(wd)
            lastLoadedWorkDay = wd
        }
    }

    private fun saveToDatabase() {
        val isRTT = swIsRTT.isChecked
        val start = times[R.id.btnDebutTravail]
        val end = times[R.id.btnFinTravail]

        if (!isRTT && (start == null || end == null)) {
            Toast.makeText(this, "Veuillez saisir au moins début et fin", Toast.LENGTH_SHORT).show()
            return
        }

        val dateId = sdfDate.format(selectedDate.time)
        val wd = if (isRTT) {
            WorkDayEntity(
                dateId = dateId,
                timestamp = selectedDate.timeInMillis,
                isRTT = true
            )
        } else {
            var amplitude = end!!.timeInMillis - start!!.timeInMillis
            if (amplitude < 0) amplitude += 24 * 60 * 60 * 1000
            
            var effective = amplitude
            if (cbGardeJour.isChecked || cbGardeNuit.isChecked) {
                amplitude = 12 * 60 * 60 * 1000L
                effective = 12 * 60 * 60 * 1000L
            }

            val sup = if (cbAllSup.isChecked) {
                amplitude
            } else {
                val base = (8.5 * 60 * 60 * 1000).toLong()
                if (amplitude > base) {
                    val diffMin = (amplitude - base) / 60000
                    (diffMin / 15 * 15) * 60000
                } else 0L
            }

            WorkDayEntity(
                dateId = dateId,
                timestamp = selectedDate.timeInMillis,
                vehiculeType = etVehiculeType.text.toString(),
                vehiculeNum = etVehiculeNum.text.toString(),
                startMillis = start.timeInMillis,
                endMillis = end.timeInMillis,
                pause1Start = times[R.id.btnPause1Debut]?.timeInMillis ?: 0,
                pause1End = times[R.id.btnPause1Fin]?.timeInMillis ?: 0,
                repasStart = times[R.id.btnRepasDebut]?.timeInMillis ?: 0,
                repasEnd = times[R.id.btnRepasFin]?.timeInMillis ?: 0,
                pause2Start = times[R.id.btnPause2Debut]?.timeInMillis ?: 0,
                pause2End = times[R.id.btnPause2Fin]?.timeInMillis ?: 0,
                pauseNuitStart = times[R.id.btnPauseNuitDebut]?.timeInMillis ?: 0,
                pauseNuitEnd = times[R.id.btnPauseNuitFin]?.timeInMillis ?: 0,
                amplitudeMillis = amplitude,
                effectiveMillis = effective,
                nightMillis = calculateNightMillis(start, end),
                supMillis = sup,
                hasExtraRepas = cbRepas.isChecked,
                isGardeJour = cbGardeJour.isChecked,
                isGardeNuit = cbGardeNuit.isChecked,
                isAllSup = cbAllSup.isChecked,
                isRTT = false
            )
        }

        lifecycleScope.launch {
            AppDatabase.getDatabase(this@AgendaActivity).workDayDao().insert(wd)
            Toast.makeText(this@AgendaActivity, if (isRTT) "Journée de repos enregistrée !" else "Journée enregistrée !", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
