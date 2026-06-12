package com.aurora.aonev3.ui.fragments.groups.eventgroupselector

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.ViewModel
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.GroupRepository
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.google.firebase.crashlytics.FirebaseCrashlytics

class EventGroupSelectorViewModel: ViewModel() {

    private val crashlytics = FirebaseCrashlytics.getInstance()
    private val groupRepository = GroupRepository()

    fun getGroups(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<Group> = groupRepository.getAllGroups(gateway)
}
