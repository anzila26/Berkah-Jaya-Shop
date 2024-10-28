package com.berkahjayashop.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.berkahjayashop.R
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var fullNameEditText: TextInputEditText
    private lateinit var addressEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var updateButton: Button
    private lateinit var profileImage: CircleImageView
    private lateinit var changeImageButton: TextView

    private lateinit var database: DatabaseReference

    private val REQUEST_IMAGE_GALLERY = 101
    private val REQUEST_IMAGE_CAMERA = 102
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Edit Profile"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fullNameEditText = findViewById(R.id.fullNameEditText)
        addressEditText = findViewById(R.id.addressEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        updateButton = findViewById(R.id.updateButton)
        profileImage = findViewById(R.id.profileImage)
        changeImageButton = findViewById(R.id.tvChangeImage)

        val userId = FirebaseAuth.getInstance().currentUser

        if (userId == null) {
            Log.d("AccountFragment", "User is not logged in")
        } else {
            database = FirebaseDatabase.getInstance().getReference("users").child(userId.uid)
        }
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userMap = snapshot.value as? Map<String, Any>
                userMap?.let {
                    fullNameEditText.setText(it["fullName"].toString())
                    addressEditText.setText(it["address"].toString())
                    phoneEditText.setText(it["phone"].toString())
                    val imageUrl = it["profileImageUrl"].toString()
                    if (imageUrl.isNotEmpty()) {
                        Glide.with(this@EditProfileActivity)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_account)
                            .error(R.drawable.ic_account)
                            .into(profileImage)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

        updateButton.setOnClickListener {
            // Update profile data in Firebase Realtime Database
            val fullName = fullNameEditText.text.toString()
            val address = addressEditText.text.toString()
            val phone = phoneEditText.text.toString()

            val userUpdates = HashMap<String, Any>()
            userUpdates["fullName"] = fullName
            userUpdates["address"] = address
            userUpdates["phone"] = phone

            // Add image URL to the database if an image was selected
            selectedImageUri?.let { uri ->
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("profile_user_images")
                    .child("${System.currentTimeMillis()}.jpg")

                storageRef.putFile(uri)
                    .addOnSuccessListener { taskSnapshot ->
                        taskSnapshot.storage.downloadUrl.addOnSuccessListener { imageUrl ->
                            userUpdates["profileImageUrl"] = imageUrl.toString()
                            database.updateChildren(userUpdates)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
            } ?: run {
                database.updateChildren(userUpdates)
                    .addOnSuccessListener {
                       Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        changeImageButton.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose your profile picture")

        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "Take Photo" -> {
                    openCamera()
                }
                options[item] == "Choose from Gallery" -> {
                    openGallery()
                }
                options[item] == "Cancel" -> {
                    dialog.dismiss()
                }
            }
        }
        builder.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAMERA)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_GALLERY -> {
                    data?.data?.let { uri ->
                        selectedImageUri = uri
                        profileImage.setImageURI(uri)
                    }
                }
                REQUEST_IMAGE_CAMERA -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    val uri = getImageUri(this, imageBitmap)
                    selectedImageUri = uri
                    profileImage.setImageBitmap(imageBitmap)
                }
            }
        }
    }

    private fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

}