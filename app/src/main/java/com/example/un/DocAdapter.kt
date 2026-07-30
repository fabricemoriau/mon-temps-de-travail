package com.example.un

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.montempsdetravail.R
import com.example.un.data.DocumentObligatoire

class DocAdapter(
    private val docs: List<DocumentObligatoire>,
    private val onClick: (DocumentObligatoire) -> Unit
) : RecyclerView.Adapter<DocAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivDoc: ImageView = view.findViewById(R.id.ivDocPreview)
        val tvTitle: TextView = view.findViewById(R.id.tvDocTitle)
        val tvExp: TextView = view.findViewById(R.id.tvDocExpiry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_doc, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = docs[position]
        holder.tvTitle.text = doc.titre
        
        if (!doc.dateExpiration.isNullOrEmpty()) {
            holder.tvExp.text = "Expire le: ${doc.dateExpiration}"
            holder.tvExp.visibility = View.VISIBLE
        } else {
            holder.tvExp.visibility = View.GONE
        }
        
        Glide.with(holder.itemView.context)
            .load(doc.imageUri)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.ivDoc)

        holder.itemView.setOnClickListener { onClick(doc) }
    }

    override fun getItemCount() = docs.size
}
