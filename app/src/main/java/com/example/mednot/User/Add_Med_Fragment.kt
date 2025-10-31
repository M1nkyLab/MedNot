package com.example.mednot

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment // 1. Changed from AppCompatActivity to Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

// 2. Class now inherits from Fragment
class Add_Med_Fragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Declare all view components
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

    // 3. Use onCreateView to setup the fragment's view
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.user_add_medicine_fragment, container, false)

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 4. Link all variables using view.findViewById
        inputMedicineName = view.findViewById(R.id.inputMedicineName)
        inputDosage = view.findViewById(R.id.inputDosage)
        spinnerDosageUnit = view.findViewById(R.id.spinnerDosageUnit)
        spinnerMedicationType = view.findViewById(R.id.spinnerMedicationType)
        spinnerEatTime = view.findViewById(R.id.spinnerEatTime)
        radioGroupScheduleMethod = view.findViewById(R.id.radioGroupScheduleMethod)
        layoutFrequency = view.findViewById(R.id.layoutFrequency)
        layoutInterval = view.findViewById(R.id.layoutInterval)
        inputTimesPerDay = view.findViewById(R.id.inputTimesPerDay)
        inputIntervalHours = view.findViewById(R.id.inputIntervalHours)
        inputTime = view.findViewById(R.id.inputTime)
        inputDuration = view.findViewById(R.id.inputDuration)
        inputStock = view.findViewById(R.id.inputStock)
        btnSaveMedicine = view.findViewById(R.id.btnSaveMedicine)

        setupSpinners()
        setupListeners()

        return view
    }

    private fun setupSpinners() {
        // 5. Use requireContext() instead of 'this' in a Fragment
        ArrayAdapter.createFromResource(
            requireContext(), R.array.dosage_units, android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDosageUnit.adapter = it
        }

        ArrayAdapter.createFromResource(
            requireContext(), R.array.medication_types, android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMedicationType.adapter = it
        }

        ArrayAdapter.createFromResource(
            requireContext(), R.array.eat_time_options, android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerEatTime.adapter = it
        }
    }

    private fun setupListeners() {
        inputTime.setOnClickListener { showTimePickerDialog() }
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

    private fun showTimePickerDialog() {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val ampm = if (selectedHour < 12) "AM" else "PM"
            val displayHour = if (selectedHour == 0 || selectedHour == 12) 12 else selectedHour % 12
            val timeString = String.format("%02d:%02d %s", displayHour, selectedMinute, ampm)
            inputTime.setText(timeString)
        }, hour, minute, false).show()
    }

    private fun saveMedicine() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val medicineName = inputMedicineName.text.toString().trim()
        val dosage = inputDosage.text.toString().trim()
        val dosageUnit = spinnerDosageUnit.selectedItem?.toString() ?: ""
        val medicationType = spinnerMedicationType.selectedItem?.toString() ?: ""
        val instruction = spinnerEatTime.selectedItem?.toString() ?: ""
        val scheduleMethod = if (radioGroupScheduleMethod.checkedRadioButtonId == R.id.radioFrequency) "Frequency" else "Interval"
        val timesPerDay = inputTimesPerDay.text.toString().trim()
        val intervalHours = inputIntervalHours.text.toString().trim()
        val startTime = inputTime.text.toString().trim()
        val duration = inputDuration.text.toString().trim()
        val stock = inputStock.text.toString().trim()

        if (medicineName.isEmpty()) {
            inputMedicineName.error = "Please enter medicine name"
            return
        }

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

        db.collection("medicines")
            .add(medData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Medicine saved successfully!", Toast.LENGTH_SHORT).show()
                clearInputs()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

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
