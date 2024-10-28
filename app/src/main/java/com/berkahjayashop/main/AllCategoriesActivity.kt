package com.berkahjayashop.main

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.berkahjayashop.R
import com.berkahjayashop.main.item.CategoriesItem
import com.berkahjayashop.main.mainfragment.adapter.CategoriesAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AllCategoriesActivity : AppCompatActivity() {

    private lateinit var categoriesAdapter: CategoriesAdapter
    private lateinit var categories: MutableList<CategoriesItem>
    private lateinit var database: FirebaseDatabase
    private lateinit var btBack: ImageView
    private lateinit var progressDialog: ProgressDialog
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_all_categories)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = FirebaseDatabase.getInstance()
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Memuat Kategori....")
        progressDialog.setCancelable(false)

        categories = mutableListOf()
        categoriesAdapter = CategoriesAdapter(categories) {
            val intent = Intent(this, ProductsByCategoryActivity::class.java)
            intent.putExtra("category_id", it.id)
            intent.putExtra("name_categori", it.name)
            startActivity(intent)
        }

        val rvAllCategories: RecyclerView = findViewById(R.id.rvAllCategories)
        rvAllCategories.layoutManager = GridLayoutManager(this, 4)
        rvAllCategories.setHasFixedSize(true)
        rvAllCategories.adapter = categoriesAdapter

        searchView = findViewById(R.id.searchProduct)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    filterCategories(newText.toLowerCase().trim())
                }
                return true
            }
        })
        btBack = findViewById(R.id.ivBack)
        btBack.setOnClickListener {
            finish()
        }

        getCategories()
    }

    private fun getCategories() {
        val categoryRef = database.getReference("categories")
        progressDialog.show()
        categoryRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                for (categorySnapshot in snapshot.children) {
                    val id = categorySnapshot.key ?: ""
                    val name = categorySnapshot.child("name").value.toString()
                    val image = categorySnapshot.child("image").value.toString()
                    val category = CategoriesItem(id, name, image)
                    categories.add(category)

                }

                categoriesAdapter.notifyDataSetChanged()
                progressDialog.dismiss()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AllCategoriesActivity, error.message, Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
            }
        })
    }

    private fun filterCategories(query: String) {
        val filteredList = categories.filter {
            it.name?.toLowerCase()!!.contains(query)
        }
        categoriesAdapter.filterList(filteredList)
    }

}
