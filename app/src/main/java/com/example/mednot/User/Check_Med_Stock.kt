package com.example.mednot.User

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
// import androidx.appcompat.app.AlertDialog // CHANGED: No longer needed
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

    fun loadMedicines() {
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
                    val docId = doc.id
                    val medName = doc.getString("medicineName") ?: "Unknown"
                    val quantity = doc.getString("quantity") ?: "0"
                    val remaining = doc.getString("stock") ?: "0"
                    val dosage = doc.getString("dosage") ?: ""
                    val dosageUnit = doc.getString("dosageUnit") ?: ""
                    val lastUpdate = doc.getString("lastUpdated") ?: "N/A"

                    // Inflate card layout
                    val cardView = layoutInflater.inflate(R.layout.item_checkstock, containerLayout, false)

                    // Bind data
                    val tvMedName = cardView.findViewById<TextView>(R.id.tvMedName)
                    val tvQuantity = cardView.findViewById<TextView>(R.id.tvQuantity)
                    val tvRemaining = cardView.findViewById<TextView>(R.id.tvRemaining)
                    val tvDosage = cardView.findViewById<TextView>(R.id.tvDosage)
                    val tvLastUpdated = cardView.findViewById<TextView>(R.id.tvLastUpdated)
                    // val tvLowStock = cardView.findViewById<TextView>(R.id.tvLowStockWarning) // CHANGED: Removed

                    tvMedName.text = medName
                    tvQuantity.text = "Quantity: $quantity"
                    tvRemaining.text = "Remaining: $remaining"
                    tvDosage.text = "Dosage: $dosage $dosageUnit"
                    tvLastUpdated.text = "Last Updated: $lastUpdate"

                    // CHANGED: Removed low stock logic block

                    // --- Button Logic ---
                    val btnEdit = cardView.findViewById<Button>(R.id.btnEditStock)
                    val btnDelete = cardView.findViewById<Button>(R.id.btnDeleteStock)

                    // Set Edit listener
                    btnEdit.setOnClickListener {
                        // TODO: Create an "EditStockActivity" to handle editing
                        val intent = Intent(this, Edit_Med_Stock::class.java)
                            intent.putExtra("MEDICINE_ID", docId)
                            startActivity(intent)

                        Toast.makeText(this, "Edit: $medName (ID: $docId)", Toast.LENGTH_SHORT).show()
                    }

                    // CHANGED: Set Delete listener to delete directly
                    btnDelete.setOnClickListener {
                        deleteMedicine(docId)
                    }

                    containerLayout.addView(cardView)
                }
            }
            .addOnFailureListener {
                containerLayout.removeAllViews()
                val errorText = TextView(this)
                errorText.text = "Failed to load medicines: ${it.message}"
                errorText.textSize = 16f
                errorText.setPadding(16, 16, 16, 16)
                containerLayout.addView(errorText)
            }
    }

    // --- CHANGED: Removed the showDeleteConfirmationDialog function ---

    // --- This function handles the actual deletion ---
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

    // Refresh the list when the user comes back from the Edit activity
    override fun onResume() {
        super.onResume()
        loadMedicines()
    }
}