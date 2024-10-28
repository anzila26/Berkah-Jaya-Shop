// ProductOrderAdapter.kt

package com.berkahjayashop.transaction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.bumptech.glide.Glide

class ProductOrderAdapter(private var products: MutableList<ProductOrder>) :
    RecyclerView.Adapter<ProductOrderAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_checkout, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int {
        return products.size
    }


    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        private val tvNameProduct: TextView = itemView.findViewById(R.id.tvNameProduct)
        private val tvPriceProduct: TextView = itemView.findViewById(R.id.tvPriceProduct)
        private val tvCount: TextView = itemView.findViewById(R.id.tvCount)

        fun bind(product: ProductOrder) {
            tvNameProduct.text = product.name
            tvPriceProduct.text = "Rp. ${product.price}"
            tvCount.text = "${product.quantity}x"

            Glide.with(itemView.context)
                .load(product.image)
                .into(ivProduct)
        }
    }
}
