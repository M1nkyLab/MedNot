package com.example.mednot.User

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.mednot.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// Data class to store medicine information
data class Medicine(
    val id: String = "",               // Unique ID in Firestore
    val medicineName: String = "",     // Name of the medicine
    val dosage: String = "",           // Dosage amount
    val dosageUnit: String = "",       // Dosage unit (mg, ml, etc.)
    val startTime: String = "",        // Time to take medicine
    var status: String = "upcoming",   // Current status (upcoming, taken, missed, complete)
    var takenAt: String = "",          // Time when medicine was taken
    var stock: String = "0"            // ADDED: Current stock
)

// Adapter to link medicine list to RecyclerView
class ReminderAdapter(
    private val medicineList: MutableList<Medicine>,          // List of medicines
    private val onMedicineStatusChanged: (() -> Unit)? = null // Callback when medicine status changes
) : RecyclerView.Adapter<ReminderAdapter.MedicineViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance() // Firestore database instance

    // ViewHolder holds references to UI elements for one medicine item
    class MedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMedicineName: TextView = itemView.findViewById(R.id.tvMedicineName) // Medicine name
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)   // Status text
        val tvDosageTime: TextView? = itemView.findViewById(R.id.tvDosageTime)   // Time + dosage
        val btnTake: Button? = itemView.findViewById(R.id.btnTake)  // Button to mark as taken
    }

    // Called to create a new item view
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        Log.d("ReminderAdapter", "onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder_today_medicines_schedule, parent, false)
        return MedicineViewHolder(view)
    }

    // Returns the number of items in the list
    override fun getItemCount(): Int {
        Log.d("ReminderAdapter", "getItemCount: ${medicineList.size}")
        return medicineList.size
    }

    // Called for each item to bind data to UI
    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        Log.d("ReminderAdapter", "onBindViewHolder called for position: $position")

        try {
            val currentMedicine = medicineList[position]

            // Set medicine name with an emoji
            holder.tvMedicineName.text = "💊 ${currentMedicine.medicineName}"

            // Set dosage and time text
            holder.tvDosageTime?.text = "${currentMedicine.startTime} | ${currentMedicine.dosage} ${currentMedicine.dosageUnit}"

            // Set status text (capitalize first letter)
            holder.tvStatus.text = "Status: ${currentMedicine.status.replaceFirstChar { it.uppercase() }}"

            val button = holder.btnTake

            // Change button text & color based on medicine status
            when (currentMedicine.status.lowercase()) {
                "taken", "complete" -> {
                    button?.text = "Taken"
                    button?.isEnabled = false
                    button?.alpha = 0.5f
                    holder.tvStatus.setTextColor(0xFF4CAF50.toInt()) // Green
                }
                "missed" -> {
                    button?.text = "Missed"
                    button?.isEnabled = false
                    button?.alpha = 0.5f
                    holder.tvStatus.setTextColor(0xFFF44336.toInt()) // Red
                }
                else -> {
                    button?.text = "Mark as Taken"
                    button?.isEnabled = true
                    button?.alpha = 1.0f
                    holder.tvStatus.setTextColor(0xFF00796B.toInt()) // Teal
                }
            }

            // Button click listener
            button?.setOnClickListener {
                // Only show dialog if medicine is not already taken/missed
                if (currentMedicine.status.lowercase() !in listOf("taken", "complete", "missed")) {
                    showTakeConfirmationDialog(holder, currentMedicine, position)
                }
            }

            Log.d("ReminderAdapter", "Successfully bound medicine: ${currentMedicine.medicineName}")
        } catch (e: Exception) {
            Log.e("ReminderAdapter", "Error binding view at position $position: ${e.message}", e)
        }
    }

    // Show confirmation dialog before marking as taken
    private fun showTakeConfirmationDialog(
        holder: MedicineViewHolder,
        medicine: Medicine,
        position: Int
    ) {
        try {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Confirm")
                .setMessage("Mark ${medicine.medicineName} as taken?")
                .setPositiveButton("Yes") { dialog, _ ->
                    markAsTaken(medicine, position, holder)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } catch (e: Exception) {
            Log.e("ReminderAdapter", "Error showing dialog: ${e.message}", e)
            Toast.makeText(holder.itemView.context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Mark the medicine as taken in Firestore and update UI
    private fun markAsTaken(medicine: Medicine, position: Int, holder: MedicineViewHolder) {
        Log.d("ReminderAdapter", "Marking medicine as taken: ${medicine.medicineName}")

        // --- START OF CHANGES ---

        // Calculate new stock
        val currentStock = medicine.stock.toDoubleOrNull() ?: 0.0
        val dosageToDeduct = medicine.dosage.toDoubleOrNull() ?: 0.0
        var newStock = currentStock

        if (dosageToDeduct > 0) {
            newStock = currentStock - dosageToDeduct
            if (newStock < 0) {
                newStock = 0.0 // Don't allow negative stock
                Log.w("ReminderAdapter", "${medicine.medicineName} stock is now 0 or negative.")
            }
        } else {
            Log.w("ReminderAdapter", "Dosage for ${medicine.medicineName} is 0 or invalid, not deducting stock.")
        }

        // Convert new stock back to String to save in Firestore
        val newStockString = newStock.toString()

        // Get current time in two formats
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Prepare updates for Firestore
        val updates = hashMapOf<String, Any>(
            "status" to "complete",
            "takenAt" to currentTime,
            "completedDateTime" to currentDateTime,
            "stock" to newStockString  // ADDED: Update the stock in Firestore
        )

        // --- END OF CHANGES ---

        // Update Firestore document
        firestore.collection("medicines")
            .document(medicine.id)
            .update(updates)
            .addOnSuccessListener {
                Log.d("ReminderAdapter", "Successfully updated status and stock in Firestore")

                // Update local list and refresh UI
                if (position < medicineList.size) {
                    medicineList[position].status = "complete"
                    medicineList[position].takenAt = currentTime
                    medicineList[position].stock = newStockString // ADDED: Update local stock
                    notifyItemChanged(position)
                }

                // Show a toast message
                Toast.makeText(
                    holder.itemView.context,
                    "✓ ${medicine.medicineName} marked as taken at $currentTime",
                    Toast.LENGTH_SHORT
                ).show()

                // Call optional callback
                onMedicineStatusChanged?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e("ReminderAdapter", "Failed to update status: ${e.message}", e)
                Toast.makeText(
                    holder.itemView.context,
                    "Failed to update: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}