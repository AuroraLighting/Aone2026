package com.aurora.aonev3.network.handlers

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.data.AppDatabase
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.debug
import com.aurora.aonev3.indices
import com.aurora.aonev3.isEmpty
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class TemplateHandler(val gateway: NabtoHandler.NabtoGateway) {

    private var latestIncrement = -1
    private var gwIncrement = 0
    private var zigbeeTemplates = JSONArray()
    private var gatewayTemplates = MutableLiveData<JSONArray>()
    private var needToCheckTemplates = MutableLiveData<Boolean>()
    private var changes = false
    private var runthrough = 0
    private var attempt = 0

    private val crashlytics = FirebaseCrashlytics.getInstance()

    private suspend fun checkIncrements() {
        CoroutineScope(Dispatchers.Main).launch {
            OtaHandler.templates.observeForever { templates ->
                if (!templates.isEmpty() && zigbeeTemplates.isEmpty()) {
                    zigbeeTemplates = templates
                    latestIncrement = OtaHandler.templatesObject.optInt("fw_increment")

                    if (gwIncrement == -1 || gatewayTemplates.value == null || gatewayTemplates.value?.isEmpty() == true) return@observeForever

                    if (gwIncrement <= latestIncrement) {
                        needToCheckTemplates.postValue(true)
                    }
                }
            }

            SyncHandler.devices.observeForever { devices ->
                val device =
                    devices.toList().find {
                        it.parentGateway == gateway.serial
                                && (it.id == 0 || it.deviceClass == Device.DeviceClass.GATEWAY)
                    } ?: return@observeForever
                gwIncrement = device.metadata.optInt("template_version", -1)

                if (latestIncrement == -1 || gatewayTemplates.value == null || gatewayTemplates.value?.isEmpty() == true) return@observeForever

                if (gwIncrement <= latestIncrement) {
                    needToCheckTemplates.postValue(true)
                }
            }

            gatewayTemplates.observeForever(Observer { gatewayTemplates ->
                if (!gatewayTemplates.isEmpty()) {
                    OtaHandler.templates.value?.let { zigbeeTemplates = it }
                    latestIncrement = OtaHandler.templatesObject.optInt("fw_increment")

                    if (latestIncrement == -1 || gwIncrement == 0) return@Observer

                    if (gwIncrement <= latestIncrement) {
                        needToCheckTemplates.postValue(true)
                    }
                }
            })
        }

        try {
            val gatewayTemplates =
                DevelcoHandler.getTemplates(gateway).optJSONArray("body")

            if (gatewayTemplates != null) {
                this.gatewayTemplates.postValue(gatewayTemplates)
            }
        } catch (err: VolleyError) {
            if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                gateway.isConnected = false
                NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
            }
        }
    }

    private suspend fun removeMissingTemplates() {
        val gatewayTemplates = gatewayTemplates.value ?: return

        for (i in gatewayTemplates.indices()) {
            val gatewayTemplate = gatewayTemplates.optJSONObject(i) ?: JSONObject()
            var keepTemplate = false

            for (j in zigbeeTemplates.indices()) {
                val zigbeeTemplate = zigbeeTemplates.optJSONObject(j) ?: JSONObject()

                if (gatewayTemplate.optString("type").equals("static", ignoreCase = true) ||
                    gatewayTemplate.optString("type").equals("persisted", ignoreCase = true) ||
                    gatewayTemplate.optString("name")
                        .equals(zigbeeTemplate.optString("name"), ignoreCase = true)) {
                    keepTemplate = true
                    break
                }
            }

            if (!keepTemplate) {
                try {
                    DevelcoHandler.deleteTemplate(gateway, gatewayTemplate.optString("hash"))
                } catch (err: VolleyError) {
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                    }
                    err.printStackTrace()
                }
                changes = true
            }
        }
    }

    private suspend fun addMissingTemplates() {
        val gatewayTemplates = gatewayTemplates.value ?: return

        for (i in zigbeeTemplates.indices()) {
            val zigbeeTemplate = zigbeeTemplates.optJSONObject(i) ?: JSONObject()
            var addTemplate = true

            for (j in gatewayTemplates.indices()) {
                val gatewayTemplate = gatewayTemplates.optJSONObject(j) ?: JSONObject()

                if (gatewayTemplate.optString("name") == zigbeeTemplate.optString("name")) {
                    addTemplate = false
                    break
                }
            }

            if (addTemplate) {
                try {
                    DevelcoHandler.postTemplate(gateway, zigbeeTemplate)
                } catch (err: VolleyError) {
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                    }
                    err.printStackTrace()
                }
                changes = true
            }
        }
    }

    private suspend fun updateTemplates() {
        val gatewayTemplates = gatewayTemplates.value ?: return

        for (i in zigbeeTemplates.indices()) {
            val zigbeeTemplate = zigbeeTemplates.optJSONObject(i) ?: JSONObject()
            var addTemplate = false

            for (j in gatewayTemplates.indices()) {
                val gatewayTemplate = gatewayTemplates.optJSONObject(j) ?: JSONObject()

                if (gatewayTemplate.optString("type").equals("persisted", ignoreCase = true)) {
                    continue
                }

                try {
                    if (gatewayTemplate.optString("name") == zigbeeTemplate.optString("name")) {
                        val gwMajorV = gatewayTemplate.optString("version").split(".")[0].toInt()
                        val gwMinorV = gatewayTemplate.optString("version").split(".")[1].toInt()
                        val gwHotfixV = gatewayTemplate.optString("version").split(".")[2].toInt()
                        val zMajorV = zigbeeTemplate.optString("tplVersion").split(".")[0].toInt()
                        val zMinorV = zigbeeTemplate.optString("tplVersion").split(".")[1].toInt()
                        val zHotfixV = zigbeeTemplate.optString("tplVersion").split(".")[2].toInt()

                        if ((zMajorV > gwMajorV) ||
                            (zMajorV == gwMajorV && zMinorV > gwMinorV) ||
                            (zMajorV == gwMajorV && zMinorV == gwMinorV && zHotfixV > gwHotfixV)) {
                            try {
                                DevelcoHandler.deleteTemplate(
                                    gateway,
                                    gatewayTemplate.optString("hash")
                                )
                            } catch (err: VolleyError) {
                                if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                                    gateway.isConnected = false
                                    NabtoHandler.openTunnel(
                                        gateway,
                                        CloudHandler.getCredentials().first
                                    )
                                }
                                err.printStackTrace()
                            }
                            addTemplate = true
                            changes = true
                        }
                    }
                } catch (ex: Exception) {
                    crashlytics.recordException(ex)
                }
            }

            if (addTemplate) {
                try {
                    DevelcoHandler.postTemplate(gateway, zigbeeTemplate)
                } catch (err: VolleyError) {
                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                        gateway.isConnected = false
                        NabtoHandler.openTunnel(gateway, CloudHandler.getCredentials().first)
                    }
                    err.printStackTrace()
                }
                changes = true
            }
        }
    }

    suspend fun check() {
        if (attempt++ > 3) return
        checkIncrements()

        CoroutineScope(Dispatchers.Main).launch main@{
            needToCheckTemplates.observeForever { needToCheckTemplates ->
                if (needToCheckTemplates == false) return@observeForever
                CoroutineScope(Dispatchers.IO).launch {
                    removeMissingTemplates()
                    addMissingTemplates()
                    updateTemplates()

                    if (!changes) {
                        val metadata: JSONObject? = SyncHandler.getHubMetadata(gateway)

                        if (metadata != null) {
                            if (metadata.optInt("template_version") != latestIncrement) {
                                metadata.put("template_version", latestIncrement)

                                try {
                                    DevelcoHandler.putDevice(
                                        gateway,
                                        0,
                                        JSONObject().put("metadata", metadata.toString())
                                    )
                                } catch (err: VolleyError) {
                                    if ((err is NoConnectionError || gateway.port == null) && gateway.isConnected) {
                                        gateway.isConnected = false
                                        NabtoHandler.openTunnel(
                                            gateway,
                                            CloudHandler.getCredentials().first
                                        )
                                    }
                                    err.printStackTrace()
                                }
                            }
                        } else {
                            check()
                            return@launch
                        }
                    } else {
                        if (runthrough++ < 3) {
                            checkIncrements()
                        }
                    }
                }
            }
        }
    }
}