package com.example.mednot.User

import android.os.Bundle
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
                    val reminderList = mutableListOf<MutableMap<String, Any>>()

                    for (document in result) {
                        val data = document.data.toMutableMap()
                        data["id"] = document.id
                        reminderList.add(data)
                    }

                    val adapter = ReminderAdapter(this, reminderList, uid)
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
