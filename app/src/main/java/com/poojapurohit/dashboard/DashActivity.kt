package com.poojapurohit.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationDrawer: ListView
    private lateinit var tvWelcome: TextView
    private lateinit var recyclerServices: RecyclerView
    private lateinit var servicesAdapter: ServicesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initViews()
        setupNavigationDrawer()
        setupServicesRecycler()
        loadUserProfile()
        setupBackPressedHandler()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationDrawer = findViewById(R.id.navigationDrawer)
        tvWelcome = findViewById(R.id.tvWelcome)
        recyclerServices = findViewById(R.id.recyclerServices)
        
        val btnHamburger = findViewById<ImageButton>(R.id.btnHamburger)
        btnHamburger.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupNavigationDrawer() {
        val navItems = arrayOf(
            "Edit Account",
            "About Us", 
            "Terms & Conditions",
            "Sign Out",
            "Delete Account"
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, navItems)
        navigationDrawer.adapter = adapter
        
        navigationDrawer.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> handleEditAccount()
                1 -> handleAboutUs()
                2 -> handleTermsConditions()
                3 -> handleSignOut()
                4 -> handleDeleteAccount()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun setupServicesRecycler() {
        val dummyServices = listOf(
            ServiceItem("Puja Services", "Traditional puja ceremonies", R.drawable.ic_service_placeholder),
            ServiceItem("Wedding Rituals", "Complete wedding ceremonies", R.drawable.ic_service_placeholder),
            ServiceItem("Housewarming", "Griha pravesh ceremonies", R.drawable.ic_service_placeholder),
            ServiceItem("Festival Pujas", "Special festival rituals", R.drawable.ic_service_placeholder),
            ServiceItem("Astrology", "Horoscope consultation", R.drawable.ic_service_placeholder),
            ServiceItem("Spiritual Guidance", "Personal spiritual advice", R.drawable.ic_service_placeholder)
        )
        
        servicesAdapter = ServicesAdapter(dummyServices)
        recyclerServices.layoutManager = GridLayoutManager(this, 2)
        recyclerServices.adapter = servicesAdapter
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val document = firestore.collection("users").document(uid).get().await()
                    val userName = document.getString("name") ?: "User"
                    
                    withContext(Dispatchers.Main) {
                        tvWelcome.text = "Welcome, $userName!"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        tvWelcome.text = "Welcome!"
                    }
                }
            }
        }
    }

    private fun handleEditAccount() {
        Toast.makeText(this, "Edit Account clicked", Toast.LENGTH_SHORT).show()
    }

    private fun handleAboutUs() {
        val intent = Intent(this, InfoActivity::class.java)
        intent.putExtra("title", "About Us")
        intent.putExtra("content", "Pooja Purohit is your trusted platform for connecting with qualified religious service providers. We help you find experienced Purohits for all your spiritual and ceremonial needs.")
        startActivity(intent)
    }

    private fun handleTermsConditions() {
        val intent = Intent(this, InfoActivity::class.java)
        intent.putExtra("title", "Terms & Conditions")
        intent.putExtra("content", "By using Pooja Purohit, you agree to our terms of service. Please use our platform responsibly and respect all service providers and customers.")
        startActivity(intent)
    }

    private fun handleSignOut() {
        auth.signOut()
        navigateToAuth()
    }

    private fun handleDeleteAccount() {
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

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    finish()
                }
            }
        })
    }
}
