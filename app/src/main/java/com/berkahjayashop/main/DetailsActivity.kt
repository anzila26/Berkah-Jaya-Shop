package com.berkahjayashop.main

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.berkahjayashop.auth.LoginActivity
import com.berkahjayashop.cart.CartActivity
import com.berkahjayashop.cart.CartItem
import com.berkahjayashop.main.item.ProductsItem
import com.berkahjayashop.main.item.UlasanItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.nikartm.support.ImageBadgeView

class DetailsActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase

    private lateinit var ivBack: ImageView
    private lateinit var ibvCart: ImageBadgeView
    private lateinit var ivWishList: ImageView
    private lateinit var tvProductName: TextView
    private lateinit var tvProductAmount: TextView
    private lateinit var tvStockProduct: TextView
    private lateinit var tvDesc: TextView
    private lateinit var rvUlasanProduct: RecyclerView
    private lateinit var buttonAddCart: Button
    private lateinit var ivProduct: ImageView

    private lateinit var progressDialog: ProgressDialog
    private var productId: String? = null
    private var isInWishlist: Boolean = false

    private lateinit var ulasanAdapter: UlasanProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)

        ivBack = findViewById(R.id.ivBack)
        ibvCart = findViewById(R.id.ibvCart)
        ivWishList = findViewById(R.id.ivWishList)
        tvProductName = findViewById(R.id.tvProductName)
        tvProductAmount = findViewById(R.id.tvProductAmount)
        tvStockProduct = findViewById(R.id.tvStockProduct)
        tvDesc = findViewById(R.id.tvDesc)
        ivProduct = findViewById(R.id.ivProduct)
        rvUlasanProduct = findViewById(R.id.rvUlasanProduct)
        buttonAddCart = findViewById(R.id.buttonAddCart)

        ivBack.setOnClickListener { finish() }
        productId = intent.getStringExtra("product_id")
        productId?.let { getProductById(it) }

        ibvCart.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }
        ivWishList.setOnClickListener {
            toggleWishlist()
        }

        rvUlasanProduct.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvUlasanProduct.setHasFixedSize(true)
        ulasanAdapter = UlasanProductAdapter(this, emptyList())
        rvUlasanProduct.adapter = ulasanAdapter
        CoroutineScope(Dispatchers.Main).launch {
            updateCartBadge()
            productId?.let { checkWishlistStatus(it) }
        }
        getUlasan()

    }

    private fun getProductById(id: String) {
        progressDialog.show()
        database = FirebaseDatabase.getInstance()
        val ref = database.getReference("products").child(id)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val product = snapshot.getValue(ProductsItem::class.java)
                    product?.let { displayProductDetails(it) }
                    progressDialog.dismiss()
                } else {
                    Toast.makeText(this@DetailsActivity, "Product not found", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DetailsActivity, "Failed to retrieve product", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
        })
    }

    private fun displayProductDetails(product: ProductsItem) {
        tvProductName.text = product.name
        tvProductAmount.text = "Rp. ${product.selling_price}"
        tvStockProduct.text = "Stok: ${product.stock}"
        tvDesc.text = product.description

        buttonAddCart.setOnClickListener {
            addToCart(product)
        }
    }

    private fun addToCart(product: ProductsItem) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val createAt = System.currentTimeMillis()
        if (userId != null) {
            progressDialog.show()
            val database = FirebaseDatabase.getInstance()
            val cartRef = database.getReference("cart").child(userId)
            cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var productExistsInCart = false
                    for (cartSnapshot in snapshot.children) {
                        val existingItem = cartSnapshot.getValue(CartItem::class.java)
                        if (existingItem?.productId == productId) {
                            productExistsInCart = true
                            val updatedQuantity = existingItem?.quantity?.plus(1)
                            cartSnapshot.ref.child("quantity").setValue(updatedQuantity)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this@DetailsActivity,
                                        "Product added to cart",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    progressDialog.dismiss()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this@DetailsActivity,
                                        "Failed to update product quantity in cart: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    progressDialog.dismiss()
                                }
                            break
                        }
                    }
                    if (!productExistsInCart) {
                        cartRef.push().setValue(
                            CartItem(
                                productId!!,
                                createAt,
                                product.name!!,
                                product.selling_price!!.toString(),
                                1,
                                product.image!!
                            )
                        )
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@DetailsActivity,
                                    "Product added to cart",
                                    Toast.LENGTH_SHORT
                                ).show()
                                progressDialog.dismiss()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this@DetailsActivity,
                                    "Failed to add to cart: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                progressDialog.dismiss()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DetailsActivity, "Failed to add to cart: ${error.message}", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }
            })
        } else {
            progressDialog.dismiss()
            Toast.makeText(this@DetailsActivity, "Please log in first to add to cart", Toast.LENGTH_SHORT).show()
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

    private fun checkWishlistStatus(productId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val wishlistRef = FirebaseDatabase.getInstance().getReference("wishlist").child(userId).child(productId)
            wishlistRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    isInWishlist = snapshot.exists()
                    updateWishlistIcon()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Error", error.message)
                }
            })
        }
    }

    private fun toggleWishlist() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val wishlistRef = FirebaseDatabase.getInstance().getReference("wishlist").child(userId).child(productId!!)
            if (isInWishlist) {
                wishlistRef.removeValue()
                    .addOnSuccessListener {
                        isInWishlist = false
                        updateWishlistIcon()
                        Toast.makeText(this, tvProductName.text.toString()+ " Removed from Wishlist", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to remove from Wishlist: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                val wishlistItem = mapOf(
                    "productId" to productId,
                    "timestamp" to System.currentTimeMillis()
                )
                wishlistRef.setValue(wishlistItem)
                    .addOnSuccessListener {
                        isInWishlist = true
                        updateWishlistIcon()
                        Toast.makeText(this, tvProductName.text.toString() + " Added to Wishlist", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to add to Wishlist: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            Toast.makeText(this, "Please log in first to manage Wishlist", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateWishlistIcon() {
        if (isInWishlist) {
            ivWishList.setImageResource(R.drawable.ic_wishlist_filled)
        } else {
            ivWishList.setImageResource(R.drawable.ic_wishlist)
        }
    }

    private fun getUlasan() {
        val ulasanRef = FirebaseDatabase.getInstance().reference
        val query = ulasanRef.child("ulasan").orderByChild("productId").equalTo(productId)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ulasanList = mutableListOf<UlasanItem>()
                for (ulasanSnapshot in snapshot.children) {
                    val ulasan = ulasanSnapshot.getValue(UlasanItem::class.java)
                    ulasan?.let { ulasanList.add(it) }
                    Log.d("Ulasan", ulasan.toString())
                }
                ulasanList.reverse()
                ulasanAdapter.updateData(ulasanList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching data", error.toException())
            }
        })
    }




}
