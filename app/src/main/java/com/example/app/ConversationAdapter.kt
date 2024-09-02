package com.example.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.final_project.R

class ConversationAdapter : RecyclerView.Adapter<ConversationAdapter.MessageViewHolder>() {

    private val messages = mutableListOf<Pair<String, Boolean>>() // Pair: (message, isUserMessage)

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.messageTextView) // Define this in your layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false) // Define this layout
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val (message, isUserMessage) = messages[position]
        holder.messageTextView.text = message
        holder.itemView.setBackgroundColor(
            if (isUserMessage) Color.LTGRAY else Color.WHITE // Different colors for user and bot messages
        )
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: String, isUserMessage: Boolean) {
        messages.add(Pair(message, isUserMessage))
        notifyItemInserted(messages.size - 1)
    }
}

