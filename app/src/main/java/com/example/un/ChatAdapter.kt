package com.example.un

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val sdf = SimpleDateFormat("HH:mm", Locale.FRANCE)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView = view.findViewById(R.id.tvChatSender)
        val tvText: TextView = view.findViewById(R.id.tvChatText)
        val tvTime: TextView = view.findViewById(R.id.tvChatTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val m = messages[position]
        holder.tvSender.text = m.sender
        holder.tvText.text = m.text
        holder.tvTime.text = sdf.format(Date(m.timestamp))
    }

    override fun getItemCount() = messages.size
}
