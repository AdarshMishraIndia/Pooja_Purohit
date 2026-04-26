package com.poojapurohit.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.poojapurohit.R
import com.poojapurohit.auth.compose.AuthActivity
import com.poojapurohit.dashboard.DashActivity
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    // In a production app with Hilt/Dagger, use by viewModels()
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract deep link and notification extras immediately
        val deepLinkData = intent.data
        val intentExtras = intent.extras

        // Trigger the auth check on creation
        if (savedInstanceState == null) {
            viewModel.checkAuthStatus(deepLinkData, intentExtras)
        }

        setContent {
            // collectAsStateWithLifecycle ensures collection stops when app is in background, saving resources
            val uiState by viewModel.splashState.collectAsStateWithLifecycle()

            SplashRoute(
                uiState = uiState,
                onNavigate = { destination, uri, extras ->
                    val targetClass = when (destination) {
                        Destination.DASHBOARD -> DashActivity::class.java
                        Destination.AUTH -> AuthActivity::class.java
                    }

                    val targetIntent = Intent(this, targetClass).apply {
                        // Forward deep links and notification payloads
                        uri?.let { data = it }
                        extras?.let { putExtras(it) }

                        // Clear the activity stack so user can't press back to return to Splash
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(targetIntent)
                    finish()
                },
                onRetry = {
                    viewModel.checkAuthStatus(deepLinkData, intentExtras)
                }
            )
        }
    }
}

@Composable
private fun SplashRoute(
    uiState: SplashState,
    onNavigate: (Destination, Uri?, Bundle?) -> Unit,
    onRetry: () -> Unit
) {
    // Handle Navigation Events
    // LaunchedEffect keys on the specific state. Because the Activity finishes upon
    // navigation, we don't have to worry about re-triggering this on config changes.
    LaunchedEffect(uiState) {
        if (uiState is SplashState.Navigate) {
            // Optional: Ensure the splash screen is visible for a minimum time (e.g., branding)
            // If performance is paramount, remove this delay entirely.
            delay(1000)
            onNavigate(uiState.destination, uiState.deepLink, uiState.extras)
        }
    }

    // UI Rendering
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.ic_launcher_background))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is SplashState.Loading, is SplashState.Navigate -> {
                SplashAnimation()
            }
            is SplashState.Error -> {
                ErrorScreen(
                    message = uiState.message,
                    onRetry = onRetry
                )
            }
        }
    }
}

@Composable
private fun SplashAnimation() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.splash_bg_animation)
    )

    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = Modifier.fillMaxSize()
    )

    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "App Logo"
    )
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}