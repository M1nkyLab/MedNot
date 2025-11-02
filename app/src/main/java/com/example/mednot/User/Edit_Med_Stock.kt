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

    // ... (All your view variables) ...
    private lateinit var tvEditTitle: TextView
    private lateinit var etEditMedName: TextInputEditText
    private lateinit var etEditStock: TextInputEditText
    private lateinit var etEditDosage: TextInputEditText
    private lateinit var etEditDosageUnit: TextInputEditText
    private lateinit var btnSaveChanges: Button

    private val db = FirebaseFirestore.getInstance()
    private var currentMedId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_med_stock)

        currentMedId = intent.getStringExtra("MEDICINE_ID")
        if (currentMedId == null) {
            Toast.makeText(this, "Error: Medicine ID not found", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize views
        tvEditTitle = findViewById(R.id.tvEditTitle)
        etEditMedName = findViewById(R.id.etEditMedName)
        etEditStock = findViewById(R.id.etEditStock)
        etEditDosage = findViewById(R.id.etEditDosage)
        etEditDosageUnit = findViewById(R.id.etEditDosageUnit)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)

        loadMedicineData()

        btnSaveChanges.setOnClickListener {
            saveMedicineData()
        }
    }

    // ... (loadMedicineData is unchanged) ...
    private fun loadMedicineData() {
        currentMedId?.let { medId ->
            db.collection("medicines").document(medId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Load all fields from the document
                        val medName = document.getString("medicineName") ?: "N/A"
                        val quantity = document.getString("quantity") ?: "0"
                        val stock = document.getString("stock") ?: "0"
                        val dosage = document.getString("dosage") ?: "0"
                        val dosageUnit = document.getString("dosageUnit") ?: ""

                        // Set text in editable fields
                        etEditMedName.setText(medName)
                        etEditStock.setText(stock)
                        etEditDosage.setText(dosage)
                        etEditDosageUnit.setText(dosageUnit)

                    } else {
                        Toast.makeText(this, "Error: Medicine not found", Toast.LENGTH_LONG).show()
                        finish() // Close if medicine does not exist
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
                    finish() // Close on failure
                }
        }
    }


    private fun saveMedicineData() {
        // Get text from all input fields
        val newMedName = etEditMedName.text.toString()
        val newStock = etEditStock.text.toString()
        val newDosage = etEditDosage.text.toString()
        val newDosageUnit = etEditDosageUnit.text.toString()

        // Validate that all fields are filled
        if (newMedName.isBlank()  || newStock.isBlank() || newDosage.isBlank() || newDosageUnit.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // --- START OF FIX ---
        // ADDED VALIDATION
        if (newStock.toDoubleOrNull() == null) {
            etEditStock.error = "Please enter a valid number for stock"
            return
        }
        if (newDosage.toDoubleOrNull() == null) {
            etEditDosage.error = "Please enter a valid number for dosage"
            return
        }
        // --- END OF FIX ---

        // Prepare a map with updated values
        val updates = hashMapOf<String, Any>(
            "medicineName" to newMedName,
            "stock" to newStock,
            "dosage" to newDosage,
            "dosageUnit" to newDosageUnit
        )

        // Update Firestore document
        currentMedId?.let { medId ->
            btnSaveChanges.isEnabled = false
            btnSaveChanges.text = "Saving..."

            db.collection("medicines").document(medId)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Stock updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                    btnSaveChanges.isEnabled = true
                    btnSaveChanges.text = "Save Changes"
                }
        }
    }
}