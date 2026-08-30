package com.example.un.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.un.utils.SalaryCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryScreen(onBack: () -> Unit) {
    var tauxHoraire by remember { mutableStateOf("12.50") }
    var totalHours by remember { mutableStateOf("151.67") }
    var resultText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculateur de Salaire") },
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
            OutlinedTextField(
                value = tauxHoraire,
                onValueChange = { tauxHoraire = it },
                label = { Text("Taux Horaire Net (€)") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = totalHours,
                onValueChange = { totalHours = it },
                label = { Text("Total Heures Effectives") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val rate = tauxHoraire.toDoubleOrNull() ?: 0.0
                    val hours = totalHours.toDoubleOrNull() ?: 0.0
                    
                    // Utilisation du calculateur partagé
                    val res = SalaryCalculator.calculate(
                        isAmbulancier = true,
                        isTaxi = false,
                        totalEffMillis = (hours * 3600 * 1000).toLong(),
                        totalSupMillis = 0,
                        totalNightMillis = 0,
                        sundayHolidayEffMillis = 0,
                        tauxHoraire = rate,
                        panierCount = 0,
                        tauxPanier = 0.0,
                        gardeCount = 0
                    )
                    resultText = res.details
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("CALCULER")
            }

            if (resultText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = resultText,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
