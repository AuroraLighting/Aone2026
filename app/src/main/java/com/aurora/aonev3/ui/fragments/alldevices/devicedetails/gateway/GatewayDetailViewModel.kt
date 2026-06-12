package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.gateway

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.ViewModel
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.devices.DeviceRepository

class GatewayDetailViewModel: ViewModel() {
    private val deviceRepository = DeviceRepository()

    fun getDevices(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<Device> = deviceRepository.getAllDevices(gateway)
//    fun getDevice(gateway: NabtoHandler.NabtoGateway, id: Int): LiveData<Device?> = deviceRepository.getDevice(gateway, id)
}
