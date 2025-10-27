package com.example.mednot.User

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Check_Med_Stock : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var containerLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_check_med_stock)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        containerLayout = findViewById(R.id.containerStockList)

        loadMedicines()
    }

    private fun loadMedicines() {
        val uid = firebaseAuth.currentUser?.uid ?: return

        db.collection("medicines")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { result ->
                containerLayout.removeAllViews()
                if (result.isEmpty) {
                    val emptyText = TextView(this)
                    emptyText.text = "No medicines found."
                    emptyText.textSize = 18f
                    emptyText.setPadding(16, 16, 16, 16)
                    containerLayout.addView(emptyText)
                    return@addOnSuccessListener
                }

                for (doc in result) {
                    val medName = doc.getString("medicineName") ?: "Unknown"
                    val quantity = doc.getString("quantity") ?: "0"
                    val remaining = doc.getString("stock") ?: "0"
                    val dosage = doc.getString("dosage") ?: ""
                    val dosageUnit = doc.getString("dosageUnit") ?: ""
                    val lastUpdate = doc.getString("lastUpdated") ?: "N/A"

                    // Inflate card layout
                    val cardView = layoutInflater.inflate(R.layout.item_card_medicine, containerLayout, false)

                    // Bind data
                    val tvMedName = cardView.findViewById<TextView>(R.id.tvMedName)
                    val tvQuantity = cardView.findViewById<TextView>(R.id.tvQuantity)
                    val tvRemaining = cardView.findViewById<TextView>(R.id.tvRemaining)
                    val tvDosage = cardView.findViewById<TextView>(R.id.tvDosage)
                    val tvLastUpdated = cardView.findViewById<TextView>(R.id.tvLastUpdated)
                    val tvLowStock = cardView.findViewById<TextView>(R.id.tvLowStockWarning)

                    tvMedName.text = medName
                    tvQuantity.text = "Quantity: $quantity"
                    tvRemaining.text = "Remaining: $remaining"
                    tvDosage.text = "Dosage: $dosage $dosageUnit"
                    tvLastUpdated.text = "Last Updated: $lastUpdate"

                    // Low stock warning (threshold e.g. < 10)
                    val remainingInt = remaining.toIntOrNull() ?: 0
                    if (remainingInt < 10) {
                        tvLowStock.visibility = TextView.VISIBLE
                    }

                    containerLayout.addView(cardView)
                }
            }
            .addOnFailureListener {
                val errorText = TextView(this)
                errorText.text = "Failed to load medicines: ${it.message}"
                errorText.textSize = 16f
                errorText.setPadding(16, 16, 16, 16)
                containerLayout.addView(errorText)
            }
    }

}

