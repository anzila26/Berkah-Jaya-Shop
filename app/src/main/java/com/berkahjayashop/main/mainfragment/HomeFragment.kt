package com.berkahjayashop.main.mainfragment

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.cart.CartActivity
import com.berkahjayashop.main.DetailsActivity
import com.berkahjayashop.main.ProductsByCategoryActivity
import com.berkahjayashop.R
import com.berkahjayashop.main.AllCategoriesActivity
import com.berkahjayashop.main.AllProductsPopularActivity
import com.berkahjayashop.main.mainfragment.adapter.CategoriesAdapter
import com.berkahjayashop.main.mainfragment.adapter.ProductsPopularAdapter
import com.berkahjayashop.main.item.CategoriesItem
import com.berkahjayashop.main.item.ProductsItem
import com.google.android.material.search.SearchBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.nikartm.support.ImageBadgeView

class HomeFragment : Fragment() {

    private lateinit var ibvCart: ImageBadgeView
    private lateinit var seeAllProductPopular: TextView
    private lateinit var seeAllCategories: TextView
    private lateinit var searchBar: SearchBar
    private lateinit var rvCategory: RecyclerView
    private lateinit var rvProduct: RecyclerView
    private lateinit var categoriesAdapter: CategoriesAdapter
    private lateinit var productsPopularAdapter: ProductsPopularAdapter

    private lateinit var progressDialog: ProgressDialog
    private lateinit var database: FirebaseDatabase

    private lateinit var categories: MutableList<CategoriesItem>
    private lateinit var products: MutableList<ProductsItem>
    private var productId = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        ibvCart = view.findViewById(R.id.ibvCart)
        searchBar = view.findViewById(R.id.searchBar)
        rvCategory = view.findViewById(R.id.rvCategories)
        rvProduct = view.findViewById(R.id.rvPopular)
        seeAllProductPopular = view.findViewById(R.id.tvSeeAllPopular)
        seeAllCategories = view.findViewById(R.id.tvSeeAllCategories)

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Memuat Data....")
        progressDialog.setCancelable(false)

        database = FirebaseDatabase.getInstance()

        categories = mutableListOf()
        products = mutableListOf()

        rvCategory.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvCategory.setHasFixedSize(true)
        categoriesAdapter = CategoriesAdapter(categories) {
            val intent = Intent(requireContext(), ProductsByCategoryActivity::class.java)
            intent.putExtra("category_id", it.id)
            intent.putExtra("name_categori", it.name)
            startActivity(intent)
        }
        rvCategory.adapter = categoriesAdapter

        rvProduct.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvProduct.setHasFixedSize(true)
        productsPopularAdapter = ProductsPopularAdapter(products) {
            val intent = Intent(requireContext(), DetailsActivity::class.java)
            intent.putExtra("product_id", it.id)
            startActivity(intent)
        }
        rvProduct.adapter = productsPopularAdapter

        ibvCart.setOnClickListener {
            startActivity(Intent(requireContext(), CartActivity::class.java))
        }
        searchBar.setOnClickListener {
            val intent = Intent(requireContext(), AllProductsPopularActivity::class.java)
            startActivity(intent)
        }
        seeAllProductPopular.setOnClickListener {
            val intent = Intent(requireContext(), AllProductsPopularActivity::class.java)
            startActivity(intent)
        }
        seeAllCategories.setOnClickListener {
            val intent = Intent(requireContext(), AllCategoriesActivity::class.java)
            startActivity(intent)
        }

        getCategories()
        getProducts()
        CoroutineScope(Dispatchers.IO).launch {
            updateCartBadge()
        }
        return view
    }

    private fun getCategories() {
        val categoryRef = database.getReference("categories")
        progressDialog.show()
        categoryRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                for (categorySnapshot in snapshot.children) {
                    val id = categorySnapshot.key ?: ""
                    val name = categorySnapshot.child("name").value.toString()
                    val image = categorySnapshot.child("image").value.toString()
                    val category = CategoriesItem(id, name, image)
                    categories.add(category)
                }
                categoriesAdapter.notifyDataSetChanged()
                progressDialog.dismiss()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
        })
    }

    private fun getProducts() {
        val productRef = database.getReference("products")
        progressDialog.show()
        productRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                products.clear()
                val productRatings = mutableMapOf<String, MutableList<Double>>()

                for (productSnapshot in snapshot.children) {
                    productId = productSnapshot.key ?: ""
                    val category = productSnapshot.child("category").getValue(String::class.java) ?: ""
                    val createdAt = productSnapshot.child("createdAt").getValue(String::class.java) ?: ""
                    val description = productSnapshot.child("description").getValue(String::class.java) ?: ""
                    val image = productSnapshot.child("image").getValue(String::class.java) ?: ""
                    val name = productSnapshot.child("name").getValue(String::class.java) ?: ""
                    val purchasePrice = productSnapshot.child("purchase_price").getValue(Long::class.java)
                    val sellingPrice = productSnapshot.child("selling_price").getValue(Long::class.java)
                    val stock = productSnapshot.child("stock").getValue(Long::class.java)

                    val product = ProductsItem(productId, category, createdAt, description, image, name, purchasePrice, sellingPrice, stock)
                    products.add(product)

                    val ulasanRef = database.getReference("ulasan")
                    ulasanRef.orderByChild("productId").equalTo(productId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(ratingSnapshot: DataSnapshot) {
                            val ratings = mutableListOf<Double>()
                            for (ratingSnap in ratingSnapshot.children) {
                                val rating = ratingSnap.child("rating").getValue(Double::class.java)
                                rating?.let { ratings.add(it) }
                            }
                            productRatings[productId] = ratings
                            updateProductRatingsAndSort(productRatings)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("Firebase", "Error fetching ratings", error.toException())
                        }
                    })
                }
                productsPopularAdapter.notifyDataSetChanged()
                progressDialog.dismiss()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
        })
    }

    private fun updateProductRatingsAndSort(productRatings: Map<String, MutableList<Double>>) {
        for (product in products) {
            val ratings = productRatings[product.id] ?: continue
            val averageRating = if (ratings.isNotEmpty()) ratings.average() else 0.0
            product.averageRating = averageRating
        }
        products.sortBy { it.averageRating }
        productsPopularAdapter.notifyDataSetChanged()
    }

    private fun updateCartBadge() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            val database = FirebaseDatabase.getInstance()
            val cartRef = database.getReference("cart").child(it)

            cartRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val itemCount = snapshot.childrenCount.toInt()
                    ibvCart.setBadgeValue(itemCount)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Error", error.message)
                }
            })
        }
    }
}
