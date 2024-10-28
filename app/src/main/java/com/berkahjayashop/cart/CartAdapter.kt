package com.berkahjayashop.cart

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.bumptech.glide.Glide

class CartAdapter(
    private val context: Context,
    private val selectedItems: Set<Int>,
    private val cartItemList: List<CartItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    interface OnItemClickListener {
        fun onDeleteItemClick(position: Int)
        fun onMinusClick(position: Int)
        fun onPlusClick(position: Int)
        fun onCheckBoxClick(position: Int, isChecked: Boolean)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cartItem = cartItemList[position]
        holder.bind(cartItem)

    }

    override fun getItemCount(): Int {
        return cartItemList.size
    }

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.ivProduct)
        private val nameTextView: TextView = itemView.findViewById(R.id.tvNameProduct)
        private val amountTextView: TextView = itemView.findViewById(R.id.tvAmountProduct)
        private val minusTextView: TextView = itemView.findViewById(R.id.tvMin)
        private val countTextView: TextView = itemView.findViewById(R.id.tvCount)
        private val plusTextView: TextView = itemView.findViewById(R.id.tvPlus)
        private val deleteImageView: ImageView = itemView.findViewById(R.id.ivDelete)
        private val cbProduct: CheckBox = itemView.findViewById(R.id.cbProduct)

        fun bind(cartItem: CartItem) {

            Glide.with(context)
                .load(cartItem.image)
                .into(imageView)

            nameTextView.text = cartItem.name
            amountTextView.text = "Rp. " + cartItem.price
            countTextView.text = cartItem.quantity.toString()

            deleteImageView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteItemClick(position)
                }
            }

            minusTextView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onMinusClick(position)
                }
            }

            plusTextView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onPlusClick(position)
                }
            }
            cbProduct.isChecked = selectedItems.contains(position)
            cbProduct.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onCheckBoxClick(position, isChecked)
                }
            }
        }
    }
}
