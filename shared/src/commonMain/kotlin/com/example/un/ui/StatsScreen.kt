package com.example.un.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.un.data.StatsViewModel
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel, onBack: () -> Unit) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    var currentYear by remember { mutableStateOf(now.year) }
    var currentMonth by remember { mutableStateOf(now.month) }

    LaunchedEffect(currentYear, currentMonth) {
        viewModel.loadMonthStats(currentYear, currentMonth)
    }

    val statsState by viewModel.stats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiques") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
            MonthSelector(currentYear, currentMonth, onPrev = {
                if (currentMonth.number == 1) {
                    currentMonth = Month.DECEMBER
                    currentYear--
                } else {
                    currentMonth = Month(currentMonth.number - 1)
                }
            }, onNext = {
                if (currentMonth.number == 12) {
                    currentMonth = Month.JANUARY
                    currentYear++
                } else {
                    currentMonth = Month(currentMonth.number + 1)
                }
            })

            Spacer(modifier = Modifier.height(24.dp))

            statsState?.let { stats ->
                SummaryGrid(stats)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text("Graphique d'activité (Heures)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                
                SimpleBarChart(stats.dailyData)
            } ?: Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.padding(32.dp))
            }
        }
    }
}

@Composable
fun MonthSelector(year: Int, month: Month, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Button(onClick = onPrev) { Text("<") }
        Text("${month.name} $year", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onNext) { Text(">") }
    }
}

@Composable
fun SummaryGrid(stats: StatsViewModel.MonthStats) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem("Travaillés", "${stats.workedDaysCount}j", Modifier.weight(1f))
            StatItem("Repos", "${stats.offDaysCount}j", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem("Eff. Total", formatMillis(stats.totalEffective), Modifier.weight(1f))
            StatItem("Amplitude", formatMillis(stats.totalAmplitude), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem("Heures Sup", formatMillis(stats.totalSup), Modifier.weight(1f), Color(0xFFFF9800))
            StatItem("Nuit", formatMillis(stats.totalNight), Modifier.weight(1f), Color(0xFF673AB7))
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier, color: Color = Color.Gray) {
    Card(modifier = modifier.padding(4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        }
    }
}

@Composable
fun SimpleBarChart(data: List<com.example.un.data.local.WorkDayEntity>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val barWidth = size.width / 31
        val maxMillis = (15 * 3600 * 1000).toFloat() // Max 15h pour le graphique
        
        data.forEachIndexed { index, day ->
            val dayOfMonth = day.dateId.split("-").last().toIntOrNull() ?: 0
            val x = (dayOfMonth - 1) * barWidth
            val barHeight = (day.effectiveMillis.toFloat() / maxMillis) * size.height
            
            drawRect(
                color = primaryColor,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth - 2.dp.toPx(), barHeight)
            )
        }
        
        // Ligne de base
        drawLine(Color.LightGray, Offset(0f, size.height), Offset(size.width, size.height))
    }
}

private fun formatMillis(ms: Long): String {
    val h = ms / 3600000
    val m = (ms / 60000) % 60
    return "${h}h${if (m < 10) "0$m" else m}"
}
