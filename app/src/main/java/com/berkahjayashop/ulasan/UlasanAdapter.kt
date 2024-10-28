package com.berkahjayashop.ulasan

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase

class UlasanAdapter(
    private val context: Context,
    private val productList: MutableList<UlasanProduct>,
    private val userId: String
) : RecyclerView.Adapter<UlasanAdapter.UlasanViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UlasanViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_ulasan, parent, false)
        return UlasanViewHolder(view)
    }

    override fun onBindViewHolder(holder: UlasanViewHolder, position: Int) {
        val product = productList[position]
        holder.tvNameProduct.text = product.name
        holder.tvPriceProduct.text = product.price
        holder.tvCount.text = product.quantity
        holder.ratingBar.rating = product.rating
        holder.etReview.setText(product.review)
        Glide.with(context)
            .load(product.image)
            .into(holder.ivProduct)
        holder.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            product.rating = rating
        }

        holder.etReview.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                product.review = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun getItemCount(): Int {
        return productList.size
    }
    inner class UlasanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        val tvNameProduct: TextView = itemView.findViewById(R.id.tvNameProduct)
        val tvPriceProduct: TextView = itemView.findViewById(R.id.tvPriceProduct)
        val tvCount: TextView = itemView.findViewById(R.id.tvCount)
        val ratingBar: RatingBar = itemView.findViewById(R.id.rbUlasan)
        val etReview: TextInputEditText = itemView.findViewById(R.id.etUlasan)
    }


    fun saveDataToFirebase() {
        val database = FirebaseDatabase.getInstance().reference
        val ulasanRef = database.child("ulasan")

        for (product in productList) {
            val ulasanMap = mapOf(
                "userId" to userId,
                "productId" to product.productId,
                "rating" to product.rating,
                "review" to product.review,
                "timePost" to System.currentTimeMillis()
            )
            ulasanRef.push().setValue(ulasanMap)
        }
    }


}
