package com.example.un

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.Collegue

class ChatCollegueHeaderAdapter(
    private val collegues: List<Collegue>,
    private val onSelected: (Collegue) -> Unit
) : RecyclerView.Adapter<ChatCollegueHeaderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvColName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_collegue_chat_header, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val c = collegues[position]
        holder.tvName.text = c.prenom
        holder.itemView.setOnClickListener { onSelected(c) }
    }

    override fun getItemCount() = collegues.size
}
