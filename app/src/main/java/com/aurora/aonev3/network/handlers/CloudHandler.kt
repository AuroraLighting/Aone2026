package com.aurora.aonev3.network.handlers

import com.aurora.aonev3.synthetic.*
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import com.android.volley.VolleyError
import com.aurora.aonev3.*
import com.aurora.aonev3.network.volley.JwtTokenRequest
import com.aurora.aonev3.network.volley.Request
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.experimental.inv

object CloudHandler {

    private val crashlytics = FirebaseCrashlytics.getInstance()
    private const val HEX_CHARS = "0123456789ABCDEF"

    private enum class Endpoints(val url: String) {
        BASE("https://api.auroralighting.com/v1"),
        REGISTER("register/"),
        ACTIVATE("activate/"),
        LOGIN("login/"),
        USER("user/"),
        DELETE("user/delete/"),
        ACQUIRE("acquire/"),
        GATEWAYS("gateways/"),
        GATEWAY("gateways/{SERIAL}/"),
        FINGERPRINTS("fingerprint/"),
        MIGRATE("migrate/"),
        RELEASE_GATEWAY("release/"),
        RESET_PASSWORD("reset/"),
        VERIFY("verify/"),
        ACCESS("access/"),
        SHARE("share/"),
        UNSHARE("unshare/"),
        MQTT_CONFIG("mqttconfig/")
    }

    var token = ""
        private set

    private const val INTEGRATION_KEY = "JeNXD3XpfapV2SXD"
    val loggedIn: MutableLiveData<Boolean> = MutableLiveData()
    var user: JSONObject = JSONObject()

    var isBeta: Boolean = false

    @Throws(VolleyError::class)
    suspend fun register(firstName: String, lastName: String, email: String, password: String): JSONObject {
        return Request.post(
            url = "${Endpoints.BASE.url}/${Endpoints.REGISTER.url}",
            body = JSONObject()
                .put("integration_key", INTEGRATION_KEY)
                .put("first_name", firstName)
                .put("last_name", lastName)
                .put("email", email)
                .put("password", password)
        )
    }

    @Throws(VolleyError::class)
    suspend fun activate(activationCode: String): JSONObject {
        return JwtTokenRequest.post(
            url = "${Endpoints.BASE.url}/${Endpoints.ACTIVATE.url}",
            body = JSONObject()
                .put("activation_code", activationCode),
            token = token
        )
    }

    @Throws(VolleyError::class)
    suspend fun login(username: String, password: String): JSONObject {
        val response = Request.post(
            url = "${Endpoints.BASE.url}/${Endpoints.LOGIN.url}",
            body = JSONObject()
                .put("username", username)
                .put("password", password)
                .put("tags", JSONArray().put("Android UI3 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})").put("UI3"))
        )

        val body = response.optJSONObject("body") ?: JSONObject()
        token = body.optString("token")
        isBeta = body.optJSONArray("tags")?.contains("is_beta") ?: false

        SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
            putString("abc012", encodeCredentials(username, password))
        }

        loggedIn.postValue(true)

