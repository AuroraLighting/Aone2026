package com.aurora.aonev3.ui.fragments.pairing

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.datapoints.DeviceDatapointRepository
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.devices.DeviceRepository

class PairingViewModel : ViewModel() {
    private val deviceRepository = DeviceRepository()
    private val deviceDatapointRepository = DeviceDatapointRepository()
    var newDevices = emptyList<Device>()
    var deviceFound = MutableLiveData<Boolean>(false)

    fun getDevices(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<Device> = deviceRepository.getAllDevices(gateway)
    fun getAllDeviceDatapoints(gateway: NabtoHandler.NabtoGateway) = deviceDatapointRepository.getAllDeviceDatapoints(gateway)
}
