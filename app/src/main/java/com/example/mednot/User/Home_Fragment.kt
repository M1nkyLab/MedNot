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

    // Firebase instances
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Top section views
    private lateinit var welcomeMessage: TextView
    private lateinit var lowStockCountTextView: TextView
    private lateinit var todayMedCountTextView: TextView

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
        val view = inflater.inflate(R.layout.user_home_fragment, container, false)

        // Initialize top section views
        welcomeMessage = view.findViewById(R.id.welcomeMessage)
        lowStockCountTextView = view.findViewById(R.id.lowStockCount)
        todayMedCountTextView = view.findViewById(R.id.todayMedCount)

        // Initialize reminder lists views
        setupReminderViews(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        // Load all data when fragment becomes visible
        loadUserName()
        loadAllData()
    }

    private fun setupReminderViews(view: View) {
        // Setup for Today's Reminders
        tvNoReminders = view.findViewById(R.id.tvNoReminders)
        todayRecyclerView = view.findViewById(R.id.todayRemindersRecyclerView)
        todayRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        todayAdapter = ReminderAdapter(todayList)
        todayRecyclerView.adapter = todayAdapter

        // Setup for Medication History
        tvNoHistory = view.findViewById(R.id.tvNoHistory)
        historyRecyclerView = view.findViewById(R.id.medicationLogRecyclerView)
        historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        historyAdapter = ReminderAdapter(historyList)
        historyRecyclerView.adapter = historyAdapter
    }

    private fun loadUserName() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            welcomeMessage.text = "Welcome, Guest"
            return
        }

        val uid = currentUser.uid

        // Fetch user name from Firestore
        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userName = document.getString("name") ?: "User"
                    // Extract first name (everything before the first space)
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

    private fun loadAllData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            lowStockCountTextView.text = "Login to see your medicine stock."
            todayMedCountTextView.text = "Login to see today's schedule."

            // Clear lists and update UI
            todayList.clear()
            historyList.clear()
            updateReminderLists()
            return
        }

        val uid = currentUser.uid

        Log.d("HomeFragment", "Loading data for user: $uid")

        // Single Firestore query to fetch all medicines for the user
        firestore.collection("medicines")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("HomeFragment", "Documents fetched: ${documents.size()}")

                if (documents.isEmpty) {
                    // Handle case where user has no medicines
                    todayMedCountTextView.text = "No medicines scheduled! 🎉"
                    lowStockCountTextView.text = "No medicines to track."

                    // Clear lists and update UI
                    todayList.clear()
                    historyList.clear()
                    updateReminderLists()
                    return@addOnSuccessListener
                }

                // Clear previous data
                todayList.clear()
                historyList.clear()

                val lowStockList = mutableListOf<String>()
                var upcomingCount = 0

                for (document in documents) {
                    Log.d("HomeFragment", "Processing document: ${document.id}")

                    // Get status field (default to "upcoming" if not present)
                    val status = document.getString("status") ?: "upcoming"

                    try {
                        // Convert to Medicine object using toObject
                        val medicine = document.toObject(Medicine::class.java).copy(id = document.id)

                        // Sort into reminder lists based on status
                        if (status.equals("complete", ignoreCase = true)) {
                            historyList.add(medicine)
                        } else {
                            todayList.add(medicine)
                            upcomingCount++
                        }

                        // low stock calculation
                        val name = document.getString("medicineName") ?: "Unnamed Medicine"
                        val stockStr = document.getString("stock") ?: "0"
                        val stock = stockStr.toDoubleOrNull() ?: 0.0
                        val dosageUnit = document.getString("dosageUnit")?.lowercase() ?: ""
                        val scheduleMethod = document.getString("scheduleMethod")
                        val timesPerDay = document.getString("timesPerDay")
                        val intervalHours = document.getString("intervalHours")
                        val dosage = document.getString("dosage")?.toDoubleOrNull() ?: 1.0
                        val durationDays = document.getString("duration")?.toDoubleOrNull() ?: 0.0

                        if (stock > 0) {
                            // nie untuk kita sehari bape kali makan ubat
                            val dosesPerDay = when (scheduleMethod) {
                                "Frequency" -> timesPerDay?.toDoubleOrNull() ?: 0.0
                                "Interval" -> {
                                    val interval = intervalHours?.toDoubleOrNull() ?: 0.0
                                    if (interval > 0) 24.0 / interval else 0.0
                                }
                                else -> 0.0
                            }

                            if (dosesPerDay > 0) {
                                // nie untuk kira stock tinggal lagi untuk ape hari
                                val daysRemaining = stock / dosesPerDay

                                // untuk ccheck kalau stock ubat tu kurang dari 7 hari
                                if (daysRemaining < 7) {
                                    val daysLeft = daysRemaining.roundToInt().coerceAtLeast(0)

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

                                    // add to low stock list
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

                // Sort lists by time (if Medicine has startTime field)
                todayList.sortBy { it.startTime }
                historyList.reverse() // Most recent first

                // Update all UI elements
                todayMedCountTextView.text = "You have $upcomingCount medicine(s) scheduled for today."
                updateLowStockUI(lowStockList)
                updateReminderLists()

                Log.d("HomeFragment", "Today list: ${todayList.size}, History list: ${historyList.size}")
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error loading data: ${exception.message}", exception)
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                todayMedCountTextView.text = "Error loading schedule."
                lowStockCountTextView.text = "Error loading stock data."
            }
    }

    private fun updateReminderLists() {
        // Update today's reminders UI
        todayAdapter.notifyDataSetChanged()
        tvNoReminders.visibility = if (todayList.isEmpty()) View.VISIBLE else View.GONE
        todayRecyclerView.visibility = if (todayList.isEmpty()) View.GONE else View.VISIBLE

        // Update history UI
        historyAdapter.notifyDataSetChanged()
        tvNoHistory.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
        historyRecyclerView.visibility = if (historyList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateLowStockUI(lowStockList: List<String>) {
        if (lowStockList.isNotEmpty()) {
            lowStockCountTextView.text = "${lowStockList.size} medicine(s) low:\n${lowStockList.joinToString(separator = "\n• ", prefix = "• ")}"
        } else {
            lowStockCountTextView.text = "All medicines are well-stocked. ✓"
        }
    }

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