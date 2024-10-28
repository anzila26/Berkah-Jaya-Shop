package com.berkahjayashop.main.mainfragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.berkahjayashop.account.EditProfileActivity
import com.berkahjayashop.R
import com.berkahjayashop.account.TransaksiAndaActivity
import com.berkahjayashop.account.WishlistAndaActivity
import com.berkahjayashop.auth.LoginActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class AccountFragment : Fragment() {

    private lateinit var fullNameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var profileImageView: CircleImageView
    private lateinit var editProfileButton: Button
    private lateinit var transactionButton: Button
    private lateinit var wishlistButton: Button
    private lateinit var logoutButton: Button

    private var currentUser: FirebaseUser? = null
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        fullNameTextView = view.findViewById(R.id.tvFullName)
        emailTextView = view.findViewById(R.id.tvEmail)
        editProfileButton = view.findViewById(R.id.buttonEditProfile)
        transactionButton = view.findViewById(R.id.buttonTransaction)
        wishlistButton = view.findViewById(R.id.buttonWishlist)
        logoutButton = view.findViewById(R.id.buttonLogout)
        profileImageView = view.findViewById(R.id.profile_image)

        currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            Log.d("AccountFragment", "User is not logged in")
            navigateToLogin()
        } else {
            databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser!!.uid)
            fetchAndDisplayUserData()
        }
        setupButtonClickListeners()

        return view
    }

    private fun fetchAndDisplayUserData() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val fullName = snapshot.child("fullName").value?.toString() ?: "N/A"
                    val email = snapshot.child("email").value?.toString() ?: "N/A"
                    val imageUrl = snapshot.child("profileImageUrl").value?.toString() ?: ""
                    Glide.with(this@AccountFragment)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_account)
                        .into(profileImageView)
                    fullNameTextView.text = fullName
                    emailTextView.text = email
                } else {
                    Log.d("AccountFragment", "User data does not exist")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AccountFragment", "Database error: ${error.message}")
            }
        })
    }

    private fun setupButtonClickListeners() {
        editProfileButton.setOnClickListener {
            if (currentUser != null) {
                val intent = Intent(requireContext(), EditProfileActivity::class.java)
                startActivity(intent)
            }else{
                Toast.makeText(requireContext(), "Anda Belum Login!!", Toast.LENGTH_SHORT).show()
            }
        }

        transactionButton.setOnClickListener {
            startActivity(Intent(requireContext(), TransaksiAndaActivity::class.java))
        }

        wishlistButton.setOnClickListener {
            startActivity(Intent(requireContext(), WishlistAndaActivity::class.java))
        }

        logoutButton.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Logout")
            builder.setMessage("Are you sure you want to log out?")
            builder.setPositiveButton("Yes") { dialog, _ ->
                FirebaseAuth.getInstance().signOut()
                navigateToLogin()
                dialog.dismiss()
            }

            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }
}
