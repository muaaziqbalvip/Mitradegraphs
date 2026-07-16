package com.muslimislam.mitradeanalyzer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etLicenseKey: EditText
    private lateinit var btnActivate: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etLicenseKey = findViewById(R.id.etLicenseKey)
        btnActivate = findViewById(R.id.btnActivate)
        tvStatus = findViewById(R.id.tvLoginStatus)
        progressBar = findViewById(R.id.loginProgress)

        btnActivate.setOnClickListener {
            attemptActivation()
        }
    }

    private fun attemptActivation() {
        val key = etLicenseKey.text.toString().trim()
        if (key.isEmpty()) {
            tvStatus.text = "License key likhna zaroori hai"
            return
        }

        tvStatus.text = ""
        progressBar.visibility = android.view.View.VISIBLE
        btnActivate.isEnabled = false
        btnActivate.text = "Checking..."

        val backendUrl = AppStore.loadBackendUrl(this)

        LicenseClient.verify(this, backendUrl, key) { result ->
            progressBar.visibility = android.view.View.GONE
            btnActivate.isEnabled = true
            btnActivate.text = "✅ Activate Karein"

            if (result.valid) {
                AppStore.saveLicenseKey(this, key)
                AppStore.saveUserName(this, result.userName ?: "")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                tvStatus.text = "❌ ${result.message}"
            }
        }
    }
}
