package com.aurora.aonev3.network.handlers

import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.network.volley.Request
import com.aurora.aonev3.data.AppDatabase
import com.aurora.aonev3.data.templates.Identity
import org.json.JSONArray
import org.json.JSONObject

object OtaHandler {
    private val TAG = this::class.simpleName
    private val identitiesDao = AppDatabase.getDatabase().identitiesDao()
    private const val  LATEST_URL = "http://hubota.auroralighting.com/latest/{otaChannel}.json"

    var gatewayFirmware: LiveData<JSONObject> = MutableLiveData()
    var templatesObject = JSONObject()
    var templates: LiveData<JSONArray> = MutableLiveData()
    var zigbeeFirmwareBundle: LiveData<JSONObject> = MutableLiveData()
    val zigbeeFirmwareArray: JSONArray?
    get () {
        return zigbeeFirmwareBundle.value?.optJSONArray("fw_bins")
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun  syncLatest() {
        val otaChannel = SharedPreferencesHandler.getPrefs().sharedPreferences.getString("ota_channel", "uk_api_bundle")

        val response = try {
            Request.get(LATEST_URL.replace(Pair("{otaChannel}", otaChannel)), maxRetries = 4)
        } catch (ex: Exception) {
            ex.printStackTrace()
            Log.e(TAG, "syncLatest exception")
            return
        }

        val body = response.optJSONObject("body") ?: JSONObject()
        if (body.has("gateway")) {
            val gatewayFirmware = body.optJSONObject("gateway")

            if (gatewayFirmware != null) {
                (this.gatewayFirmware as? MutableLiveData<JSONObject>)?.postValue(gatewayFirmware)
            }
        }
        if (body.has("templates")) {
            templatesObject = body.optJSONObject("templates") ?: JSONObject()
        }
        if (body.has("ota_firmware")) {
            val zigbeeFirmwareBundle = body.optJSONObject("ota_firmware")

            if (zigbeeFirmwareBundle != null) {
                (this.zigbeeFirmwareBundle as? MutableLiveData<JSONObject>)?.postValue(zigbeeFirmwareBundle)
            }
        }
        if (body.has("feature_flags")) {
            val json = body.optJSONArray("feature_flags") ?: JSONArray()
            val featureFlags: Array<FeatureFlag> = gson.fromJson(json.toString(), Array<FeatureFlag>::class.java)

            for (featureFlag in featureFlags) {
                addFeatureFlag(featureFlag)
            }
        }
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun syncIdentities() {
        val identifiers = try {
            Request.get("http://${templatesObject.optString("identifiers")}", maxRetries = 4)
        } catch (ex: Exception) {
            ex.printStackTrace()
            Log.e(TAG, "syncIdentities exception")
            return
        }

        val identities = ArrayList<Identity>()
        val identifiersArray = identifiers.optJSONArray("body") ?: JSONArray()

        for (i in identifiersArray.indices()) {
            val identity = identifiersArray.optJSONObject(i)
            val identitiesArray = identity.optJSONArray("identities") ?: JSONArray()

            for (j in identitiesArray.indices()) {
                val defaultName = identitiesArray.optJSONObject(j).optString("default_name")

                identities.add(Identity(
                    identity.optString("device_class"),
                    defaultName,
                    identity.optInt("date"),
                    identity.optString("version")
                ))
            }
        }

        identitiesDao.delete(*identitiesDao.getAll().filter {
            !identities.contains(it)
        }.toTypedArray())

        identitiesDao.insert(*identities.toTypedArray())
    }

    @WorkerThread
    @Throws(VolleyError::class)
    suspend fun  syncTemplates() {
        try {
            val templates = Request.get("http://${templatesObject.optString("uri")}", maxRetries = 4).optJSONArray("body")

            if (templates?.isEmpty() == false) {
                (this.templates as? MutableLiveData<JSONArray>)?.postValue(templates)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            Log.e(TAG, "syncLatest exception")
        }
    }

    val isDynamicEventsAvailable: Boolean
        get() = SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("dynamic_events", false) || CloudHandler.isBeta

    val isAccountDeleteAvailable: Boolean
        get() = SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("account_delete", false) || CloudHandler.isBeta

    private fun addFeatureFlag(flag: FeatureFlag) {
        SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
            putBoolean(flag.feature, flag.released)
        }
    }
}