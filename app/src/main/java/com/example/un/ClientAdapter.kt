package com.example.un

import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.local.ClientEntity

class ClientAdapter(
    private val onEdit: (ClientEntity) -> Unit,
    private val onDelete: (ClientEntity) -> Unit,
    private val onShare: (ClientEntity) -> Unit
) : ListAdapter<ClientEntity, ClientAdapter.ViewHolder>(ClientDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvClientName)
        val tvAddress: TextView = view.findViewById(R.id.tvClientAddress)
        val tvTel: TextView = view.findViewById(R.id.tvClientTel)
        val tvNotes: TextView = view.findViewById(R.id.tvClientNotes)
        val btnEdit: Button = view.findViewById(R.id.btnEditClient)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteClient)
        val btnShare: ImageButton = view.findViewById(R.id.btnShareClient)
        val btnNav: ImageButton = view.findViewById(R.id.btnNavClient)
        val llCall: View = view.findViewById(R.id.llCall)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_client, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val client = getItem(position)
        
        // Gestion de la couleur si décédé
        if (client.isDeleted) {
            holder.itemView.findViewById<androidx.cardview.widget.CardView>(R.id.cardClientRoot)?.setCardBackgroundColor(Color.parseColor("#FFCDD2"))
        } else {
            holder.itemView.findViewById<androidx.cardview.widget.CardView>(R.id.cardClientRoot)?.setCardBackgroundColor(Color.WHITE)
        }

        val displayName = "${client.nom} ${client.prenom}".trim()
        holder.tvName.text = if (displayName.isEmpty()) "Patient sans nom" else displayName
        holder.tvAddress.text = client.adresse.ifEmpty { "Pas d'adresse" }
        holder.tvTel.text = client.tel.ifEmpty { "Pas de téléphone" }

        if (client.notes.isNotEmpty()) {
            holder.tvNotes.text = client.notes
            holder.tvNotes.visibility = View.VISIBLE
        } else {
            holder.tvNotes.visibility = View.GONE
        }

        // Clic sur toute la carte pour éditer
        holder.itemView.setOnClickListener { onEdit(client) }
        holder.btnEdit.setOnClickListener { onEdit(client) }
        
        holder.btnDelete.setOnClickListener { onDelete(client) }
        holder.btnShare.setOnClickListener { onShare(client) }
        
        // Bloc appel (clic sur le numéro ou le logo)
        holder.llCall.setOnClickListener {
            if (client.tel.isNotEmpty()) {
                try {
                    // Nettoyer le numéro pour le dialer : on enlève les textes entre parenthèses
                    // et on ne garde que les chiffres et le '+'
                    val cleanTel = client.tel.substringBefore("(").filter { it.isDigit() || it == '+' }
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanTel"))
                    holder.itemView.context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(holder.itemView.context, "Erreur appel", Toast.LENGTH_SHORT).show()
                }
            }
        }

        holder.btnNav.setOnClickListener {
            if (client.adresse.isNotEmpty()) {
                // Essayer d'ouvrir avec l'adresse
                val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(client.adresse)}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                try {
                    holder.itemView.context.startActivity(mapIntent)
                } catch (e: Exception) {
                    try {
                        val wazeUri = Uri.parse("https://waze.com/ul?q=${Uri.encode(client.adresse)}")
                        val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri)
                        holder.itemView.context.startActivity(wazeIntent)
                    } catch (e2: Exception) {
                        Toast.makeText(holder.itemView.context, "Aucun GPS trouvé", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(holder.itemView.context, "Pas d'adresse", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class ClientDiffCallback : DiffUtil.ItemCallback<ClientEntity>() {
        override fun areItemsTheSame(oldItem: ClientEntity, newItem: ClientEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ClientEntity, newItem: ClientEntity) = oldItem == newItem
    }
}
