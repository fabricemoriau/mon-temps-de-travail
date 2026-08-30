package com.example.un

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.ForumTopic
import java.text.SimpleDateFormat
import java.util.*

class ForumAdapter(
    private var topics: List<ForumTopic>,
    private val currentUserId: String,
    private val isMaster: Boolean,
    private val onClick: (ForumTopic) -> Unit,
    private val onDelete: (ForumTopic) -> Unit
) : RecyclerView.Adapter<ForumAdapter.ViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTopicTitle)
        val tvAuthor: TextView = view.findViewById(R.id.tvTopicAuthor)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteTopic)
    }

    fun updateList(newList: List<ForumTopic>) {
        this.topics = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forum_topic, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val topic = topics[position]
        holder.tvTitle.text = topic.title
        holder.tvAuthor.text = "Par ${topic.creatorName} le ${sdf.format(Date(topic.timestamp))}"

        // Afficher le bouton supprimer si c'est l'auteur ou le maître
        if (isMaster || topic.creatorId == currentUserId) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener { onDelete(topic) }
        } else {
            holder.btnDelete.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(topic) }
    }

    override fun getItemCount() = topics.size
}
