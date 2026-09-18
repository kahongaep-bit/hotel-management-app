package com.hotelmanagementsystem

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ManagerHistoryAdapter(
    private val requisitions: List<RequisitionItem>,
    private val onEditClick: (RequisitionItem) -> Unit,
    private val onDeleteClick: (RequisitionItem) -> Unit
) : RecyclerView.Adapter<ManagerHistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvItemName: TextView = itemView.findViewById(R.id.tvHistoryItemName)
        val tvDetails: TextView = itemView.findViewById(R.id.tvHistoryDetails)
        val tvStatus: TextView = itemView.findViewById(R.id.tvHistoryStatus)
        val llRejectionContainer: LinearLayout = itemView.findViewById(R.id.llRejectionContainer)
        val tvRejectionComment: TextView = itemView.findViewById(R.id.tvRejectionComment)

        val llActionButtons: LinearLayout = itemView.findViewById(R.id.llActionButtons)
        val btnEditRequisition: Button = itemView.findViewById(R.id.btnEditRequisition)
        val btnDeleteRequisition: Button = itemView.findViewById(R.id.btnDeleteRequisition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manager_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = requisitions[position]

        holder.tvItemName.text = item.item_name ?: "Bidhaa"
        val deptText = item.department ?: "Jikoni"
        holder.tvDetails.text = "Kiasi: ${item.quantity ?: 0.0} ${item.unit ?: ""} [$deptText]"

        val statusText = item.status ?: "Pending"
        holder.tvStatus.text = "Status: $statusText"

        val isRejected = statusText.contains("Rejected", ignoreCase = true)

        when {
            statusText.contains("Approved", ignoreCase = true) || statusText.equals("Procured", ignoreCase = true) -> {
                holder.tvStatus.setTextColor(Color.parseColor("#2F855A"))
            }
            isRejected -> {
                holder.tvStatus.setTextColor(Color.parseColor("#E53E3E"))
            }
            else -> {
                holder.tvStatus.setTextColor(Color.parseColor("#DD6B20"))
            }
        }

        // Onyesha Comments kama ipo
        if (!item.rejection_comment.isNullOrBlank()) {
            holder.llRejectionContainer.visibility = View.VISIBLE
            holder.tvRejectionComment.text = "💬 Sababu ya Kurudishwa:\n\"${item.rejection_comment}\""
        } else {
            holder.llRejectionContainer.visibility = View.GONE
        }

        // Onyesha vitufe vya Kurekebisha na Kufuta iwapo ombi limekataliwa/kurudishwa
        if (isRejected) {
            holder.llActionButtons.visibility = View.VISIBLE
            holder.btnEditRequisition.setOnClickListener { onEditClick(item) }
            holder.btnDeleteRequisition.setOnClickListener { onDeleteClick(item) }
        } else {
            holder.llActionButtons.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = requisitions.size
}