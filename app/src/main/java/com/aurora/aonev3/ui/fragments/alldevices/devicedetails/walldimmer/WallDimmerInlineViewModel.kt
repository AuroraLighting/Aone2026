package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.walldimmer

import androidx.lifecycle.ViewModel
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.datapoints.DeviceDatapointRepository
import com.aurora.aonev3.data.devices.Device

class WallDimmerInlineViewModel : ViewModel() {
    private val deviceDatapointRepository = DeviceDatapointRepository()

    fun getAllDeviceDatapoints(gateway: NabtoHandler.NabtoGateway) =
        deviceDatapointRepository.getAllDeviceDatapoints(gateway)

    suspend fun setBacklight(device: Device, isOn: Boolean) {
        val gateway = NabtoHandler.selectedGateway ?: return

        if (!gateway.isConnected) return
        try {
            DevelcoHandler.putDeviceDatapoint(
                gateway,
                device.id,
                device.ldevs.first(),
                "ringstate",
                isOn
            )
        } catch (err: VolleyError) {
            App.actionFailed()
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
}