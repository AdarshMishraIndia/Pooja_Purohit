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

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // Delay 1 second then navigate to AuthActivity
        lifecycleScope.launch {
            delay(2000)
            startActivity(
                Intent(
                    this@SplashActivity,
                    com.poojapurohit.auth.AuthActivity::class.java
                )
            )
            finish()
        }
    }
}
