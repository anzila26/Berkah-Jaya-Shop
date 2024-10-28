package com.berkahjayashop.transaction

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.berkahjayashop.ulasan.UlasanActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DetailTransactionActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var productListView: RecyclerView
    private lateinit var textViewAddress: TextView
    private lateinit var tvIdOrder: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvEstimationDate: TextView
    private lateinit var tvShippingName: TextView
    private lateinit var tvStatusShipping: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvLabelTotalHargaBarang: TextView
    private lateinit var tvTotalHargaBarang: TextView
    private lateinit var tvOngkir: TextView
    private lateinit var btKomplain: Button
    private lateinit var btUlasan: Button
    private lateinit var productList: MutableList<ProductOrder>
    private lateinit var database: DatabaseReference
    private lateinit var adapter: ProductOrderAdapter
    private var ongkir: Double = 0.0
    private val standardShippingCost = 10000.0
    private val expressShippingCost = 20000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail_transaction)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        productListView = findViewById(R.id.productListView)
        productListView.layoutManager = LinearLayoutManager(this)

        textViewAddress = findViewById(R.id.textViewAddress)
        tvIdOrder = findViewById(R.id.tvIdOrder)
        tvOrderDate = findViewById(R.id.tvOrderDate)
        tvEstimationDate = findViewById(R.id.tvEstimationDate)
        tvShippingName = findViewById(R.id.tvShippingName)
        tvStatusShipping = findViewById(R.id.tvStatusShipping)
        tvLabelTotalHargaBarang = findViewById(R.id.tvLabelTotalHargaBarang)
        tvTotalHargaBarang = findViewById(R.id.tvTotalHargaBarang)
        tvTotal = findViewById(R.id.tvTotal)
        tvOngkir = findViewById(R.id.tvOngkir)
        btKomplain = findViewById(R.id.btnKomplain)
        btUlasan = findViewById(R.id.btnUlasan)

        database = FirebaseDatabase.getInstance().reference

        productList = mutableListOf()
        adapter = ProductOrderAdapter(productList)
        productListView.adapter = adapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val transactionId = intent.getStringExtra("orderId")
        if (transactionId != null && userId != null) {
            fetchTransactionById(userId, transactionId)
        } else {

            Log.e(TAG, "Transaction ID or User ID is null")
        }

        if (tvShippingName.text == "Standard Shipping"){
            ongkir = standardShippingCost
       }else{
            ongkir = expressShippingCost
        }


        btKomplain.setOnClickListener {
            val intent = Intent(this, KomplainActivity::class.java)
            intent.putExtra("orderId", transactionId)
            startActivity(intent)
        }
        btUlasan.setOnClickListener {
            val intent = Intent(this, UlasanActivity::class.java)
            intent.putExtra("orderId", transactionId)
            startActivity(intent)
        }
    }

    private fun fetchTransactionById(userId: String, transactionId: String) {

        val transactionRef = database.child("orders").child(userId).child(transactionId)

        transactionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val orderId = snapshot.child("orderID").getValue(String::class.java)
                val orderDate = snapshot.child("orderDate").getValue(String::class.java)
                val address = snapshot.child("address").getValue(String::class.java)
                val estimationDate = snapshot.child("estimationDate").getValue(String::class.java)
                val shipping = snapshot.child("shipping").getValue(String::class.java)
                val statusPayment = snapshot.child("statusPayment").getValue(String::class.java)
                val statusShipping = snapshot.child("statusShipping").getValue(String::class.java)
                val totalPrice = snapshot.child("totalPrice").getValue(Double::class.java)


                supportActionBar?.title = "Order Detail - ${orderId ?: ""}"

                textViewAddress.text = address ?: ""
                tvIdOrder.text = orderId ?: ""
                tvOrderDate.text = orderDate ?: ""
                tvEstimationDate.text = estimationDate ?: ""
                tvShippingName.text = shipping ?: ""
                tvStatusShipping.text = statusShipping ?: ""
                tvTotal.text = totalPrice?.toString() ?: ""

                val productsSnapshot = snapshot.child("products")
                val products = mutableListOf<ProductOrder>()
                for (productSnapshot in productsSnapshot.children) {
                    val productId = productSnapshot.child("productId").getValue(String::class.java)
                    val image = productSnapshot.child("image").getValue(String::class.java)
                    val name = productSnapshot.child("name").getValue(String::class.java)
                    val quantity = productSnapshot.child("quantity").getValue(String::class.java)
                    val price = productSnapshot.child("price").getValue(String::class.java)

                    val productOrder = ProductOrder(productId ?: "", image ?: "", name ?: "", quantity.toString(), price.toString())
                    products.add(productOrder)
                }

                productList.clear()
                productList.addAll(products)
                adapter.notifyDataSetChanged()

                calculateTotalPrice(productList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to retrieve transaction details.", error.toException())
            }
        })
    }

    private fun calculateTotalPrice(products: List<ProductOrder>) {
        var totalItems = 0
        var totalPrice = 0.0
        for (product in products) {
            val quantity = product.quantity.toInt()
            val price = product.price.toDouble()
            totalItems += quantity
            totalPrice += quantity * price
        }
        tvLabelTotalHargaBarang.text = "Total Harga ($totalItems Barang)"
        tvTotalHargaBarang.text = "Rp. $totalPrice"
        tvOngkir.text = "Rp. $ongkir"
        updateTotalPrice(totalPrice)
    }

    private fun updateTotalPrice(totalBarang: Double) {
        val totalBelanja = totalBarang + ongkir
        tvTotal.text = "Rp. $totalBelanja"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
