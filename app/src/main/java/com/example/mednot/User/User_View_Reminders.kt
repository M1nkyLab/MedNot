package com.example.mednot.User

import android.os.Bundle
import android.util.Log
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

    private lateinit var remindersRecyclerView: RecyclerView
    private lateinit var tvReminderTitle: TextView
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val medicineList = mutableListOf<Medicine>()
    private lateinit var reminderAdapter: ReminderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_view_reminders)

        Log.d("ViewReminders", "onCreate called")

        tvReminderTitle = findViewById(R.id.tvReminderTitle)
        remindersRecyclerView = findViewById(R.id.remindersRecyclerView)

        // Setup RecyclerView
        remindersRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with callback
        reminderAdapter = ReminderAdapter(medicineList) {
            loadReminders()
        }
        remindersRecyclerView.adapter = reminderAdapter

        Log.d("ViewReminders", "RecyclerView setup complete")

        loadReminders()
    }

    private fun loadReminders() {
        val uid = auth.currentUser?.uid

        Log.d("ViewReminders", "loadReminders called")
        Log.d("ViewReminders", "Current user UID: $uid")

        if (uid == null) {
            tvReminderTitle.text = "Please log in to view reminders"
            Log.e("ViewReminders", "User not logged in")
            return
        }

        tvReminderTitle.text = "Loading reminders..."

        firestore.collection("medicines")
            .whereEqualTo("userId", uid)
            .orderBy("startTime", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                Log.d("ViewReminders", "Firestore query successful")
                Log.d("ViewReminders", "Documents found: ${result.size()}")

                medicineList.clear()

                for (document in result) {
                    Log.d("ViewReminders", "Processing document: ${document.id}")
                    Log.d("ViewReminders", "Document data: ${document.data}")

                    try {
                        val medicine = Medicine(
                            id = document.id,
                            medicineName = document.getString("medicineName") ?: "",
                            dosage = document.getString("dosage") ?: "",
                            dosageUnit = document.getString("dosageUnit") ?: "",
                            startTime = document.getString("startTime") ?: "",
                            status = document.getString("status") ?: "upcoming"
                        )

                        Log.d("ViewReminders", "Medicine created: ${medicine.medicineName}")
                        medicineList.add(medicine)
                    } catch (e: Exception) {
                        Log.e("ViewReminders", "Error parsing medicine: ${e.message}", e)
                        Toast.makeText(this, "Error parsing medicine: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                Log.d("ViewReminders", "Total medicines in list: ${medicineList.size}")

                reminderAdapter.notifyDataSetChanged()

                if (medicineList.isEmpty()) {
                    tvReminderTitle.text = "No reminders found"
                    Log.d("ViewReminders", "No medicines to display")
                } else {
                    tvReminderTitle.text = "Your Medicine Reminders (${medicineList.size})"
                    Log.d("ViewReminders", "Displaying ${medicineList.size} medicines")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ViewReminders", "Firestore query failed: ${e.message}", e)
                tvReminderTitle.text = "Failed to load reminders"
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}