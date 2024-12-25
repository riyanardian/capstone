package com.example.siadum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistorisAdapter(private val dataList: List<HistorisData>) :
    RecyclerView.Adapter<HistorisAdapter.HistorisViewHolder>() {

    class HistorisViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val musim: TextView = itemView.findViewById(R.id.musim)
        val tvSensorType: TextView = itemView.findViewById(R.id.tvSensorType)
        val tvValue: TextView = itemView.findViewById(R.id.tvValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistorisViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.itemhistoris, parent, false)
        return HistorisViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistorisViewHolder, position: Int) {
        val item = dataList[position]
        holder.tvTimestamp.text = item.timestamp
        holder.musim.text = item.musim
        holder.tvSensorType.text = item.sensorType
        holder.tvValue.text = item.value
    }

    override fun getItemCount(): Int = dataList.size
}
