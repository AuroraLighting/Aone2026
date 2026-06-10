package com.aurora.aonev3.ui.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowInsets.Type.statusBars
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.ClientError
import com.android.volley.VolleyError
import com.aurora.aonev3.R
import com.aurora.aonev3.SharedPreferencesHandler
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.OtaHandler
import com.aurora.aonev3.ui.activities.createaccount.ActivateAccountActivity
import com.aurora.aonev3.ui.activities.login.LoginActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SplashscreenActivity : AppCompatActivity() {

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(applicationContext) }
    private var waitForUpdate = false

    companion object {
        const val TAG = "SplashscreenActivity"
        const val APP_UPDATE_REQUEST_CODE = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(statusBars())
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                OtaHandler.syncLatest()
                OtaHandler.syncIdentities()
                OtaHandler.syncTemplates()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener {
            if (it.updateAvailability() == UPDATE_AVAILABLE &&
                    it.isUpdateTypeAllowed(IMMEDIATE)) {
                appUpdateManager.startUpdateFlowForResult(
                    it,
                    IMMEDIATE,
                    this,
                    APP_UPDATE_REQUEST_CODE
                )
                waitForUpdate = true
            }
        }

        chooseStartLocation()
    }

    private fun chooseStartLocation() {
        if (SharedPreferencesHandler.getPrefs().sharedPreferences.contains("abc012") && CloudHandler.getCredentials().first.isNotBlank()) {
            val credentials = CloudHandler.getCredentials()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = CloudHandler.login(credentials.first, credentials.second)
                    if (response.optJSONObject("body")?.optString("status") == "PE") {
                        CoroutineScope(Dispatchers.Main).launch {
                            if (!waitForUpdate) {
                                startActivity(
                                    Intent(
                                        this@SplashscreenActivity,
                                        ActivateAccountActivity::class.java
                                    )
                                )
                                finish()
                            }
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
                    try {
                        CloudHandler.postFingerprint(fingerprint)
                    } catch (ex: VolleyError) {
                    }
                    NabtoHandler.openTunnels(credentials.first)
                }
                CoroutineScope(Dispatchers.Main).launch {
                    if (!waitForUpdate) {
                        if (SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean(
                                "introDone",
                                false
                            )
                        ) {
                            startActivity(
                                Intent(
                                    this@SplashscreenActivity,
                                    MainActivity::class.java
                                )
                            )
                        } else {
                            val intent =
                                Intent(this@SplashscreenActivity, IntroActivity::class.java)
                            intent.putExtra("target", "main")
                            startActivity(intent)
                        }
                        finish()
                    }
                }
            }
        } else {
            Handler(mainLooper).postDelayed({
                if (!waitForUpdate) {
                    if (SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean(
                            "introDone",
                            false
                        )
                    ) {
                        startActivity(Intent(this, LoginActivity::class.java))
                    } else {
                        val intent = Intent(this, IntroActivity::class.java)
                        intent.putExtra("target", "login")
                        startActivity(intent)
                    }
                    finish()
                }
            }, 2000)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_UPDATE_REQUEST_CODE) {
            waitForUpdate = false

            if (resultCode == RESULT_OK) {
                chooseStartLocation()
            } else if (resultCode == RESULT_CANCELED) {
                if (!isFinishing) {
                    AlertDialog.Builder(this)
                        .setMessage("You can update the App from the Play Store or from Help > About")
                        .setPositiveButton(R.string.ok) { _, _ ->
                            chooseStartLocation()
                        }
                        .create()
                        .show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->

                if (appUpdateInfo.updateAvailability() == DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        IMMEDIATE,
                        this,
                        APP_UPDATE_REQUEST_CODE
                    )
                }
            }
    }
}
