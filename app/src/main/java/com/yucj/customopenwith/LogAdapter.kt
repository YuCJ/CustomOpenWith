package com.yucj.customopenwith

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class LogEntry(val url: String, val time: String)

class LogAdapter(private val logList: List<LogEntry>) :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewUrl: TextView = view.findViewById(R.id.text_view_url)
        val textViewTime: TextView = view.findViewById(R.id.text_view_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val logEntry = logList[position]
        holder.textViewUrl.text = logEntry.url
        holder.textViewTime.text = logEntry.time
    }

    override fun getItemCount(): Int = logList.size
}
