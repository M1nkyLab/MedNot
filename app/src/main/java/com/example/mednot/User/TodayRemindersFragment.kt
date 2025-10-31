package com.example.mednot.User

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TodayRemindersFragment : Fragment() {

    // Firebase instances
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Views for upcoming reminders
    private lateinit var todayRecyclerView: RecyclerView
    private lateinit var todayAdapter: ReminderAdapter
    private val todayList = mutableListOf<Medicine>()
    private lateinit var tvNoReminders: TextView

    // Views for medication history
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: ReminderAdapter
    private val historyList = mutableListOf<Medicine>()
    private lateinit var tvNoHistory: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_today_reminders, container, false)

        // Initialize views from the layout
        setupViews(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        // Load or refresh data every time the fragment is shown
        loadAllReminders()
    }

    private fun setupViews(view: View) {
        // --- Setup for Today's Reminders ---
        tvNoReminders = view.findViewById(R.id.tvNoReminders)
        todayRecyclerView = view.findViewById(R.id.todayRemindersRecyclerView)
        todayRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        todayAdapter = ReminderAdapter(todayList) // Use the improved RecyclerView adapter
        todayRecyclerView.adapter = todayAdapter

        // --- Setup for Medication History ---
        tvNoHistory = view.findViewById(R.id.tvNoHistory)
        historyRecyclerView = view.findViewById(R.id.medicationLogRecyclerView)
        historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        historyAdapter = ReminderAdapter(historyList)
        historyRecyclerView.adapter = historyAdapter
    }

    private fun loadAllReminders() {
        val uid = auth.currentUser?.uid ?: return

        // Correct Firestore query: fetch from the top-level "medicines" collection
        firestore.collection("medicines")
            .whereEqualTo("userId", uid)
            .orderBy("startTime", Query.Direction.ASCENDING) // Sort by time
            .get()
            .addOnSuccessListener { documents ->
                // Clear previous data
                todayList.clear()
                historyList.clear()

                for (document in documents) {
                    // Convert each document to our Medicine data class
                    val medicine = document.toObject(Medicine::class.java).copy(id = document.id)

                    // Sort into the correct list based on status
                    if (medicine.status.equals("complete", ignoreCase = true)) {
                        historyList.add(medicine)
                    } else {
                        todayList.add(medicine)
                    }
                }

                // Update UI for today's reminders
                todayAdapter.notifyDataSetChanged()
                tvNoReminders.visibility = if (todayList.isEmpty()) View.VISIBLE else View.GONE
                todayRecyclerView.visibility = if (todayList.isEmpty()) View.GONE else View.VISIBLE

                // Update UI for history
                historyAdapter.notifyDataSetChanged()
                tvNoHistory.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
                historyRecyclerView.visibility = if (historyList.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}
