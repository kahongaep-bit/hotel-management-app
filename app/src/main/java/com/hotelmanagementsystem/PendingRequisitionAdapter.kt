package com.hotelmanagementsystem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PendingRequisitionAdapter(
    private val itemList: List<RequisitionItem>,
    private val onItemClick: (item: RequisitionItem) -> Unit
) : RecyclerView.Adapter<PendingRequisitionAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvRequestedBy: TextView = itemView.findViewById(R.id.tvRequestedBy)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_requisition, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]

        holder.tvProductName.text = item.item_name ?: "Bidhaa"
        holder.tvQuantity.text = "Kiasi: ${item.quantity ?: 0.0} ${item.unit ?: ""}"
        holder.tvRequestedBy.text = "Aliyeomba: ${item.requested_by ?: "Manager"}"

        holder.btnViewDetails.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = itemList.size
}