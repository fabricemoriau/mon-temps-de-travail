package com.example.un.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.un.data.SharedDocsViewModel
import com.example.un.data.local.ScanEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsScreen(viewModel: SharedDocsViewModel, onBack: () -> Unit) {
    var selectedType by remember { mutableStateOf("ROUTE") }
    val scans by viewModel.getScans(selectedType).collectAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes Documents (Scans)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Sélecteur de type
            ScrollableTabRow(
                selectedTabIndex = if (selectedType == "ROUTE") 0 else if (selectedType == "CARNET") 1 else 2,
                edgePadding = 16.dp
            ) {
                Tab(selected = selectedType == "ROUTE", onClick = { selectedType = "ROUTE" }, text = { Text("Feuille Route") })
                Tab(selected = selectedType == "CARNET", onClick = { selectedType = "CARNET" }, text = { Text("Carnet Bord") })
                Tab(selected = selectedType == "PAIE", onClick = { selectedType = "PAIE" }, text = { Text("Fiches Paie") })
            }

            if (scans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucun document dans cette catégorie")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(scans) { scan ->
                        ScanGridItem(scan)
                    }
                }
            }
        }
    }
}

@Composable
fun ScanGridItem(scan: ScanEntity) {
    Card(
        modifier = Modifier.padding(4.dp).aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Note: En multiplateforme, l'affichage d'images locales nécessite une gestion spécifique par plateforme
            // On met une icône temporaire en attendant
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(scan.dateFormatted, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
