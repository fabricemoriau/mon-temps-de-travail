package com.example.un

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.montempsdetravail.R
import com.example.un.data.Collegue
import com.example.un.data.LocalDataManager

class CollegueAdapter(
    private var list: List<Collegue>,
    private val onEdit: (Collegue) -> Unit,
    private val onDelete: (Collegue) -> Unit
) : RecyclerView.Adapter<CollegueAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvCollegueName)
        val tvTel: TextView = view.findViewById(R.id.tvCollegueTel)
        val btnCall: Button = view.findViewById(R.id.btnCallCollegue)
        val btnEdit: Button = view.findViewById(R.id.btnEditCollegue)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteCollegue)
    }

    fun updateList(newList: List<Collegue>) {
        this.list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_collegue, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val context = holder.itemView.context
        val myId = LocalDataManager.getUserId(context)
        
        val suffix = if (item.id == myId) " (Moi)" else ""
        holder.tvName.text = "${item.nom} ${item.prenom}$suffix"
        holder.tvTel.text = item.tel
        
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
        
        holder.btnCall.setOnClickListener {
            if (item.tel.isNotEmpty()) {
                // Nettoyer le numéro : on enlève tout ce qui est entre parenthèses (société) 
                // et on ne garde que les chiffres et le '+'
                val cleanTel = item.tel.substringBefore("(").filter { it.isDigit() || it == '+' }
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanTel"))
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = list.size
}
