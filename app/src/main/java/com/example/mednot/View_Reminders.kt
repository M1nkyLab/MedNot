package com.example.mednot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


data class Medicine(
    val name: String,
    val dosage: String,
    val type: String,
    val eatTime: String,
    val frequencySummary: String,
    val startTime: String,
    val durationSummary: String,
    val stock: Int
)

class View_Reminders : AppCompatActivity() {

    private lateinit var reminderListView: ListView
    private lateinit var btnGoToAddMedicine: Button
    private lateinit var adapter: ArrayAdapter<String>
    private val medicineList = mutableListOf<Medicine>()

    private val ADD_MEDICINE_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_reminders)

        reminderListView = findViewById(R.id.reminderListView)
        btnGoToAddMedicine = findViewById(R.id.btnAddMedicine)

        loadAndDisplayReminders()

        btnGoToAddMedicine.setOnClickListener {
            val intent = Intent(this, Add_Medicine::class.java)
            // Start the activity and tell Android we expect a result back
            startActivityForResult(intent, ADD_MEDICINE_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_MEDICINE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Refreshing list...", Toast.LENGTH_SHORT).show()
            loadAndDisplayReminders()
        }
    }


    private fun loadAndDisplayReminders() {
        val sharedPrefs = getSharedPreferences("MedicineData", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPrefs.getString("medicine_list", null)
        val type = object : TypeToken<MutableList<Medicine>>() {}.type


        medicineList.clear()

        // cari data & tunjuk
        if (json != null) {
            val savedMedicines: MutableList<Medicine> = gson.fromJson(json, type)
            medicineList.addAll(savedMedicines)
        }

        // String untuk flex
        val displayList = medicineList.map { medicine ->
            "${medicine.name} - ${medicine.dosage}\nTaken ${medicine.eatTime}"
        }

        // ini yg hantar string ke flex
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        reminderListView.adapter = adapter
    }
}
