package com.berkahjayashop.main.mainfragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.transaction.DetailTransactionActivity
import com.berkahjayashop.R
import com.berkahjayashop.transaction.Transaction
import com.berkahjayashop.transaction.TransactionAdapter
import com.berkahjayashop.transaction.ProductOrder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TransactionsFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private var userId: String? = ""
    private val transactions = mutableListOf<Transaction>()
    private val filteredTransactions = mutableListOf<Transaction>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
        }else{
            database = FirebaseDatabase.getInstance().getReference("orders").child(userId!!)
            recyclerView = view.findViewById(R.id.rvTransactions)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            adapter = TransactionAdapter(requireContext(), filteredTransactions) { transaction ->
                val intent = Intent(context, DetailTransactionActivity::class.java)
                intent.putExtra("orderId", transaction.orderID)
                startActivity(intent)
            }
            recyclerView.adapter = adapter

            val searchView = view.findViewById<SearchView>(R.id.sbTransactions)
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filterTransactions(newText)
                    return true
                }
            })

            fetchOrders()
        }

    }

    private fun fetchOrders() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactions.clear()
                for (orderSnapshot in snapshot.children) {
                    val orderId = orderSnapshot.child("orderID").getValue(String::class.java)
                    val orderDate = orderSnapshot.child("orderDate").getValue(String::class.java)
                    val address = orderSnapshot.child("address").getValue(String::class.java)
                    val estimationDate = orderSnapshot.child("estimationDate").getValue(String::class.java)
                    val shipping = orderSnapshot.child("shipping").getValue(String::class.java)
                    val statusPayment = orderSnapshot.child("statusPayment").getValue(String::class.java)
                    val statusShipping = orderSnapshot.child("statusShipping").getValue(String::class.java)
                    val totalPrice = orderSnapshot.child("totalPrice").getValue(Double::class.java)

                    val productsSnapshot = orderSnapshot.child("products")
                    val products = mutableListOf<ProductOrder>()
                    for (productSnapshot in productsSnapshot.children) {
                        val productId = productSnapshot.child("productId").getValue(String::class.java)
                        val image = productSnapshot.child("image").getValue(String::class.java)
                        val name = productSnapshot.child("name").getValue(String::class.java)
                        val quantity = productSnapshot.child("quantity").getValue(String::class.java)
                        val price = productSnapshot.child("price").getValue(String::class.java)

                        val productOrder = ProductOrder(
                            productId = productId ?: "",
                            image = image ?: "",
                            name = name ?: "",
                            quantity = quantity ?: "",
                            price = price  ?: ""
                        )
                        products.add(productOrder)
                    }

                    val transaction = Transaction(
                        address = address ?: "",
                        estimationDate = estimationDate ?: "",
                        orderDate = orderDate ?: "",
                        orderID = orderId ?: "",
                        shipping = shipping ?: "",
                        statusPayment = statusPayment ?: "",
                        statusShipping = statusShipping ?: "",
                        totalPrice = totalPrice ?: 0.0,
                        products = products
                    )
                    transactions.add(transaction)
                }
                filterTransactions("")
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun filterTransactions(query: String?) {
        val lowerCaseQuery = query?.toLowerCase() ?: ""
        filteredTransactions.clear()
        for (transaction in transactions) {
            if (transaction.orderID.toLowerCase().contains(lowerCaseQuery) ||
                transaction.address.toLowerCase().contains(lowerCaseQuery) ||
                transaction.orderDate.toLowerCase().contains(lowerCaseQuery) ||
                transaction.statusPayment.toLowerCase().contains(lowerCaseQuery) ||
                transaction.statusShipping.toLowerCase().contains(lowerCaseQuery)) {
                filteredTransactions.add(transaction)
            }
        }
        adapter.notifyDataSetChanged()
    }

}
