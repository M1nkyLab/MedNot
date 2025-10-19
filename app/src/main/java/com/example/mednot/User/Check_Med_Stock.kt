package com.example.mednot.User

import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.SimpleAdapter

class Check_Med_Stock : AppCompatActivity() {

    private lateinit var stockListView: ListView
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_check_med_stock)

        stockListView = findViewById(R.id.stockListView)

        val uid = auth.currentUser?.uid ?: return
        val stockList = ArrayList<HashMap<String, String>>()

        firestore.collection("users")
            .document(uid)
            .collection("medicines")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val name = doc.getString("name") ?: "Unknown"
                    val stock = doc.getLong("stock")?.toInt() ?: 0

                    val map = HashMap<String, String>()
                    map["name"] = name
                    map["stock"] = "Stock: $stock"

                    stockList.add(map)
                }

                val adapter = SimpleAdapter(
                    this,
                    stockList,
                    R.layout.item_check_med_stock,
                    arrayOf("name", "stock"),
                    intArrayOf(R.id.tvMedName, R.id.tvMedStock)
                )

                stockListView.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
    }
}
