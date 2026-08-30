package com.example.un.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.un.data.ClientViewModel
import com.example.un.data.local.ClientEntity
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(viewModel: ClientViewModel, onBack: () -> Unit) {
    val clientList by viewModel.clients.collectAsState(emptyList())
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredList = clientList.filter { 
        it.nom.contains(searchQuery, ignoreCase = true) || it.prenom.contains(searchQuery, ignoreCase = true) 
    }

    var editingClient by remember { mutableStateOf<ClientEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes Patients") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Rechercher un patient...") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(filteredList) { client ->
                    ClientItem(
                        client = client,
                        onEdit = { editingClient = client },
                        onDelete = { /* Logique delete */ }
                    )
                }
            }
        }
    }

    // Dialog pour Ajouter / Modifier
    if (showAddDialog || editingClient != null) {
        ClientEditDialog(
            client = editingClient ?: ClientEntity(id = ""), // Nouveau ou existant
            onDismiss = { 
                showAddDialog = false
                editingClient = null 
            },
            onSave = { 
                viewModel.updateClient(it)
                showAddDialog = false
                editingClient = null
            }
        )
    }
}

@Composable
fun ClientItem(client: ClientEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEdit() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${client.nom} ${client.prenom}", style = MaterialTheme.typography.titleMedium)
                if (client.tel.isNotEmpty()) Text("📞 ${client.tel}", style = MaterialTheme.typography.bodyMedium)
                if (client.adresse.isNotEmpty()) Text("📍 ${client.adresse}", style = MaterialTheme.typography.bodySmall)
                if (client.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("🔑 CODE / NOTES : ${client.notes}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD32F2F))
                }
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editer") }
        }
    }
}

@Composable
fun ClientEditDialog(client: ClientEntity, onDismiss: () -> Unit, onSave: (ClientEntity) -> Unit) {
    var nom by remember { mutableStateOf(client.nom) }
    var prenom by remember { mutableStateOf(client.prenom) }
    var tel by remember { mutableStateOf(client.tel) }
    var adresse by remember { mutableStateOf(client.adresse) }
    var notes by remember { mutableStateOf(client.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (client.id.isEmpty()) "Nouveau Patient" else "Modifier Patient") },
        text = {
            Column {
                TextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") })
                TextField(value = prenom, onValueChange = { prenom = it }, label = { Text("Prénom") })
                TextField(value = tel, onValueChange = { tel = it }, label = { Text("Téléphone") })
                TextField(value = adresse, onValueChange = { adresse = it }, label = { Text("Adresse") })
                TextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Code d'entrée, etc.)") })
            }
        },
        confirmButton = {
            Button(onClick = { 
                onSave(client.copy(
                    id = if (client.id.isEmpty()) "id_${Clock.System.now().toEpochMilliseconds()}" else client.id,
                    nom = nom,
                    prenom = prenom,
                    tel = tel,
                    adresse = adresse,
                    notes = notes,
                    updatedAt = Clock.System.now().toEpochMilliseconds()
                ))
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
