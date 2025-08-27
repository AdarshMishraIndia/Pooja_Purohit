package com.poojapurohit.auth.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.poojapurohit.R

class ServicesAdapter(
    private val onSelectionChanged: (service: String, isSelected: Boolean) -> Unit
) : ListAdapter<String, ServicesAdapter.ServiceViewHolder>(ServiceDiffCallback()) {

    private val selectedServices = mutableSetOf<String>()

    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.item_text)
        private val btnSelect: ImageButton = itemView.findViewById(R.id.item_button)

        fun bind(service: String) {
            tvName.text = service
            val isSelected = selectedServices.contains(service)
            updateButtonState(isSelected)

            itemView.setOnClickListener {
                val newSelected = !selectedServices.contains(service)

                if (newSelected) {
                    selectedServices.add(service)
                } else {
                    selectedServices.remove(service)
                }

                updateButtonState(newSelected)
                onSelectionChanged(service, newSelected)
            }
        }

        private fun updateButtonState(isSelected: Boolean) {
            if (isSelected) {
                btnSelect.setImageResource(R.drawable.bg_radio_tick)
            } else {
                btnSelect.setImageResource(R.drawable.bg_radio_untick)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_recycler_auth, parent, false)
        return ServiceViewHolder(v)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedServices(): List<String> = selectedServices.toList()

    fun clearSelection() {
        val previouslySelected = selectedServices.toList()
        selectedServices.clear()
        previouslySelected.forEach { service ->
            val pos = currentList.indexOf(service)
            if (pos != -1) notifyItemChanged(pos)
        }
    }
}

private class ServiceDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
}
