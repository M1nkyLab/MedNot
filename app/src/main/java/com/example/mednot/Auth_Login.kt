package com.example.mednot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Auth_Login : AppCompatActivity() {

    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.auth_login)

        inputEmail = findViewById(R.id.inputEmail)
        inputPassword = findViewById(R.id.inputPassword)
        btnLogin = findViewById(R.id.btnLogin)

        // kalau user tekan btn login kemana dia pegi
        btnLogin.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                // untuk sekarang ini adalah mock je
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                // dia akan go to home page
                // tak connect firebase atau mana ii database lagi untuk ni
                val intent = Intent(this, Home::class.java)
                intent.putExtra("USER_EMAIL", email)
                startActivity(intent)
                finish()
            }
        }
    }
}
