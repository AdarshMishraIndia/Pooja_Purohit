package com.poojapurohit

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.poojapurohit.auth.AuthRepository
import com.poojapurohit.auth.compose.AuthActivity
import com.poojapurohit.dashboard.DashActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // Check login session during 2-second splash delay
        lifecycleScope.launch {
            delay(2000) // Keep the 2-second splash delay

            try {
                val isUserRegistered = authRepository.isUserRegistered()
                val targetActivity = if (isUserRegistered) {
                    DashActivity::class.java
                } else {
                    AuthActivity::class.java
                }

                startActivity(Intent(this@SplashActivity, targetActivity))
                finish()
            } catch (_: Exception) {
                // If auth check fails, go to AuthActivity
                startActivity(Intent(this@SplashActivity, AuthActivity::class.java))
                finish()
            }
        }
    }
}