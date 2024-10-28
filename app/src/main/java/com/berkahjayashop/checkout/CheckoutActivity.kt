package com.berkahjayashop.checkout

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.main.item.Product
import com.berkahjayashop.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.google.gson.Gson

class CheckoutActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var productListView: RecyclerView
    private lateinit var editTextAddress: EditText
    private lateinit var buttonAddAddress: Button
    private lateinit var radioButtonStandard: RadioButton
    private lateinit var radioButtonExpress: RadioButton
    private lateinit var radioButtonFree: RadioButton
    private lateinit var buttonCheckout: Button
    private lateinit var tvTotal: TextView
    private lateinit var tvTotalHargaBarang: TextView
    private lateinit var tvLabelTotalHargaBarang: TextView
    private lateinit var tvOngkir: TextView
    private lateinit var radioGroupShipping: RadioGroup
    private lateinit var tvIdOrder: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvEstimationDate: TextView

    private var ongkir = 0.0
    private val freeShippingCost = 0.0
    private val standardShippingCost = 10000.0
    private val expressShippingCost = 20000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_check_out)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Rincian Pemesanan"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        productListView = findViewById(R.id.productListView)
        editTextAddress = findViewById(R.id.editTextAddress)
        buttonAddAddress = findViewById(R.id.buttonAddAddress)
        radioButtonStandard = findViewById(R.id.radioButtonStandard)
        radioButtonExpress = findViewById(R.id.radioButtonExpress)
        radioButtonFree = findViewById(R.id.radioButtonFree)
        buttonCheckout = findViewById(R.id.buttonCheckout)
        tvTotal = findViewById(R.id.tvTotal)
        tvTotalHargaBarang = findViewById(R.id.tvTotalHargaBarang)
        tvLabelTotalHargaBarang = findViewById(R.id.tvLabelTotalHargaBarang)
        tvOngkir = findViewById(R.id.tvOngkir)
        radioGroupShipping = findViewById(R.id.radioGroupShipping)
        tvIdOrder = findViewById(R.id.tvIdOrder)
        tvOrderDate = findViewById(R.id.tvOrderDate)
        tvEstimationDate = findViewById(R.id.tvEstimationDate)

        val selectedProducts = intent.getSerializableExtra("selectedProducts") as ArrayList<Array<String>>
        Log.d("CheckoutActivity", "Selected Products: $selectedProducts")

        val totalPrice = intent.getDoubleExtra("totalPrice", 0.0)
        tvTotal.text = "Rp. ${totalPrice}"

        val adapter = CheckoutAdapter(this, selectedProducts)
        productListView.layoutManager = LinearLayoutManager(this)
        productListView.adapter = adapter

        radioButtonStandard.isChecked = true
        ongkir = standardShippingCost
        calculateTotalPrice(selectedProducts)
        calculateShippingCost()
        updateTotalPrice()

        buttonAddAddress.setOnClickListener {
            if (editTextAddress.text.isNotEmpty()) {
                if (buttonAddAddress.text == "Edit Address") {
                    editTextAddress.isEnabled = true
                    updateAddressInFirebase()
                    buttonAddAddress.text = "Save Address"
                } else {
                    editTextAddress.isEnabled = false
                    saveAddress()
                    buttonAddAddress.text = "Edit Address"
                }
            } else {
                editTextAddress.isEnabled = true
                saveAddress()
                buttonAddAddress.text = "Save Address"
            }
        }

        getAddress()

        radioGroupShipping.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonStandard -> ongkir = standardShippingCost
                R.id.radioButtonExpress -> ongkir = expressShippingCost
                R.id.radioButtonFree -> ongkir = freeShippingCost
            }
            calculateShippingCost()
            updateTotalPrice()
        }

        val orderId = generateOrderId()
        tvIdOrder.text = orderId

        val orderDate = getCurrentDateTime()
        tvOrderDate.text = orderDate

        val selectedShipping = when {
            radioButtonStandard.isChecked -> "Standard Shipping"
            radioButtonExpress.isChecked -> "Express Shipping"
            radioButtonFree.isChecked -> "Free Shipping"
            else -> "Standard Shipping"
        }
        val estimationDate = calculateEstimationDate(orderDate, selectedShipping)
        tvEstimationDate.text = estimationDate

        buttonCheckout.setOnClickListener {
            val orderId = tvIdOrder.text.toString()
            val orderDate = tvOrderDate.text.toString()
            val estimationDate = tvEstimationDate.text.toString()
            val totalPrice = tvTotal.text.toString().replace("Rp. ", "").toDouble()
            val shipping = when {
                radioButtonStandard.isChecked -> "Standard Shipping"
                radioButtonExpress.isChecked -> "Express Shipping"
                radioButtonFree.isChecked -> "Free Shipping"
                else -> "Standard Shipping"
            }
            val shippingCost = when {
                radioButtonStandard.isChecked -> standardShippingCost
                radioButtonExpress.isChecked -> expressShippingCost
                radioButtonFree.isChecked -> freeShippingCost
                else -> standardShippingCost
            }
            val selectedProducts = intent.getSerializableExtra("selectedProducts") as ArrayList<Array<String>>

            val products = selectedProducts.map {
                Product(
                    productId = it[0],
                    name = it[2],
                    quantity = it[3].toInt(),
                    price = it[4].toDouble()

                )
            }
            val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()
            saveProductsToSharedPreferences(this, products)
            saveOrderToFirebase(orderId, orderDate, estimationDate, selectedProducts, totalPrice)

            val intent = Intent(this, PaymentActivity::class.java)
            intent.putExtra("orderId", orderId)
            intent.putExtra("shippingName", shipping)
            intent.putExtra("shippingCost", shippingCost)
            intent.putExtra("totalPrice", totalPrice)

            startActivity(intent)
        }
    }

    private fun calculateTotalPrice(products: ArrayList<Array<String>>) {
        var totalItems = 0
        var totalPrice = 0.0
        for (product in products) {
            val quantity = product[3].toInt()
            val price = product[4].toDouble()
            totalItems += quantity
            totalPrice += quantity * price
        }
        tvLabelTotalHargaBarang.text = "Total Harga ($totalItems Barang)"
        tvTotalHargaBarang.text = "Rp. $totalPrice"
    }

    private fun calculateShippingCost() {
        tvOngkir.text = "Rp. $ongkir"
    }

    private fun updateTotalPrice() {
        val totalBarang = tvTotalHargaBarang.text.toString().replace("Rp. ", "").toDouble()
        val totalBelanja = totalBarang + ongkir
        tvTotal.text = "Rp. $totalBelanja"
    }

    private fun getAddress() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance()
        val addressRef = database.getReference("users").child(userId!!).child("address")
        addressRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val address = snapshot.getValue(String::class.java)
                editTextAddress.setText(address)
                editTextAddress.isEnabled = false
                buttonAddAddress.text = "Edit Address"
            }
        }
    }

    private fun saveAddress() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance()
        val addressRef = database.getReference("users").child(userId!!).child("address")
        addressRef.setValue(editTextAddress.text.toString())
    }

    private fun updateAddressInFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance()
        val addressRef = database.getReference("users").child(userId!!).child("address")
        addressRef.setValue(editTextAddress.text.toString())
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun generateOrderId(): String {
        return "BJSHOP-${(100000..999999).random()}"
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun calculateEstimationDate(orderDate: String, selectedShipping: String): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = sdf.parse(orderDate.split(" ")[0])!!

        val estimationDays = when (selectedShipping) {
            "Standard Shipping" -> 4
            "Express Shipping" -> 2
            "Free Shipping" -> 5
            else -> 3
        }

        calendar.add(Calendar.DAY_OF_MONTH, estimationDays)
        return sdf.format(calendar.time)
    }

    private fun saveOrderToFirebase(orderId: String, orderDate: String, estimationDate: String, selectedProducts: ArrayList<Array<String>>, totalPrice: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance()
        val orderRef = database.getReference("orders").child(userId!!).child(orderId)
        val shipping = when {
            radioButtonStandard.isChecked -> "Standard Shipping"
            radioButtonExpress.isChecked -> "Express Shipping"
            radioButtonFree.isChecked -> "Free Shipping"
            else -> "Standard Shipping"
        }
        val order = HashMap<String, Any>()
        order["orderID"] = orderId
        order["orderDate"] = orderDate
        order["estimationDate"] = estimationDate
        order["totalPrice"] = totalPrice
        order["address"] = editTextAddress.text.toString()
        order["shipping"] = shipping
        order["statusPayment"] = "Belum Lunas"
        order["statusShipping"] = ""

        orderRef.setValue(order)
            .addOnSuccessListener {
                Log.d("CheckoutActivity", "Order successfully saved to Firebase")

                for (product in selectedProducts) {
                    val productId = product[0]
                    val productRef = orderRef.child("products").child(productId)
                    val productData = HashMap<String, Any>()
                    productData["productID"] = productId
                    productData["image"] = product[1]
                    productData["name"] = product[2]
                    productData["price"] = product[4]
                    productData["quantity"] = product[3]

                    productRef.setValue(productData)
                        .addOnSuccessListener {
                            Log.d("CheckoutActivity", "Product $productId successfully saved to Firebase")
                        }
                        .addOnFailureListener { e ->
                            Log.e("CheckoutActivity", "Error saving product $productId to Firebase", e)
                        }
                }

            }
            .addOnFailureListener { e ->
                Log.e("CheckoutActivity", "Error saving order to Firebase", e)

            }
    }

    private fun saveProductsToSharedPreferences(context: Context, products: List<Product>) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(products)
        editor.putString("products", json)
        editor.apply()
    }
}
