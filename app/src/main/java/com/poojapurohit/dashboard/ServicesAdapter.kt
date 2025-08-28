package com.poojapurohit.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.poojapurohit.R

class ServicesAdapter(private val services: List<ServiceItem>) : 
    RecyclerView.Adapter<ServicesAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivServiceIcon: ImageView = itemView.findViewById(R.id.ivServiceIcon)
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
        holder.ivServiceIcon.setImageResource(service.iconResId)
        holder.tvServiceName.text = service.name
        holder.tvServiceDescription.text = service.description
    }

    override fun getItemCount(): Int = services.size
}
