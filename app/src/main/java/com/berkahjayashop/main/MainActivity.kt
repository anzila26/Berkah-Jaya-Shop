package com.berkahjayashop.main

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.berkahjayashop.R
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView
    private var backPressedTwice = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottom_navigation)
        viewPager.adapter = ViewPager2Adapter(this)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNavigation.selectedItemId = getBottomNavigationMenuItemId(position)
            }
        })
        bottomNavigation.setOnItemSelectedListener { item ->
            viewPager.currentItem = getFragmentPosition(item.itemId)
            return@setOnItemSelectedListener true
        }
        viewPager.isUserInputEnabled = false

    }
    private fun getFragmentPosition(itemId: Int): Int {
        return when (itemId) {
            R.id.nav_home -> 0
            R.id.nav_wishlist -> 1
            R.id.nav_transactions -> 2
            R.id.nav_account -> 3
            else -> 0
        }
    }
    private fun getBottomNavigationMenuItemId(position: Int): Int {
        return when (position) {
            0 -> R.id.nav_home
            1 -> R.id.nav_wishlist
            2 -> R.id.nav_transactions
            3 -> R.id.nav_account
            else -> R.id.nav_home
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (backPressedTwice) {
            super.onBackPressed()
            finishAffinity()
            return
        }
        if (viewPager.currentItem >= 1) {
            viewPager.setCurrentItem(0, true)
        } else if (viewPager.currentItem == 0) {
            backPressedTwice = true
            Toast.makeText(this@MainActivity, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                backPressedTwice = false
            }, 2000)
        }
    }


}