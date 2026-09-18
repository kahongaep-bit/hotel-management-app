package com.hotelmanagementsystem

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BarOrderAdapter(
    private val ordersList: List<OrderResponseItem>,
    private val onUpdateStatus: (String, String) -> Unit,
    private val onRejectOrder: (OrderResponseItem) -> Unit
) : RecyclerView.Adapter<BarOrderAdapter.BarViewHolder>() {

    class BarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvToken: TextView = itemView.findViewById(R.id.tvToken)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvItems: TextView = itemView.findViewById(R.id.tvItems)
        val btnStartCooking: Button = itemView.findViewById(R.id.btnStartCooking)
        val btnFoodReady: Button = itemView.findViewById(R.id.btnFoodReady)
        val btnRejectOrder: Button = itemView.findViewById(R.id.btnRejectOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pending_order, parent, false)
        return BarViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarViewHolder, position: Int) {
        val order = ordersList[position]

        // 1. Kichwa cha Tokeni na Mteja - RANGI NYEUPE (WHITE)
        holder.tvToken.text = "TOKEN: #${order.token_number ?: ""} (${order.customerName})"
        holder.tvToken.setTextColor(Color.parseColor("#FFFFFF"))

        // 2. Status ya Oda - Rangi za Kutetemesha
        val currentStatus = order.status ?: "Pending"
        holder.tvStatus.text = currentStatus.uppercase()

        when (currentStatus.lowercase()) {
            "preparing", "cooking" -> holder.tvStatus.setTextColor(Color.parseColor("#FF9800"))
            "pending" -> holder.tvStatus.setTextColor(Color.parseColor("#FFC107"))
            else -> holder.tvStatus.setTextColor(Color.parseColor("#4CAF50"))
        }

        // 3. Format ya Majina ya Vinywaji na Bei - RANGI NYEUPE SAFARI
        val itemsFormatted = if (order.items.isNotEmpty()) {
            order.items.joinToString(separator = "\n") { item ->
                val p = item.price ?: 0.0
                val q = item.quantity ?: 1
                "• ${item.name ?: "Kinywaji"} x$q   (TSH ${String.format("%,.0f", p * q)})"
            }
        } else {
            "• Oda ya Kinywaji"
        }

        holder.tvItems.text = itemsFormatted
        holder.tvItems.setTextColor(Color.parseColor("#FFFFFF")) // Rangi Nyeupe
        holder.tvItems.textSize = 16f

        // 4. Maandishi ya Vitufe vya Bar
        holder.btnStartCooking.text = "MAANDALIZI"
        holder.btnFoodReady.text = "TAYARI"
        holder.btnRejectOrder.text = "❌ KINYWAJI HAKIPO / RUDISHA"

        holder.btnStartCooking.setOnClickListener {
            onUpdateStatus(order.id, "Preparing")
        }

        holder.btnFoodReady.setOnClickListener {
            onUpdateStatus(order.id, "Ready")
        }

        holder.btnRejectOrder.setOnClickListener {
            onRejectOrder(order)
        }
    }

    override fun getItemCount(): Int = ordersList.size
}