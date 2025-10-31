package com.example.mednot.User

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

class Home_Fragment : Fragment() {

    private lateinit var welcomeMessage: TextView
    private lateinit var lowStockCountTextView: TextView
    private lateinit var todayMedCountTextView: TextView
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_home_fragment, container, false)

        // Initialize views
        welcomeMessage = view.findViewById(R.id.welcomeMessage)
        lowStockCountTextView = view.findViewById(R.id.lowStockCount)
        todayMedCountTextView = view.findViewById(R.id.todayMedCount)

        // Load the child fragment that displays the reminder lists
        if (savedInstanceState == null) { // Prevents re-adding fragment on config change
            childFragmentManager.beginTransaction()
                .replace(R.id.todayRemindersContainer, TodayRemindersFragment())
                .commit()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        // Fetch data every time the fragment becomes visible
        loadUserDataAndStats()
    }

    private fun loadUserDataAndStats() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            welcomeMessage.text = "Welcome, Guest"
            lowStockCountTextView.text = "Login to see your medicine stock."
            todayMedCountTextView.text = "Login to see today's schedule."
            return
        }

        val uid = currentUser.uid

        // Set a default welcome message while data loads
        welcomeMessage.text = "Welcome!"

        // Single Firestore call to fetch all medicine data for the user
        firestore.collection("medicines")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    // Handle case where user has no medicines
                    todayMedCountTextView.text = "No medicines scheduled! 🎉"
                    lowStockCountTextView.text = "No medicines to track."
                    return@addOnSuccessListener
                }

                val lowStockList = mutableListOf<String>()
                var upcomingCount = 0

                for (doc in result) {
                    // --- 1. Calculate Today's Upcoming Medicine Count ---
                    val status = doc.getString("status") ?: "upcoming"
                    if (!status.equals("complete", ignoreCase = true)) {
                        upcomingCount++
                    }

                    // --- 2. Calculate Low Stock ---
                    val name = doc.getString("medicineName") ?: "Unnamed Medicine"
                    val stock = doc.getString("stock")?.toDoubleOrNull() ?: 0.0
                    if (stock <= 0) continue // Skip meds with no stock tracking

                    val scheduleMethod = doc.getString("scheduleMethod")
                    val timesPerDay = doc.getString("timesPerDay")
                    val intervalHours = doc.getString("intervalHours")
                    val dosesPerDay = calculateDosesPerDay(scheduleMethod, timesPerDay, intervalHours)

                    if (dosesPerDay > 0) {
                        val daysOfSupply = stock / dosesPerDay
                        // Simplified low stock rule: less than 7 days of supply
                        if (daysOfSupply < 7) {
                            val daysLeft = daysOfSupply.roundToInt().coerceAtLeast(0)
                            lowStockList.add("$name ($daysLeft days left)")
                        }
                    }
                }

                // Update UI with calculated stats
                todayMedCountTextView.text = "You have $upcomingCount medicine(s) scheduled for today."
                updateLowStockUI(lowStockList)
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error loading stats: ${e.message}")
                todayMedCountTextView.text = "Error loading schedule."
                lowStockCountTextView.text = "Error loading stock data."
            }
    }

    private fun updateLowStockUI(lowStockList: List<String>) {
        if (lowStockList.isNotEmpty()) {
            val medNames = lowStockList.joinToString(separator = ", ")
            lowStockCountTextView.text = "${lowStockList.size} medicine(s) are low: $medNames"
        } else {
            lowStockCountTextView.text = "All medicines are well-stocked."
        }
    }

    private fun calculateDosesPerDay(
        scheduleMethod: String?,
        timesPerDay: String?,
        intervalHours: String?
    ): Double {
        return when (scheduleMethod) {
            "Frequency" -> timesPerDay?.toDoubleOrNull() ?: 0.0 // Corrected "frequency" to "Frequency"
            "Interval" -> {
                val interval = intervalHours?.toDoubleOrNull() ?: 0.0
                if (interval > 0) 24.0 / interval else 0.0
            }
            else -> 0.0
        }
    }
}
