package com.example.mednot.User

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mednot.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class Edit_Med_Stock : AppCompatActivity() {

    // Declare all the view components used in this activity
    private lateinit var tvEditTitle: TextView
    private lateinit var etEditMedName: TextInputEditText
    private lateinit var etEditStock: TextInputEditText
    private lateinit var etEditDosage: TextInputEditText
    private lateinit var etEditDosageUnit: TextInputEditText
    private lateinit var btnSaveChanges: Button

    // Initialize Firestore database instance
    private val db = FirebaseFirestore.getInstance()

    // Variable to store the current medicine ID
    private var currentMedId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_med_stock)

        // Get medicine ID passed from previous activity
        currentMedId = intent.getStringExtra("MEDICINE_ID")
        if (currentMedId == null) {
            Toast.makeText(this, "Error: Medicine ID not found", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Link all the view components with the layout elements
        tvEditTitle = findViewById(R.id.tvEditTitle)
        etEditMedName = findViewById(R.id.etEditMedName)
        etEditStock = findViewById(R.id.etEditStock)
        etEditDosage = findViewById(R.id.etEditDosage)
        etEditDosageUnit = findViewById(R.id.etEditDosageUnit)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)

        // Load data from Firestore for this medicine
        loadMedicineData()

        // Save data when the save button is clicked
        btnSaveChanges.setOnClickListener {
            saveMedicineData()
        }
    }

    // This function loads existing medicine data from Firestore
    private fun loadMedicineData() {
        currentMedId?.let { medId ->
            db.collection("medicines").document(medId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Read data from the Firestore document
                        val medName = document.getString("medicineName") ?: "N/A"
                        val quantity = document.getString("quantity") ?: "0"
                        val stock = document.getString("stock") ?: "0"
                        val dosage = document.getString("dosage") ?: "0"
                        val dosageUnit = document.getString("dosageUnit") ?: ""

                        // Display the data in the text fields
                        etEditMedName.setText(medName)
                        etEditStock.setText(stock)
                        etEditDosage.setText(dosage)
                        etEditDosageUnit.setText(dosageUnit)
                    } else {
                        // Show an error if no document is found
                        Toast.makeText(this, "Error: Medicine not found", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    // Show an error message if data fails to load
                    Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
        }
    }

    // This function saves updated medicine data back to Firestore
    private fun saveMedicineData() {
        // Get values from input fields
        val newMedName = etEditMedName.text.toString()
        val newStock = etEditStock.text.toString()
        val newDosage = etEditDosage.text.toString()
        val newDosageUnit = etEditDosageUnit.text.toString()

        // Check if any field is empty
        if (newMedName.isBlank() || newStock.isBlank() || newDosage.isBlank() || newDosageUnit.isBlank()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate that stock and dosage contain valid numbers
        if (newStock.toDoubleOrNull() == null) {
            etEditStock.error = "Please enter a valid number for stock"
            return
        }
        if (newDosage.toDoubleOrNull() == null) {
            etEditDosage.error = "Please enter a valid number for dosage"
            return
        }

        // Create a map with updated values
        val updates = hashMapOf<String, Any>(
            "medicineName" to newMedName,
            "stock" to newStock,
            "dosage" to newDosage,
            "dosageUnit" to newDosageUnit
        )

        // Update the Firestore document using the medicine ID
        currentMedId?.let { medId ->
            btnSaveChanges.isEnabled = false
            btnSaveChanges.text = "Saving..."

            db.collection("medicines").document(medId)
                .update(updates)
                .addOnSuccessListener {
                    // Show success message and close the screen
                    Toast.makeText(this, "Stock updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    // Show error message if saving fails
                    Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                    btnSaveChanges.isEnabled = true
                    btnSaveChanges.text = "Save Changes"
                }
        }
    }
}
