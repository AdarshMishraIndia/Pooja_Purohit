package com.poojapurohit.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.poojapurohit.R
import com.poojapurohit.auth.AuthActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class DashActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var tvWelcome: TextView
    private lateinit var recyclerServices: RecyclerView
    private lateinit var servicesAdapter: DashboardAdapter
    private lateinit var tvContactNum: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initViews()
        setupToolbar()
        setupNavigationDrawer()
        setupServicesRecycler()
        loadUserProfile()
        setupBackPressedHandler()
        setupContactNumberClick()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        toolbar = findViewById(R.id.toolbar)
        tvWelcome = findViewById(R.id.tvWelcome)
        recyclerServices = findViewById(R.id.recyclerServices)
        tvContactNum = findViewById(R.id.tvContactNum)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_edit_account -> handleEditAccount()
            R.id.nav_about_us -> handleAboutUs()
            R.id.nav_terms_conditions -> handleTermsConditions()
            R.id.nav_sign_out -> handleSignOut()
            R.id.nav_delete_account -> handleDeleteAccount()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setupServicesRecycler() {
        val dummyServices = listOf(
            ServiceItem("Book a Purohit", "Search and book a Purohit near your area.", R.drawable.ic_service_placeholder_book),
            ServiceItem("Horoscope\n(ଜାତକ)", "Get your horoscope in Odia/South style.", R.drawable.ic_service_placeholder_horoscope),
            ServiceItem("Match Making\n(ବିବାହ ମେଳକ)", "Match Horoscope before planning marriage.", R.drawable.ic_service_placeholder_matchmaking)
        )
        
        servicesAdapter = DashboardAdapter(dummyServices) { service ->
            // Handle item click here
            Toast.makeText(this, "Selected: ${service.name}", Toast.LENGTH_SHORT).show()
        }
        
        recyclerServices.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@DashActivity)
            adapter = servicesAdapter
            setHasFixedSize(true)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val document = firestore.collection("users").document(uid).get().await()
                    val userName = document.getString("name") ?: "User"
                    val userEmail = document.getString("email") ?: auth.currentUser?.email ?: ""
                    
                    withContext(Dispatchers.Main) {
                        // Get header views
                        val headerView = navigationView.getHeaderView(0)
                        val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName)
                        val tvUserEmail = headerView.findViewById<TextView>(R.id.tvUserEmail)
                        
                        // Update UI
                        tvUserName.text = userName
                        tvUserEmail.text = userEmail
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

    private fun setupContactNumberClick() {
    tvContactNum.setOnClickListener {
        val phoneNumber = 9438245904
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:$phoneNumber".toUri()
        }
        startActivity(intent)
    }
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
