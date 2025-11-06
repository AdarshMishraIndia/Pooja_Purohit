package com.poojapurohit.dashboard

import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.poojapurohit.R

class InfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        val tvPageTitle = findViewById<TextView>(R.id.tvPageTitle)
        val tvContent = findViewById<TextView>(R.id.tvContent)
        val toolbar = findViewById<Toolbar>(R.id.toolbarInfo)

        // Get data from intent
        val title = intent.getStringExtra("title") ?: "Information"
        val content = intent.getStringExtra("content") ?: "No content available"

        tvPageTitle.text = title
        tvContent.text = content

        // Back button (navigation icon) functionality
        toolbar.setNavigationOnClickListener {
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
