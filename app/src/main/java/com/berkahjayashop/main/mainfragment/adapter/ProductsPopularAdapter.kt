package com.berkahjayashop.main.mainfragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.berkahjayashop.main.item.ProductsItem
import com.bumptech.glide.Glide

class ProductsPopularAdapter(
    private var products: List<ProductsItem>,
    private val onItemClick: (ProductsItem) -> Unit,
) : RecyclerView.Adapter<ProductsPopularAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_popular, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        Glide.with(holder.itemView.context)
            .load(product.image)
            .into(holder.imageView)

        holder.productName.text = product.name
        holder.productPrice.text = ("Rp " + product.selling_price)

        holder.itemView.setOnClickListener { onItemClick(product) }
    }

    override fun getItemCount(): Int {
        return products.size
    }
    fun updateList(newProductList: MutableList<ProductsItem>) {
        products = newProductList
        notifyDataSetChanged()
    }
    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivProduct)
        val productName: TextView = itemView.findViewById(R.id.tvProductName)
        val productPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
    }
}
