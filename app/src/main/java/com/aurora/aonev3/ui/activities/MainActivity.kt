package com.aurora.aonev3.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.R
import com.aurora.aonev3.SharedPreferencesHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.hideSoftKeyboard
import com.aurora.aonev3.network.handlers.*
import com.aurora.aonev3.network.volley.RequestQueue
import com.aurora.aonev3.service.ConnectionService
import com.aurora.aonev3.synthetic.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private var latestFirmwareVersion: String? = null
    private var latestFirmwareIncrement: Int = -1
    private var gwFirmwareVersion: String? = null
    private var gwFirmwareIncrement: Int = 0
    private var firmwareBundleIncrement: Int = 0

    private val isFirmwareChecked = MutableLiveData<Boolean>()
    private var checkMqttCerts = MutableLiveData<Boolean>()
    private var hasCheckedMqttCerts = false

    private var gateway: NabtoHandler.NabtoGateway? = null

    private var mPreviousGateway: NabtoHandler.NabtoGateway? = null
    private var firmwareVerified: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }

        val loggedInObserver = Observer<Boolean?> { loggedIn ->
            if (loggedIn != false) return@Observer
            NabtoHandler.nabtoGateways.forEach {
                it.isConnected = false
            }
            NabtoHandler.signOut()
            RequestQueue.clear()

            val homeTourDone = SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("homeTourDone", false)
            val allDevicesTourDone = SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("allDevicesTourDone", false)
            val groupTourDone = SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("groupTourDone", false)
            val introDone = SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("introDone", false)
            SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                clear()
                putBoolean("homeTourDone", homeTourDone)
                putBoolean("allDevicesTourDone", allDevicesTourDone)
                putBoolean("groupTourDone", groupTourDone)
                putBoolean("introDone", introDone)
            }
            SyncHandler.signOut()
        }
        CloudHandler.loggedIn.removeObserver(loggedInObserver)
        CloudHandler.loggedIn.observe(this, loggedInObserver)

        val observer = Observer<Boolean> {
            runOnUiThread {
                val builder =
                    AlertDialog.Builder(this)
                        .setMessage(getString(R.string.gateway_migrating))
                        .setPositiveButton(R.string.ok) { _, _ ->
                            finishAffinity()
                        }
                        .create()

                if (it == true && !isFinishing && !isDestroyed) {
                    builder.show()
                }
            }
        }

        NabtoHandler.selectedGatewayLive.observeForever(Observer gatewayObserver@{ gateway ->
            if (gateway == null) return@gatewayObserver

            mPreviousGateway?.isMigrating?.removeObserver(observer)
            gateway.isMigrating.observeForever(observer)
            mPreviousGateway = gateway

            ConnectionService.updateStatus(this, gateway.isConnected)

            OtaHandler.gatewayFirmware.observeForever(Observer { gatewayFirmware ->
                val version = gatewayFirmware.optString("version")
                val increment = gatewayFirmware.optInt("fw_increment", -1)
                if (version.isBlank() || increment == -1) return@Observer
                latestFirmwareVersion = version
                latestFirmwareIncrement = increment

                if (gwFirmwareVersion.isNullOrBlank() || gwFirmwareIncrement == 0) return@Observer

                if (verifyFirmware(gateway)) {
                    isFirmwareChecked.postValue(true)
                }
            })

            gateway.gwFirmware.observeForever(Observer { version ->
                if (version?.isBlank() != false) return@Observer
                gwFirmwareVersion = version

                if (latestFirmwareVersion.isNullOrBlank() || gwFirmwareIncrement == 0) return@Observer

                if (verifyFirmware(gateway)) {
                    isFirmwareChecked.postValue(true)
                }
            })

            SyncHandler.devices
                .observeForever(Observer { devices ->
                    val device =
                        devices.toList().find { it.parentGateway == gateway.serial && it.deviceClass == Device.DeviceClass.GATEWAY } ?: return@Observer
                    gwFirmwareIncrement = device.metadata.optInt("firmware_version", -1)
                    firmwareBundleIncrement = device.metadata.optInt("zigbee_firmware_bundle", -1)

                    if (latestFirmwareVersion.isNullOrBlank() || gwFirmwareVersion.isNullOrBlank()) return@Observer

                    if (verifyFirmware(gateway)) {
                        isFirmwareChecked.postValue(true)
                    }
                })

            isFirmwareChecked.observeForever(Observer firmwareCheckedObserver@{ isFirmwareChecked ->
                if (isFirmwareChecked == true) {
                    OtaHandler.zigbeeFirmwareBundle.observeForever(Observer { zigbeeFirmwareBundle ->
                        val firmwareBundleIncrement = firmwareBundleIncrement
                            val increment = zigbeeFirmwareBundle.optInt("fw_increment", -1)
                            if (increment == -1) return@Observer

                            if (increment > firmwareBundleIncrement) {
                                if (!gateway.isConnected) return@Observer
                                SyncHandler.syncHandlerCoroutineScope.launch {
                                    try {
                                        SyncHandler.syncZigbeeFirmware(gateway)
                                    } catch (err: VolleyError) {
                                        handleConnectionError(err, gateway)
                                    }
                                }
                        }
                    })
                }
            })

            gateway.isLatestFirmware.observeForever { isLatest ->
                if (isLatest == false && !gateway.firmwarePopupDismissed && !gateway.firmwarePopupShown) {
                    gateway.firmwarePopupShown = true
                    val fw = OtaHandler.gatewayFirmware.value ?: return@observeForever
                    val builder = AlertDialog.Builder(this)
                        .setTitle("Firmware upgrade")
                        .setMessage(getString(R.string.fw_available))
                        .setPositiveButton("Upgrade") { _, _ ->
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    DevelcoHandler.postFirmwareDownload(
                                        gateway,
                                        JSONObject()
                                            .put("uri", fw.optString("uri"))
                                            .put("hash", fw.optString("hash"))
                                            .put("technology", fw.optString("technology"))
                                            .put("algorithm", fw.optString("algorithm"))
                                            .put("storage", "persisted")
                                    )

                                    runOnUiThread {
                                        if (!isFinishing) {
                                            AlertDialog.Builder(this@MainActivity)
                                                .setTitle("Firmware upgrade")
                                                .setMessage(getString(R.string.hub_upgrading))
                                                .setPositiveButton("OK", null)
                                                .create()
                                                .show()
                                        }
                                    }
                                } catch (err: VolleyError) {
                                    handleConnectionError(err, gateway)
                                    err.printStackTrace()
                                }
                            }
                        }
                        .setNegativeButton("Later") { _, _ ->
                            gateway.firmwarePopupDismissed = true
                        }
                        .create()

                    if (!isFinishing) {
                        builder.show()
                    }
                }
            }
        })

        NabtoHandler.selectedGatewayLive.observe(this, Observer { gateway ->
            if (gateway == null) return@Observer

            this.gateway = gateway
            hasCheckedMqttCerts = false
            firmwareVerified = false
            runOnUiThread {
                if (gateway.isConnecting) {
                    connectingLayout.visibility = View.VISIBLE
                } else {
                    connectingLayout.visibility = View.GONE
                }
            }
        })

        NabtoHandler.gatewaysConnecting.observe(this) {
            runOnUiThread {
                if (NabtoHandler.selectedGateway?.isConnected != true && NabtoHandler.selectedGateway?.isConnecting == true) {
                    connectingLayout.visibility = View.VISIBLE
                } else {
                    connectingLayout.visibility = View.GONE
                }
            }
        }

        checkMqttCerts.observe(this) {
            if (hasCheckedMqttCerts) return@observe

            SyncHandler.syncHandlerCoroutineScope.launch(Dispatchers.IO) {
                val gateway = gateway ?: return@launch
                val mqttConfig = CloudHandler.getMqttConfig().optJSONObject("body") ?: JSONObject()
                val hubMqttConfig = DevelcoHandler.getMqttConfig(gateway).optJSONObject("body") ?: JSONObject()

                if (mqttConfig.optString("serverurl") != hubMqttConfig.optString("serverurl")
                    || mqttConfig.optString("cacrt") != hubMqttConfig.optString("cacrt")
                    || mqttConfig.optString("crt") != hubMqttConfig.optString("crt")
                    || mqttConfig.optString("key") != hubMqttConfig.optString("key")) {
                    DevelcoHandler.putMqttConfig(
                        gateway,
                        mqttConfig.put("keypassword", "")
                    )
                    delay(10 * 1000)
                }

                if (DevelcoHandler.getMqttStatus(gateway).optJSONObject("body")?.optBoolean("connected") != true) {
                    DevelcoHandler.putMqttConfig(gateway, JSONObject())
                }

                hasCheckedMqttCerts = true
            }
        }
    }

    override fun onResume() {
        ConnectionService.start(this)

        SyncHandler.restartCoroutineScope()
        NabtoHandler.cancelClosing()

        if (NabtoHandler.isClosed) {
            val credentials = CloudHandler.getCredentials()
            if (credentials.first.isEmpty()) {
                finishAffinity()
                startActivity(Intent(applicationContext, SplashscreenActivity::class.java))
            }
            NabtoHandler.openTunnels(credentials.first)
            NabtoHandler.selectedGateway =
                NabtoHandler.nabtoGateways.find { it.serial == NabtoHandler.selectedGateway?.serial }
        }

        NabtoHandler.selectedGateway?.let {
            if (it.isConnected) {
                SyncHandler.syncHandlerCoroutineScope.launch {
                    try {
                        SyncHandler.syncGroups(it)
                    } catch (err: VolleyError) {
                        handleConnectionError(err, it)
                    }
                }
            }
        }

        super.onResume()
    }

    override fun onPause() {
        NabtoHandler.closeNabtoDelayed()
        super.onPause()
    }

    override fun onDestroy() {
        ConnectionService.stop(this)
        super.onDestroy()
    }

    private fun handleConnectionError(
        err: VolleyError,
        gateway: NabtoHandler.NabtoGateway
    ) {
        if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
            gateway.isConnected = false
            val credentials = CloudHandler.getCredentials()
            if (credentials.first.isEmpty()) {
                finishAffinity()
                startActivity(Intent(applicationContext, SplashscreenActivity::class.java))
                return
            }
            SyncHandler.syncHandlerCoroutineScope.launch(Dispatchers.IO) {
                NabtoHandler.openTunnel(gateway, credentials.first)
            }
        }
    }

    private fun verifyFirmware(gateway: NabtoHandler.NabtoGateway): Boolean {
        if (firmwareVerified) return true

        if (latestFirmwareIncrement > gwFirmwareIncrement
            && gwFirmwareVersion != latestFirmwareVersion) {
            val latestVersion = latestFirmwareVersion
                ?.split("-")?.get(0)
                ?.split(".")?.get(2)?.toInt() ?: 0
            val gwVersion = gwFirmwareVersion
                ?.split("-")?.get(0)
                ?.split(".")?.get(2)?.toInt() ?: 0

            if (gwVersion < latestVersion) {
                gateway.isLatestFirmware.postValue(false)
                return false
            }
        } else if (latestFirmwareIncrement > gwFirmwareIncrement
            && gwFirmwareVersion == latestFirmwareVersion) {
            CoroutineScope(Dispatchers.IO).launch {
                val metadata = SyncHandler.getHubMetadata(gateway) ?: return@launch
                metadata.put("firmware_version", latestFirmwareIncrement)

                try {
                    SyncHandler.devicesList.find { it.parentGateway == gateway.serial && it.deviceClass == Device.DeviceClass.GATEWAY }?.let { gatewayDevice ->
                        DevelcoHandler.putDevice(
                            gateway,
                            gatewayDevice.id,
                            JSONObject().put("metadata", metadata.toString())
                        )
                    }
                } catch (err: VolleyError) {
                    handleConnectionError(err, gateway)
                    err.printStackTrace()
                }
            }
        }

        checkMqttCerts.postValue(true)
        firmwareVerified = true
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupUI(view: View) {
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                hideSoftKeyboard(this)
                val focusedView = view.findFocus()
                if (focusedView is EditText) {
                    focusedView.clearFocus()
                }
                false
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUI(innerView)
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
        private const val REQUEST_NOTIFICATION_PERMISSION = 100
    }
}
