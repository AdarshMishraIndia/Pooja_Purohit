package com.poojapurohit.dashboard

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.poojapurohit.R

class InfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        val tvPageTitle = findViewById<TextView>(R.id.tvPageTitle)
        val tvContent = findViewById<TextView>(R.id.tvContent)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Get data from intent
        val title = intent.getStringExtra("title") ?: "Information"
        val content = intent.getStringExtra("content") ?: "No content available"

        tvPageTitle.text = title
        tvContent.text = content

        // Back button functionality
        btnBack.setOnClickListener {
            finish()
        }

        // Setup OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }
}
