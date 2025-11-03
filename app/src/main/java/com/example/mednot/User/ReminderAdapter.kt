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

// Data class: holds information for each medicine reminder
data class Medicine(
    val id: String = "",          // Unique ID for this medicine (from Firestore)
    val medicineName: String = "",// Name of medicine
    val dosage: String = "",      // How much to take (e.g., "2")
    val dosageUnit: String = "",  // Unit of dosage (e.g., "tablet", "ml")
    val startTime: String = "",   // Time to take the medicine
    var status: String = "upcoming", // Current status: upcoming, complete, or missed
    var takenAt: String = "",     // Time when user marked it as taken
    var stock: String = "0"       // Remaining stock in the bottle
)

// ReminderAdapter: shows list of medicines using RecyclerView
class ReminderAdapter(
    private val medicineList: MutableList<Medicine>,          // List of all medicines
    private val onMedicineStatusChanged: (() -> Unit)? = null // Callback when status changes
) : RecyclerView.Adapter<ReminderAdapter.MedicineViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance() // Connect to Firebase Firestore

    // ViewHolder: holds all views for each medicine item
    class MedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMedicineName: TextView = itemView.findViewById(R.id.tvMedicineName) // Medicine name text
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)             // Status text (Upcoming / Taken / Missed)
        val tvDosageTime: TextView? = itemView.findViewById(R.id.tvDosageTime)    // Shows time and dosage
        val btnTake: Button? = itemView.findViewById(R.id.btnTake)                // Button to mark as taken
    }

    // Creates new ViewHolder when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        Log.d("ReminderAdapter", "onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder_today_medicines_schedule, parent, false)
        return MedicineViewHolder(view)
    }

    // Returns how many items are in the list
    override fun getItemCount(): Int {
        Log.d("ReminderAdapter", "getItemCount: ${medicineList.size}")
        return medicineList.size
    }

    // Binds data from the list to the views (called for every item)
    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        Log.d("ReminderAdapter", "onBindViewHolder called for position: $position")

        try {
            val currentMedicine = medicineList[position]

            // Set the text for name, time, and dosage
            holder.tvMedicineName.text = "💊 ${currentMedicine.medicineName}"
            holder.tvDosageTime?.text =
                "${currentMedicine.startTime} | ${currentMedicine.dosage} ${currentMedicine.dosageUnit}"
            holder.tvStatus.text =
                "Status: ${currentMedicine.status.replaceFirstChar { it.uppercase() }}"

            val button = holder.btnTake

            // Change button style and color based on medicine status
            when (currentMedicine.status.lowercase()) {
                "taken", "complete" -> {
                    // Already taken
                    button?.text = "Taken"
                    button?.isEnabled = false
                    button?.alpha = 0.5f
                    holder.tvStatus.setTextColor(0xFF4CAF50.toInt()) // Green color
                }

                "missed" -> {
                    // Missed dose
                    button?.text = "Missed"
                    button?.isEnabled = false
                    button?.alpha = 0.5f
                    holder.tvStatus.setTextColor(0xFFF44336.toInt()) // Red color
                }

                else -> {
                    // Upcoming dose (not yet taken)
                    button?.text = "Mark as Taken"
                    button?.isEnabled = true
                    button?.alpha = 1.0f
                    holder.tvStatus.setTextColor(0xFF00796B.toInt()) // Teal color
                }
            }

            // When user clicks "Mark as Taken" button
            button?.setOnClickListener {
                // Only allow marking if not already complete or missed
                if (currentMedicine.status.lowercase() !in listOf("taken", "complete", "missed")) {
                    showTakeConfirmationDialog(holder, currentMedicine, position)
                }
            }

            Log.d("ReminderAdapter", "Successfully bound medicine: ${currentMedicine.medicineName}")
        } catch (e: Exception) {
            Log.e("ReminderAdapter", "Error binding view: ${e.message}", e)
        }
    }

    // Show a confirmation dialog before marking as taken
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
                    markAsTaken(medicine, position, holder) // Call the update function
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

    // Mark the medicine as "Taken" in Firestore and update the app screen
    private fun markAsTaken(medicine: Medicine, position: Int, holder: MedicineViewHolder) {
        Log.d("ReminderAdapter", "Marking medicine as taken: ${medicine.medicineName}")

        val currentStock = medicine.stock.toDoubleOrNull() ?: 0.0 // Convert stock to number

        // Decide how much stock to reduce
        val dosageToDeduct: Double
        val unit = medicine.dosageUnit.lowercase()

        if (unit.contains("tablet") || unit.contains("capsule") || unit.contains("pill")) {
            // If medicine counted by pieces, reduce based on dosage (e.g., 2 tablets)
            dosageToDeduct = medicine.dosage.toDoubleOrNull() ?: 1.0
        } else {
            // If measured by mg, ml, etc., just reduce 1 dose
            dosageToDeduct = 1.0
        }

        Log.d("ReminderAdapter", "Deducting: $dosageToDeduct from $currentStock for unit '$unit'")

        var newStock = currentStock - dosageToDeduct
        if (newStock < 0) newStock = 0.0 // Do not allow negative stock

        val newStockString = newStock.toString() // Convert back to text
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) // Example: 10:30 AM
        val currentDateTime =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())   // Example: 2025-11-03 10:30:00

        // Prepare data to update in Firestore
        val updates = hashMapOf<String, Any>(
            "status" to "complete",
            "takenAt" to currentTime,
            "completedDateTime" to currentDateTime,
            "stock" to newStockString
        )

        // Update medicine info in Firestore database
        firestore.collection("medicines")
            .document(medicine.id)
            .update(updates)
            .addOnSuccessListener {
                Log.d("ReminderAdapter", "Successfully updated status and stock in Firestore")

                // Update local data too (so RecyclerView refreshes)
                if (position < medicineList.size) {
                    medicineList[position].status = "complete"
                    medicineList[position].takenAt = currentTime
                    medicineList[position].stock = newStockString
                    notifyItemChanged(position)
                }

                // Show success message to user
                Toast.makeText(
                    holder.itemView.context,
                    "${medicine.medicineName} marked as taken at $currentTime",
                    Toast.LENGTH_SHORT
                ).show()

                // Call callback to notify main screen (optional)
                onMedicineStatusChanged?.invoke()
            }
            .addOnFailureListener { e ->
                // If update failed
                Log.e("ReminderAdapter", "Failed to update: ${e.message}", e)
                Toast.makeText(
                    holder.itemView.context,
                    "Failed to update: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
