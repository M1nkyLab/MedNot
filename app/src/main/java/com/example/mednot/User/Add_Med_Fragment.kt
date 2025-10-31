package com.example.mednot.User

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.parentFragmentManager
import com.example.mednot.AlarmReceiver
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class Add_Med_Fragment : Fragment() {

    // Declare Firebase
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Declare all UI components
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

    // To store the selected time in 24-hour format
    private var selectedHour: Int = -1
    private var selectedMinute: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_add_medicine_fragment, container, false)
        initializeComponents(view)
        setupSpinners()
        setupListeners()
        return view
    }

    private fun initializeComponents(view: View) {
        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Link all variables with their IDs
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
    }

    private fun setupSpinners() {
        ArrayAdapter.createFromResource(requireContext(), R.array.dosage_units, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDosageUnit.adapter = it
        }
        ArrayAdapter.createFromResource(requireContext(), R.array.medication_types, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerMedicationType.adapter = it
        }
        ArrayAdapter.createFromResource(requireContext(), R.array.eat_time_options, android.R.layout.simple_spinner_item).also {
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
        btnSaveMedicine.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                saveMedicineAndScheduleAlarms()
            } else {
                Toast.makeText(requireContext(), "Your Android version is too old to support this feature.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showTimePickerDialog() {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, hourOfDay, minuteOfHour ->
            // Store the 24-hour time for accurate alarm scheduling
            selectedHour = hourOfDay
            selectedMinute = minuteOfHour

            // Format and display the time in 12-hour AM/PM format
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minuteOfHour)
            }
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            inputTime.setText(timeFormat.format(calendar.time))
        }, hour, minute, false).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveMedicineAndScheduleAlarms() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "You must be logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val medicineName = inputMedicineName.text.toString().trim()
        val dosageValue = inputDosage.text.toString().trim()
        val dosageUnit = spinnerDosageUnit.selectedItem?.toString() ?: ""
        val fullDosage = "$dosageValue $dosageUnit"

        val selectedMethodId = radioGroupScheduleMethod.checkedRadioButtonId
        val timesPerDay = inputTimesPerDay.text.toString().toIntOrNull() ?: 0
        val intervalHours = inputIntervalHours.text.toString().toIntOrNull() ?: 0

        if (medicineName.isEmpty()) {
            inputMedicineName.error = "Medicine name is required"
            return
        }
        if (selectedHour == -1 || selectedMinute == -1) {
            inputTime.error = "Start time is required"
            Toast.makeText(requireContext(), "Please select a start time.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- SCHEDULE ALARMS ---
        val alarmSchedules = mutableListOf<Calendar>()
        if (selectedMethodId == R.id.radioFrequency) {
            if (timesPerDay > 0) {
                val interval = 24 / timesPerDay
                for (i in 0 until timesPerDay) {
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, selectedHour)
                        set(Calendar.MINUTE, selectedMinute)
                        set(Calendar.SECOND, 0)
                        add(Calendar.HOUR_OF_DAY, i * interval)
                    }
                    alarmSchedules.add(calendar)
                }
            }
        } else { // Interval method
            if (intervalHours > 0) {
                val times = 24 / intervalHours
                for (i in 0 until times) {
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, selectedHour)
                        set(Calendar.MINUTE, selectedMinute)
                        set(Calendar.SECOND, 0)
                        add(Calendar.HOUR_OF_DAY, i * intervalHours)
                    }
                    alarmSchedules.add(calendar)
                }
            }
        }

        // Schedule all calculated alarms
        for (schedule in alarmSchedules) {
            scheduleAlarm(schedule, medicineName, fullDosage)
        }
        // --- END OF ALARM SCHEDULING ---

        saveDataToFirestore(uid, medicineName, fullDosage)
    }

    private fun saveDataToFirestore(uid: String, medicineName: String, fullDosage: String) {
        val medData = hashMapOf(
            "userId" to uid,
            "medicineName" to medicineName,
            "dosage" to fullDosage,
            "medicationType" to (spinnerMedicationType.selectedItem?.toString() ?: ""),
            "instruction" to (spinnerEatTime.selectedItem?.toString() ?: ""),
            "startTime" to inputTime.text.toString().trim(),
            "duration" to (inputDuration.text.toString().trim().toIntOrNull() ?: 0).toLong(),
            "stock" to (inputStock.text.toString().trim().toIntOrNull() ?: 0).toLong(),
            "status" to "Upcoming" // Default status
        )

        // Save to user's reminder sub-collection for easier querying
        db.collection("users").document(uid).collection("reminders")
            .add(medData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Medicine saved and reminders set!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error saving data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun scheduleAlarm(calendar: Calendar, medicineName: String, dosage: String) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(requireContext(), "Permission needed to set reminders.", Toast.LENGTH_LONG).show()
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also { startActivity(it) }
            return
        }

        // If the time is in the past for today, set it for the next day
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.MEDICINE_NAME, medicineName)
            putExtra(AlarmReceiver.MEDICINE_DOSAGE, dosage)
            // A unique ID for the notification itself
            putExtra(AlarmReceiver.NOTIFICATION_ID, Random.nextInt())
        }

        // A unique request code for the PendingIntent is crucial
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set a repeating alarm that fires daily at the specified time
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
