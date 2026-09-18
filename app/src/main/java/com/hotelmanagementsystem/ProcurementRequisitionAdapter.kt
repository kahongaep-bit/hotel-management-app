package com.hotelmanagementsystem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProcurementRequisitionAdapter(
    private var requisitions: List<RequisitionItem>,
    private val onItemClick: (RequisitionItem) -> Unit
) : RecyclerView.Adapter<ProcurementRequisitionAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val tvRequestedBy: TextView = itemView.findViewById(R.id.tvRequestedBy)
        val btnAddQuotation: Button = itemView.findViewById(R.id.btnAddQuotation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_procurement_requisition, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = requisitions[position]

        holder.tvItemName.text = item.item_name ?: "Bidhaa"
        holder.tvQuantity.text = "Kiasi: ${item.quantity} ${item.unit}"
        holder.tvRequestedBy.text = "Aliyeomba: ${item.requested_by ?: "Production Coordinator"}"

        holder.btnAddQuotation.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = requisitions.size

    fun updateData(newItems: List<RequisitionItem>) {
        requisitions = newItems
        notifyDataSetChanged()
    }
}