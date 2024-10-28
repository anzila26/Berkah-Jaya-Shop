package com.berkahjayashop.checkout

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.berkahjayashop.main.item.Product
import com.berkahjayashop.R
import com.berkahjayashop.main.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PaymentFinishActivity : AppCompatActivity() {

    private lateinit var tvOrderId: TextView
    private lateinit var ivPaymentStatus: ImageView
    private lateinit var tvPaymentStatus: TextView
    private lateinit var btnToHome: Button
    private lateinit var productList: List<Product>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment_finish)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        productList = getProductsFromSharedPreferences(this)
        val orderId = intent.getStringExtra("orderId")
        val status = intent.getStringExtra("status")
        ivPaymentStatus = findViewById(R.id.imageView3)
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus)
        tvOrderId = findViewById(R.id.tvOrderId)
        btnToHome = findViewById(R.id.btn_home)

        if (status == "Berhasil") {
            ivPaymentStatus.setImageResource(R.drawable.ic_success)
            tvPaymentStatus.text = "Pembayaran Berhasil"
            val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()
            updateStock()

            if (orderId != null) {
                updateOrder(orderId, "Lunas")
            }
        }else if (status == "Gagal") {
            ivPaymentStatus.setImageResource(R.drawable.ic_gagal)
            tvPaymentStatus.text = "Pembayaran Gagal"
            val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()
            updateStock()
            if (orderId != null) {
                updateOrder(orderId, "Gagal")
            }
        }

        tvOrderId.text = orderId
        btnToHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            clearCart(FirebaseAuth.getInstance().currentUser?.uid ?: "")
            startActivity(intent)
            finish()
        }
    }
    private fun updateStock() {
        val database = FirebaseDatabase.getInstance()
        val productRef = database.getReference("products")

        for (product in productList) {
            val productId = product.productId
            val quantity = product.quantity
            productRef.child(productId).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val currentStock = snapshot.child("stock").getValue(Int::class.java) ?: 0
                    val newStock = currentStock - quantity
                    productRef.child(productId).child("stock").setValue(newStock)
                        .addOnSuccessListener {
                            Log.d("PaymentActivity", "Stock for product $productId successfully updated")
                        }.addOnFailureListener { e ->
                            Log.e("PaymentActivity", "Error updating stock for product $productId", e)
                        }
                }
            }
        }
    }

    private fun clearCart(userId: String) {
        val database = FirebaseDatabase.getInstance()

        for (product in productList) {
            val productId = product.productId
            val cartRef = database.getReference("cart").child(userId).orderByChild("productId")
                .equalTo(productId)
            cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (cartSnapshot in snapshot.children) {
                        cartSnapshot.ref.removeValue().addOnCompleteListener {
                            if (it.isSuccessful) {
                                Log.d("CartActivity", "Item removed from cart")
                            } else {
                                Log.e("CartActivity", "Failed to remove item from cart", it.exception)
                            }
                        }
                    }

                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("CartActivity", "Failed to retrieve cart items", error.toException())
                }
            })

        }



    }

    private fun getProductsFromSharedPreferences(context: Context): List<Product> {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String? = sharedPreferences.getString("products", null)
        val type = object : TypeToken<List<Product>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    private fun updateOrder(orderId: String, status: String) {
        val database = FirebaseDatabase.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val orderRef = database.getReference("orders").child(userId).child(orderId)
        orderRef.child("statusPayment").setValue(status)
        if (status == "Lunas") {
            orderRef.child("statusShipping").setValue("Di proses")
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        clearCart(FirebaseAuth.getInstance().currentUser?.uid ?: "")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}