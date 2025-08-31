package com.poojapurohit.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.poojapurohit.R
import com.poojapurohit.auth.adapter.ServicesAdapter
import com.poojapurohit.dashboard.DashActivity
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager
    private lateinit var uiManager: AuthUiManager

    // UI Elements
    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etLoc: EditText
    private lateinit var etExperience: EditText
    private lateinit var btnNext: Button
    private lateinit var btnRegister: Button
    private lateinit var tvRegisterHere: TextView
    private lateinit var googleAuthButton: View
    private lateinit var rvServices: RecyclerView
    private lateinit var servicesAdapter: ServicesAdapter
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        credentialManager = CredentialManager.create(this)

        initializeViews()
        observeViewModel()

        lifecycleScope.launch { viewModel.checkIfUserSignedIn() }

        googleAuthButton.setOnClickListener {
            uiManager.isServicePartnerFlow = false
            viewModel.signInWithGoogle(
                activity = this,
                credentialManager = credentialManager,
                clientId = getString(R.string.google_client_id),
                isServicePartner = false
            )
        }

        tvRegisterHere.setOnClickListener {
            uiManager.isServicePartnerFlow = true
            viewModel.signInWithGoogle(
                activity = this,
                credentialManager = credentialManager,
                clientId = getString(R.string.google_client_id),
                isServicePartner = true
            )
        }

        // Back handling
        onBackPressedDispatcher.addCallback(this) {
            val handled = uiManager.goBackToPreviousStep(viewModel.currentStep)
            if (handled) {
                viewModel.currentStep--
            } else {
                finish()
            }
        }
    }

    private fun initializeViews() {
        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        etLoc = findViewById(R.id.etLoc)
        etExperience = findViewById(R.id.etExperience)
        btnNext = findViewById(R.id.btnNext)
        btnRegister = findViewById(R.id.btnRegister)
        tvRegisterHere = findViewById(R.id.tvRegisterHere)
        googleAuthButton = findViewById(R.id.google_auth_button)
        rvServices = findViewById(R.id.recyclerSpecialization)

        setupRecyclerView()
        setupUiManager()
    }

    private fun setupRecyclerView() {
        rvServices.layoutManager = LinearLayoutManager(this)
        servicesAdapter = ServicesAdapter { service, isSelected ->
            val current = viewModel.formData.services
            viewModel.formData.services = if (isSelected) current + service else current - service
        }
        rvServices.adapter = servicesAdapter
    }

    private fun setupUiManager() {
        uiManager = AuthUiManager(
            tvWelcome = findViewById(R.id.tvWelcome),
            tvNameLabel = findViewById(R.id.tvNameLabel),
            tvPhoneLabel = findViewById(R.id.tvPhoneLabel),
            tvLoc = findViewById(R.id.tvLoc),
            tvSpec = findViewById(R.id.tvSpec),
            tvExperienceLabel = findViewById(R.id.tvExperience),
            etName = etName,
            etPhone = etPhone,
            etLoc = etLoc,
            etExperience = etExperience,
            btnNext = btnNext,
            btnRegister = btnRegister,
            line1 = findViewById(R.id.line1),
            line2 = findViewById(R.id.line2),
            googleAuthButton = googleAuthButton,
            tvAreYouPurohit = findViewById(R.id.tvAreYouPurohit),
            tvRegisterHere = tvRegisterHere,
            rvServices = rvServices,
            onNextClickListener = { handleNextClick() },
            onRegisterClickListener = { handleRegisterClick() }
        )
        uiManager.showInitialState()
    }

    private fun handleNextClick() {
        // If step is 0, it means we're just starting the flow
        val currentStep = if (viewModel.currentStep == 0) 1 else viewModel.currentStep
        
        when (currentStep) {
            1 -> {
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                
                // Use AuthFormValidator for validation
                val error = AuthFormValidator().validateNameAndPhone(name, phone)
                if (error != null) {
                    Toast.makeText(this@AuthActivity, error, Toast.LENGTH_SHORT).show()
                    return
                }
                
                viewModel.formData.name = name
                viewModel.formData.phone = phone
                viewModel.nextStep(name = name, phone = phone)
            }
            2 -> {
                val location = etLoc.text.toString().trim()
                
                // Use AuthFormValidator for location validation
                val error = AuthFormValidator().validateLocation(location)
                if (error != null) {
                    Toast.makeText(this@AuthActivity, error, Toast.LENGTH_SHORT).show()
                    return
                }
                
                viewModel.formData.location = location
                viewModel.nextStep(location = location)
            }
            else -> {
                // If we hit an unexpected step, try to recover by moving to step 1
                viewModel.currentStep = 1
            }
        }
    }

    private fun handleRegisterClick() {
        viewModel.formData.experience = etExperience.text.toString().trim()

        if (uiManager.isServicePartnerFlow) {
            viewModel.registerServicePartner(
                experience = viewModel.formData.experience,
                services = viewModel.formData.services
            )
        } else {
            // Update form data with current EditText values for customer registration
            viewModel.formData.name = etName.text.toString().trim()
            viewModel.formData.phone = etPhone.text.toString().trim()
            viewModel.registerUser()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is AuthUiState.Error -> {
                    hideLoading()
                    Toast.makeText(this@AuthActivity, state.message, Toast.LENGTH_LONG).show()
                }
                is AuthUiState.Loading -> {
                    showLoading()
                }
                is AuthUiState.Success -> {
                    hideLoading()
                    startActivity(Intent(this, DashActivity::class.java))
                    finish()
                }
                is AuthUiState.ShowInitialState -> {
                    uiManager.showInitialState()
                }
                is AuthUiState.ShowCustomerFields -> {
                    hideLoading()
                    uiManager.showCustomerFields()
                }
                is AuthUiState.ShowServicePartnerStep1 -> {
                    hideLoading()
                    uiManager.showServicePartnerStep1()
                }
                is AuthUiState.ShowServicePartnerStep2 -> {
                    hideLoading()
                    uiManager.showServicePartnerStep2()
                }
                is AuthUiState.ShowServicePartnerStep3 -> {
                    hideLoading()
                    servicesAdapter.submitList(state.services)
                    uiManager.showServicePartnerStep3()
                }
                AuthUiState.Idle -> {
                    hideLoading()
                }
            }
        }
    }

    private fun showLoading() {
        // Prevent multiple dialogs
        if (loadingDialog?.isShowing == true) return

        // Inflate custom loading layout
        val dialogView = layoutInflater.inflate(R.layout.layout_loading, null)

        // Start Lottie animation if present
        val animationView = dialogView.findViewById<LottieAnimationView>(R.id.loading_animation)
        animationView?.playAnimation()

        // Build AlertDialog
        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Transparent background
        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Show dialog
        loadingDialog?.show()

        // Set fixed width from dimens
        loadingDialog?.window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun hideLoading() {
        loadingDialog?.let { dialog ->
            // Stop the Lottie animation before dismissing
            val animationView = dialog.findViewById<LottieAnimationView>(R.id.loading_animation)
            animationView?.cancelAnimation()
            dialog.dismiss()
        }
        loadingDialog = null
    }
}
