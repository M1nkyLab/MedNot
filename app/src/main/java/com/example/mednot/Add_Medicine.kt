package com.example.mednot

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class Add_Medicine : AppCompatActivity() {

    // declare firebase firestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // declare all functional component
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

        // initialize firebase firestore
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // link all varible with their id (exactly like in xml)
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

        setupSpinners()
        setupListeners()
    }

    // function for dropdown
    private fun setupSpinners() {

        // dropdown for dosage units
        ArrayAdapter.createFromResource(this,
            R.array.dosage_units,
            android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDosageUnit.adapter = it
        }

        //dropdown for medication types
        ArrayAdapter.createFromResource(this,
            R.array.medication_types,
            android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMedicationType.adapter = it
        }

        // dropdown for what time to eat
        ArrayAdapter.createFromResource(this,
            R.array.eat_time_options,
            android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerEatTime.adapter = it
        }
    }

    // function listener
    private fun setupListeners() {
        // will show time picker dialog when clicked
        inputTime.setOnClickListener { showTimePickerDialog() }
        // will listen whether freq or interval clicked
        radioGroupScheduleMethod.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioFrequency) {
                layoutFrequency.visibility = View.VISIBLE
                layoutInterval.visibility = View.GONE
            } else {
                layoutFrequency.visibility = View.GONE
                layoutInterval.visibility = View.VISIBLE
            }
        }

        btnSaveMedicine.setOnClickListener { saveMedicine() }
    }

    // function time picker dialog
    private fun showTimePickerDialog() {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            // using formart 12 hour time
            val ampm = if (selectedHour < 12) "AM" else "PM"
            val displayHour = if (selectedHour == 0 || selectedHour == 12) 12 else selectedHour % 12
            val timeString = String.format("%02d:%02d %s", displayHour, selectedMinute, ampm)
            inputTime.setText(timeString)
        }, hour, minute, false).show()
    }

    // function to medicine data to firestore
    private fun saveMedicine() {
        val uid = firebaseAuth.currentUser?.uid
        // collect all info from all field
        val medicineName = inputMedicineName.text.toString().trim()
        val dosage = inputDosage.text.toString().trim()
        val dosageUnit = spinnerDosageUnit.selectedItem?.toString() ?: ""
        val medicationType = spinnerMedicationType.selectedItem?.toString() ?: ""
        val instruction = spinnerEatTime.selectedItem?.toString() ?: ""
        val selectedMethodId = radioGroupScheduleMethod.checkedRadioButtonId
        val scheduleMethod = if (selectedMethodId == R.id.radioFrequency) "Frequency" else "Interval"
        val timesPerDay = inputTimesPerDay.text.toString().trim()
        val intervalHours = inputIntervalHours.text.toString().trim()
        val startTime = inputTime.text.toString().trim()
        val duration = inputDuration.text.toString().trim()
        val stock = inputStock.text.toString().trim()
        // just too make sure the name isnt empty
        if (medicineName.isEmpty()) {
            inputMedicineName.error = "Please enter medicine name"
            return
        }

        // attributes of all data
        val medData = hashMapOf(
            "userId" to uid,
            "medicineName" to medicineName,
            "dosage" to dosage,
            "dosageUnit" to dosageUnit,
            "medicationType" to medicationType,
            "instruction" to instruction,
            "scheduleMethod" to scheduleMethod,
            "timesPerDay" to timesPerDay,
            "intervalHours" to intervalHours,
            "startTime" to startTime,
            "durationDays" to duration,
            "stock" to stock,
            "timestamp" to System.currentTimeMillis()
        )

        // save data to firestore
        // Class = "medicines"
        db.collection("medicines")
            .add(medData)
            .addOnSuccessListener {
                Toast.makeText(this, "Medicine saved successfully!", Toast.LENGTH_SHORT).show()
                clearInputs()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // function to clear all field after success save
    private fun clearInputs() {
        inputMedicineName.text.clear()
        inputDosage.text.clear()
        inputTimesPerDay.text.clear()
        inputIntervalHours.text.clear()
        inputTime.text.clear()
        inputDuration.text.clear()
        inputStock.text.clear()
    }
}
