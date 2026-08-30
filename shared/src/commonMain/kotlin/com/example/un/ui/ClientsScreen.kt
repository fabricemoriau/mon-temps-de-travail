package com.example.un.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.un.data.ClientViewModel
import com.example.un.data.local.ClientEntity
import kotlinx.coroutines.CoroutineScope

@Composable
fun ClientsScreen(viewModel: ClientViewModel) {
    val clientList by viewModel.clients.collectAsState(emptyList())

    Scaffold(
        topBar = {
            Text(
                "Mes Patients",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.headlineSmall
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(clientList) { client ->
                ClientItem(client)
            }
        }
    }
}

@Composable
fun ClientItem(client: ClientEntity) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${client.nom} ${client.prenom}", style = MaterialTheme.typography.titleMedium)
            if (client.tel.isNotEmpty()) {
                Text(client.tel, style = MaterialTheme.typography.bodyMedium)
            }
            if (client.adresse.isNotEmpty()) {
                Text(client.adresse, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
