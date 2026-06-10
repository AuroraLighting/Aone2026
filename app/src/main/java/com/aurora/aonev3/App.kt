package com.aurora.aonev3

import android.app.Application
import android.content.Context
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.*

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        app = this
    }

    companion object {
        private var app: App? = null
        val startTime = Calendar.getInstance().time

        val context: Context
            get() = app!!.applicationContext

        const val defaultTransitionTime = 5

        fun actionSuccessful() {}
        fun actionFailed() {}
        fun requestReviewIfAppropriate(activity: android.app.Activity) {}
    }
}

data class FeatureFlag(val feature: String, val released: Boolean)

val gson: Gson = GsonBuilder()
    .registerTypeAdapter(SubType::class.java, SubTypeAdapter())
    .registerTypeAdapter(CollectionType::class.java, CollectionTypeAdapter())
    .registerTypeAdapter(SunriseSunsetType::class.java, SunriseSunsetAdapter())
    .registerTypeAdapter(RuleMetadataType::class.java, RuleMetadataTypeAdapter())
    .registerTypeAdapter(TriggerEnum::class.java, TriggerEnumTypeAdapter())
    .registerTypeAdapter(Trigger::class.java, TriggerTypeAdapter())
    .registerTypeAdapter(Condition::class.java, ConditionTypeAdapter())
    .registerTypeAdapter(Action::class.java, ActionTypeAdapter())
    .create()
