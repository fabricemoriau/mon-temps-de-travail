package com.example.un

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.un.data.ClientViewModel
import com.example.un.ui.ClientsScreen

@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()
    val clientViewModel = remember { ClientViewModel(coroutineScope) }
    
    var currentScreen by remember { mutableStateOf("home") }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                "home" -> HomeScreen(onNavigateToClients = { currentScreen = "clients" })
                "clients" -> ClientsScreen(viewModel = clientViewModel)
            }
        }
    }
}

@Composable
fun HomeScreen(onNavigateToClients: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mon Temps de Travail",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Version iPhone / Android partagée",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onNavigateToClients) {
            Text("Voir les Patients")
        }
    }
}
