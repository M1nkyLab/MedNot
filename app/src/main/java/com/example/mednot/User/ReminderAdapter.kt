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

data class Medicine(
    val id: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val dosageUnit: String = "",
    val startTime: String = "",
    var status: String = "upcoming",
    var takenAt: String = ""
)

class ReminderAdapter(
    private val medicineList: MutableList<Medicine>,
    private val onMedicineStatusChanged: (() -> Unit)? = null
) : RecyclerView.Adapter<ReminderAdapter.MedicineViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    class MedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMedicineName: TextView = itemView.findViewById(R.id.tvMedicineName)
        val tvDosage: TextView? = itemView.findViewById(R.id.tvDosage)
        val tvTime: TextView? = itemView.findViewById(R.id.tvTime)
        val tvFrequency: TextView? = itemView.findViewById(R.id.tvFrequency)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnMarkTaken: Button? = itemView.findViewById(R.id.btnMarkTaken)

        // Alternative IDs for different layouts
        val tvDosageTime: TextView? = itemView.findViewById(R.id.tvDosageTime)
        val btnTake: Button? = itemView.findViewById(R.id.btnTake)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        Log.d("ReminderAdapter", "onCreateViewHolder called")
        return try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_reminder_today_medicines_schedule, parent, false)
            MedicineViewHolder(view)
        } catch (e: Exception) {
            Log.e("ReminderAdapter", "Error inflating view: ${e.message}", e)
            throw e
        }
    }

    override fun getItemCount(): Int {
        Log.d("ReminderAdapter", "getItemCount: ${medicineList.size}")
        return medicineList.size
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        Log.d("ReminderAdapter", "onBindViewHolder called for position: $position")

        try {
            val currentMedicine = medicineList[position]

            // Medicine Name (required - exists in both layouts)
            holder.tvMedicineName.text = "💊 ${currentMedicine.medicineName}"

            // Handle different layout structures
            if (holder.tvDosage != null && holder.tvTime != null) {
                // Layout has separate dosage and time fields
                holder.tvDosage.text = "${currentMedicine.dosage} ${currentMedicine.dosageUnit}"

                if (currentMedicine.status.lowercase() in listOf("taken", "complete") && currentMedicine.takenAt.isNotEmpty()) {
                    holder.tvTime.text = "Taken at: ${currentMedicine.takenAt}"
                } else {
                    holder.tvTime.text = "Scheduled: ${currentMedicine.startTime}"
                }

                holder.tvFrequency?.text = "Daily"
            } else if (holder.tvDosageTime != null) {
                // Layout has combined dosage and time field
                holder.tvDosageTime.text = "${currentMedicine.startTime} | ${currentMedicine.dosage} ${currentMedicine.dosageUnit}"
            }

            // Status (required - exists in both layouts)
            holder.tvStatus.text = "Status: ${currentMedicine.status.replaceFirstChar { it.uppercase() }}"

            // Get the button (could be btnMarkTaken or btnTake depending on layout)
            val button = holder.btnMarkTaken ?: holder.btnTake

            // Change button appearance and color based on status
            when (currentMedicine.status.lowercase()) {
                "taken", "complete" -> {
                    button?.text = "Taken ✓"
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

    private fun markAsTaken(medicine: Medicine, position: Int, holder: MedicineViewHolder) {
        Log.d("ReminderAdapter", "Marking medicine as taken: ${medicine.medicineName}")

        // Get current timestamp
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Update Firestore with status and timestamp
        val updates = hashMapOf<String, Any>(
            "status" to "complete",
            "takenAt" to currentTime,
            "completedDateTime" to currentDateTime
        )

        firestore.collection("medicines")
            .document(medicine.id)
            .update(updates)
            .addOnSuccessListener {
                Log.d("ReminderAdapter", "Successfully updated status in Firestore")

                // Update local list
                medicineList[position].status = "complete"
                medicineList[position].takenAt = currentTime
                notifyItemChanged(position)

                Toast.makeText(
                    holder.itemView.context,
                    "✓ ${medicine.medicineName} marked as taken at $currentTime",
                    Toast.LENGTH_SHORT
                ).show()

                // Notify parent to refresh (this will move item to history)
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