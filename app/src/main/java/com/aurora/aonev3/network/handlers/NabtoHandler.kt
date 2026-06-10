package com.aurora.aonev3.network.handlers

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.SharedPreferencesHandler
import com.aurora.aonev3.network.handlers.NabtoHandler.NabtoGateway.GatewayUpdate.UpdateStatus.*
import com.aurora.aonev3.network.volley.RequestQueue
import com.nabto.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


object NabtoHandler {
    private val TAG = this::class.simpleName
    private val api: NabtoApi = NabtoApi(NabtoAndroidAssetManager(App.context))

    init {
        api.startup()
    }

    private var session: Session? = null

    val nabtoGateways: ArrayList<NabtoGateway> = ArrayList()

    var selectedGatewayLive: MutableLiveData<NabtoGateway?> = MutableLiveData()
    var selectedGateway: NabtoGateway? = null
        get() = nabtoGateways.find { gateway -> gateway == field }
        set(value) {
            field = value

            SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
                putString("selectedGateway", value?.serial)
            }

            selectedGatewayLive.postValue(value)
        }
    var connectingCallback: NabtoConnecting? = null

    private val closingHandler = Handler(Looper.getMainLooper())
    private var closingRunnable: Runnable? = null

    val gatewaysConnecting = MutableLiveData(0)
    val gatewaysConnected = MutableLiveData(0)

    var isClosed = true
        private set

    fun openSession(email: String) {
        if (email.isBlank()) return
        session = api.openSession(email, "12345").also {
            if (it.status == NabtoStatus.OPEN_CERT_OR_PK_FAILED) {
                api.createSelfSignedProfile(email, "12345")

                openSession(email)
            }
        }
    }

    fun getFingerprint(email: String): String? {
        val fp: String
        val byteFingerprint: Array<out String> = Array(16) { "" }

        api.certificates
        val status = api.getFingerprint(email, byteFingerprint)

        if (status != NabtoStatus.OK) {
            return null
        }

        fp = byteFingerprint.first()

        return fp
    }

    fun closeNabtoDelayed(delay: Long = 60000) {
        closingRunnable = Runnable {
            SyncHandler.signOut()
            RequestQueue.clear()
            for (i in nabtoGateways.indices) {
                try {
                    val gateway = nabtoGateways[i]
                    val isConnected = gateway.isConnected
                    gateway.isConnected = false
                    gateway.websocket?.close()
                    gateway.nabtoTunnel?.let { api.tunnelClose(it) }

                    if (isConnected) {
                        gatewaysConnected.postValue(gatewaysConnected.value?.minus(1))
                    }
                    if (gateway.isConnecting) {
                        gatewaysConnecting.postValue(gatewaysConnecting.value?.minus(1))
                    }

                    nabtoGateways[i] =
                        NabtoGateway(
                            gateway.id,
                            gateway.name,
                            gateway.serial,
                            gateway.nabtoId,
                            gateway.status,
                            gateway.shares,
                            gateway.accessLevel
                        )

                    nabtoGateways[i].port = null
                    nabtoGateways[i].nabtoTunnel = null
                } catch (ex: IndexOutOfBoundsException) {
                    ex.printStackTrace()
                }
            }

            session = session?.let {
                api.closeSession(it)
                null
            }
            isClosed = true
        }

        closingRunnable?.let {
            closingHandler.postDelayed(it, delay)
        }
    }

    fun cancelClosing() {
        closingRunnable?.let {
            closingHandler.removeCallbacks(it)
        }
    }

    fun closeNabtoImmediate() {
        for (i in nabtoGateways.indices) {
            val gateway = nabtoGateways[i]
            gateway.websocket?.close()

            if (gateway.isConnected) {
                gatewaysConnected.postValue(gatewaysConnected.value?.minus(1))
            }
            if (gateway.isConnecting) {
                gatewaysConnecting.postValue(gatewaysConnecting.value?.minus(1))
            }

            gateway.nabtoTunnel?.let { api.tunnelClose(it) }

            nabtoGateways[i] = NabtoGateway(
                gateway.id,
                gateway.name,
                gateway.serial,
                gateway.nabtoId,
                gateway.status,
                gateway.shares,
                gateway.accessLevel
            )

            nabtoGateways[i].isConnected = false
            nabtoGateways[i].port = null
            nabtoGateways[i].nabtoTunnel = null
        }

        session = session?.let {
            api.closeSession(it)
            null
        }
        isClosed = true
    }

    fun closeNabtoImmediate(gateway: NabtoGateway) {
        gateway.websocket?.close()

        if (gateway.isConnected) {
            gatewaysConnected.postValue(gatewaysConnected.value?.minus(1))
        }
        if (gateway.isConnecting) {
            gatewaysConnecting.postValue(gatewaysConnecting.value?.minus(1))
        }

        gateway.nabtoTunnel?.let { api.tunnelClose(it) }

        nabtoGateways[nabtoGateways.indexOf(gateway)] =
            NabtoGateway(gateway.id, gateway.name, gateway.serial, gateway.nabtoId, gateway.status, gateway.shares, gateway.accessLevel)

        nabtoGateways[nabtoGateways.indexOf(gateway)].isConnected = false
        nabtoGateways[nabtoGateways.indexOf(gateway)].port = null
        nabtoGateways[nabtoGateways.indexOf(gateway)].nabtoTunnel = null
    }

    fun openTunnels(email: String) {
        nabtoGateways.toList().forEach { gateway ->
            CoroutineScope(Dispatchers.IO).launch {
                openTunnel(gateway, email)
            }
        }
    }

    suspend fun openTunnel(gateway: NabtoGateway, email: String, startTime: Long = 0) {
        if (gateway.isConnecting || gateway.isConnected || gateway.isMigrating.value == true) return
        isClosed = false
        if (!gateway.isConnecting) {
            gatewaysConnecting.postValue(gatewaysConnecting.value?.plus(1))
        }
        gateway.isConnecting = true
        if (!gateway.status.contains("AuroraIot")) {
            try {
                val response = CloudHandler.migrateGateway(gateway.serial)

                if (response.optJSONObject("body")?.optString("status") == "pending") {
                    gateway.isMigrating.postValue(true)
                    return
                }
            } catch (err: VolleyError) {
                err.printStackTrace()
                gatewaysConnecting.postValue(gatewaysConnecting.value?.minus(1))
                if (gateway == selectedGateway) {
                    connectingCallback?.finish(false)
                }
                gateway.isConnecting = false
                gateway.isConnected = false
                gateway.lastError = 1000015
                gateway.nabtoTunnel = null
                return
            }

        }

        val st: Long = if (startTime > 0) {
            startTime
        } else {
            Calendar.getInstance().timeInMillis
        }

        gateway.nabtoTunnel = api.tunnelOpenTcp(0, gateway.nabtoId, "127.0.0.1", 80, session)

        if (gateway.nabtoTunnel?.status == NabtoStatus.INVALID_SESSION) {
            gateway.isConnecting = false
            session = null
            openSession(email)
            openTunnel(gateway, email)
            return
        }

        val handler = Handler(Looper.getMainLooper())

        handler.post(object : Runnable {
            override fun run() {
                gateway.nabtoTunnel ?: return
                val tunnelInfo = api.tunnelInfo(gateway.nabtoTunnel)

                gateway.tunnelState = tunnelInfo.tunnelState

                when (tunnelInfo.tunnelState) {
                    NabtoTunnelState.CONNECTING -> {
                        Log.v(
                            TAG,
                            "Nabto connecting: ${gateway.serial}"
                        )
                        gateway.isConnected = false
                        handler.postDelayed(this, 500)
                    }
                    NabtoTunnelState.CLOSED,
                    NabtoTunnelState.READY_FOR_RECONNECT,
                    NabtoTunnelState.UNKNOWN -> {
                        val endTime = Calendar.getInstance().timeInMillis
                        Log.e(
                            TAG,
                            "Nabto failed: ${gateway.serial}, ${tunnelInfo.tunnelState} - ${tunnelInfo.lastError}, st - $st, et - $endTime, ${endTime - st}"
                        )

                        gateway.isConnecting = false
                        gateway.isConnected = false
                        gateway.lastError = tunnelInfo.lastError
                        gateway.nabtoTunnel = null

                        if (gateway.lastError != 1000015 && endTime - st < 30000) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                CoroutineScope(Dispatchers.IO).launch {
                                    openTunnel(gateway, email, st)
                                }
                            }, 1000)
                        } else {
                            gatewaysConnecting.postValue(gatewaysConnecting.value?.minus(1))
                            if (gateway == selectedGateway) {
                                connectingCallback?.finish(false)
                            }
                        }
                    }
                    NabtoTunnelState.LOCAL,
                    NabtoTunnelState.REMOTE_P2P,
                    NabtoTunnelState.REMOTE_RELAY,
                    NabtoTunnelState.REMOTE_RELAY_MICRO -> {
                        Log.v(
                            TAG,
                            "Nabto success: ${gateway.serial}, ${tunnelInfo.tunnelState}, ${tunnelInfo.port}"
                        )
                        gateway.port = tunnelInfo.port

                        SyncHandler.syncHandlerCoroutineScope.launch {
                            try {
                                SyncHandler.syncDevices(gateway)
                                SyncHandler.syncFirmwareStatus(gateway)
                            } catch (error: VolleyError) {
                                val endTime = Calendar.getInstance().timeInMillis
                                if (endTime - st < 30000) {
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        gateway.isConnected = false
                                        gateway.isConnecting = false
                                        gateway.lastError = tunnelInfo.lastError
                                        gateway.nabtoTunnel = null

                                        CoroutineScope(Dispatchers.IO).launch {
                                            openTunnel(gateway, email, st)
                                        }
                                    }, 5000)
                                } else {
                                    gatewaysConnecting.postValue(gatewaysConnecting.value?.minus(1))
                                    if (gateway == selectedGateway) {
                                        connectingCallback?.finish(false)
                                    }
                                }

                                return@launch
                            }

                            if (gateway == selectedGateway) {
                                connectingCallback?.finish(true)
                            }
                            if (!gateway.isConnecting) {
                                gatewaysConnecting.postValue(gatewaysConnecting.value?.minus(1))
                            }
                            if (!gateway.isConnected) {
                                gatewaysConnected.postValue(gatewaysConnected.value?.plus(1))
                            }
                            gateway.isConnected = true
                            gateway.isConnecting = false
                            gateway.lastError = null
                        }

                    }
                    null -> {
                    }
                }
            }
        })
    }

    fun signOut() {
        closeNabtoImmediate()
        selectedGateway = null
        nabtoGateways.clear()
        selectedGatewayLive.postValue(null)
    }

    data class NabtoGateway(
        val id: String,
        var name: String,
        val serial: String,
        val nabtoId: String,
        val status: String,
        val shares: ArrayList<Share>,
        val accessLevel: GatewayAccessLevel
    ) {
        var nabtoTunnel: Tunnel? = null
        var port: Int? = null
        var lastError: Int? = null
        var tunnelState: NabtoTunnelState? = null
        var isConnecting = false
        var isConnected = false
            set(value) {
                if (field == value) return
                field = value
                websocket = if (value) {
                    WebsocketHandler(this).apply {
                        connect()
                    }
                } else {
                    websocket?.closeBlocking()
                    null
                }
                isConnectedLiveData.postValue(value)
            }
        get() {
            val connected = nabtoTunnel?.let {
                val tunnelInfo = api.tunnelInfo(it) //NabtoCApiWrapper_.nabtoTunnelInfo(it)
                port = tunnelInfo.port
                lastError = tunnelInfo.lastError
                tunnelState = tunnelInfo.tunnelState
                when (tunnelInfo.tunnelState) {
                    NabtoTunnelState.CLOSED,
                    NabtoTunnelState.CONNECTING,
                    NabtoTunnelState.READY_FOR_RECONNECT,
                    NabtoTunnelState.UNKNOWN -> false
                    else -> true
                }
            } ?: false
            isConnected = connected
            return connected
        }
        var isConnectedLiveData = MutableLiveData<Boolean>()
        var websocket: WebsocketHandler? = null
        var update = GatewayUpdate()
        val gwFirmware = MutableLiveData("")
        val isLatestFirmware = MutableLiveData(true)
        var firmwarePopupDismissed = false
        var firmwarePopupShown = false
        val isMigrating = MutableLiveData(false)

        override fun equals(other: Any?): Boolean {
            return if (other is NabtoGateway) {
                other.serial == serial
            } else {
                false
            }
        }

        override fun hashCode(): Int {
            return serial.hashCode()
        }

        data class GatewayUpdate(var isUpdating: MutableLiveData<Boolean> = MutableLiveData(false)) {

            var perCent: MutableLiveData<Int> = MutableLiveData(0)
            var status: MutableLiveData<UpdateStatus> = MutableLiveData(IDLE)
                set(value) {
                    field = value

                    when (value.value) {
                        WAITING,
                        PENDING,
                        COMPLETE,
                        FAILED,
                        CANCELLED,
                        IDLE -> isUpdating.postValue(false)
                        DOWNLOADING,
                        VALIDATING,
                        APPLYING -> isUpdating.postValue(true)
                        null -> {
                        }
                    }
                }

            enum class UpdateStatus {
                WAITING,
                PENDING,
                DOWNLOADING,
                VALIDATING,
                COMPLETE,
                FAILED,
                APPLYING,
                CANCELLED,
                IDLE
            }
        }
    }

    enum class GatewayAccessLevel {
        OWNER,
        FULL_ACCESS,
        BASIC_ACCESS
    }

    interface NabtoConnecting {
        fun finish(success: Boolean)
    }
}

data class Share(val email: String, val name: String, val grants: JSONArray) {
    override fun toString() = JSONObject()
        .put("email", email)
        .put("name", name)
        .put("grants", grants)
        .toString()

    companion object {
        fun fromJsonString(string: String): Share {
            val json = JSONObject(string)

            return Share(json.getString("email"), json.getString("name"), json.optJSONArray("grants") ?: JSONArray())
        }
    }
}