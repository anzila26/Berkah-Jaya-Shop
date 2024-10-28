package com.berkahjayashop.main

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.berkahjayashop.cart.CartActivity
import com.berkahjayashop.main.item.ProductsItem
import com.berkahjayashop.main.mainfragment.adapter.ProductsPopularAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.nikartm.support.ImageBadgeView

class AllProductsPopularActivity : AppCompatActivity() {

    private lateinit var productsPopularAdapter: ProductsPopularAdapter
    private lateinit var progressDialog: ProgressDialog
    private lateinit var database: FirebaseDatabase
    private lateinit var searchProduct: SearchView
    private lateinit var btBack: ImageView
    private lateinit var products: MutableList<ProductsItem>
    private lateinit var ibvCart: ImageBadgeView
    private lateinit var rvProduct: RecyclerView
    private lateinit var filteredList: MutableList<ProductsItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_all_products_popular)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ibvCart = findViewById(R.id.ibvCart)
        rvProduct = findViewById(R.id.rvAllProductPopular)
        searchProduct = findViewById(R.id.searchProduct)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Memuat Product....")
        progressDialog.setCancelable(false)

        database = FirebaseDatabase.getInstance()

        products = mutableListOf()
        filteredList = mutableListOf()

        rvProduct.layoutManager = GridLayoutManager(this, 2)
        rvProduct.setHasFixedSize(true)
        productsPopularAdapter = ProductsPopularAdapter(products) { product ->
            val intent = Intent(this, DetailsActivity::class.java)
            intent.putExtra("product_id", product.id)
            startActivity(intent)
        }
        rvProduct.adapter = productsPopularAdapter

        ibvCart.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        searchProduct.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    filter(newText)
                }
                return false
            }
        })
        btBack = findViewById(R.id.ivBack)
        btBack.setOnClickListener {
            finish()
        }

        getProducts()
        CoroutineScope(Dispatchers.Main).launch {
            updateCartBadge()
        }

    }

    private fun filter(query: String) {
        filteredList.clear()
        val searchText = query.toLowerCase().trim()
        for (product in products) {
            if (product.name?.toLowerCase()!!.contains(searchText)) {
                filteredList.add(product)
            }
        }
        productsPopularAdapter.updateList(filteredList)
    }

    private fun getProducts() {
        val productRef = database.getReference("products")
        progressDialog.show()
        productRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                products.clear()
                filteredList.clear()
                val productRatings = mutableMapOf<String, MutableList<Double>>()

                for (productSnapshot in snapshot.children) {
                    val productId = productSnapshot.key ?: ""
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
                    filteredList.add(product)

                    val ulasanRef = database.getReference("ulasan")
                    ulasanRef.orderByChild("productId").equalTo(productId).addListenerForSingleValueEvent(object :
                        ValueEventListener {
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
                progressDialog.dismiss()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AllProductsPopularActivity, error.message, Toast.LENGTH_SHORT).show()
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
        products.sortByDescending { it.averageRating }
        productsPopularAdapter.notifyDataSetChanged()
    }

    private fun updateCartBadge() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
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
