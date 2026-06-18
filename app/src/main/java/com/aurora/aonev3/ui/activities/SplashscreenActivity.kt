package com.aurora.aonev3.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowInsets.Type.statusBars
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.ClientError
import com.android.volley.VolleyError
import com.aurora.aonev3.SharedPreferencesHandler
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.OtaHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.ui.activities.createaccount.ActivateAccountActivity
import com.aurora.aonev3.ui.activities.login.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SplashscreenActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SplashscreenActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.aurora.aonev3.R.layout.activity_splashscreen)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        // Sync OTA identities in background. When done, force a device re-sync
        // so any devices initially classified by name inference get reclassified
        // correctly using the now-populated identities DB.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                OtaHandler.syncLatest()
                OtaHandler.syncIdentities()
                OtaHandler.syncTemplates()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            CoroutineScope(Dispatchers.Main).launch {
                chooseStartLocation()
            }
        }
    }

    private fun chooseStartLocation() {
        if (SharedPreferencesHandler.getPrefs().sharedPreferences.contains("abc012") &&
            CloudHandler.getCredentials().first.isNotBlank()) {
            val credentials = CloudHandler.getCredentials()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = CloudHandler.login(credentials.first, credentials.second)
                    if (response.optJSONObject("body")?.optString("status") == "PE") {
                        CoroutineScope(Dispatchers.Main).launch {
                            startActivity(Intent(this@SplashscreenActivity, ActivateAccountActivity::class.java))
                            finish()
                        }
                        return@launch
                    }
                    CloudHandler.getGateways()
                } catch (err: VolleyError) {
                } catch (ex: ClientError) {
                }

                NabtoHandler.openSession(credentials.first)
                val fingerprint = NabtoHandler.getFingerprint(credentials.first) ?: ""
                CoroutineScope(Dispatchers.IO).launch {
                    try { CloudHandler.postFingerprint(fingerprint) } catch (ex: VolleyError) { }
                    NabtoHandler.openTunnels(credentials.first)
                }
                CoroutineScope(Dispatchers.Main).launch {
                    if (SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("introDone", false)) {
                        startActivity(Intent(this@SplashscreenActivity, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this@SplashscreenActivity, IntroActivity::class.java).apply {
                            putExtra("target", "main")
                        })
                    }
                    finish()
                }
            }
        } else {
            Handler(mainLooper).postDelayed({
                if (SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("introDone", false)) {
                    startActivity(Intent(this, LoginActivity::class.java))
                } else {
                    startActivity(Intent(this, IntroActivity::class.java).apply {
                        putExtra("target", "login")
                    })
                }
                finish()
            }, 2000)
        }
    }

    override fun onResume() {
        super.onResume()
    }
}
