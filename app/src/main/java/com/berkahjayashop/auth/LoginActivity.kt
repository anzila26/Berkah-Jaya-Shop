package com.berkahjayashop.auth

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.berkahjayashop.R
import com.berkahjayashop.main.MainActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var registerLinkTextView: TextView
    private lateinit var emailTextInputLayout: TextInputLayout
    private lateinit var passwordTextInputLayout: TextInputLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog


    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = Firebase.auth

        emailEditText = findViewById(R.id.emailtextInputEditText)
        passwordEditText = findViewById(R.id.passwordtextInputEditText)
        loginButton = findViewById(R.id.buttonLogin)
        registerLinkTextView = findViewById(R.id.tvDaftar)
        emailTextInputLayout = findViewById(R.id.emailtextInputLayout)
        passwordTextInputLayout = findViewById(R.id.passwordtextInputLayout)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Masuk...")
        progressDialog.setCancelable(false)

        registerLinkTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        loginButton.setOnClickListener {
            if (validateFields()) {
                progressDialog.show()

                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        progressDialog.dismiss()

                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()

                        } else {
                            Toast.makeText(this, "Login gagal", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

    }
    private fun validateFields(): Boolean {
        var isValid = true

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

}