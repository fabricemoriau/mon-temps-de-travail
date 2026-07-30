package com.example.un

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.Collegue

class SelectableCollegueAdapter(
    private val collegues: List<Collegue>,
    private val onSelectionChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<SelectableCollegueAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbSelect: CheckBox = view.findViewById(R.id.cbSelect)
        val tvName: TextView = view.findViewById(R.id.tvColName)
        val tvTel: TextView = view.findViewById(R.id.tvColTel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_collegue_selectable, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val collegue = collegues[position]
        holder.tvName.text = "${collegue.prenom} ${collegue.nom}"
        holder.tvTel.text = collegue.tel
        
        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.cbSelect.isChecked = false // Reset par défaut
        
        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            onSelectionChanged(collegue.id, isChecked)
        }
    }

    override fun getItemCount() = collegues.size
}
