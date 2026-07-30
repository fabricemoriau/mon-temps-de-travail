package com.example.un

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.local.WorkDayEntity
import java.text.SimpleDateFormat
import java.util.*

class WorkHistoryAdapter(private val onClick: (WorkDayEntity) -> Unit) :
    ListAdapter<WorkDayEntity, WorkHistoryAdapter.ViewHolder>(WorkDayDiffCallback()) {

    private val sdf = SimpleDateFormat("EEE d MMM", Locale.FRANCE)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvWorkDate)
        val tvVehicule: TextView = view.findViewById(R.id.tvWorkVehicule)
        val tvHours: TextView = view.findViewById(R.id.tvWorkHours)
        val tvSup: TextView = view.findViewById(R.id.tvWorkSup)
        val btnEdit: android.widget.Button = view.findViewById(R.id.btnEditDay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_work_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val wd = getItem(position)
        holder.tvDate.text = sdf.format(Date(wd.timestamp))
        
        holder.btnEdit.visibility = if (wd.vehiculeType == "Non saisi") View.GONE else View.VISIBLE
        holder.btnEdit.setOnClickListener { onClick(wd) }

        when {
            wd.isVacation -> {
                holder.tvVehicule.text = "Congés / Vacances"
                holder.tvHours.text = "VACANCES"
                holder.tvHours.setTextColor(Color.parseColor("#4CAF50"))
                holder.tvSup.visibility = View.GONE
            }
            wd.isRTT -> {
                holder.tvVehicule.text = "Repos / RTT"
                holder.tvHours.text = "REPOS"
                holder.tvHours.setTextColor(Color.parseColor("#FBC02D")) // Jaune RTT
                holder.tvSup.visibility = View.GONE
            }
            wd.vehiculeType == "Non saisi" -> {
                holder.tvVehicule.text = "Journée non saisie"
                holder.tvHours.text = "---"
                holder.tvHours.setTextColor(Color.GRAY)
                holder.tvSup.visibility = View.GONE
            }
            else -> {
                holder.tvVehicule.text = "${wd.vehiculeType} - ${wd.vehiculeNum}"
                holder.tvHours.text = formatMillis(wd.effectiveMillis)
                holder.tvHours.setTextColor(Color.BLACK)
                
                if (wd.supMillis > 0) {
                    holder.tvSup.text = "+ ${formatMillis(wd.supMillis)} supp."
                    holder.tvSup.visibility = View.VISIBLE
                } else {
                    holder.tvSup.visibility = View.GONE
                }
            }
        }

        holder.itemView.setOnClickListener { onClick(wd) }
    }

    private fun formatMillis(ms: Long): String {
        val h = ms / (1000 * 60 * 60)
        val m = (ms / (1000 * 60)) % 60
        return String.format(Locale.FRANCE, "%02dh%02d", h, m)
    }

    class WorkDayDiffCallback : DiffUtil.ItemCallback<WorkDayEntity>() {
        override fun areItemsTheSame(oldItem: WorkDayEntity, newItem: WorkDayEntity) = oldItem.dateId == newItem.dateId
        override fun areContentsTheSame(oldItem: WorkDayEntity, newItem: WorkDayEntity) = oldItem == newItem
    }
}
