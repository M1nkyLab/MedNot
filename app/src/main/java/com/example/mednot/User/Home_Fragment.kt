package com.example.mednot.User

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Home_Fragment : Fragment() {

    private lateinit var welcomeMessage: TextView
    private lateinit var lowStockCount: TextView
    private lateinit var todayMedCount: TextView
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_home_fragment, container, false)
        welcomeMessage = view.findViewById(R.id.Welcomemessage)
        lowStockCount = view.findViewById(R.id.lowStockCount)
        todayMedCount = view.findViewById(R.id.todayMedCount)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid

            // ✅ Get user name
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("name") ?: "User"
                        welcomeMessage.text = "Welcome, $username"
                    } else {
                        welcomeMessage.text = "Welcome, User"
                    }
                }
                .addOnFailureListener {
                    welcomeMessage.text = "Welcome, User"
                }

            // ✅ Get medicines for this user
            firestore.collection("medicines")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener { result ->
                    val lowStockList = mutableListOf<String>()

                    for (doc in result) {
                        val name = doc.getString("medicineName") ?: "Unnamed"
                        val stockString = doc.getString("stock") ?: "0"
                        val stock = stockString.toIntOrNull() ?: 0

                        // Example threshold: less than or equal to 10 means "low stock"
                        if (stock <= 10) {
                            lowStockList.add(name)
                        }
                    }

                    if (lowStockList.isNotEmpty()) {
                        lowStockCount.text = "Low stock:\n${lowStockList.joinToString("\n")}"
                    } else {
                        lowStockCount.text = "All medicines are well stocked."
                    }
                }
                .addOnFailureListener {
                    lowStockCount.text = "Error loading medicines."
                }

        } else {
            welcomeMessage.text = "Welcome, Guest"
            lowStockCount.text = "Login to see your medicine stock."
        }

        return view
    }
}
