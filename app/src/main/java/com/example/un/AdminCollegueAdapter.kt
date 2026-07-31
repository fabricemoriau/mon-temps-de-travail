package com.example.un

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.AdminConfig
import com.example.un.data.Collegue
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminCollegueAdapter(
    private val list: List<Collegue>,
    private val onBlockToggle: (Collegue, Boolean) -> Unit
) : RecyclerView.Adapter<AdminCollegueAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvAdminColName)
        val btnAction: Button = view.findViewById(R.id.btnAdminBlockAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_collegue, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvName.text = "${item.nom} ${item.prenom}"

        // Vérifier le statut actuel sur Firebase
        val blockedRef = FirebaseDatabase.getInstance().getReference(AdminConfig.PATH_BLOCKED_USERS).child(item.id)
        blockedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isBlocked = snapshot.exists() && snapshot.getValue(Boolean::class.java) == true
                holder.btnAction.text = if (isBlocked) "DÉBLOQUER" else "BLOQUER"
                holder.btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    if (isBlocked) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#D32F2F")
                )
                
                holder.btnAction.setOnClickListener { onBlockToggle(item, !isBlocked) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun getItemCount() = list.size
}
