package com.poojapurohit

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.poojapurohit.auth.AuthRepository
import com.poojapurohit.auth.compose.AuthActivity
import com.poojapurohit.dashboard.DashActivity
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SplashScreen(
                onNavigate = { isUserRegistered ->
                    val targetActivity = if (isUserRegistered) {
                        DashActivity::class.java
                    } else {
                        AuthActivity::class.java
                    }
                    startActivity(Intent(this, targetActivity))
                    finish()
                }
            )
        }
    }

    @Composable
    private fun SplashScreen(onNavigate: (Boolean) -> Unit) {
        val composition = rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.splash_bg_animation)
        )

        LaunchedEffect(Unit) {
            delay(2000)
            try {
                val isUserRegistered = authRepository.isUserRegistered()
                onNavigate(isUserRegistered)
            } catch (_: Exception) {
                onNavigate(false)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(id = R.color.ic_launcher_background))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = composition.value,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.fillMaxSize()
            )

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = null
            )
        }
    }
}