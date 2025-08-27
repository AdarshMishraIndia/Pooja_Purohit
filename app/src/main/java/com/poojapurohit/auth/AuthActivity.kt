package com.poojapurohit.auth

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.poojapurohit.R
import com.poojapurohit.auth.adapter.ServicesAdapter
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager
    private val validator = AuthFormValidator()
    private lateinit var uiManager: AuthUiManager

    // FormData (single source of truth)
    private val formData = AuthFormData()

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

    private var isServicePartnerRegistration = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        credentialManager = CredentialManager.create(this)

        initializeViews()
        observeViewModel()

        lifecycleScope.launch { viewModel.checkIfUserSignedIn() }

        googleAuthButton.setOnClickListener {
            isServicePartnerRegistration = false
            viewModel.signInWithGoogle(
                this,
                credentialManager,
                getString(R.string.google_client_id),
                isServicePartner = false
            )
        }

        tvRegisterHere.setOnClickListener {
            isServicePartnerRegistration = true
            viewModel.signInWithGoogle(
                this,
                credentialManager,
                getString(R.string.google_client_id),
                isServicePartner = true
            )
        }

        // Back handling
        onBackPressedDispatcher.addCallback(this) {
            val handled = uiManager.goBackToPreviousStep()
            if (!handled) finish()
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
            formData.services = if (isSelected) {
                formData.services + service
            } else {
                formData.services - service
            }
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
        when (uiManager.currentStep) {
            1 -> {
                formData.name = etName.text.toString().trim()
                formData.phone = etPhone.text.toString().trim()
                val error = validator.validateNameAndPhone(formData.name, formData.phone)
                if (error != null) {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                } else {
                    uiManager.showServicePartnerStep2()
                }
            }
            2 -> {
                formData.location = etLoc.text.toString().trim()
                val error = validator.validateLocation(formData.location)
                if (error != null) {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                } else {
                    loadServicesAndShowStep3()
                }
            }
        }
    }

    private fun handleRegisterClick() {
        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: return Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show()

        val uid = currentUser.uid
        val email = currentUser.email ?: ""
        formData.experience = etExperience.text.toString().trim()

        if (uiManager.isServicePartnerFlow) {
            val err1 = validator.validateNameAndPhone(formData.name, formData.phone)
            val err2 = validator.validateLocation(formData.location)
            val err3 = validator.validateServices(formData.services)
            val err4 = validator.validateExperience(formData.experience)

            val error = err1 ?: err2 ?: err3 ?: err4
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                return
            }

            viewModel.registerServicePartner(
                uid = uid,
                name = formData.name,
                phone = formData.getFormattedPhone(),
                email = email,
                location = formData.location,
                services = formData.services,
                experience = formData.experience
            )
        } else {
            val error = validator.validateNameAndPhone(formData.name, formData.phone)
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                return
            }

            viewModel.registerUser(
                uid = uid,
                name = formData.name,
                phone = formData.getFormattedPhone(),
                email = email
            )
        }
    }

    private fun loadServicesAndShowStep3() {
        val docRef = FirebaseFirestore.getInstance()
            .collection("services")
            .document("BookAPurohit")

        docRef.get()
            .addOnSuccessListener { doc ->
                val services = (doc?.get("name") as? List<*>)?.filterIsInstance<String>()?.sorted()
                    ?: emptyList()
                servicesAdapter.submitList(services)
                uiManager.showServicePartnerStep3()
            }
            .addOnFailureListener {
                servicesAdapter.submitList(emptyList())
                uiManager.showServicePartnerStep3()
            }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is AuthUiState.Loading -> { /* show loader if needed */ }
                is AuthUiState.Success -> finish()
                is AuthUiState.Error ->
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                is AuthUiState.ShowInitialState -> uiManager.showInitialState()
                is AuthUiState.ShowCustomerFields -> uiManager.showCustomerFields()
                is AuthUiState.ShowServicePartnerStep1 -> uiManager.showServicePartnerStep1()
                is AuthUiState.ShowServicePartnerStep2 -> uiManager.showServicePartnerStep2()
                is AuthUiState.ShowServicePartnerStep3 -> {
                    servicesAdapter.submitList(state.services.sorted())
                    uiManager.showServicePartnerStep3()
                }
                AuthUiState.Idle -> { /* no-op */ }
            }
        }
    }

}
