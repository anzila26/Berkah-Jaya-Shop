package com.berkahjayashop.ulasan

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UlasanActivity : AppCompatActivity()  {
    private var orderId: String? = null
    private lateinit var toolbar: Toolbar
    private lateinit var database: DatabaseReference
    private val productList = mutableListOf<UlasanProduct>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UlasanAdapter
    private lateinit var btnKirimUlasan: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ulasan)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Ulasan"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btnKirimUlasan = findViewById(R.id.btnKirimUlasan)
        recyclerView = findViewById(R.id.rvUlasan)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UlasanAdapter(this, productList, getUserId()!!)
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().reference
        orderId = intent.getStringExtra("orderId")
        if (getUserId() != null && orderId != null) {
            fetchTransactionById(getUserId(), orderId!!)
        }
        btnKirimUlasan.setOnClickListener {
            adapter.saveDataToFirebase()
            Toast.makeText(this, "Ulasan berhasil dikirim", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun getUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun fetchTransactionById(userId: String?, transactionId: String) {
        userId ?: return

        val transactionRef = database.child("orders").child(userId).child(transactionId)

        transactionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productsSnapshot = snapshot.child("products")
                val products = mutableListOf<UlasanProduct>()
                for (productSnapshot in productsSnapshot.children) {
                    val productId = productSnapshot.child("productID").getValue(String::class.java)
                    val image = productSnapshot.child("image").getValue(String::class.java)
                    val name = productSnapshot.child("name").getValue(String::class.java)
                    val quantity = productSnapshot.child("quantity").getValue(String::class.java)
                    val price = productSnapshot.child("price").getValue(String::class.java)

                    val productOrder = UlasanProduct(productId ?: "", image ?: "", name ?: "", quantity.toString(), price.toString())
                    products.add(productOrder)
                }

                productList.clear()
                productList.addAll(products)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(ContentValues.TAG, "Failed to retrieve transaction details.", error.toException())
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
