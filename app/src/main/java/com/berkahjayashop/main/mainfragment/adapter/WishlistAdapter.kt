package com.berkahjayashop.main.mainfragment.adapter

import com.bumptech.glide.Glide
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.berkahjayashop.main.item.ProductsItem

class WishlistAdapter(
    private val context: Context,
    private var productList: MutableList<ProductsItem>,
    private val itemClickListener: OnItemClickListener,
    private val favoriteClickListener: OnFavoriteClickListener
) : RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false)
        return WishlistViewHolder(view)
    }

    interface OnItemClickListener {
        fun onItemClick(product: ProductsItem)
    }

    interface OnFavoriteClickListener {
        fun onFavoriteClick(product: ProductsItem)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        val product = productList[position]
        holder.tvProductName.text = product.name
        holder.tvProductPrice.text = "Rp ${product.selling_price}"

        holder.ivFavorite.setImageResource(R.drawable.ic_wishlist_filled)

        Glide.with(context)
            .load(product.image)
            .into(holder.ivProduct)

        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(product)
        }

        holder.ivFavorite.setOnClickListener {
            favoriteClickListener.onFavoriteClick(product)
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    fun updateList(newProductList: MutableList<ProductsItem>) {
        productList = newProductList
        notifyDataSetChanged()
    }

    class WishlistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        val ivFavorite: ImageView = itemView.findViewById(R.id.ivFavorite)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
    }
}
