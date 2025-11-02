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
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDosageTime: TextView? = itemView.findViewById(R.id.tvDosageTime)
        val btnTake: Button? = itemView.findViewById(R.id.btnTake)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        Log.d("ReminderAdapter", "onCreateViewHolder called")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder_today_medicines_schedule, parent, false)
        return MedicineViewHolder(view)
    }

    override fun getItemCount(): Int {
        Log.d("ReminderAdapter", "getItemCount: ${medicineList.size}")
        return medicineList.size
    }

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

            // CHANGED: Fixed .name to .medicineName
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

        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

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

                if (position < medicineList.size) {
                    medicineList[position].status = "complete"
                    medicineList[position].takenAt = currentTime
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