package com.example.mednot.User

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// This activity shows a list of all medicines and allows the user to edit or delete them
class Check_Med_Stock : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth     // Firebase Authentication instance
    private lateinit var db: FirebaseFirestore         // Firestore database instance
    private lateinit var containerLayout: LinearLayout // The layout container that holds medicine cards

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_check_med_stock) // Set the UI layout for this activity

        firebaseAuth = FirebaseAuth.getInstance()      // Initialize FirebaseAuth
        db = FirebaseFirestore.getInstance()           // Initialize Firestore
        containerLayout = findViewById(R.id.containerStockList) // Get reference to LinearLayout

        loadMedicines() // Load medicines from Firestore
    }

    // Function to load all medicines for the current user
    fun loadMedicines() {
        val uid = firebaseAuth.currentUser?.uid ?: return // Get current user's ID

        db.collection("medicines")
            .whereEqualTo("userId", uid) // Only get medicines that belong to this user
            .get()
            .addOnSuccessListener { result ->

                containerLayout.removeAllViews() // Clear old medicine views

                // If no medicines found, show a message
                if (result.isEmpty) {
                    val emptyText = TextView(this)
                    emptyText.text = "No medicines found."
                    emptyText.textSize = 18f
                    emptyText.setPadding(16, 16, 16, 16)
                    containerLayout.addView(emptyText)
                    return@addOnSuccessListener
                }

                // Loop through all medicines and create a card for each
                for (doc in result) {
                    val docId = doc.id
                    val medName = doc.getString("medicineName") ?: "Unknown"
                    // val quantity = doc.getString("quantity") ?: "0" // <- REMOVED
                    val remaining = doc.getString("stock") ?: "0"
                    val dosage = doc.getString("dosage") ?: ""
                    val dosageUnit = doc.getString("dosageUnit") ?: ""

                    // Inflate the medicine card layout
                    val cardView = layoutInflater.inflate(R.layout.item_checkstock, containerLayout, false)

                    // Bind data to UI elements in the card
                    val tvMedName = cardView.findViewById<TextView>(R.id.tvMedName)
                    // val tvQuantity = cardView.findViewById<TextView>(R.id.tvQuantity) // <- REMOVED
                    val tvRemaining = cardView.findViewById<TextView>(R.id.tvRemaining)
                    val tvDosage = cardView.findViewById<TextView>(R.id.tvDosage)
                    // val tvLastUpdated = cardView.findViewById<TextView>(R.id.tvLastUpdated) // <- REMOVED

                    tvMedName.text = medName
                    // tvQuantity.text = "Quantity: $quantity" // <- REMOVED
                    tvRemaining.text = "Remaining: $remaining"
                    tvDosage.text = "Dosage: $dosage $dosageUnit"
                    // tvLastUpdated.text = "Last Updated: $lastUpdate" // <- REMOVED (and was bugged)

                    // --- Button Logic ---
                    val btnEdit = cardView.findViewById<Button>(R.id.btnEditStock)
                    val btnDelete = cardView.findViewById<Button>(R.id.btnDeleteStock)

                    // Edit button opens Edit_Med_Stock activity with medicine ID
                    btnEdit.setOnClickListener {
                        val intent = Intent(this, Edit_Med_Stock::class.java)
                        intent.putExtra("MEDICINE_ID", docId)
                        startActivity(intent)
                        Toast.makeText(this, "Edit: $medName (ID: $docId)", Toast.LENGTH_SHORT).show()
                    }

                    // Delete button deletes the medicine from Firestore
                    btnDelete.setOnClickListener {
                        deleteMedicine(docId)
                    }

                    // Add the card to the container layout
                    containerLayout.addView(cardView)
                }
            }
            .addOnFailureListener { e ->
                // Show error message if loading fails
                containerLayout.removeAllViews()
                val errorText = TextView(this)
                errorText.text = "Failed to load medicines: ${e.message}"
                errorText.textSize = 16f
                errorText.setPadding(16, 16, 16, 16)
                containerLayout.addView(errorText)
            }
    }

    // Function to delete a medicine document from Firestore
    private fun deleteMedicine(docId: String) {
        db.collection("medicines").document(docId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Medicine deleted", Toast.LENGTH_SHORT).show()
                loadMedicines() // Refresh the list after deletion
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting medicine: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Refresh the list when returning from Edit_Med_Stock activity
    override fun onResume() {
        super.onResume()
        loadMedicines()
    }
}