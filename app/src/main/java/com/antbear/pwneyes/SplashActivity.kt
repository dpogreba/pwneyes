package com.antbear.pwneyes

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // Ensure `activity_splash.xml` exists

        // Navigate to MainActivity after splash screen
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
