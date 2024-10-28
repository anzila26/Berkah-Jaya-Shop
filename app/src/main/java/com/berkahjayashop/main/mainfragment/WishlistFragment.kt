package com.berkahjayashop.main.mainfragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.main.DetailsActivity
import com.berkahjayashop.R
import com.berkahjayashop.main.mainfragment.adapter.WishlistAdapter
import com.berkahjayashop.main.item.ProductsItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class WishlistFragment : Fragment(), WishlistAdapter.OnItemClickListener, WishlistAdapter.OnFavoriteClickListener {

    private lateinit var rvWishlist: RecyclerView
    private lateinit var wishlistAdapter: WishlistAdapter
    private lateinit var productList: MutableList<ProductsItem>
    private lateinit var filteredList: MutableList<ProductsItem>
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var searchWishlist: SearchView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wishlist, container, false)
        rvWishlist = view.findViewById(R.id.rvWishlist)
        rvWishlist.layoutManager = GridLayoutManager(requireContext(), 2)

        productList = mutableListOf()
        wishlistAdapter = WishlistAdapter(requireContext(), productList, this, this)
        rvWishlist.adapter = wishlistAdapter

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        filteredList = mutableListOf()
        searchWishlist = view.findViewById(R.id.sbWishlist)
        searchWishlist.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    filter(query)
                }

                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    fetchWishlist()
                }
                return true
            }
        })
        if (searchWishlist.query.isEmpty() || filteredList.isEmpty()) {
            fetchWishlist()
        }

        return view
    }

    private fun filter(query: String) {
        filteredList.clear()
        val searchText = query.toLowerCase().trim()
        for (product in productList) {
            if (product.name!!.toLowerCase().contains(searchText)) {
                filteredList.add(product)
            } else {
                Toast.makeText(requireContext(), "No Data Found", Toast.LENGTH_SHORT).show()
            }

        }
        if (filteredList.isEmpty()) {
            wishlistAdapter.updateList(productList)
        } else {
            wishlistAdapter.updateList(filteredList)
        }
    }

    private fun fetchWishlist() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }
        val wishlistRef = database.child("wishlist").child(userId)
        wishlistRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                for (wishlistSnapshot in snapshot.children) {
                    val productId = wishlistSnapshot.child("productId").getValue(String::class.java)
                    if (productId != null) {
                        fetchProductDetails(productId)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("WishlistFragment", "Failed to fetch wishlist", error.toException())
            }
        })
    }


    private fun fetchProductDetails(productId: String) {
        val productRef = database.child("products").child(productId)
        productRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val id = snapshot.key ?: ""
                val category = snapshot.child("category").getValue(String::class.java) ?: ""
                val createdAt = snapshot.child("createdAt").getValue(String::class.java) ?: ""
                val description = snapshot.child("description").getValue(String::class.java) ?: ""
                val image = snapshot.child("image").getValue(String::class.java) ?: ""
                val name = snapshot.child("name").getValue(String::class.java) ?: ""
                val purchasePrice = snapshot.child("purchase_price").getValue(Long::class.java)
                val sellingPrice = snapshot.child("selling_price").getValue(Long::class.java)
                val stock = snapshot.child("stock").getValue(Long::class.java)

                val product = ProductsItem(id, category, createdAt, description, image, name, purchasePrice, sellingPrice, stock)
                productList.add(product)
                wishlistAdapter.notifyDataSetChanged()

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("WishlistFragment", "Failed to fetch product details", error.toException())
            }
        })
    }
    override fun onItemClick(product: ProductsItem) {
        val intent = Intent(requireContext(), DetailsActivity::class.java)
        intent.putExtra("product_id", product.id)
        startActivity(intent)

    }

    override fun onFavoriteClick(product: ProductsItem) {
        val userId = auth.currentUser?.uid

        if (userId != null && product.id != null) {
            val wishlistRef = database.child("wishlist").child(userId).child(product.id!!)
            wishlistRef.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "${product.name} removed from wishlist", Toast.LENGTH_SHORT).show()
                    productList.remove(product)
                    wishlistAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(context, "Failed to remove from wishlist", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}