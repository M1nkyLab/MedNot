package com.example.mednot

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class Add_Medicine : AppCompatActivity() {

    private lateinit var inputMedicineName: EditText
    private lateinit var inputDosage: EditText
    private lateinit var inputTime: EditText
    private lateinit var spinnerEatTime: Spinner
    private lateinit var btnSaveMedicine: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_medicine)

        inputMedicineName = findViewById(R.id.inputMedicineName)
        inputDosage = findViewById(R.id.inputDosage)
        inputTime = findViewById(R.id.inputTime)
        spinnerEatTime = findViewById(R.id.spinnerEatTime)
        btnSaveMedicine = findViewById(R.id.btnSaveMedicine)

        // Spinner setup (Before / After Eat)
        val eatOptions = arrayOf("Before Eat", "After Eat")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, eatOptions)
        spinnerEatTime.adapter = adapter

        // Time Picker punya function
        inputTime.setOnClickListener {
            showTimePickerDialog()
        }

        // Save button mock ii
        btnSaveMedicine.setOnClickListener {
            saveMedicine()
        }
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                inputTime.setText(formattedTime)
            },
            hour,
            minute,
            true
        )
        timePicker.show()
    }

    // save medicine punya funtion mock
    private fun saveMedicine() {
        val name = inputMedicineName.text.toString().trim()
        val dosage = inputDosage.text.toString().trim()
        val time = inputTime.text.toString().trim()
        val eatTime = spinnerEatTime.selectedItem.toString()

        if (name.isEmpty() || dosage.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // untuk sekarang x show kt database lgi for this mock
        val message = "Medicine saved:\n$name - $dosage\n$eatTime at $time"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // input akan clear bila kita click button save
        inputMedicineName.text.clear()
        inputDosage.text.clear()
        inputTime.text.clear()
        spinnerEatTime.setSelection(0)
    }
}
