package com.berkahjayashop.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.berkahjayashop.auth.LoginActivity
import com.berkahjayashop.cart.CartActivity
import com.berkahjayashop.main.item.ProductsItem
import com.berkahjayashop.main.mainfragment.adapter.ProductAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import ru.nikartm.support.ImageBadgeView

class ProductsByCategoryActivity : AppCompatActivity(), ProductAdapter.OnItemClickListener,
    ProductAdapter.OnFavoriteClickListener {

    private lateinit var rvProducts: RecyclerView
    private lateinit var ibvCart: ImageBadgeView
    private lateinit var searchProduct: SearchView
    private lateinit var productList: MutableList<ProductsItem>
    private lateinit var filteredList: MutableList<ProductsItem>
    private lateinit var database: FirebaseDatabase
    private lateinit var btBack: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var productAdapter: ProductAdapter
    private var isInWishlist: Boolean = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products_by_category)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        ibvCart = findViewById(R.id.ibvCart)
        searchProduct = findViewById(R.id.searchProduct)

        ibvCart.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }
        productList = mutableListOf()
        rvProducts = findViewById(R.id.rvProductByCategory)
        productAdapter = ProductAdapter(this, productList, this, this)
        rvProducts.adapter = productAdapter
        rvProducts.layoutManager = GridLayoutManager(this, 2)

        val categoryId = intent.getStringExtra("category_id")
        filteredList = mutableListOf()
        searchProduct.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    filter(query)
                }
                if (categoryId != null) {
                    getProducts(categoryId)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    if (categoryId != null) {
                        getProducts(categoryId)
                    }
                }
                return true
            }
        })
        if (searchProduct.query.isEmpty() || filteredList.isEmpty()) {
            if (categoryId != null) {
                getProducts(categoryId)
            }
        }


        updateCartBadge()

        btBack = findViewById(R.id.ivBack)
        btBack.setOnClickListener {
            finish()
        }
    }
    private fun filter(query: String) {
        filteredList.clear()
        val searchText = query.toLowerCase().trim()
        for (product in productList) {
            if (product.name!!.toLowerCase().contains(searchText)) {
                filteredList.add(product)
            } else {
                Toast.makeText(this, "No Data Found", Toast.LENGTH_SHORT).show()
            }

        }
        if (filteredList.isEmpty()) {
            productAdapter.updateList(productList)
        } else {
            productAdapter.updateList(filteredList)
        }
    }
    override fun onItemClick(product: ProductsItem) {
        val intent = Intent(this, DetailsActivity::class.java)
        intent.putExtra("product_id", product.id)
        startActivity(intent)
    }

    override fun onFavoriteClick(product: ProductsItem) {
        product.id?.let { toggleWishlist(it) }
    }

    private fun getProducts(categoryId: String) {
        val productRef = database.getReference("products").orderByChild("category").equalTo(categoryId)
        val userId = auth.currentUser?.uid

        productRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                for (productSnapshot in snapshot.children) {
                    val id = productSnapshot.key ?: ""
                    val category = productSnapshot.child("category").getValue(String::class.java) ?: ""
                    val createdAt = productSnapshot.child("createdAt").getValue(String::class.java) ?: ""
                    val description = productSnapshot.child("description").getValue(String::class.java) ?: ""
                    val image = productSnapshot.child("image").getValue(String::class.java) ?: ""
                    val name = productSnapshot.child("name").getValue(String::class.java) ?: ""
                    val purchasePrice = productSnapshot.child("purchase_price").getValue(Long::class.java)
                    val sellingPrice = productSnapshot.child("selling_price").getValue(Long::class.java)
                    val stock = productSnapshot.child("stock").getValue(Long::class.java)

                    val product = ProductsItem(id, category, createdAt, description, image, name, purchasePrice, sellingPrice, stock)
                    productList.add(product)

                    if (userId != null) {
                        val wishlistRef = database.getReference("wishlist").child(userId).child(id)
                        wishlistRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(wishlistSnapshot: DataSnapshot) {
                                val isFavorite = wishlistSnapshot.exists()
                                isInWishlist = isFavorite
                                productAdapter.updateFavoriteStatus(id, isFavorite)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@ProductsByCategoryActivity, error.message, Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
                productAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProductsByCategoryActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun toggleWishlist(productId: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val wishlistRef = database.getReference("wishlist").child(userId).child(productId)
            wishlistRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        wishlistRef.removeValue()
                            .addOnSuccessListener {
                                productAdapter.updateFavoriteStatus(productId, false)
                                isInWishlist = false
                                Toast.makeText(this@ProductsByCategoryActivity, "Removed from Wishlist", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@ProductsByCategoryActivity, "Failed to remove from Wishlist: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        val wishlistItem = mapOf(
                            "productId" to productId,
                            "timestamp" to System.currentTimeMillis()
                        )
                        wishlistRef.setValue(wishlistItem)
                            .addOnSuccessListener {
                                productAdapter.updateFavoriteStatus(productId, true)
                                isInWishlist = true
                                Toast.makeText(this@ProductsByCategoryActivity, "Added to Wishlist", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@ProductsByCategoryActivity, "Failed to add to Wishlist: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProductsByCategoryActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "Please log in first to manage Wishlist", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
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
