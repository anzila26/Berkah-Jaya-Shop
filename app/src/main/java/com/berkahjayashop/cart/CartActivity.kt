package com.berkahjayashop.cart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.berkahjayashop.checkout.CheckoutActivity
import com.berkahjayashop.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class CartActivity : AppCompatActivity(), CartAdapter.OnItemClickListener {

    private lateinit var toolbar: Toolbar
    private lateinit var rvCart: RecyclerView
    private lateinit var dividerView: View
    private lateinit var cbAll: CheckBox
    private lateinit var tvTotal: TextView
    private lateinit var buttonBuy: Button

    private lateinit var cartAdapter: CartAdapter
    private var cartItems = mutableListOf<CartItem>()
    private var selectedItems = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_cart)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        toolbar = findViewById(R.id.toolbar)
        rvCart = findViewById(R.id.rvCart)
        dividerView = findViewById(R.id.view)
        cbAll = findViewById(R.id.cbAll)
        tvTotal = findViewById(R.id.tvTotal)
        buttonBuy = findViewById(R.id.buttonBuy)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Keranjang"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        cartAdapter = CartAdapter(this,selectedItems, cartItems, this)
        rvCart.layoutManager = LinearLayoutManager(this)
        rvCart.adapter = cartAdapter

        cbAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                for (i in cartItems.indices) {
                    if (!selectedItems.contains(i)) {
                        selectedItems.add(i)
                        cartAdapter.notifyItemChanged(i)
                    }
                }
            }else {
                selectedItems.clear()
                cartAdapter.notifyDataSetChanged()
            }
            if (cartItems.isNotEmpty()){
                updateButtonAndTotal()
            }


        }


        buttonBuy.setOnClickListener {
            if (selectedItems.isNotEmpty()) {
                val intent = Intent(this, CheckoutActivity::class.java)

                val selectedProducts = selectedItems.map { index ->
                    val cartItem = cartItems[index]
                    arrayOf(cartItem.productId, cartItem.image, cartItem.name, cartItem.quantity.toString(), cartItem.price)
                }

                intent.putExtra("selectedProducts", ArrayList(selectedProducts))
                intent.putExtra("totalPrice", tvTotal.text.toString().replace("Rp. ", "").replace(",", "").toDoubleOrNull() ?: 0.0)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select at least one item", Toast.LENGTH_SHORT).show()
            }
        }
        loadCartItems()

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadCartItems() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance()
        val cartRef = database.getReference("cart").child(userId)

        cartRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cartItems.clear()
                for (cartSnapshot in snapshot.children) {
                    val cartItem = cartSnapshot.getValue(CartItem::class.java)
                    cartItem?.let { cartItems.add(it) }

                }
                cartAdapter.notifyDataSetChanged()
                updateButtonAndTotal()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CartActivity", "Failed to load cart items: ${error.message}")
                Toast.makeText(this@CartActivity, "Failed to load cart items", Toast.LENGTH_SHORT).show()
            }
        })
    }


    override fun onDeleteItemClick(position: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance()

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Delete Item")
        alertDialogBuilder.setMessage("Are you sure you want to remove this item from your cart?")
        alertDialogBuilder.setPositiveButton("Yes") { _, _ ->
            val cartItem = cartItems[position]
            val productId = cartItem.productId

            if (productId != null) {
                cartItems.removeAt(position)
                cartAdapter.notifyItemRemoved(position)
                updateButtonAndTotal()
                cartAdapter.notifyDataSetChanged()
                val productRef = database.getReference("cart").child(userId).orderByChild("productId").equalTo(productId)
                productRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (cartSnapshot in snapshot.children) {
                            cartSnapshot.ref.removeValue().addOnCompleteListener {
                                if (it.isSuccessful) {
                                    Toast.makeText(this@CartActivity, "Product removed from cart", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@CartActivity, "Failed to remove item", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@CartActivity, "Failed to remove item: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })

            }
        }
        alertDialogBuilder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }



    override fun onMinusClick(position: Int) {
        val item = cartItems[position]
        if (item.quantity > 1) {
            item.quantity--
            updateCartItemQuantity(item)
            cartAdapter.notifyItemChanged(position)
            updateButtonAndTotal()
        }
    }

    override fun onPlusClick(position: Int) {
        val item = cartItems[position]
        item.quantity++
        updateCartItemQuantity(item)
        cartAdapter.notifyItemChanged(position)
        updateButtonAndTotal()
    }


    override fun onCheckBoxClick(position: Int, isChecked: Boolean) {
        if (position < cartItems.size) {
            if (isChecked) {
                selectedItems.add(position)
            } else {
                selectedItems.remove(position)
            }
            updateButtonAndTotal()
            updateSelectAllCheckbox()
        }
    }

    private fun updateCartItemQuantity(cartItem: CartItem) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance()
        val cartRef = database.getReference("cart").child(userId)
        cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (cartSnapshot in snapshot.children) {
                        val existingItem = cartSnapshot.getValue(CartItem::class.java)
                        if (existingItem?.productId == cartItem.productId) {
                            cartSnapshot.ref.child("quantity").setValue(cartItem.quantity)

                            break
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CartActivity", "Failed to update product quantity: ${error.message}")
            }
        })

    }

    private fun updateButtonAndTotal() {
        var totalAmount = 0.0
        if (selectedItems.isNotEmpty()) {
            for (i in selectedItems) {
                if (i < cartItems.size) {
                    val item = cartItems[i]
                    val price = item.price?.replace("Rp. ", "")?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                    totalAmount += price * item.quantity
                }
            }
            tvTotal.text = "Rp. ${totalAmount}"
            buttonBuy.isEnabled = true
        } else {
            tvTotal.text = "-"
            buttonBuy.isEnabled = false
        }
    }

    private fun updateSelectAllCheckbox() {
        cbAll.setOnCheckedChangeListener(null)
        cbAll.isChecked = selectedItems.size == cartItems.size
        cbAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                for (i in cartItems.indices) {
                    if (!selectedItems.contains(i)) {
                        selectedItems.add(i)
                        cartAdapter.notifyItemChanged(i)
                    }
                }
            } else {
                selectedItems.clear()
            }
            updateButtonAndTotal()
            cartAdapter.notifyDataSetChanged()
            }
    }
}
