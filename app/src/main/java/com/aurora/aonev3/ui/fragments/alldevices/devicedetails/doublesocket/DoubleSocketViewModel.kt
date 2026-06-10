package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.doublesocket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.NoConnectionError
import com.android.volley.VolleyError
import com.aurora.aonev3.App
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.CloudHandler
import com.aurora.aonev3.network.handlers.DevelcoHandler
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.datapoints.DeviceDatapoint
import com.aurora.aonev3.data.datapoints.DeviceDatapointRepository
import com.aurora.aonev3.data.devices.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class DoubleSocketViewModel : ViewModel() {
    private val deviceDatapointRepository = DeviceDatapointRepository()

    fun getAllDeviceDatapoints(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<DeviceDatapoint> = deviceDatapointRepository.getAllDeviceDatapoints(gateway)

    fun setLevel(device: Device, level: Int) {
        val gateway = NabtoHandler.selectedGateway ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                DevelcoHandler.putDeviceDatapoint(
                    gateway,
                    device.id,
                    "lights",
                    "level",
                    level,
                    first = true
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
}