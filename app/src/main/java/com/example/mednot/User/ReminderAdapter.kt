package com.example.mednot.User

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.mednot.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ReminderAdapter(
    private val context: Context,
    private val reminderList: MutableList<MutableMap<String, Any>>,
    private val userId: String
) : BaseAdapter() {

    private val firestore = FirebaseFirestore.getInstance()

    override fun getCount(): Int = reminderList.size

    override fun getItem(position: Int): Any = reminderList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_reminder, parent, false)

        val reminder = reminderList[position]
        val docId = reminder["id"].toString()

        val tvMedicineName = view.findViewById<TextView>(R.id.tvMedicineName)
        val tvDosageTime = view.findViewById<TextView>(R.id.tvDosageTime)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val btnEdit = view.findViewById<Button>(R.id.btnEdit)
        val btnTake = view.findViewById<Button>(R.id.btnTake)

        val medName = reminder["medicineName"] ?: "Unknown Medicine"
        val time = reminder["time"] ?: "No time set"
        val dosage = reminder["dosage"] ?: "N/A"
        val status = reminder["status"] ?: "upcoming"

        tvMedicineName.text = "💊 $medName"
        tvDosageTime.text = "$time | $dosage"
        tvStatus.text = "Status: ${status.toString().replaceFirstChar { it.uppercase() }}"

        // Color based on status
        when (status.toString().lowercase()) {
            "complete" -> tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32")) // green
            "upcoming" -> tvStatus.setTextColor(android.graphics.Color.parseColor("#F9A825")) // yellow
            else -> tvStatus.setTextColor(android.graphics.Color.parseColor("#C62828")) // red
        }

        // Edit button
        btnEdit.setOnClickListener {
            showEditDialog(reminder, docId, position)
        }

        // Take Medicine button
        btnTake.setOnClickListener {
            firestore.collection("users").document(userId)
                .collection("reminders").document(docId)
                .update("status", "complete")
                .addOnSuccessListener {
                    Toast.makeText(context, "Marked as Complete ✅", Toast.LENGTH_SHORT).show()
                    reminder["status"] = "complete"
                    notifyDataSetChanged()
                }
        }

        return view
    }

    private fun showEditDialog(reminder: MutableMap<String, Any>, docId: String, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_reminder, null)

        val etName = dialogView.findViewById<EditText>(R.id.etMedicineName)
        val etDosage = dialogView.findViewById<EditText>(R.id.etDosage)
        val btnPickTime = dialogView.findViewById<Button>(R.id.btnPickTime)
        val tvPickedTime = dialogView.findViewById<TextView>(R.id.tvPickedTime)

        etName.setText(reminder["medicineName"].toString())
        etDosage.setText(reminder["dosage"].toString())
        tvPickedTime.text = reminder["time"].toString()

        btnPickTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(context, { _, hour, minute ->
                val formattedTime = String.format("%02d:%02d", hour, minute)
                tvPickedTime.text = formattedTime
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        AlertDialog.Builder(context)
            .setTitle("Edit Reminder")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedData = mapOf(
                    "medicineName" to etName.text.toString(),
                    "dosage" to etDosage.text.toString(),
                    "time" to tvPickedTime.text.toString()
                )

                firestore.collection("users").document(userId)
                    .collection("reminders").document(docId)
                    .update(updatedData)
                    .addOnSuccessListener {
                        reminder["medicineName"] = etName.text.toString()
                        reminder["dosage"] = etDosage.text.toString()
                        reminder["time"] = tvPickedTime.text.toString()

                        notifyDataSetChanged()
                        Toast.makeText(context, "Reminder Updated ✅", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
