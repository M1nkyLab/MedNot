package com.example.mednot.User

import android.os.Bundle
import android.util.Log
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
import kotlin.math.roundToInt

class Home_Fragment : Fragment() {

    // Firebase setup
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Text views at the top
    private lateinit var welcomeMessage: TextView
    private lateinit var lowStockCountTextView: TextView
    private lateinit var todayMedCountTextView: TextView

    // RecyclerView and adapter for today's reminders
    private lateinit var todayRecyclerView: RecyclerView
    private lateinit var todayAdapter: ReminderAdapter
    private val todayList = mutableListOf<Medicine>()

    // RecyclerView and adapter for medicine history
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: ReminderAdapter
    private val historyList = mutableListOf<Medicine>()
    private lateinit var tvNoHistory: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Load layout for this screen
        val view = inflater.inflate(R.layout.user_home_fragment, container, false)

        // Connect top section views with layout
        welcomeMessage = view.findViewById(R.id.welcomeMessage)
        lowStockCountTextView = view.findViewById(R.id.lowStockCount)
        todayMedCountTextView = view.findViewById(R.id.todayMedCount)

        // Setup RecyclerViews for reminders and history
        setupReminderViews(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        // When fragment is shown again, reload everything
        loadUserName()
        loadAllData()
    }

    private fun setupReminderViews(view: View) {
        // Set up RecyclerView for today's reminders
        todayRecyclerView = view.findViewById(R.id.todayRemindersRecyclerView)
        todayRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Create adapter for today’s reminders with callback when status changes
        todayAdapter = ReminderAdapter(todayList) {
            loadAllData() // Refresh when user marks medicine as taken
        }
        todayRecyclerView.adapter = todayAdapter

        // Set up RecyclerView for history (past medicines)
        tvNoHistory = view.findViewById(R.id.tvNoHistory)
        historyRecyclerView = view.findViewById(R.id.medicationLogRecyclerView)
        historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Create adapter for history list with same callback
        historyAdapter = ReminderAdapter(historyList) {
            loadAllData() // Refresh after changes
        }
        historyRecyclerView.adapter = historyAdapter
    }

