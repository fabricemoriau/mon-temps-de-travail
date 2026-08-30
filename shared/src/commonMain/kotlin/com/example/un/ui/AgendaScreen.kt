package com.example.un.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.un.data.SharedAgendaViewModel
import com.example.un.data.local.WorkDayEntity
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(viewModel: SharedAgendaViewModel, onBack: () -> Unit) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val dateId = today.toString()
    
    LaunchedEffect(dateId) {
        viewModel.loadDay(dateId, Clock.System.now().toEpochMilliseconds())
    }

    val workDayState by viewModel.workDay.collectAsState()
    var currentWorkDay by remember(workDayState) { mutableStateOf(workDayState ?: WorkDayEntity(dateId, 0)) }

    val stats = viewModel.calculateDailyStats(currentWorkDay)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agenda - $dateId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveDay(currentWorkDay) }) {
                        Icon(Icons.Default.Check, contentDescription = "Enregistrer")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Options", style = MaterialTheme.typography.titleMedium)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = currentWorkDay.isRTT, onCheckedChange = { currentWorkDay = currentWorkDay.copy(isRTT = it) })
                Text("Jour de Repos / RTT")
            }

            if (!currentWorkDay.isRTT) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Heures de travail", style = MaterialTheme.typography.titleMedium)
                
                // Version simplifiée : Text Fields pour les heures
                TimeInputRow(
                    label = "Début",
                    millis = currentWorkDay.startMillis,
                    onUpdate = { currentWorkDay = currentWorkDay.copy(startMillis = it) }
                )
                
                TimeInputRow(
                    label = "Fin",
                    millis = currentWorkDay.endMillis,
                    onUpdate = { currentWorkDay = currentWorkDay.copy(endMillis = it) }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Particularités", style = MaterialTheme.typography.titleMedium)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = currentWorkDay.isGardeJour, onCheckedChange = { currentWorkDay = currentWorkDay.copy(isGardeJour = it) })
                    Text("Garde Jour (12h)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = currentWorkDay.isGardeNuit, onCheckedChange = { currentWorkDay = currentWorkDay.copy(isGardeNuit = it) })
                    Text("Garde Nuit (12h)")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = currentWorkDay.isAllSup, onCheckedChange = { currentWorkDay = currentWorkDay.copy(isAllSup = it) })
                    Text("Tout en Heures Sup")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            StatsCard(stats)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TimeInputRow(label: String, millis: Long, onUpdate: (Long) -> Unit) {
    var hourText by remember(millis) { 
        val h = (millis / 3600000) % 24
        mutableStateOf(if (millis == 0L) "" else h.toString()) 
    }
    var minText by remember(millis) { 
        val m = (millis / 60000) % 60
        mutableStateOf(if (millis == 0L) "" else if (m < 10) "0$m" else m.toString()) 
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.width(80.dp))
        OutlinedTextField(
            value = hourText,
            onValueChange = { 
                if (it.length <= 2) {
                    hourText = it
                    updateMillis(hourText, minText, onUpdate)
                }
            },
            modifier = Modifier.width(70.dp),
            placeholder = { Text("HH") }
        )
        Text(" : ", modifier = Modifier.padding(horizontal = 4.dp))
        OutlinedTextField(
            value = minText,
            onValueChange = { 
                if (it.length <= 2) {
                    minText = it
                    updateMillis(hourText, minText, onUpdate)
                }
            },
            modifier = Modifier.width(70.dp),
            placeholder = { Text("MM") }
        )
    }
}

private fun updateMillis(h: String, m: String, onUpdate: (Long) -> Unit) {
    val hh = h.toIntOrNull() ?: 0
    val mm = m.toIntOrNull() ?: 0
    onUpdate((hh * 3600000 + mm * 60000).toLong())
}

@Composable
fun StatsCard(stats: SharedAgendaViewModel.DailyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("RÉSUMÉ DE LA JOURNÉE", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            StatRow("Amplitude", formatMillis(stats.amplitude))
            StatRow("Temps Effectif", formatMillis(stats.effective))
            StatRow("Heures Sup", formatMillis(stats.sup))
            StatRow("Heures Nuit", formatMillis(stats.night))
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

private fun formatMillis(ms: Long): String {
    val h = ms / 3600000
    val m = (ms / 60000) % 60
    val mStr = if (m < 10) "0$m" else "$m"
    return "${h}h$mStr"
}
