package com.poojapurohit.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.poojapurohit.R

class DashboardAdapter(
    private val services: List<ServiceItem>,
    private val onItemClick: (ServiceItem) -> Unit = {}
) : RecyclerView.Adapter<DashboardAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvServiceName: TextView = itemView.findViewById(R.id.tvServiceName)
        val tvServiceDescription: TextView = itemView.findViewById(R.id.tvServiceDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_card, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]
        holder.apply {
            tvServiceName.text = service.name
            tvServiceDescription.text = service.description
            itemView.setOnClickListener { onItemClick(service) }
        }
    }

    override fun getItemCount(): Int = services.size
}
