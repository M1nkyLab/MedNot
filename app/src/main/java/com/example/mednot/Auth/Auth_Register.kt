package com.example.mednot.Auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Auth_Register : AppCompatActivity() {

    private lateinit var inputName: EditText
    private lateinit var inputAge: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputRePassword: EditText
    private lateinit var inputEmerCont: EditText
    private lateinit var btnReg: Button
    private lateinit var loginRedirect: TextView

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_register)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        inputName = findViewById(R.id.inputName)
        inputAge = findViewById(R.id.inputAge)
        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        inputRePassword = findViewById(R.id.inputRePassword)
        inputEmerCont = findViewById(R.id.inputEmerCont)
        btnReg = findViewById(R.id.btnReg)
        loginRedirect = findViewById(R.id.loginRedirect)

        btnReg.setOnClickListener {
            val name = inputName.text.toString().trim()
            val age = inputAge.text.toString().trim()
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString()
            val confirmPassword = inputRePassword.text.toString()
            val emergencyContact = inputEmerCont.text.toString().trim()

            if (name.isEmpty() || age.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || emergencyContact.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener
                            val userData = hashMapOf(
                                "name" to name,
                                "age" to age,
                                "email" to email,
                                "emergencyContact" to emergencyContact
                            )

                            db.collection("users").document(uid).set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, Auth_Login::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error saving data: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, task.exception?.message, Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        // Redirect to Login
        loginRedirect.setOnClickListener {
            val intent = Intent(this, Auth_Login::class.java)
            startActivity(intent)
        }
    }
}