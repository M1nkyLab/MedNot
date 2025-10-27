package com.example.mednot.User

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class User_View_Reminders : AppCompatActivity() {

    private lateinit var reminderListView: ListView
    private lateinit var tvReminderTitle: TextView
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_view_reminders)

        reminderListView = findViewById(R.id.reminderListView)
        tvReminderTitle = findViewById(R.id.tvReminderTitle)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            firestore.collection("users")
                .document(uid)
                .collection("reminders")
                .get()
                .addOnSuccessListener { result ->
                    val reminderList = mutableListOf<String>()

                    for (document in result) {
                        val name = document.getString("medicineName") ?: "Unknown Medicine"
                        val time = document.getString("time") ?: "No time set"
                        val dosage = document.getString("dosage") ?: "N/A"
                        val status = document.getString("status") ?: "Upcoming"

                        reminderList.add("$name\n💊 $dosage | ⏰ $time | 📋 $status")
                    }

                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_list_item_1,
                        reminderList
                    )
                    reminderListView.adapter = adapter
                }
                .addOnFailureListener {
                    tvReminderTitle.text = "Failed to load reminders ❌"
                }
        } else {
            tvReminderTitle.text = "Please log in to view reminders"
        }
    }
}