package com.berkahjayashop.main

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.berkahjayashop.main.item.UlasanItem
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class UlasanProductAdapter(
    private val context: Context,
    private var ulasanList: List<UlasanItem>
) : RecyclerView.Adapter<UlasanProductAdapter.UlasanProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UlasanProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ulasan_user, parent, false)
        return UlasanProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: UlasanProductViewHolder, position: Int) {
        val ulasan = ulasanList[position]
        holder.bind(ulasan)
    }

    override fun getItemCount(): Int = ulasanList.size

    fun updateData(newUlasanList: List<UlasanItem>) {
        ulasanList = newUlasanList
        notifyDataSetChanged()
    }

    inner class UlasanProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ciUserProfile: CircleImageView = itemView.findViewById(R.id.ciUserProfile)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        private val tvTimePost: TextView = itemView.findViewById(R.id.tvTimePost)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        private val tvUlasan: TextView = itemView.findViewById(R.id.tvUlasan)
        private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

        fun bind(ulasan: UlasanItem) {
            ulasan.userId?.let { userId ->
                database.child("users").child(userId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val username = snapshot.child("fullName").getValue(String::class.java) ?: "Unknown"
                                val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""
                                tvUsername.text = username
                                if (imageUrl.isNullOrEmpty()){
                                    ciUserProfile.setImageResource(R.drawable.ic_account)
                                }else{
                                    Glide.with(context).load(imageUrl).into(ciUserProfile)
                                }

                            } else {
                                Log.d("UlasanActivity", "User data not found")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("UlasanActivity", "Failed to fetch user data: ${error.message}")
                        }
                    })
            } ?: run {
                tvUsername.text = "Unknown"
                ciUserProfile.setImageResource(R.drawable.ic_account) // set default image if user ID is null
            }

            tvTimePost.text = ulasan.timePost?.let { getTimeAgo(it) } ?: "Unknown"
            ratingBar.rating = ulasan.rating ?: 0f
            tvUlasan.text = ulasan.review ?: "No review"
        }

        private fun getTimeAgo(time: Long): String {
            val currentTime = System.currentTimeMillis()
            val timeDifference = currentTime - time

            return when {
                timeDifference < 1000 -> "Baru saja"
                timeDifference < 60000 -> "${timeDifference / 1000} detik yang lalu"
                timeDifference < 3600000 -> "${timeDifference / 60000} menit yang lalu"
                timeDifference < 86400000 -> "${timeDifference / 3600000} jam yang lalu"
                timeDifference < 2592000000 -> "${timeDifference / 86400000} hari yang lalu"
                timeDifference < 31536000000 -> "${timeDifference / 2592000000} bulan yang lalu"
                else -> "${timeDifference / 31536000000} tahun yang lalu"
            }
        }
    }
}
