package com.hotelmanagementsystem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CartAdapter(
    private val cartList: MutableList<CartItem>,
    private val onItemChanged: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCartItemName: TextView = itemView.findViewById(R.id.tvCartItemName)
        val tvCartItemQty: TextView = itemView.findViewById(R.id.tvCartItemQty)
        val tvCartItemPrice: TextView = itemView.findViewById(R.id.tvCartItemPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartList[position]

        holder.tvCartItemName.text = item.name
        holder.tvCartItemQty.text = "Idadi: x${item.quantity}"

        val itemTotal = item.price * item.quantity
        holder.tvCartItemPrice.text = "TSH ${String.format("%,.0f", itemTotal)}"

        // UKIBONYEZA BIDHAA KWENYE CART: Inapunguza idadi, ikifika 0 inaifuta
        holder.itemView.setOnClickListener {
            if (item.quantity > 1) {
                item.quantity -= 1
            } else {
                cartList.removeAt(position)
            }
            notifyDataSetChanged()
            onItemChanged()
        }
    }

    override fun getItemCount(): Int = cartList.size
}