package com.example.mednot

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class View_Reminders : AppCompatActivity() {

    private lateinit var btnSaveMedicine: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_reminders)
    }
}