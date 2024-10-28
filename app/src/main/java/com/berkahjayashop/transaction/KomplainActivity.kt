package com.berkahjayashop.transaction

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.berkahjayashop.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class KomplainActivity : AppCompatActivity() {

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private var orderId: String? = ""
    private lateinit var database: DatabaseReference
    private lateinit var emailEditText: TextInputEditText
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var dateEditText: TextInputEditText
    private lateinit var orderIdEditText: TextInputEditText
    private lateinit var complaintEditText: TextInputEditText
    private lateinit var sendButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_komplain)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Form Komplain"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        database = FirebaseDatabase.getInstance().reference

        orderId = intent.getStringExtra("orderId")

        emailEditText = findViewById(R.id.textInputEditTextEmail)
        phoneEditText = findViewById(R.id.textInputEditTextPhone)
        dateEditText = findViewById(R.id.textInputEditTextDate)
        complaintEditText = findViewById(R.id.textInputEditTextComplaint)
        sendButton = findViewById(R.id.buttonSend)
        orderIdEditText = findViewById(R.id.textInputEditTextOrderId)

        orderId?.let {
            orderIdEditText.setText(it)
            orderIdEditText.isEnabled = false
            orderIdEditText.isFocusable = false
        }

        val currentDate = getCurrentDate()
        dateEditText.setText(currentDate)
        dateEditText.isEnabled = false
        dateEditText.isFocusable = false

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val emailUser = FirebaseAuth.getInstance().currentUser?.email
        emailEditText.setText(emailUser)
        emailEditText.isEnabled = false
        emailEditText.isFocusable = false

        sendButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val date = dateEditText.text.toString().trim()
            val complaint = complaintEditText.text.toString().trim()

            if (email.isNotEmpty() && phone.isNotEmpty() && date.isNotEmpty() && complaint.isNotEmpty() && orderId != null && userId != null) {
                saveComplaintData(orderId!!, email, "+62 $phone", date, complaint, userId)
            } else {
                Toast.makeText(this, "Isi semua kolom dengan benar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveComplaintData(orderId: String, email: String, phone: String, date: String, complaint: String, userId: String) {
        val complaintData = Complaint(orderId, email, phone, date, complaint, userId)
        database.child("complaints").push().setValue(complaintData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Komplain berhasil dikirim", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Gagal mengirim komplain", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    data class Complaint(val orderId: String, val email: String, val phone: String, val date: String, val complaint: String, val userId: String, val status: String = "Pengajuan")
}
