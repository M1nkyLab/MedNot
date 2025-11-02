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

// Data class (Unchanged)
data class Medicine(
    val id: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val dosageUnit: String = "",
    val startTime: String = "",
    var status: String = "upcoming",
    var takenAt: String = "",
    var stock: String = "0"
)

class ReminderAdapter(
    private val medicineList: MutableList<Medicine>,
    private val onMedicineStatusChanged: (() -> Unit)? = null
) : RecyclerView.Adapter<ReminderAdapter.MedicineViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    // ViewHolder (Unchanged)
    class MedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMedicineName: TextView = itemView.findViewById(R.id.tvMedicineName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDosageTime: TextView? = itemView.findViewById(R.id.tvDosageTime)
        val btnTake: Button? = itemView.findViewById(R.id.btnTake)
    }

    // onCreateViewHolder (Unchanged)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        Log.d("ReminderAdapter", "onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder_today_medicines_schedule, parent, false)
        return MedicineViewHolder(view)
    }

    // getItemCount (Unchanged)
    override fun getItemCount(): Int {
        Log.d("ReminderAdapter", "getItemCount: ${medicineList.size}")
        return medicineList.size
    }

    // onBindViewHolder (Unchanged)
    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        Log.d("ReminderAdapter", "onBindViewHolder called for position: $position")

        try {
            val currentMedicine = medicineList[position]

            holder.tvMedicineName.text = "💊 ${currentMedicine.medicineName}"
            holder.tvDosageTime?.text = "${currentMedicine.startTime} | ${currentMedicine.dosage} ${currentMedicine.dosageUnit}"
            holder.tvStatus.text = "Status: ${currentMedicine.status.replaceFirstChar { it.uppercase() }}"

            val button = holder.btnTake

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

            button?.setOnClickListener {
                if (currentMedicine.status.lowercase() !in listOf("taken", "complete", "missed")) {
                    showTakeConfirmationDialog(holder, currentMedicine, position)
                }
            }

            Log.d("ReminderAdapter", "Successfully bound medicine: ${currentMedicine.medicineName}")
        } catch (e: Exception) {
            Log.e("ReminderAdapter", "Error binding view at position $position: ${e.message}", e)
        }
    }

    // showTakeConfirmationDialog (Unchanged)
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

        val currentStock = medicine.stock.toDoubleOrNull() ?: 0.0

        // --- START OF LOGIC FIX ---
        val dosageToDeduct: Double
        val unit = medicine.dosageUnit.lowercase()

        if (unit.contains("tablet") || unit.contains("capsule") || unit.contains("pill")) {
            // If the unit is a countable item (tablet, capsule),
            // deduct the dosage amount (e.g., "2" tablets).
            dosageToDeduct = medicine.dosage.toDoubleOrNull() ?: 1.0
        } else {
            // If the unit is "mg", "ml", "g", etc., the stock is counted in "doses",
            // so we only deduct 1 dose.
            dosageToDeduct = 1.0
        }

        Log.d("ReminderAdapter", "Deducting: $dosageToDeduct from $currentStock for unit '$unit'")
        // --- END OF LOGIC FIX ---

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

        val newStockString = newStock.toString()
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val updates = hashMapOf<String, Any>(
            "status" to "complete",
            "takenAt" to currentTime,
            "completedDateTime" to currentDateTime,
            "stock" to newStockString  // Update the stock
        )

        firestore.collection("medicines")
            .document(medicine.id)
            .update(updates)
            .addOnSuccessListener {
                Log.d("ReminderAdapter", "Successfully updated status and stock in Firestore")

                if (position < medicineList.size) {
                    medicineList[position].status = "complete"
                    medicineList[position].takenAt = currentTime
                    medicineList[position].stock = newStockString // Update local stock
                    notifyItemChanged(position)
                }

                Toast.makeText(
                    holder.itemView.context,
                    "✓ ${medicine.medicineName} marked as taken at $currentTime",
                    Toast.LENGTH_SHORT
                ).show()

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