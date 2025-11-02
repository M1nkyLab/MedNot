package com.example.mednot.User

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mednot.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Edit_Med_Stock : AppCompatActivity() {

    private lateinit var tvEditTitle: TextView
    // private lateinit var tvEditMedName: TextView // CHANGED: Removed this
    private lateinit var etEditQuantity: TextInputEditText
    private lateinit var etEditStock: TextInputEditText
    private lateinit var btnSaveChanges: Button

    // CHANGED: Added new fields
    private lateinit var etEditMedName: TextInputEditText
    private lateinit var etEditDosage: TextInputEditText
    private lateinit var etEditDosageUnit: TextInputEditText

    private val db = FirebaseFirestore.getInstance()
    private var currentMedId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_med_stock)

        // Get the ID passed from Check_Med_Stock activity
        currentMedId = intent.getStringExtra("MEDICINE_ID")

        if (currentMedId == null) {
            Toast.makeText(this, "Error: Medicine ID not found", Toast.LENGTH_LONG).show()
            finish() // Close activity if there's no ID
            return
        }

        // Initialize views
        tvEditTitle = findViewById(R.id.tvEditTitle)
        // tvEditMedName = findViewById(R.id.tvEditMedName) // CHANGED: Removed this
        etEditQuantity = findViewById(R.id.etEditQuantity)
        etEditStock = findViewById(R.id.etEditStock)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)

        // CHANGED: Find new views
        etEditMedName = findViewById(R.id.etEditMedName)
        etEditDosage = findViewById(R.id.etEditDosage)
        etEditDosageUnit = findViewById(R.id.etEditDosageUnit)


        // Load the data for this medicine
        loadMedicineData()

        // Set listener for the save button
        btnSaveChanges.setOnClickListener {
            saveMedicineData()
        }
    }

    private fun loadMedicineData() {
        currentMedId?.let { medId ->
            db.collection("medicines").document(medId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // CHANGED: Load all the data
                        val medName = document.getString("medicineName") ?: "N/A"
                        val quantity = document.getString("quantity") ?: "0"
                        val stock = document.getString("stock") ?: "0"
                        val dosage = document.getString("dosage") ?: "0"
                        val dosageUnit = document.getString("dosageUnit") ?: ""

                        // CHANGED: Set text for all editable fields
                        // tvEditMedName.text = medName // Removed
                        etEditMedName.setText(medName)
                        etEditQuantity.setText(quantity)
                        etEditStock.setText(stock)
                        etEditDosage.setText(dosage)
                        etEditDosageUnit.setText(dosageUnit)

                    } else {
                        Toast.makeText(this, "Error: Medicine not found", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
        }
    }

    private fun saveMedicineData() {
        // CHANGED: Get text from all fields
        val newMedName = etEditMedName.text.toString()
        val newQuantity = etEditQuantity.text.toString()
        val newStock = etEditStock.text.toString()
        val newDosage = etEditDosage.text.toString()
        val newDosageUnit = etEditDosageUnit.text.toString()
        val lastUpdate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // CHANGED: Updated validation
        if (newMedName.isBlank() || newQuantity.isBlank() || newStock.isBlank() || newDosage.isBlank() || newDosageUnit.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // CHANGED: Create a map with all the data to update
        val updates = hashMapOf<String, Any>(
            "medicineName" to newMedName,
            "quantity" to newQuantity,
            "stock" to newStock,
            "dosage" to newDosage,
            "dosageUnit" to newDosageUnit,
            "lastUpdated" to lastUpdate
        )

        // Save to Firestore
        currentMedId?.let { medId ->
            btnSaveChanges.isEnabled = false // Disable button to prevent double-click
            btnSaveChanges.text = "Saving..."

            db.collection("medicines").document(medId)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Stock updated successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Close this activity and go back to the list
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                    btnSaveChanges.isEnabled = true // Re-enable button on failure
                    btnSaveChanges.text = "Save Changes"
                }
        }
    }
}