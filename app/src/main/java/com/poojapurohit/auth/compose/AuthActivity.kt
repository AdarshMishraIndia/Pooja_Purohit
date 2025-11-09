package com.poojapurohit.auth.compose

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.poojapurohit.auth.compose.presentation.AuthViewModel
import com.poojapurohit.auth.compose.presentation.screens.AuthScreen
import com.poojapurohit.dashboard.DashActivity
import com.poojapurohit.ui.theme.PoojaPurohitTheme
import kotlinx.coroutines.launch

class AuthActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already signed in
        lifecycleScope.launch {
            viewModel.checkIfUserSignedIn()
        }

        setContent {
            PoojaPurohitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthScreen(
                        viewModel = viewModel,
                        onNavigateToDashboard = {
                            startActivity(Intent(this, DashActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}