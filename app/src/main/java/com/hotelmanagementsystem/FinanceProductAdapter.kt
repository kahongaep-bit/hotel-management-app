package com.hotelmanagementsystem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class FinanceProductAdapter(
    private var productList: MutableList<MenuItem>,
    private val onDeleteClick: (MenuItem) -> Unit,
    private val onEditClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<FinanceProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvProductCategory)
        val tvPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val btnEdit: Button = itemView.findViewById(R.id.btnEditProduct)
        val btnDelete: Button = itemView.findViewById(R.id.btnDeleteProduct)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_finance_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = productList[position]
        holder.tvName.text = item.name
        holder.tvCategory.text = "Aina: ${item.category ?: "Chakula"}"
        holder.tvPrice.text = "TSH ${String.format("%,.0f", item.price)}"

        // Kitufe cha Hariri (Edit)
        holder.btnEdit.setOnClickListener {
            onEditClick(item)
        }

        // Kitufe cha Futa (Delete)
        holder.btnDelete.setOnClickListener {
            val context = holder.itemView.context
            AlertDialog.Builder(context)
                .setTitle("Futa Bidhaa")
                .setMessage("Je, una uhakika unataka kufuta '${item.name}'? Haitaonekana tena kwa Cashier.")
                .setPositiveButton("Ndiyo, Futa") { _, _ ->
                    onDeleteClick(item)
                }
                .setNegativeButton("Ghairi", null)
                .show()
        }
    }

    override fun getItemCount(): Int = productList.size

    fun updateList(newList: List<MenuItem>) {
        productList.clear()
        productList.addAll(newList)
        notifyDataSetChanged()
    }

    fun removeItem(item: MenuItem) {
        val position = productList.indexOf(item)
        if (position != -1) {
            productList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}