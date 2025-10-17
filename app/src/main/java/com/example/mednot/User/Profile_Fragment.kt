package com.example.mednot.User

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.mednot.R
import com.example.mednot.Auth.Auth_Login
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Profile_Fragment : Fragment() {

    private lateinit var tvUserName: TextView
    private lateinit var btnLogout: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.user_profile_fragment, container, false)

        tvUserName = view.findViewById(R.id.tvUserName)
        btnLogout = view.findViewById(R.id.btnLogout)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Read user name dari firebase kita
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "User"
                        tvUserName.text = name
                    } else {
                        tvUserName.text = "User"
                    }
                }
                .addOnFailureListener {
                    tvUserName.text = "User"
                }
        }

        // Logout action
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), Auth_Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
