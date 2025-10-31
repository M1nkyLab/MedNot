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
import com.example.mednot.Medicine
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Home_Fragment : Fragment() {

    // --- UI Elements ---
    private lateinit var welcomeMessage: TextView
    private lateinit var todayMedCountTextView: TextView
    private lateinit var lowStockCountTextView: TextView
    private lateinit var medicinesRecyclerView: RecyclerView // This needs the ID in the XML

    // --- Adapter for the RecyclerView ---
    private lateinit var medicineAdapter: MedicineAdapter

    // --- Firebase Instances ---
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // --- Logic Constant ---
    private val LOW_STOCK_THRESHOLD = 10

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_home_fragment, container, false)

        // Initialize UI
        welcomeMessage = view.findViewById(R.id.Welcomemessage)
        todayMedCountTextView = view.findViewById(R.id.todayMedCount)
        lowStockCountTextView = view.findViewById(R.id.lowStockCount)
        // THIS LINE CAUSES THE ERROR IF THE ID IS MISSING FROM THE XML
        medicinesRecyclerView = view.findViewById(R.id.medicinesRecyclerView)

        // Setup RecyclerView
        medicinesRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        setupWelcomeMessage()

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchMedicineData()
    }

    private fun setupWelcomeMessage() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("name") ?: "User"
                        welcomeMessage.text = "Welcome, $username"
                    } else {
                        welcomeMessage.text = "Welcome, User"
                    }
                }
                .addOnFailureListener {
                    welcomeMessage.text = "Welcome, User"
                }
        } else {
            welcomeMessage.text = "Welcome, Guest"
        }
    }

    private fun fetchMedicineData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            todayMedCountTextView.text = "Log in to see your medicines."
            lowStockCountTextView.text = "Log in to check your stock."
            // Make sure the list is empty if the user is logged out
            if(::medicinesRecyclerView.isInitialized) {
                medicinesRecyclerView.adapter = MedicineAdapter(emptyList()) {}
            }
            return
        }

        firestore.collection("medicines")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    todayMedCountTextView.text = "You have no medicines scheduled."
                    lowStockCountTextView.text = "Your medicine stock is clear."
                    medicinesRecyclerView.adapter = MedicineAdapter(emptyList()) {}
                } else {
                    val medicineList = documents.mapNotNull { document ->
                        val medicine = document.toObject(Medicine::class.java)
                        medicine.documentId = document.id
                        medicine
                    }

                    medicineAdapter = MedicineAdapter(medicineList) { medicine ->
                        deductStock(medicine)
                    }
                    medicinesRecyclerView.adapter = medicineAdapter

                    updateSummaryTexts(medicineList)
                }
            }
            .addOnFailureListener { exception ->
                todayMedCountTextView.text = "Could not load medicine data."
                lowStockCountTextView.text = "Could not load stock data."
                Log.e("Home_Fragment", "Error fetching medicine data", exception)
            }
    }

    private fun deductStock(medicine: Medicine) {
        val documentId = medicine.documentId
        if (documentId == null) {
            Toast.makeText(requireContext(), "Error: Cannot find medicine to update.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentStock = medicine.inputStock ?: 0
        if (currentStock <= 0) {
            Toast.makeText(requireContext(), "${medicine.inputMedicineName} is out of stock.", Toast.LENGTH_SHORT).show()
            return
        }

        val newStock = currentStock - 1

        val medicineRef = firestore.collection("medicines").document(documentId)

        medicineRef.update("inputStock", newStock)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Stock updated for ${medicine.inputMedicineName}.", Toast.LENGTH_SHORT).show()
                fetchMedicineData() // Refresh the data
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update stock: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateSummaryTexts(medicineList: List<Medicine>) {
        val totalMeds = medicineList.size
        val medText = if (totalMeds == 1) "medicine" else "medicines"
        todayMedCountTextView.text = "You have $totalMeds $medText scheduled."

        val lowStockMeds = medicineList.count { it.inputStock != null && it.inputStock!! <= LOW_STOCK_THRESHOLD }
        if (lowStockMeds > 0) {
            val lowStockText = if (lowStockMeds == 1) "medicine is" else "medicines are"
            lowStockCountTextView.text = "$lowStockMeds $lowStockText low on stock."
            lowStockCountTextView.visibility = View.VISIBLE
        } else {
            lowStockCountTextView.visibility = View.GONE
        }
    }
}
