package com.example.mednot.User

import android.os.Bundle
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Check_Med_Stock : AppCompatActivity() {

    private lateinit var stockListView: ListView
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This layout should be for the entire screen, not a single item
        setContentView(R.layout.user_check_med_stock)

        stockListView = findViewById(R.id.stockListView)

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Please log in to see stock.", Toast.LENGTH_SHORT).show()
            return
        }

        val stockList = ArrayList<HashMap<String, String>>()

        // Query the top-level 'medicines' collection and filter by the current user's ID
        firestore.collection("medicines")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "You have no medicines added.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    // Use the correct field names from your Firestore documents
                    val name = doc.getString("inputMedicineName") ?: "Unknown Name"
                    val stock = doc.getLong("inputStock")?.toInt() ?: 0

                    val map = HashMap<String, String>()
                    map["name"] = name
                    map["stock"] = "Stock: $stock"
                    stockList.add(map)
                }

                // This adapter maps the data to the views in 'item_check_med_stock.xml'
                val adapter = SimpleAdapter(
                    this,
                    stockList,
                    R.layout.item_check_med_stock, // The layout for one row
                    arrayOf("name", "stock"),
                    intArrayOf(R.id.tvMedName, R.id.tvMedStock) // The IDs inside that row layout
                )

                stockListView.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load stock data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
