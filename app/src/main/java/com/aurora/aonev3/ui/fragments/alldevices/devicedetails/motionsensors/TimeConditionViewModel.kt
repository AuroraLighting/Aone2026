package com.aurora.aonev3.ui.fragments.alldevices.devicedetails.motionsensors

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import com.aurora.aonev3.ui.fragments.schedules.TriggerTime

class TimeConditionViewModel: ViewModel(), ITimeConditionViewModel {
    override val isAllDay = MutableLiveData(true)
    override val startTime = MutableLiveData<TriggerTime>()
    override val endTime = MutableLiveData<TriggerTime>()

    override fun setIsAllDay(isAllDay: Boolean) {
        this.isAllDay.postValue(isAllDay)
    }

    override fun updateStartTime(hour: Int, minute: Int, trigger: SunriseSunsetType, offset: Int) {
        startTime.postValue(TriggerTime(hour = hour, minute = minute, trigger = trigger, offset = offset))
    }

    override fun updateEndTime(hour: Int, minute: Int, trigger: SunriseSunsetType, offset: Int) {
        endTime.postValue(TriggerTime(hour = hour, minute = minute, trigger = trigger, offset = offset))
    }
}
