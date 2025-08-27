package com.poojapurohit.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.poojapurohit.R
import com.poojapurohit.auth.AuthActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class DashActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dash)

        Toast.makeText(this, "Welcome", Toast.LENGTH_SHORT).show()

        // --- Sign Out ---
        val btnSignOut = findViewById<Button>(R.id.btnSignOut)
        btnSignOut.setOnClickListener {
            auth.signOut()
            navigateToAuth()
        }

        // --- Delete Account ---
        val btnDeleteAccount = findViewById<Button>(R.id.btnDeleteAccount)
        btnDeleteAccount.setOnClickListener {
            val user = auth.currentUser
            val uid = user?.uid

            if (uid != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // 1. Delete Firestore document
                        firestore.collection("users").document(uid).delete().await()

                        // 2. Delete FirebaseAuth user
                        user.delete().await()

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@DashActivity, "Account deleted", Toast.LENGTH_SHORT).show()
                            navigateToAuth()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@DashActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
