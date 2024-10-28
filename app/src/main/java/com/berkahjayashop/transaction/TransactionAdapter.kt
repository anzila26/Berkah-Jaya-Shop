package com.berkahjayashop.transaction

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.bumptech.glide.Glide

class TransactionAdapter(
    private val context: Context,
    private val transactions: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    override fun getItemCount(): Int = transactions.size

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val ivProductFirst: ImageView = itemView.findViewById(R.id.ivProductFirst)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuatity)
        private val tvProductLainnya: TextView = itemView.findViewById(R.id.tvProductLainnya)
        private val tvTotalHarga: TextView = itemView.findViewById(R.id.tvTotalHarga)

        fun bind(transaction: Transaction) {
            tvOrderDate.text = "Order Date :\n"+transaction.orderDate
            if (transaction.statusPayment == "Lunas"){
                tvStatus.text = "Status Pengiriman:\n"+transaction.statusShipping
            }else{
                tvStatus.text = "Status Pembayaran:\n"+transaction.statusPayment
            }

            if (transaction.products.isNotEmpty()) {
                val firstProduct = transaction.products[0]
                Glide.with(context)
                    .load(firstProduct.image)
                    .into(ivProductFirst)

                tvProductName.text = firstProduct.name
                tvQuantity.text = "${firstProduct.quantity} barang"
                if (transaction.products.size > 1){
                    tvProductLainnya.text = "+${transaction.products.size - 1} product lainnya"
                }else{
                    tvProductLainnya.visibility = View.GONE
                }

            } else {
                ivProductFirst.setImageResource(0)
                tvProductName.text = "No products"
                tvQuantity.text = "0 barang"
                tvProductLainnya.visibility = View.GONE
            }

            tvTotalHarga.text = "Rp ${transaction.totalPrice}"
            itemView.setOnClickListener {
                onItemClick(transaction)
            }
        }
    }
}
