package com.berkahjayashop.checkout

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.berkahjayashop.main.item.Product
import com.berkahjayashop.R
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.midtrans.sdk.corekit.callback.TransactionFinishedCallback
import com.midtrans.sdk.corekit.core.MidtransSDK
import com.midtrans.sdk.corekit.core.TransactionRequest
import com.midtrans.sdk.corekit.models.CustomerDetails
import com.midtrans.sdk.corekit.models.ItemDetails
import com.midtrans.sdk.corekit.models.snap.TransactionResult
import com.midtrans.sdk.uikit.SdkUIFlowBuilder

class PaymentActivity : AppCompatActivity(), TransactionFinishedCallback {

    private lateinit var toolbar: Toolbar
    private lateinit var productList: List<Product>
    private lateinit var productIdList: List<String>
    private lateinit var productPriceList: List<Double>
    private lateinit var productQuantityList: List<Int>
    private lateinit var productNameList: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Pilih Pembayaran"
            setDisplayHomeAsUpEnabled(true)
        }

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        productList = getProductsFromSharedPreferences(this)
        productIdList = productList.map { it.productId }
        productPriceList = productList.map { it.price }
        productQuantityList = productList.map { it.quantity }
        productNameList = productList.map { it.name }
        setupMidtrans()
        setupOrder()
    }

    private fun setupOrder() {
        val orderId = intent.getStringExtra("orderId") ?: ""
        val totalPrice = intent.getDoubleExtra("totalPrice", 0.0)
        val customerDetails = customerDetails()

        val itemDetailsList = itemDetails()
        MidtransSDK.getInstance().transactionRequest = transactionRequest(orderId, totalPrice, customerDetails, itemDetailsList)
        MidtransSDK.getInstance().startPaymentUiFlow(this@PaymentActivity)
    }

    private fun setupMidtrans() {
        SdkUIFlowBuilder.init()
            .setContext(this)
            .setMerchantBaseUrl("https://shop-berkah-jaya.000webhostapp.com/api/")
            .setClientKey("SB-Mid-client-GAszLHNeAZsypeSg")
            .setTransactionFinishedCallback(this@PaymentActivity)
            .enableLog(true)
            .setLanguage("id")
            .buildSDK()
    }

    private fun customerDetails(): CustomerDetails {
        val cd = CustomerDetails()
        val user = FirebaseAuth.getInstance().currentUser
        cd.customerIdentifier = user?.uid ?: ""
        cd.firstName = user?.displayName ?: ""
        cd.email = user?.email ?: ""
        return cd
    }

    private fun itemDetails(): ArrayList<ItemDetails> {
        val itemDetails = arrayListOf<ItemDetails>()
        val maxNameLength = 10
        val shippingCost = intent.getDoubleExtra("shippingCost", 0.0)
        val shippingName = intent.getStringExtra("shippingName") ?: ""
        for (product in productList) {
            val itemName = if (product.name.length > maxNameLength) {
                product.name.substring(0, maxNameLength)
            } else {
                product.name
            }

            itemDetails.add(
                ItemDetails(
                    product.productId,
                    product.price,
                    product.quantity,
                    itemName
                )
            )
        }
        itemDetails.add(
            ItemDetails(
                "shipping",
                shippingCost,
                1,
                shippingName
            )
        )
        return itemDetails
    }


    private fun transactionRequest(
        id: String,
        price: Double,
        customerDetails: CustomerDetails,
        itemDetails: ArrayList<ItemDetails>
    ): TransactionRequest {
        val request = TransactionRequest(id, price)
        request.itemDetails = itemDetails
        request.customerDetails = customerDetails
        return request
    }

    override fun onTransactionFinished(transactionResult: TransactionResult) {
        when {
            transactionResult.response != null -> {
                when (transactionResult.status) {
                    (TransactionResult.STATUS_SUCCESS) -> {
                       Log.e("PaymentActivity", "Transaction status: ${transactionResult.status}")

                    }
                    TransactionResult.STATUS_PENDING -> {
                        Log.e("PaymentActivity", "Transaction status: ${transactionResult.status}")
                    }
                    TransactionResult.STATUS_FAILED -> {
                        Log.e("PaymentActivity", "Transaction status: ${transactionResult.status}")
                    }

                }
                if (transactionResult.status.equals("Pending", true)) {
                    val orderId = intent.getStringExtra("orderId") ?: ""

                    val intent = Intent(this, PaymentFinishActivity::class.java)
                    intent.putExtra("orderId", orderId)
                    intent.putExtra("status", "Berhasil")
                    startActivity(intent)
                    finish()
                }else if (transactionResult.status.equals("Failed", true)) {
                    val orderId = intent.getStringExtra("orderId") ?: ""

                    val intent = Intent(this, PaymentFinishActivity::class.java)
                    intent.putExtra("orderId", orderId)
                    intent.putExtra("status", "Gagal")
                    startActivity(intent)
                    finish()
                }

            }
            transactionResult.isTransactionCanceled -> {
                Toast.makeText(this, "Canceled transaction", Toast.LENGTH_LONG).show()
                finish()
            }
            else -> {
                if (transactionResult.status.equals(TransactionResult.STATUS_INVALID, true)) {
                    Toast.makeText(this, "Invalid transaction", Toast.LENGTH_LONG).show()
                    finish()
                }
                else {
                    Toast.makeText(this, "Failure transaction", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
        Log.e("PaymentActivity", "Transaction status: ${transactionResult.status}")
    }


    private fun getProductsFromSharedPreferences(context: Context): List<Product> {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json: String? = sharedPreferences.getString("products", null)
        val type = object : TypeToken<List<Product>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }



    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return false
    }
}
