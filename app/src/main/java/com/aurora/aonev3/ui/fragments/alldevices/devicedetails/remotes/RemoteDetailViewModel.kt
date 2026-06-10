package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.remotes

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.LogicCollectionRepository

class RemoteDetailViewModel : ViewModel() {
    private val logicCollectionRepository = LogicCollectionRepository()
    var selectedGroup: Group? = null
    val targetDevice = MutableLiveData<Pair<Device, String>?>(null)
    val targetGroup = MutableLiveData<Pair<Group, String>?>(null)
    val targetRecall = MutableLiveData<RecallMode?>(null)
    var previousTargetDevice: Pair<Device, String>? = null
    var previousTargetGroup: Pair<Group, String>? = null
    var previousTargetRecall: RecallMode? = null

    fun getLogicCollections(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicCollection> = logicCollectionRepository.getLogicCollections(gateway)

    fun clearViewModel() {
        selectedGroup = null
        targetDevice.postValue(null)
        targetGroup.postValue(null)
        targetRecall.postValue(null)
        previousTargetDevice = null
        previousTargetGroup = null
        previousTargetRecall = null
    }
}

