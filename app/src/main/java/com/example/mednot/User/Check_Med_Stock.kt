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

// This screen shows all the medicines the user has.
// The user can edit or delete each medicine from here.
class Check_Med_Stock : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth     // Used to check the logged-in user
    private lateinit var db: FirebaseFirestore          // Used to connect to Firestore database
    private lateinit var containerLayout: LinearLayout  // Layout that will show all medicine cards

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_check_med_stock) // Set the layout for this screen

        firebaseAuth = FirebaseAuth.getInstance()      // Initialize Firebase Authentication
        db = FirebaseFirestore.getInstance()           // Initialize Firestore database
        containerLayout = findViewById(R.id.containerStockList) // Get reference to layout that holds cards

        loadMedicines() // Load the medicines when the screen starts
    }

    // This function gets all medicines from Firestore for the logged-in user
    fun loadMedicines() {
        val uid = firebaseAuth.currentUser?.uid ?: return // Get current user's ID, or stop if not logged in

        db.collection("medicines")
            .whereEqualTo("userId", uid) // Only get medicines that belong to this user
            .get()
            .addOnSuccessListener { result ->

                containerLayout.removeAllViews() // Remove old cards before adding new ones

                // If no medicines are found, show a message
                if (result.isEmpty) {
                    val emptyText = TextView(this)
                    emptyText.text = "No medicines found."
                    emptyText.textSize = 18f
                    emptyText.setPadding(16, 16, 16, 16)
                    containerLayout.addView(emptyText)
                    return@addOnSuccessListener
                }

                // Go through all the medicines and make a card for each one
                for (doc in result) {
                    val docId = doc.id
                    val medName = doc.getString("medicineName") ?: "Unknown"
                    val remaining = doc.getString("stock") ?: "0"
                    val dosage = doc.getString("dosage") ?: ""
                    val dosageUnit = doc.getString("dosageUnit") ?: ""

                    // Create (inflate) a card layout from XML
                    val cardView = layoutInflater.inflate(R.layout.item_checkstock, containerLayout, false)

                    // Get the text views in the card and fill them with data
                    val tvMedName = cardView.findViewById<TextView>(R.id.tvMedName)
                    val tvRemaining = cardView.findViewById<TextView>(R.id.tvRemaining)
                    val tvDosage = cardView.findViewById<TextView>(R.id.tvDosage)

                    tvMedName.text = medName
                    tvRemaining.text = "Remaining: $remaining"
                    tvDosage.text = "Dosage: $dosage $dosageUnit"

                    // Get the buttons from the card
                    val btnEdit = cardView.findViewById<Button>(R.id.btnEditStock)
                    val btnDelete = cardView.findViewById<Button>(R.id.btnDeleteStock)

                    // When Edit is clicked, open the Edit_Med_Stock screen
                    btnEdit.setOnClickListener {
                        val intent = Intent(this, Edit_Med_Stock::class.java)
                        intent.putExtra("MEDICINE_ID", docId)
                        startActivity(intent)
                        Toast.makeText(this, "Editing: $medName", Toast.LENGTH_SHORT).show()
                    }

                    // When Delete is clicked, remove the medicine from Firestore
                    btnDelete.setOnClickListener {
                        deleteMedicine(docId)
                    }

                    // Add the card to the screen
                    containerLayout.addView(cardView)
                }
            }
            .addOnFailureListener { e ->
                // Show an error message if loading fails
                containerLayout.removeAllViews()
                val errorText = TextView(this)
                errorText.text = "Failed to load medicines: ${e.message}"
                errorText.textSize = 16f
                errorText.setPadding(16, 16, 16, 16)
                containerLayout.addView(errorText)
            }
    }

    // This function deletes a medicine from Firestore by its document ID
    private fun deleteMedicine(docId: String) {
        db.collection("medicines").document(docId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Medicine deleted", Toast.LENGTH_SHORT).show()
                loadMedicines() // Reload the list after deleting
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error deleting medicine: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // When returning to this screen (like after editing), reload the list
    override fun onResume() {
        super.onResume()
        loadMedicines()
    }
}
