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

// Activity to edit the stock information of a single medicine
class Edit_Med_Stock : AppCompatActivity() {

    // --- Views ---
    private lateinit var tvEditTitle: TextView             // Title of the screen
    private lateinit var etEditMedName: TextInputEditText  // Editable medicine name
    private lateinit var etEditStock: TextInputEditText    // Editable remaining stock
    private lateinit var etEditDosage: TextInputEditText   // Editable dosage
    private lateinit var etEditDosageUnit: TextInputEditText // Editable dosage unit
    private lateinit var btnSaveChanges: Button           // Button to save changes

    private val db = FirebaseFirestore.getInstance()      // Firestore instance
    private var currentMedId: String? = null             // ID of the medicine being edited

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_med_stock) // Set the layout for this screen

        // Get the medicine ID from the previous activity
        currentMedId = intent.getStringExtra("MEDICINE_ID")
        if (currentMedId == null) {
            Toast.makeText(this, "Error: Medicine ID not found", Toast.LENGTH_LONG).show()
            finish() // Close the activity if no ID was passed
            return
        }

        // --- Initialize views ---
        tvEditTitle = findViewById(R.id.tvEditTitle)
        etEditMedName = findViewById(R.id.etEditMedName)
        etEditStock = findViewById(R.id.etEditStock)
        etEditDosage = findViewById(R.id.etEditDosage)
        etEditDosageUnit = findViewById(R.id.etEditDosageUnit)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)

        // Load current medicine data from Firestore into the fields
        loadMedicineData()

        // Set click listener to save changes when button is pressed
        btnSaveChanges.setOnClickListener {
            saveMedicineData()
        }
    }

    // Load the current medicine data from Firestore
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

    // Save the updated medicine data back to Firestore
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

        // Prepare a map with updated values
        val updates = hashMapOf<String, Any>(
            "medicineName" to newMedName,
            "stock" to newStock,
            "dosage" to newDosage,
            "dosageUnit" to newDosageUnit,
        )

        // Update Firestore document
        currentMedId?.let { medId ->
            btnSaveChanges.isEnabled = false   // Prevent multiple clicks
            btnSaveChanges.text = "Saving..."  // Change button text while saving

            db.collection("medicines").document(medId)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Stock updated successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Close activity and go back to the list
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                    btnSaveChanges.isEnabled = true    // Re-enable button on failure
                    btnSaveChanges.text = "Save Changes" // Reset button text
                }
        }
    }
}
