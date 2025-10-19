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
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_home_fragment, container, false)
        welcomeMessage = view.findViewById(R.id.Welcomemessage)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
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
        } else {
            welcomeMessage.text = "Welcome, Guest"
        }

        return view
    }
}
