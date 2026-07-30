package com.example.un

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.local.WorkDayEntity
import com.example.un.utils.HolidayHelper
import java.util.*

class CalendarGridAdapter(private val onDayClick: (WorkDayEntity) -> Unit) :
    RecyclerView.Adapter<CalendarGridAdapter.ViewHolder>() {

    private var days = listOf<WorkDayEntity>()

    fun submitList(newDays: List<WorkDayEntity>) {
        days = newDays
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.rootCalendarDay)
        val tvDay: TextView = view.findViewById(R.id.tvDayNumber)
        val indicator: View = view.findViewById(R.id.viewIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val wd = days[position]
        
        if (wd.dateId.startsWith("pad_")) {
            holder.tvDay.text = ""
            holder.root.setBackgroundColor(Color.TRANSPARENT)
            holder.indicator.setBackgroundColor(Color.TRANSPARENT)
            holder.root.setOnClickListener(null)
            return
        }

        val cal = Calendar.getInstance().apply { timeInMillis = wd.timestamp }
        
        holder.tvDay.text = cal.get(Calendar.DAY_OF_MONTH).toString()

        val isHoliday = HolidayHelper.isHoliday(cal)
        if (isHoliday) {
            holder.tvDay.setTextColor(Color.RED)
            holder.tvDay.paint.isFakeBoldText = true
        } else {
            holder.tvDay.setTextColor(Color.parseColor("#333333"))
            holder.tvDay.paint.isFakeBoldText = false
        }

        // Gestion des couleurs de fond et indicateurs (Mise à jour Code Couleur)
        when {
            wd.isVacation -> {
                holder.root.setBackgroundColor(Color.parseColor("#FFE0B2")) // Orange clair
                holder.indicator.setBackgroundColor(Color.parseColor("#FF9800")) // Orange
            }
            wd.isGardeJour || wd.isGardeNuit -> {
                holder.root.setBackgroundColor(Color.parseColor("#BBDEFB")) // Bleu clair
                holder.indicator.setBackgroundColor(Color.parseColor("#2196F3")) // Bleu
            }
            wd.isRTT -> {
                holder.root.setBackgroundColor(Color.parseColor("#FFF9C4")) // Jaune clair (RTT)
                holder.indicator.setBackgroundColor(Color.parseColor("#FBC02D")) // Jaune
            }
            wd.effectiveMillis > 0 -> {
                holder.root.setBackgroundColor(Color.parseColor("#FFCDD2")) // Rouge clair
                holder.indicator.setBackgroundColor(Color.parseColor("#F44336")) // Rouge
            }
            else -> {
                // Non rempli -> Vert
                holder.root.setBackgroundColor(Color.parseColor("#C8E6C9")) // Vert clair
                holder.indicator.setBackgroundColor(Color.TRANSPARENT)
            }
        }

        holder.root.setOnClickListener { onDayClick(wd) }
    }

    override fun getItemCount() = days.size
}