    // Load user's name from Firestore
    private fun loadUserName() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            welcomeMessage.text = "Welcome, Guest"
            return
        }

        val uid = currentUser.uid

        // Read user document using UID
        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userName = document.getString("name") ?: "User"
                    // Get first word only (first name)
                    val firstName = userName.split(" ").firstOrNull() ?: userName
                    welcomeMessage.text = "Welcome, $firstName!"
                } else {
                    welcomeMessage.text = "Welcome!"
                }
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error loading user name: ${exception.message}")
                welcomeMessage.text = "Welcome!"
            }
    }

    // Load all medicine data from Firestore
    private fun loadAllData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // If no user logged in
            lowStockCountTextView.text = "Login to see your medicine stock."
            todayMedCountTextView.text = "Login to see today's schedule."

            // Clear lists and update screen
            todayList.clear()
            historyList.clear()
            updateReminderLists()
            return
        }

        val uid = currentUser.uid
        Log.d("HomeFragment", "Loading data for user: $uid")

        // Get all medicine data for the user
        firestore.collection("medicines")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("HomeFragment", "Documents fetched: ${documents.size()}")

                if (documents.isEmpty) {
                    // If no medicines exist
                    todayMedCountTextView.text = "No medicines scheduled!"
                    lowStockCountTextView.text = "No medicines to track."
                    todayList.clear()
                    historyList.clear()
                    updateReminderLists()
                    return@addOnSuccessListener
                }

                // Clear old data before adding new ones
                todayList.clear()
                historyList.clear()

                val lowStockList = mutableListOf<String>()
                var upcomingCount = 0

                // Go through each medicine document
                for (document in documents) {
                    Log.d("HomeFragment", "Processing document: ${document.id}")

                    // Get medicine status or set default as "upcoming"
                    val status = document.getString("status") ?: "upcoming"
                    val stockStr = document.getString("stock") ?: "0"

                    try {
                        // Create a Medicine object from Firestore data
                        val medicine = Medicine(
                            id = document.id,
                            medicineName = document.getString("medicineName") ?: "",
                            dosage = document.getString("dosage") ?: "",
                            dosageUnit = document.getString("dosageUnit") ?: "",
                            startTime = document.getString("startTime") ?: "",
                            status = status,
                            takenAt = document.getString("takenAt") ?: "",
                            stock = stockStr
                        )

                        // Sort: completed items to history, others to today's list
                        if (status.lowercase() in listOf("complete", "taken")) {
                            historyList.add(medicine)
                        } else {
                            todayList.add(medicine)
                            if (status.lowercase() == "upcoming") {
                                upcomingCount++
                            }
                        }

                        // Calculate if stock is low
                        val name = document.getString("medicineName") ?: "Unnamed Medicine"
                        val stock = stockStr.toDoubleOrNull() ?: 0.0
                        val dosageUnit = document.getString("dosageUnit")?.lowercase() ?: ""
                        val scheduleMethod = document.getString("scheduleMethod")
                        val timesPerDay = document.getString("timesPerDay")
                        val intervalHours = document.getString("intervalHours")
                        val dosage = document.getString("dosage")?.toDoubleOrNull() ?: 1.0

                        // Only check if stock > 0
                        if (stock > 0) {
                            // Figure out how many times per day user takes this medicine
                            val dosesPerDay = when (scheduleMethod) {
                                "Frequency" -> timesPerDay?.toDoubleOrNull() ?: 0.0
                                "Interval" -> {
                                    val interval = intervalHours?.toDoubleOrNull() ?: 0.0
                                    if (interval > 0) 24.0 / interval else 0.0
                                }
                                else -> 0.0
                            }

                            // Calculate how many days of medicine left
                            if (dosesPerDay > 0) {
                                val daysRemaining = stock / dosesPerDay

                                // If less than 7 days left, mark as low stock
                                if (daysRemaining < 7) {
                                    val daysLeft = daysRemaining.toInt().coerceAtLeast(0)

                                    // Display unit name based on dosage unit
                                    val unitDisplay = when {
                                        dosageUnit.contains("tablet") -> "tablets"
                                        dosageUnit.contains("capsule") -> "capsules"
                                        dosageUnit.contains("ml") -> "ml"
                                        dosageUnit.contains("mg") -> "mg"
                                        dosageUnit.contains("mcg") || dosageUnit.contains("µg") -> "mcg"
                                        dosageUnit.contains("g") -> "g"
                                        dosageUnit.contains("iu") -> "IU"
                                        else -> "units"
                                    }

                                    // Add info to low stock list
                                    lowStockList.add(
                                        "$name (${stock.toInt()} $unitDisplay, ~${daysLeft} days left)"
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Error parsing medicine: ${e.message}")
                    }
                }

                // Sort the lists (today's reminders and history)
                todayList.sortBy { it.startTime }
                historyList.sortByDescending { it.takenAt.ifEmpty { it.startTime } }

                // Update all text views and adapters
                todayMedCountTextView.text =
                    "You have $upcomingCount medicine(s) scheduled for today."
                updateLowStockUI(lowStockList)
                updateReminderLists()

                Log.d("HomeFragment", "Today list: ${todayList.size}, History list: ${historyList.size}")
            }
            .addOnFailureListener { exception ->
                // If Firestore data load failed
                Log.e("HomeFragment", "Error loading data: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                todayMedCountTextView.text = "Error loading schedule."
                lowStockCountTextView.text = "Error loading stock data."
            }
    }

    // Update RecyclerViews visibility and content
    private fun updateReminderLists() {
        todayAdapter.notifyDataSetChanged()
        todayRecyclerView.visibility = if (todayList.isEmpty()) View.GONE else View.VISIBLE

        historyAdapter.notifyDataSetChanged()
        tvNoHistory.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
        historyRecyclerView.visibility = if (historyList.isEmpty()) View.GONE else View.VISIBLE
    }

    // Show list of medicines with low stock
    private fun updateLowStockUI(lowStockList: List<String>) {
        if (lowStockList.isNotEmpty()) {
            lowStockCountTextView.text =
                "${lowStockList.size} medicine(s) low:\n${lowStockList.joinToString(separator = "\n• ", prefix = "• ")}"
        } else {
            lowStockCountTextView.text = "All medicines are well-stocked."
        }
    }

    // Helper function: calculate how many doses per day
    private fun calculateDosesPerDay(
        scheduleMethod: String?,
        timesPerDay: String?,
        intervalHours: String?
    ): Double {
        return when (scheduleMethod) {
            "Frequency" -> timesPerDay?.toDoubleOrNull() ?: 0.0
            "Interval" -> {
                val interval = intervalHours?.toDoubleOrNull() ?: 0.0
                if (interval > 0) 24.0 / interval else 0.0
            }
            else -> 0.0
        }
    }
}
