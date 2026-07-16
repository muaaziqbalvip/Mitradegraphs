package com.muslimislam.mitradeanalyzer

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private lateinit var tvPlaceholder: TextView
    private lateinit var btnAnalyze: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var resultContainer: LinearLayout
    private lateinit var tvResultSummary: TextView
    private lateinit var tvConfidenceBadge: TextView
    private lateinit var imgAnnotated: ImageView
    private lateinit var imgNext: ImageView
    private lateinit var tvNextLabel: TextView

    private var selectedBitmap: Bitmap? = null
    private var cameraPhotoUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) loadBitmapFromUri(uri)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraPhotoUri != null) loadBitmapFromUri(cameraPhotoUri!!)
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else Toast.makeText(this, "Camera permission zaroori hai", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Re-verify license silently in the background. If it was revoked
        // (or activated on another device since), this will catch it and
        // send the user back to login — otherwise the app just works.
        revalidateLicenseSilently()

        imgPreview = findViewById(R.id.imgPreview)
        tvPlaceholder = findViewById(R.id.tvPlaceholder)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        progressBar = findViewById(R.id.progressBar)
        resultContainer = findViewById(R.id.resultContainer)
        tvResultSummary = findViewById(R.id.tvResultSummary)
        tvConfidenceBadge = findViewById(R.id.tvConfidenceBadge)
        imgAnnotated = findViewById(R.id.imgAnnotated)
        imgNext = findViewById(R.id.imgNext)
        tvNextLabel = findViewById(R.id.tvNextLabel)

        findViewById<ImageView>(R.id.btnAccount).setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }

        findViewById<Button>(R.id.btnGallery).setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.btnCamera).setOnClickListener {
            requestCameraAndLaunch()
        }

        btnAnalyze.setOnClickListener {
            runAnalysis()
        }
    }

    private fun revalidateLicenseSilently() {
        val key = AppStore.loadLicenseKey(this)
        if (key.isBlank()) {
            goToLogin()
            return
        }
        val backendUrl = AppStore.loadBackendUrl(this)
        LicenseClient.verify(this, backendUrl, key) { result ->
            if (!result.valid) {
                Toast.makeText(this, "❌ ${result.message}", Toast.LENGTH_LONG).show()
                AppStore.logout(this)
                goToLogin()
            }
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun requestCameraAndLaunch() {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) launchCamera() else cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        val photoFile = File(externalCacheDir, "pattern_capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "com.muslimislam.mitradeanalyzer.fileprovider", photoFile)
        cameraPhotoUri = uri
        cameraLauncher.launch(uri)
    }

    private fun loadBitmapFromUri(uri: Uri) {
        try {
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= 28) {
                val source = android.graphics.ImageDecoder.createSource(contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            selectedBitmap = bitmap
            imgPreview.setImageBitmap(bitmap)
            tvPlaceholder.visibility = android.view.View.GONE
            btnAnalyze.isEnabled = true
            resultContainer.visibility = android.view.View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, "Image load nahi hui: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun runAnalysis() {
        val bitmap = selectedBitmap ?: return
        val backendUrl = AppStore.loadBackendUrl(this)

        progressBar.visibility = android.view.View.VISIBLE
        btnAnalyze.isEnabled = false
        resultContainer.visibility = android.view.View.GONE

        PatternClient.analyzePattern(this, backendUrl, bitmap) { result, error ->
            progressBar.visibility = android.view.View.GONE
            btnAnalyze.isEnabled = true

            if (result == null) {
                Toast.makeText(this, "❌ ${error ?: "Match nahi mila"}", Toast.LENGTH_LONG).show()
                return@analyzePattern
            }

            resultContainer.visibility = android.view.View.VISIBLE

            val outcomeEmoji = when (result.outcomeHint) {
                "win" -> "✅"
                "loss" -> "❌"
                else -> "ℹ️"
            }
            tvResultSummary.text = "🔍 ${result.similarityPercent}% Match — $outcomeEmoji ${result.outcomeHint}"

            when (result.confidenceLabel) {
                "strong" -> {
                    tvConfidenceBadge.text = "STRONG"
                    tvConfidenceBadge.setBackgroundResource(R.drawable.badge_strong)
                    tvConfidenceBadge.setTextColor(getColor(R.color.accent_light))
                }
                "moderate" -> {
                    tvConfidenceBadge.text = "MODERATE"
                    tvConfidenceBadge.setBackgroundResource(R.drawable.badge_moderate)
                    tvConfidenceBadge.setTextColor(getColor(R.color.warning))
                }
                else -> {
                    tvConfidenceBadge.text = "WEAK"
                    tvConfidenceBadge.setBackgroundResource(R.drawable.badge_weak)
                    tvConfidenceBadge.setTextColor(getColor(R.color.danger))
                }
            }

            result.annotatedImage?.let { imgAnnotated.setImageBitmap(it) }

            if (result.nextCandlesImage != null) {
                imgNext.setImageBitmap(result.nextCandlesImage)
                imgNext.visibility = android.view.View.VISIBLE
                tvNextLabel.visibility = android.view.View.VISIBLE
            } else {
                imgNext.visibility = android.view.View.GONE
                tvNextLabel.text = "Match chart ke aakhir tak pahunch gaya — aage kuch nahi hai"
            }
        }
    }
}
