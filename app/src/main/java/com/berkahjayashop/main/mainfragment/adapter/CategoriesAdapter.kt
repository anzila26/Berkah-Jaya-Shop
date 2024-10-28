package com.berkahjayashop.main.mainfragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.berkahjayashop.main.item.CategoriesItem
import com.bumptech.glide.Glide

class CategoriesAdapter(
    private var categories: List<CategoriesItem>,
    private val onItemClick: (CategoriesItem) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {

    private var filteredCategories: List<CategoriesItem> = categories

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        Glide.with(holder.itemView.context)
            .load(category.image)
            .into(holder.imageView)

        holder.textView.text = category.name
        holder.itemView.setOnClickListener { onItemClick(category) }
    }

    override fun getItemCount(): Int {
        return categories.size
    }
    fun filterList(filteredList: List<CategoriesItem>) {
        filteredCategories = filteredList
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivCategories)
        val textView: TextView = itemView.findViewById(R.id.tvCategories)
    }
}
