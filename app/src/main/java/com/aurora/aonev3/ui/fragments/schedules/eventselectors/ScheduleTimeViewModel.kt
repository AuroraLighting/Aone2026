package com.aurora.aonev3.ui.fragments.schedules.eventselectors

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import com.aurora.aonev3.ui.fragments.schedules.TriggerTime

interface IScheduleTimeViewModel {
    val trigger: LiveData<TriggerTime>

    fun updateTrigger(hour: Int = trigger.value?.hour ?: 0, minute: Int = trigger.value?.minute ?: 0, triggerType: SunriseSunsetType = trigger.value?.trigger ?: SunriseSunsetType.TIME, offset: Int = trigger.value?.offset ?: 0)
}

open class ScheduleTimeViewModel: ViewModel(), IScheduleTimeViewModel {
    override var trigger = MutableLiveData<TriggerTime>()

    override fun updateTrigger(hour: Int, minute: Int, triggerType: SunriseSunsetType, offset: Int) {
        trigger.postValue(TriggerTime(hour = hour, minute = minute, trigger = triggerType, offset = offset))
    }

    fun clearViewModel() {
        trigger = MutableLiveData()
    }
}

class ConditionStartTimeViewModel: ScheduleTimeViewModel()

class ConditionEndTimeViewModel: ScheduleTimeViewModel()