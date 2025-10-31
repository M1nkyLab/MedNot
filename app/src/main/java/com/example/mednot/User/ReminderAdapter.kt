package com.example.mednot.User

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.mednot.R
import com.google.firebase.firestore.FirebaseFirestore

// A data class to represent the structure of your medicine document in Firestore.
// This provides type safety and makes your code much cleaner.
data class Medicine(
    val id: String = "", // Will hold the document ID from Firestore
    val medicineName: String = "",
    val dosage: String = "",
    val dosageUnit: String = "",
    val startTime: String = "",
    var status: String = "upcoming" // 'var' because we might change it locally
)

class ReminderAdapter(
    private val medicineList: MutableList<Medicine>
) : RecyclerView.Adapter<ReminderAdapter.MedicineViewHolder>() {

    // The ViewHolder holds references to the views for each item.
    // This avoids repeatedly calling findViewById(), which is a major performance gain.
    class MedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMedicineName: TextView = itemView.findViewById(R.id.tvMedicineName)
        val tvDosageTime: TextView = itemView.findViewById(R.id.tvDosageTime)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnTake: Button = itemView.findViewById(R.id.btnTake)
    }

    // Called when RecyclerView needs a new ViewHolder. It inflates your item layout.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return MedicineViewHolder(view)
    }

    // Returns the total number of items in the list.
    override fun getItemCount(): Int = medicineList.size

    // This is where the data is bound to the views of a specific item.
    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val currentMedicine = medicineList[position]

        // Populate the views with data from the Medicine object
        holder.tvMedicineName.text = "💊 ${currentMedicine.medicineName}"
        holder.tvDosageTime.text = "${currentMedicine.startTime} | ${currentMedicine.dosage} ${currentMedicine.dosageUnit}"
        holder.tvStatus.text = "Status: ${currentMedicine.status.replaceFirstChar { it.uppercase() }}"

        // --- Event Listeners for Buttons ---

        holder.btnEdit.setOnClickListener {
            // TODO: Implement the edit functionality.
            // You can open a dialog or a new activity here, passing 'currentMedicine.id'
            Toast.makeText(holder.itemView.context, "Edit for ${currentMedicine.medicineName}", Toast.LENGTH_SHORT).show()
        }

        holder.btnTake.setOnClickListener {
            // TODO: Implement the logic to mark the medicine as "taken".
            // This might involve updating the status in Firestore and then refreshing the view.
            Toast.makeText(holder.itemView.context, "Taking ${currentMedicine.medicineName}", Toast.LENGTH_SHORT).show()
        }
    }
}
