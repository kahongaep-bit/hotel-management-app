package com.hotelmanagementsystem

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KitchenOrderAdapter(
    private val ordersList: List<OrderResponseItem>,
    private val onUpdateStatus: (String, String) -> Unit,
    private val onRejectOrder: (OrderResponseItem) -> Unit
) : RecyclerView.Adapter<KitchenOrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvToken: TextView = itemView.findViewById(R.id.tvToken)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvItems: TextView = itemView.findViewById(R.id.tvItems)
        val btnStartCooking: Button = itemView.findViewById(R.id.btnStartCooking)
        val btnFoodReady: Button = itemView.findViewById(R.id.btnFoodReady)
        val btnRejectOrder: Button = itemView.findViewById(R.id.btnRejectOrder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pending_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = ordersList[position]

        holder.tvToken.text = "TOKEN: #${order.token_number ?: ""} (${order.customerName})"

        val currentStatus = order.status ?: "Pending"
        holder.tvStatus.text = currentStatus.uppercase()

        when (currentStatus.lowercase()) {
            "cooking" -> holder.tvStatus.setTextColor(Color.parseColor("#C2410C"))
            "pending" -> holder.tvStatus.setTextColor(Color.parseColor("#B45309"))
            else -> holder.tvStatus.setTextColor(Color.parseColor("#104E08"))
        }

        // MAREKEBISHO: Kusoma majina ya bidhaa, idadi, pamoja na kuongeza kiasi cha pesa (Total Amount)
        val itemsFormatted = if (order.items.isNotEmpty()) {
            val listString = order.items.joinToString(separator = "\n") { item ->
                val itemName = item.name.ifEmpty { "Bidhaa" }
                val itemQty = item.quantity
                "- $itemName x$itemQty"
            }
            // Inaongeza jumla ya pesa chini ya orodha ya vitu (Kama ilivyo kwa Bartender)
            val totalFormatted = String.format("%,.0f", order.total_amount ?: 0.0)
            "$listString\n\n💰 Jumla ya Pesa: TSH $totalFormatted"
        } else {
            val totalFormatted = String.format("%,.0f", order.total_amount ?: 0.0)
            "Oda ya Chakula (Taarifa zote zimepokelewa)\n\n💰 Jumla ya Pesa: TSH $totalFormatted"
        }

        holder.tvItems.text = itemsFormatted
        holder.tvItems.setTextColor(Color.parseColor("#FFFFFF")) // Rangi nyeupe ili ione wazi

        holder.btnStartCooking.text = "ANZA KUPIKA"
        holder.btnFoodReady.text = "CHAKULA TAYARI"
        holder.btnRejectOrder.text = "❌ CHAKULA HAKIPO / RUDISHA"

        holder.btnStartCooking.setOnClickListener {
            onUpdateStatus(order.id, "Cooking")
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