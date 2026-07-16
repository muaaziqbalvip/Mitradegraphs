package com.muslimislam.mitradeanalyzer

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Animated splash screen: logo fades in + scales up, title/subtitle fade
 * in after, then a short progress spinner while we silently re-verify any
 * already-saved license (so returning users don't have to re-type their
 * key every launch — only the very first activation needs the key).
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.imgSplashLogo)
        val title = findViewById<TextView>(R.id.tvSplashTitle)
        val subtitle = findViewById<TextView>(R.id.tvSplashSubtitle)
        val progress = findViewById<ProgressBar>(R.id.splashProgress)

        val logoFade = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).setDuration(600)
        val logoScaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.7f, 1f).setDuration(600)
        val logoScaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.7f, 1f).setDuration(600)

        val titleFade = ObjectAnimator.ofFloat(title, "alpha", 0f, 1f).setDuration(400).apply { startDelay = 400 }
        val subtitleFade = ObjectAnimator.ofFloat(subtitle, "alpha", 0f, 1f).setDuration(400).apply { startDelay = 550 }
        val progressFade = ObjectAnimator.ofFloat(progress, "alpha", 0f, 1f).setDuration(400).apply { startDelay = 750 }

        val set = AnimatorSet()
        set.playTogether(logoFade, logoScaleX, logoScaleY, titleFade, subtitleFade, progressFade)
        set.interpolator = DecelerateInterpolator()
        set.start()

        // Give the animation time to breathe, then decide where to go.
        window.decorView.postDelayed({
            proceedToNextScreen()
        }, 1800)
    }

    private fun proceedToNextScreen() {
        val savedKey = AppStore.loadLicenseKey(this)
        val savedName = AppStore.loadUserName(this)

        val destination = if (savedKey.isNotBlank() && savedName.isNotBlank()) {
            // Already activated on this device before — skip straight to
            // the main screen. LicenseClient re-verifies silently in the
            // background from MainActivity in case it was revoked.
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }

        startActivity(destination)
        finish()
    }
}