        return response
    }

    @Throws(VolleyError::class)
    suspend fun verifyLogin(username: String, password: String): JSONObject {
        return Request.post(
            url = "${Endpoints.BASE.url}/${Endpoints.LOGIN.url}",
            body = JSONObject()
                .put("username", username)
                .put("password", password)
        )
    }

    @Throws(VolleyError::class)
    suspend fun getUser(): JSONObject {
        if (token.isBlank()) {
            val credentials = getCredentials()
            try {
                login(credentials.first, credentials.second)
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                loggedIn.postValue(false)

                return JSONObject()
            }
        }

        val response = JwtTokenRequest.get(
            url = "${Endpoints.BASE.url}/${Endpoints.USER.url}",
            token = token
        )

        user = response.optJSONObject("body") ?: JSONObject()

        return response
    }

    @Throws(VolleyError::class)
    suspend fun deleteUser(username: String): JSONObject {
        if (token.isBlank()) {
            val credentials = getCredentials()
            try {
                login(credentials.first, credentials.second)
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                loggedIn.postValue(false)

                return JSONObject()
            }
        }

        return JwtTokenRequest.post(
            url = "${Endpoints.BASE.url}/${Endpoints.DELETE.url}",
            body = JSONObject()
                .put("email", username)
                .put("integration_key", INTEGRATION_KEY),
            token = token
        )
    }

    @Throws(VolleyError::class)
    suspend fun acquireGateway(serial: String): JSONObject {
        if (token.isBlank()) {
            val credentials = getCredentials()
            try {
                login(credentials.first, credentials.second)
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                loggedIn.postValue(false)

                return JSONObject()
            }
        }
        return JwtTokenRequest.post(
            url = "${Endpoints.BASE.url}/${Endpoints.ACQUIRE.url}",
            body = JSONObject()
                .put("serial", serial),
            token = token
        )
    }

    @Throws(VolleyError::class)
    suspend fun getGateway(serial: String): JSONObject {
        if (token.isBlank()) {
            val credentials = getCredentials()
            try {
                login(credentials.first, credentials.second)
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                loggedIn.postValue(false)

                return JSONObject()
            }
        }
        val response = JwtTokenRequest.get(
            url = "${Endpoints.BASE.url}/${Endpoints.GATEWAY.url.replace(Pair("{SERIAL}", serial))}",
            token = token
        )

        val body = response.optJSONArray("body") ?: JSONArray()

        for (i in body.indices()) {
            val gateway = body.optJSONObject(i) ?: JSONObject()
            val sharesArray = ArrayList<Share>()

            val gatewayShares = gateway.optJSONArray("shares") ?: JSONArray()

            for (j in gatewayShares.indices()) {
                val gatewayShare = gatewayShares.optJSONObject(j) ?: JSONObject()
                val email = gatewayShare.optString("recipient")
                val name = gatewayShare.optString("recipient_name")
                val grants = gatewayShare.optJSONArray("grants") ?: JSONArray()

                if (email.isEmpty()) {
                    continue
                }

                val share = Share(email, name, grants)

                sharesArray.add(share)
            }

            val nabtoGateway = NabtoHandler.NabtoGateway(
                gateway.optString("id"),
                gateway.optString("name"),
                gateway.optString("serial"),
                gateway.optString("nabto_id"),
                gateway.optString("status"),
                sharesArray,
                NabtoHandler.GatewayAccessLevel.OWNER
            )

            if (!NabtoHandler.nabtoGateways.contains(nabtoGateway)) {
                NabtoHandler.nabtoGateways.add(nabtoGateway)
            } else {
                NabtoHandler.nabtoGateways.find { it == nabtoGateway }?.name = gateway.optString("name")
                NabtoHandler.nabtoGateways.find { it == nabtoGateway }?.shares?.clear()
                NabtoHandler.nabtoGateways.find { it == nabtoGateway }?.shares?.addAll(sharesArray)
            }

            NabtoHandler.selectedGateway = nabtoGateway
        }

        return response
    }

    @Throws(VolleyError::class)
    suspend fun getGateways(): JSONObject {
        if (token.isBlank()) {
            val credentials = getCredentials()
            try {
                login(credentials.first, credentials.second)
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                loggedIn.postValue(false)

                return JSONObject()
            }
        }
        val response = JwtTokenRequest.get(
            url = "${Endpoints.BASE.url}/${Endpoints.ACCESS.url}",
            token = token
        )

        val body = response.optJSONObject("body") ?: JSONObject()
        val acquisitions = body.optJSONArray("acquisitions") ?: JSONArray()
        val shares = body.optJSONArray("shares") ?: JSONArray()

        NabtoHandler.nabtoGateways.toList().forEach { gateway ->
            gateway.websocket?.closeBlocking()
            gateway.websocket = null
            gateway.nabtoTunnel = null
        }

        NabtoHandler.nabtoGateways.clear()

        for (i in acquisitions.indices()) {
            val gateway = acquisitions.optJSONObject(i) ?: JSONObject()
            val sharesArray = ArrayList<Share>()

            val gatewayShares = gateway.optJSONArray("shares") ?: JSONArray()

            for (j in gatewayShares.indices()) {
                val gatewayShare = gatewayShares.optJSONObject(j) ?: JSONObject()
                val email = gatewayShare.optString("recipient")
                val name = gatewayShare.optString("recipient_name")
                val grants = gatewayShare.optJSONArray("grants") ?: JSONArray()

                if (email.isEmpty()) {
                    continue
                }

                val share = Share(email, name, grants)

                sharesArray.add(share)
            }

            val nabtoGateway = NabtoHandler.NabtoGateway(
                gateway.optString("id"),
                gateway.optString("name"),
                gateway.optString("serial"),
                gateway.optString("nabto_id"),
                gateway.optString("status"),
                sharesArray,
                NabtoHandler.GatewayAccessLevel.OWNER
            )
            NabtoHandler.nabtoGateways.add(nabtoGateway)
        }

        for (i in shares.indices()) {
            val gateway = shares.optJSONObject(i) ?: JSONObject()
            val permissions = gateway.optJSONArray("permissions") ?: JSONArray()
            var accessLevel = NabtoHandler.GatewayAccessLevel.BASIC_ACCESS

            for (j in permissions.indices()) {
                val permission = permissions.optJSONObject(j) ?: JSONObject()
                val template = permission.optString("TP")
                if (template.isEmpty()) {
                    continue
                }
                accessLevel = if (template == AccessTemplate.EDIT_ROOT.displayName) {
                    NabtoHandler.GatewayAccessLevel.FULL_ACCESS
                } else {
                    NabtoHandler.GatewayAccessLevel.BASIC_ACCESS
                }
            }

            val nabtoGateway = NabtoHandler.NabtoGateway(
                gateway.optString("id"),
                gateway.optString("name"),
                gateway.optString("serial"),
                gateway.optString("nabto_id"),
                gateway.optString("status"),
                ArrayList(),
                accessLevel
            )
            NabtoHandler.nabtoGateways.add(nabtoGateway)
        }

        val selectedGatewayEuid = SharedPreferencesHandler.getPrefs().sharedPreferences.getString("selectedGateway", "")

        if (NabtoHandler.nabtoGateways.size == 1) {
            NabtoHandler.selectedGateway = NabtoHandler.nabtoGateways[0]
        } else {
            NabtoHandler.selectedGateway =
                NabtoHandler.nabtoGateways.find { it.serial == selectedGatewayEuid }
        }

        return response
    }

    suspend fun putGateway(serial: String, body: JSONObject): JSONObject {
        if (token.isBlank()) {
            val credentials = getCredentials()
            try {
                login(credentials.first, credentials.second)
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                loggedIn.postValue(false)

                return JSONObject()
            }
        }
        return JwtTokenRequest.put(
            url = "${Endpoints.BASE.url}/${Endpoints.GATEWAY.url.replace(Pair("{SERIAL}", serial))}",
            body = body,
            token = token
        )
    }

    suspend fun releaseGateway(serial: String): JSONObject {
        if (token.isBlank()) {
            val credentials = getCredentials()
            try {
                login(credentials.first, credentials.second)
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                loggedIn.postValue(false)

                return JSONObject()
            }
        }
        return JwtTokenRequest.post(
            url = "${Endpoints.BASE.url}/${Endpoints.RELEASE_GATEWAY.url}",
            body = JSONObject()
                .put("serial", serial),
            token = token
        )
    }

    suspend fun migrateGateway(serial: String): JSONObject {
        if (token.isBlank()) {
            val credentials = getCredentials()
            try {
                login(credentials.first, credentials.second)
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                loggedIn.postValue(false)

                return JSONObject()
            }
        }
        return JwtTokenRequest.post(
            url = "${Endpoints.BASE.url}/${Endpoints.MIGRATE.url}",
            body = JSONObject()
                .put("serial", serial),
            token = token
        )
    }

    @Throws(VolleyError::class)
    suspend fun postFingerprint(fingerprint: String): JSONObject {
        if (token.isBlank()) {
            val credentials = getCredentials()
            try {
                login(credentials.first, credentials.second)
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                loggedIn.postValue(false)

                return JSONObject()
            }
        }
        return JwtTokenRequest.post(
            url = "${Endpoints.BASE.url}/${Endpoints.FINGERPRINTS.url}",
            body = JSONObject()
                .put("fingerprint", fingerprint)
                .put("description", "${Build.MODEL} - ${Calendar.getInstance().time}"),
            token = token
        )
    }

    @Throws(VolleyError::class)
    suspend fun getFingerprints(): JSONObject {
        if (token.isBlank()) {
            val credentials = getCredentials()
            try {
                login(credentials.first, credentials.second)
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                loggedIn.postValue(false)

                return JSONObject()
            }
        }
        return JwtTokenRequest.get(
            url = "${Endpoints.BASE.url}/${Endpoints.FINGERPRINTS.url}",
            token = token
        )
    }

    @Throws(VolleyError::class)
    suspend fun share(serial: String, email: String, permissions: JSONArray): JSONObject {
        if (token.isBlank()) {
            val credentials = getCredentials()
            try {
                login(credentials.first, credentials.second)
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                loggedIn.postValue(false)

                return JSONObject()
            }
        }
        return JwtTokenRequest.post(
            url = "${Endpoints.BASE.url}/${Endpoints.SHARE.url}",
            body = JSONObject()
                .put("serial", serial)
                .put("email", email)
                .put("permissions", permissions),
            token = token
        )
    }

    @Throws(VolleyError::class)
    suspend fun unshare(serial: String, email: String): JSONObject {
        if (token.isBlank()) {
            val credentials = getCredentials()
            try {
                login(credentials.first, credentials.second)
            } catch (err: VolleyError) {
                crashlytics.recordException(err)
                loggedIn.postValue(false)

                return JSONObject()
            }
        }
        return JwtTokenRequest.post(
            url = "${Endpoints.BASE.url}/${Endpoints.UNSHARE.url}",
            body = JSONObject()
                .put("serial", serial)
                .put("email", email),
            token = token
        )
    }

    @Throws(VolleyError::class)
    suspend fun resetPassword(username: String): JSONObject {
        return Request.post(
            url = "${Endpoints.BASE.url}/${Endpoints.RESET_PASSWORD.url}",
            body = JSONObject()
                .put("username", username)
        )
    }

    @Throws(VolleyError::class)
    suspend fun verify(username: String, password: String): JSONObject {
        return Request.post(
            url = "${Endpoints.BASE.url}/${Endpoints.VERIFY.url}",
            body = JSONObject()
                .put("integration_key", INTEGRATION_KEY)
                .put("username", username)
                .put("password", password)
        )
    }

    suspend fun getMqttConfig(): JSONObject {
        return JwtTokenRequest.get(
            url = "${Endpoints.BASE.url.replace("v1", "vcs")}/${Endpoints.MQTT_CONFIG.url}",
            token = token
        )
    }

    fun getCredentials(): Pair<String, String> {
        val encodedCredentials = SharedPreferencesHandler
            .getPrefs()
            .sharedPreferences
            .getString("abc012", null)
            ?: SharedPreferencesHandler
                .getPrefs(force = true)
                .sharedPreferences
                .getString("abc012", "undefined") ?: "null"

        if (encodedCredentials == "undefined" || encodedCredentials == "null") {
            crashlytics.log("E/CloudHandler:$encodedCredentials")
            crashlytics.recordException(Exception("No credential string"))
            return Pair("", "")
        }
        val credentials = decodeCredentials(encodedCredentials)

        return Pair(credentials.optString("username"), credentials.optString("password"))
    }

    fun encodeCredentials(username: String, password: String): String {
        val json = JSONObject()
            .put("username", username)
            .put("password", password
                .toByteArray()
                .joinToString("") {
                    String.format("%02X", it.inv())
                })
        return json
            .toString()
            .toByteArray()
            .joinToString("") {
                String.format("%02X", it.inv())
            }
    }

    private fun decodeCredentials(hexString: String): JSONObject {
        return try {
            val jsonString = hexString
                .hexStringToByteArray()
                .map {
                    it.inv().toInt().toChar()
                }
                .joinToString("")

            val initialJSON = JSONObject(jsonString)
            val password = initialJSON.optString("password")
                .hexStringToByteArray()
                .map {
                    it.inv().toInt().toChar()
                }
                .joinToString("")

            initialJSON.put("password", password)
        } catch (ex: Exception) {
            crashlytics.log("E/CloudHandler:$hexString")
            crashlytics.recordException(ex)
            JSONObject()
        }
    }

    private fun String.hexStringToByteArray(): ByteArray {
        val result = ByteArray(length / 2)

        for (i in 0 until length step 2) {
            val firstIndex = HEX_CHARS.indexOf(this[i])
            val secondIndex = HEX_CHARS.indexOf(this[i + 1])

            val octet = firstIndex.shl(4).or(secondIndex)
            result[i.shr(1)] = octet.toByte()
        }

        return result
    }
}

enum class AccessTemplate(val displayName: String) {
    EDIT_ROOT("editRoot"),
    USE_ROOT("useRoot"),
    VIEW_ROOT("viewRoot")
}

enum class ErrorCodes(val value: Int) {
    NO_OWNER(100),
    NOT_OWNER(101),
    NO_ACQUISITION(102),
    ALREADY_OWN(103),
    NOT_GATEWAY(200),
    GATEWAY_OFFLINE(201),
    NABTO(202),
    BAD_USER(300),
    BAD_EMAIL(301),
    USER_STATUS(302),
    CODE_WRONG(303),
    TOKEN_INVALID(304),
    PASSWORD(305),
    NO_FINGERPRINT(401),
    DUP_FINGERPRINT(402),
    PARAM_FINGERPRINT(403),
    NO_SHARE(500),
    NO_RECIPIENT(501),
    DONOR_IS_RECIPIENT(502),
    NO_CHANNEL(600),
    SEND_EMAIL(601),
    INTERNAL(999);
}
