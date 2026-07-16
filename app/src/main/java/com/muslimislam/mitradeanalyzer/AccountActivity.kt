package com.muslimislam.mitradeanalyzer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AccountActivity : AppCompatActivity() {

    private lateinit var historyContainer: LinearLayout
    private lateinit var tvNoHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        val name = AppStore.loadUserName(this)
        val key = AppStore.loadLicenseKey(this)

        findViewById<TextView>(R.id.tvAccountName).text = name.ifBlank { "Unknown User" }
        findViewById<TextView>(R.id.tvAccountKey).text = key.ifBlank { "—" }

        historyContainer = findViewById(R.id.historyContainer)
        tvNoHistory = findViewById(R.id.tvNoHistory)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            AppStore.logout(this)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        loadHistory(key)
    }

    private fun loadHistory(key: String) {
        if (key.isBlank()) {
            tvNoHistory.visibility = android.view.View.VISIBLE
            return
        }
        val backendUrl = AppStore.loadBackendUrl(this)
        HistoryFetcher.fetchHistory(this, backendUrl, key) { historyJson ->
            if (historyJson == null || historyJson.length() == 0) {
                tvNoHistory.visibility = android.view.View.VISIBLE
                return@fetchHistory
            }
            tvNoHistory.visibility = android.view.View.GONE
            historyContainer.removeAllViews()
            for (i in 0 until historyJson.length()) {
                val entry = historyJson.optJSONObject(i) ?: continue
                val row = TextView(this)
                row.text = "• ${entry.optString("time", "")} — ${entry.optString("action", "")}"
                row.setTextColor(getColor(R.color.text_secondary))
                row.textSize = 12f
                row.setPadding(0, 4, 0, 4)
                historyContainer.addView(row)
            }
        }
    }
}
