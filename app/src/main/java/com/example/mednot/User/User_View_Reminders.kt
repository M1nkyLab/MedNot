package com.example.mednot.User

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class User_View_Reminders : AppCompatActivity() {

    // Use RecyclerView
    private lateinit var remindersRecyclerView: RecyclerView
    private lateinit var tvReminderTitle: TextView
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // The list now correctly holds Medicine objects
    private val medicineList = mutableListOf<Medicine>()
    private lateinit var reminderAdapter: ReminderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make sure your layout file uses a RecyclerView with the id "remindersRecyclerView"
        setContentView(R.layout.user_view_reminders)

        tvReminderTitle = findViewById(R.id.tvReminderTitle)

        // Setup RecyclerView
        remindersRecyclerView = findViewById(R.id.remindersRecyclerView) // Ensure this ID matches your XML
        remindersRecyclerView.layoutManager = LinearLayoutManager(this)

        // --- THIS IS THE MAIN FIX ---
        // Initialize the adapter correctly with only the list it needs.
        reminderAdapter = ReminderAdapter(medicineList)
        remindersRecyclerView.adapter = reminderAdapter

        // Load the reminders
        loadReminders()
    }

    private fun loadReminders() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            tvReminderTitle.text = "Please log in to view reminders"
            return
        }

        // Fetch data from the "medicines" collection, not a sub-collection
        firestore.collection("medicines")
            .whereEqualTo("userId", uid)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                // Clear the list before adding new data
                medicineList.clear()

                for (document in result) {
                    // Convert each Firestore document into a Medicine object
                    // Note: Ensure your Medicine data class fields match Firestore exactly
                    val medicine = document.toObject(Medicine::class.java).copy(id = document.id)
                    medicineList.add(medicine)
                }

                // Notify the adapter that the underlying data has changed
                reminderAdapter.notifyDataSetChanged()

                tvReminderTitle.text = "Your Medicine Reminders (${medicineList.size})"
            }
            .addOnFailureListener { e ->
                tvReminderTitle.text = "Failed to load reminders"
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
