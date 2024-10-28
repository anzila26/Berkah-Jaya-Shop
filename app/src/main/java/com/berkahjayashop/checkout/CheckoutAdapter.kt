package com.berkahjayashop.checkout

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.bumptech.glide.Glide

class CheckoutAdapter(
    private val context: Context,
    private val dataList: ArrayList<Array<String>>
) : RecyclerView.Adapter<CheckoutAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        val tvNameProduct: TextView = itemView.findViewById(R.id.tvNameProduct)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPriceProduct)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_checkout, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = dataList[position]
        val name = data[2]
        val price = data[4]
        val quantity = data[3]
        val image = data[1]

        holder.tvNameProduct.text = name
        holder.tvPrice.text = "Rp. $price"
        holder.tvQuantity.text = "$quantity x"
        Glide.with(context).load(image).into(holder.ivProduct)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
}
