package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aurora.aonev3.MutableLiveDataArrayList
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.data.devices.Device
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.data.groups.scenes.Scene
import com.aurora.aonev3.data.logic.LogicCollection
import com.aurora.aonev3.data.logic.LogicCollectionRepository
import com.aurora.aonev3.data.logic.timers.LogicTimerRepository
import com.aurora.aonev3.ui.fragments.groups.eventgroupselector.IEventGroupSelectorViewModel
import com.aurora.aonev3.ui.fragments.schedules.EventDay
import com.aurora.aonev3.ui.fragments.schedules.EventTarget
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import com.aurora.aonev3.ui.fragments.schedules.TriggerTime
import com.aurora.aonev3.ui.fragments.schedules.eventselectors.IEventDaySelectorViewModel
import com.aurora.aonev3.ui.fragments.schedules.eventselectors.IEventDeviceSelectorViewModel
import com.aurora.aonev3.ui.fragments.schedules.eventselectors.IEventSceneSelectorViewModel
import com.aurora.aonev3.ui.fragments.schedules.eventselectors.IEventTargetSelectorViewModel

class MotionSensorEventViewModel: ViewModel(), IEventTargetSelectorViewModel, IEventGroupSelectorViewModel, IEventSceneSelectorViewModel, IEventDeviceSelectorViewModel, ITimeoutViewModel, ITimeConditionViewModel, IEventDaySelectorViewModel {
    private val logicCollectionRepository = LogicCollectionRepository()
    private val logicTimerRepository = LogicTimerRepository()
    var logicCollection: LogicCollection? = null
    override var device = MutableLiveData<Pair<Device, String>?>(null)
    override var scene = MutableLiveData<Scene?>(null)
    override val isAllDay = MutableLiveData(true)
    override val startTime = MutableLiveData<TriggerTime>()
    override val endTime = MutableLiveData<TriggerTime>()
    override var eventDay = MutableLiveData<EventDay>(EventDay.EVERYDAY)
    override var eventTarget = MutableLiveData<EventTarget?> (null)
    override var targetGroup = MutableLiveData<Group?>(null)
    val targetLux = MutableLiveData<Int?>(null)
    override var timeout: MutableLiveData<Int?> = MutableLiveData(null)
    var initialTimeout: Int? = null

    fun getLogicCollections(gateway: NabtoHandler.NabtoGateway): MutableLiveDataArrayList<LogicCollection> = logicCollectionRepository.getLogicCollections(gateway)
    fun getLogicTimers(gateway: NabtoHandler.NabtoGateway) = logicTimerRepository.getAllLogicTimers(gateway)

    override fun setIsAllDay(isAllDay: Boolean) {
        this.isAllDay.postValue(isAllDay)
    }

    override fun updateStartTime(hour: Int, minute: Int, trigger: SunriseSunsetType, offset: Int) {
        startTime.postValue(TriggerTime(hour = hour, minute = minute, trigger = trigger, offset = offset))
    }

    override fun updateEndTime(hour: Int, minute: Int, trigger: SunriseSunsetType, offset: Int) {
        endTime.postValue(TriggerTime(hour = hour, minute = minute, trigger = trigger, offset = offset))
    }

    fun clearViewModel() {
        device.postValue(null)
        scene.postValue(null)
        isAllDay.postValue(false)
        startTime.postValue(TriggerTime(0, 0, SunriseSunsetType.TIME, 0))
        endTime.postValue(TriggerTime(0, 0, SunriseSunsetType.TIME, 0))
        eventDay.postValue(EventDay.EVERYDAY)
        eventTarget.postValue(null)
        targetGroup.postValue(null)
        targetLux.postValue(null)
        timeout.postValue(null)
        initialTimeout = null
    }
}