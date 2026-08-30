package com.example.un

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.LieuCode

class LieuAdapter(
    private var lieux: List<LieuCode>,
    private val onEdit: (LieuCode) -> Unit,
    private val onDelete: (LieuCode) -> Unit
) : RecyclerView.Adapter<LieuAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvLieuNom)
        val tvCode: TextView = view.findViewById(R.id.tvLieuCode)
        val tvTel: TextView = view.findViewById(R.id.tvLieuTel)
        val tvAdresse: TextView = view.findViewById(R.id.tvLieuAdresse)
        val tvNotes: TextView = view.findViewById(R.id.tvLieuNotes)
        val btnEdit: Button = view.findViewById(R.id.btnEditLieu)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteLieu)
    }

    fun updateList(newList: List<LieuCode>) {
        this.lieux = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lieu, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lieu = lieux[position]
        holder.tvName.text = lieu.nomLieu
        holder.tvCode.text = "Code: ${lieu.code}"
        
        if (lieu.tel.isNotEmpty()) {
            holder.tvTel.text = "Tel: ${lieu.tel}"
            holder.tvTel.visibility = View.VISIBLE
        } else {
            holder.tvTel.visibility = View.GONE
        }

        if (lieu.adresse.isNotEmpty()) {
            holder.tvAdresse.text = lieu.adresse
            holder.tvAdresse.visibility = View.VISIBLE
        } else {
            holder.tvAdresse.visibility = View.GONE
        }
        
        if (lieu.notes.isNotEmpty()) {
            holder.tvNotes.text = lieu.notes
            holder.tvNotes.visibility = View.VISIBLE
        } else {
            holder.tvNotes.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onEdit(lieu) }
        holder.btnEdit.setOnClickListener { onEdit(lieu) }
        holder.btnDelete.setOnClickListener { onDelete(lieu) }
    }

    override fun getItemCount() = lieux.size
}
