package com.berkahjayashop.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.berkahjayashop.main.mainfragment.AccountFragment
import com.berkahjayashop.main.mainfragment.HomeFragment
import com.berkahjayashop.main.mainfragment.TransactionsFragment
import com.berkahjayashop.main.mainfragment.WishlistFragment

class ViewPager2Adapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> WishlistFragment()
            2 -> TransactionsFragment()
            3 -> AccountFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
    override fun getItemCount(): Int {
        return 4
    }
}