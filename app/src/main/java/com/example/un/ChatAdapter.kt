package com.example.un

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val messages: List<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val sdf = SimpleDateFormat("HH:mm", Locale.FRANCE)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView = view.findViewById(R.id.tvChatSender)
        val tvText: TextView = view.findViewById(R.id.tvChatText)
        val tvTime: TextView = view.findViewById(R.id.tvChatTime)
        val cvMessage: MaterialCardView = view.findViewById(R.id.cvMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val m = messages[position]
        holder.tvText.text = m.text
        holder.tvTime.text = sdf.format(Date(m.timestamp))

        val isMe = m.senderId == currentUserId

        val params = holder.cvMessage.layoutParams as RelativeLayout.LayoutParams
        if (isMe) {
            params.addRule(RelativeLayout.ALIGN_PARENT_END)
            params.removeRule(RelativeLayout.ALIGN_PARENT_START)
            holder.cvMessage.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.brand_primary))
            holder.tvText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            holder.tvTime.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            holder.tvSender.visibility = View.GONE
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_START)
            params.removeRule(RelativeLayout.ALIGN_PARENT_END)
            holder.cvMessage.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            holder.tvText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.black))
            holder.tvTime.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.grey_600))
            holder.tvSender.visibility = View.VISIBLE
            holder.tvSender.text = m.senderName
        }
        holder.cvMessage.layoutParams = params
    }

    override fun getItemCount() = messages.size
}
