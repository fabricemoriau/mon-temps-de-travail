package com.example.un

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.un.data.ClientViewModel
import com.example.un.ui.ClientsScreen
import com.example.un.ui.SalaryScreen

@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()
    val clientViewModel = remember { ClientViewModel(coroutineScope) }
    
    var currentScreen by remember { mutableStateOf("home") }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2196F3), // Bleu Android habituel
            onPrimary = Color.White,
            secondary = Color(0xFF4CAF50), // Vert Forum
            onSecondary = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF5F5F5)
        ) {
            when (currentScreen) {
                "home" -> HomeScreen(
                    onNavigate = { screen -> currentScreen = screen }
                )
                "clients" -> ClientsScreen(viewModel = clientViewModel)
                "salary" -> SalaryScreen(onBack = { currentScreen = "home" })
                else -> PlaceholderScreen(currentScreen) { currentScreen = "home" }
            }
        }
    }
}

@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "MON TEMPS DE TRAVAIL",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 32.dp)
        )

        MenuButton(
            text = "MES PATIENTS (SÉCURISÉ)",
            icon = Icons.Default.Person,
            onClick = { onNavigate("clients") }
        )

        MenuButton(
            text = "OUVRIR AGENDA GOOGLE",
            icon = Icons.Default.DateRange,
            color = Color(0xFF4285F4),
            onClick = { /* Android Specific or URI */ }
        )

        MenuButton(
            text = "FORUM",
            icon = Icons.Default.Send,
            color = Color(0xFF4CAF50),
            onClick = { onNavigate("forum") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedMenuButton(text = "SAISIE JOURNÉE (AGENDA)", onClick = { onNavigate("agenda") })
        OutlinedMenuButton(text = "AGENDA DU MOIS (HISTO)", onClick = { onNavigate("history") })
        OutlinedMenuButton(text = "SCANNER", onClick = { onNavigate("scanner") })
        OutlinedMenuButton(text = "SALAIRE", onClick = { onNavigate("salary") })
        OutlinedMenuButton(text = "STATS", onClick = { onNavigate("stats") })
        OutlinedMenuButton(text = "LIEUX", onClick = { onNavigate("lieux") })
        OutlinedMenuButton(text = "DOCS", onClick = { onNavigate("docs") })
        OutlinedMenuButton(text = "COLLEGUES", onClick = { onNavigate("collegues") })

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun MenuButton(
    text: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 6.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = MaterialTheme.shapes.medium
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun OutlinedMenuButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text)
    }
}

@Composable
fun PlaceholderScreen(name: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Écran $name en cours de développement")
        Button(onClick = onBack, modifier = Modifier.padding(16.dp)) {
            Text("Retour")
        }
    }
}
