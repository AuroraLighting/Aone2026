package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.datapoints.DeviceDatapoint
import com.aurora.aonev3.data.datapoints.DeviceDatapointRepository

class DaylightSensitivityViewModel : ViewModel() {
    private val deviceDatapointRepository = DeviceDatapointRepository()
    val lux = MutableLiveData<Int?>(null)

    fun getAllDeviceDatapoints(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<DeviceDatapoint> = deviceDatapointRepository.getAllDeviceDatapoints(gateway)
}
