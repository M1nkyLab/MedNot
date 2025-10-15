package com.example.mednot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.example.mednot.R
import androidx.appcompat.app.AppCompatActivity

class Home : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var btnAddMedicine: Button
    private lateinit var btnViewReminders: Button
    private lateinit var btnStockTracker: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        // Initialize views
        tvWelcome = findViewById(R.id.tvWelcome)
        btnAddMedicine = findViewById(R.id.btnAddMedicine)
        btnViewReminders = findViewById(R.id.btnViewReminders)
        btnStockTracker = findViewById(R.id.btnStockTracker)

        // intent from auth login  akan pass data user_email ke home page since kita nak show welcome user email mcm introduction gitu
        val userEmail = intent.getStringExtra("USER_EMAIL")
        tvWelcome.text = "Welcome, $userEmail!"

        // bila btn add medicine kena click dia akan go to add medicine page
        btnAddMedicine.setOnClickListener {
            val intent = Intent(this, Add_Medicine::class.java)
            startActivity(intent)
        }

        // bila btn view reminder kena click dia akan go to add reminder page
        btnViewReminders.setOnClickListener {
            val intent = Intent(this, View_Reminders::class.java)
            startActivity(intent)
        }

        // bila btn stock kena click dia akan go to add stock page
        btnStockTracker.setOnClickListener {
            val intent = Intent(this, Check_Med_Stock::class.java)
            startActivity(intent)
        }
    }
}
