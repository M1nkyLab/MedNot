package com.example.mednot // Make sure this package is correct

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import kotlin.math.roundToInt

class Add_Medicine : AppCompatActivity() {

    // UI Elements
    private lateinit var inputMedicineName: EditText
    private lateinit var inputDosage: EditText
    private lateinit var spinnerDosageUnit: Spinner
    private lateinit var spinnerMedicationType: Spinner
    private lateinit var spinnerEatTime: Spinner
    private lateinit var radioGroupScheduleMethod: RadioGroup
    private lateinit var layoutFrequency: LinearLayout
    private lateinit var layoutInterval: LinearLayout
    private lateinit var inputTimesPerDay: EditText
    private lateinit var inputIntervalHours: EditText
    private lateinit var inputTime: EditText
    private lateinit var inputDuration: EditText
    private lateinit var inputStock: EditText
    private lateinit var btnSaveMedicine: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_medicine)

        initializeUi()
        setupSpinners()
        setupListeners()
    }

    private fun initializeUi() {
        inputMedicineName = findViewById(R.id.inputMedicineName)
        inputDosage = findViewById(R.id.inputDosage)
        spinnerDosageUnit = findViewById(R.id.spinnerDosageUnit)
        spinnerMedicationType = findViewById(R.id.spinnerMedicationType)
        spinnerEatTime = findViewById(R.id.spinnerEatTime)
        radioGroupScheduleMethod = findViewById(R.id.radioGroupScheduleMethod)
        layoutFrequency = findViewById(R.id.layoutFrequency)
        layoutInterval = findViewById(R.id.layoutInterval)
        inputTimesPerDay = findViewById(R.id.inputTimesPerDay)
        inputIntervalHours = findViewById(R.id.inputIntervalHours)
        inputTime = findViewById(R.id.inputTime)
        inputDuration = findViewById(R.id.inputDuration)
        inputStock = findViewById(R.id.inputStock)
        btnSaveMedicine = findViewById(R.id.btnSaveMedicine)
    }

    private fun setupSpinners() {
        ArrayAdapter.createFromResource(this, R.array.dosage_units, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDosageUnit.adapter = adapter
            }

        ArrayAdapter.createFromResource(this, R.array.medication_types, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerMedicationType.adapter = adapter
            }

        ArrayAdapter.createFromResource(this, R.array.eat_time_options, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerEatTime.adapter = adapter
            }
    }

    private fun setupListeners() {
        inputTime.setOnClickListener { showTimePickerDialog() }
        btnSaveMedicine.setOnClickListener { saveMedicineOffline() }

        radioGroupScheduleMethod.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioFrequency) {
                layoutFrequency.visibility = View.VISIBLE
                layoutInterval.visibility = View.GONE
            } else {
                layoutFrequency.visibility = View.GONE
                layoutInterval.visibility = View.VISIBLE
            }
        }
    }

    private fun showTimePickerDialog() {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val ampm = if (selectedHour < 12) "AM" else "PM"
            val displayHour = if (selectedHour == 0 || selectedHour == 12) 12 else selectedHour % 12
            val timeString = String.format("%02d:%02d %s", displayHour, selectedMinute, ampm)
            inputTime.setText(timeString)
        }, hour, minute, false).show()
    }

    private fun saveMedicineOffline() {
        // --- 1. User isi n kita check ---
        val name = inputMedicineName.text.toString().trim()
        val dosageAmount = inputDosage.text.toString().toDoubleOrNull()
        val startTime = inputTime.text.toString().trim()
        val durationDays = inputDuration.text.toString().toIntOrNull()
        val initialStock = inputStock.text.toString().toIntOrNull()

        if (name.isEmpty() || dosageAmount == null || dosageAmount <= 0 || startTime.isEmpty()) {
            Toast.makeText(this, "Please fill in Name, Dosage, and Start Time.", Toast.LENGTH_SHORT).show()
            return
        }
        if (durationDays == null || durationDays < 0 || initialStock == null || initialStock < 0) {
            Toast.makeText(this, "Please enter valid Duration and Stock (0 or more).", Toast.LENGTH_SHORT).show()
            return
        }

        var timesPerDay: Int
        var intervalHours: Int
        if (radioGroupScheduleMethod.checkedRadioButtonId == R.id.radioFrequency) {
            timesPerDay = inputTimesPerDay.text.toString().toIntOrNull() ?: 0
            if (timesPerDay <= 0) {
                Toast.makeText(this, "Please enter a valid number of times per day.", Toast.LENGTH_SHORT).show()
                return
            }
            intervalHours = (24.0 / timesPerDay).roundToInt()
        } else {
            intervalHours = inputIntervalHours.text.toString().toIntOrNull() ?: 0
            if (intervalHours <= 0) {
                Toast.makeText(this, "Please enter a valid hour interval.", Toast.LENGTH_SHORT).show()
                return
            }
            timesPerDay = (24.0 / intervalHours).roundToInt().coerceAtLeast(1)
        }

        // --- 2. Buat Medicine Object (untuk simpan data ubat-ubat) ---
        val newMedicine = Medicine(
            name = name,
            dosage = "$dosageAmount ${spinnerDosageUnit.selectedItem}",
            type = spinnerMedicationType.selectedItem.toString(),
            eatTime = spinnerEatTime.selectedItem.toString(),
            frequencySummary = "$timesPerDay times per day (~$intervalHours hours apart)",
            startTime = startTime,
            durationSummary = if (durationDays == 0) "Continuous" else "For $durationDays days",
            stock = initialStock
        )

        // --- 3. Save object tadi dalam SharedPreferences ---
        val sharedPrefs = getSharedPreferences("MedicineData", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPrefs.getString("medicine_list", null)
        val type = object : TypeToken<MutableList<Medicine>>() {}.type

        // Load data atau buat baru
        val medicineList: MutableList<Medicine> = if (json != null) {
            gson.fromJson(json, type)
        } else {
            mutableListOf()
        }

        medicineList.add(newMedicine) // Tambah objek medicine baru

        val editor = sharedPrefs.edit()
        val updatedJson = gson.toJson(medicineList) // Convert ke string
        editor.putString("medicine_list", updatedJson)
        editor.apply() // Save the changes

        Toast.makeText(this, "'$name' saved!", Toast.LENGTH_LONG).show()

        // --- 4. Hantar toast "Success" signal ke View_Reminders ---
        setResult(Activity.RESULT_OK)

        // --- 5. settle and tutup ---
        finish()
    }
}
