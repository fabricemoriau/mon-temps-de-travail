package com.example.un

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.montempsdetravail.R
import com.example.un.data.local.ScanEntity
import java.io.File

class ScanAdapter(private val onClick: (ScanEntity) -> Unit) :
    ListAdapter<ScanEntity, ScanAdapter.ViewHolder>(ScanDiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumb: ImageView = view.findViewById(R.id.ivScanThumb)
        val tvDate: TextView = view.findViewById(R.id.tvScanDate)
        val tvPath: TextView = view.findViewById(R.id.tvScanPath)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val scan = getItem(position)
        
        val month = scan.month
        val year = scan.year
        if (scan.type == "PAIE" && month != null && year != null) {
            val months = arrayOf("Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre")
            holder.tvDate.text = "${months[month - 1]} $year"
        } else {
            holder.tvDate.text = scan.dateFormatted
        }
        
        holder.tvPath.text = File(scan.imagePath).name

        Glide.with(holder.itemView.context)
            .load(scan.imagePath)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.ivThumb)

        holder.itemView.setOnClickListener { onClick(scan) }
    }

    class ScanDiffCallback : DiffUtil.ItemCallback<ScanEntity>() {
        override fun areItemsTheSame(oldItem: ScanEntity, newItem: ScanEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ScanEntity, newItem: ScanEntity) = oldItem == newItem
    }
}
