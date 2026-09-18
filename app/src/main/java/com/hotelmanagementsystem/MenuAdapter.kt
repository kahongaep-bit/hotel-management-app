package com.hotelmanagementsystem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MenuAdapter(
    private val menuList: List<MenuItem>,
    private val onItemClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvItemName: TextView = itemView.findViewById(R.id.tvMenuItemName)
        val tvItemCategory: TextView = itemView.findViewById(R.id.tvMenuItemCategory)
        val tvItemPrice: TextView = itemView.findViewById(R.id.tvMenuItemPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_grid, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = menuList[position]

        holder.tvItemName.text = item.name
        holder.tvItemCategory.text = item.category ?: "Chakula"
        holder.tvItemPrice.text = "TSH ${String.format("%,.0f", item.price)}"

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return menuList.size
    }
}