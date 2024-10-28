package com.berkahjayashop.auth

import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.berkahjayashop.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase


class RegisterActivity : AppCompatActivity() {

    private lateinit var fullNameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var registerButton: Button
    private lateinit var loginLinkTextView: TextView
    private lateinit var fullNameTextInputLayout: TextInputLayout
    private lateinit var emailTextInputLayout: TextInputLayout
    private lateinit var phoneTextInputLayout: TextInputLayout
    private lateinit var passwordTextInputLayout: TextInputLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth

        fullNameEditText = findViewById(R.id.fullnametextInputEditText)
        emailEditText = findViewById(R.id.emailtextInputEditText)
        phoneEditText = findViewById(R.id.tlptextInputEditText)
        passwordEditText = findViewById(R.id.passwordtextInputEditText)
        registerButton = findViewById(R.id.buttonRegister)
        loginLinkTextView = findViewById(R.id.tvMasuk)
        fullNameTextInputLayout = findViewById(R.id.fullnametextInputLayout)
        emailTextInputLayout = findViewById(R.id.emailtextInputLayout)
        phoneTextInputLayout = findViewById(R.id.tlptextInputLayout)
        passwordTextInputLayout = findViewById(R.id.passwordtextInputLayout)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Mendaftar...")
        progressDialog.setCancelable(false)

        registerButton.setOnClickListener {
            if (validateFields()) {
                val fullName = fullNameEditText.text.toString()
                val email = emailEditText.text.toString()
                val phone = phoneEditText.text.toString()
                val password = passwordEditText.text.toString()

                registerUser(email, password, fullName, phone)
            }
        }

        loginLinkTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        if (fullNameEditText.text.toString().isEmpty()) {
            fullNameTextInputLayout.error = "Nama lengkap harus diisi"
            isValid = false
        } else {
            fullNameTextInputLayout.error = null
        }

        if (emailEditText.text.toString().isEmpty()) {
            emailTextInputLayout.error = "Email harus diisi"
            isValid = false
        } else {
            emailTextInputLayout.error = null
        }
        val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        if (!emailEditText.text.toString().matches(emailPattern.toRegex())) {
            emailTextInputLayout.error = "Email tidak valid"
            isValid = false
        } else {
            emailTextInputLayout.error = null
        }

        val phone = phoneEditText.text.toString()
        if (phone.isEmpty()) {
            phoneTextInputLayout.error = "Nomor telepon harus diisi"
            isValid = false
        } else if (!phone.startsWith("+62")) {
            phoneTextInputLayout.error = "Nomor telepon harus diawali dengan +62"
            isValid = false
        } else {
            phoneTextInputLayout.error = null
        }

        val password = passwordEditText.text.toString()
        if (password.isEmpty()) {
            passwordTextInputLayout.error = "Password harus diisi"
            isValid = false
        } else if (password.length < 8) {
            passwordTextInputLayout.error = "Password harus memiliki minimal 8 karakter"
            isValid = false
        } else {
            passwordTextInputLayout.error = null
        }

        return isValid
    }

    private fun registerUser(email: String, password: String, fullName: String, phone: String) {
        progressDialog.show()
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    user?.let { currentUser ->
                        val uid = currentUser.uid
                        val database = FirebaseDatabase.getInstance()
                        val usersRef = database.getReference("users")
                        val userRef = usersRef.child(uid)

                        val userData = mapOf(
                            "fullName" to fullName,
                            "email" to email,
                            "phone" to phone
                        )

                        userRef.setValue(userData)
                            .addOnSuccessListener {
                                Log.d(TAG, "User data saved to the database")
                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                progressDialog.dismiss()
                                Log.w(TAG, "Error saving user data to the database", e)
                                Toast.makeText(this, "Gagal menyimpan data pengguna", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    when (task.exception) {
                        is FirebaseAuthUserCollisionException -> {
                            Toast.makeText(this, "Email sudah terdaftar", Toast.LENGTH_SHORT).show()
                            progressDialog.dismiss()
                        }

                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(this, "Email tidak valid", Toast.LENGTH_SHORT).show()
                            progressDialog.dismiss()
                        }

                        else -> {
                            Toast.makeText(this, "Gagal mendaftar", Toast.LENGTH_SHORT).show()
                            progressDialog.dismiss()
                        }
                    }
                }
            }
    }

    companion object {
        private const val TAG = "RegisterActivity"
    }
}
