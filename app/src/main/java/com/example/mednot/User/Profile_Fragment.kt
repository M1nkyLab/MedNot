package com.example.mednot.User

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mednot.Auth.Auth_Login
import com.example.mednot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User

class Profile_Fragment : Fragment() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var btnCheckStock: Button
    private lateinit var btnViewReminders: Button
    private lateinit var btnSignOut: Button
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_profile_fragment, container, false)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        btnViewReminders = view.findViewById(R.id.btnViewReminders)
        btnCheckStock = view.findViewById(R.id.btnCheckStock)
        btnSignOut = view.findViewById(R.id.btnSignOut)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            tvUserEmail.text = currentUser.email

            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name") ?: "User"
                    tvUserName.text = name
                }
        }

        btnViewReminders.setOnClickListener {
            val intent = Intent(requireContext(), User_View_Reminders::class.java)
            startActivity(intent)
        }

        btnCheckStock.setOnClickListener {
            val intent = Intent(requireContext(), Check_Med_Stock::class.java)
            startActivity(intent)
        }

        btnSignOut.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), Auth_Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
