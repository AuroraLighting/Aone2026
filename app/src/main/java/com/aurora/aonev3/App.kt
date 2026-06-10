package com.aurora.aonev3

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.core.content.edit
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.ui.fragments.schedules.SunriseSunsetType
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.*
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import com.aurora.aonev3.network.handlers.CloudHandler


class App : Application() {

    override fun onCreate() {
//        if (BuildConfig.DEBUG) {
//            StrictMode.setThreadPolicy(
//                StrictMode.ThreadPolicy.Builder()
//                    .detectNetwork() // or .detectAll() for all detectable problems
//                    .penaltyLog()
//                    .build()
//            )
//            StrictMode.setVmPolicy(
//                VmPolicy.Builder()
//                    .detectLeakedSqlLiteObjects()
//                    .detectLeakedClosableObjects()
//                    .penaltyLog()
//                    .penaltyDeath()
//                    .build()
//            )
//        }
        super.onCreate()
        app = this

    }

    companion object {
        private var app: App? = null
        public val startTime = Calendar.getInstance().time

        val context: Context
            get() {
                return app!!.applicationContext
            }

        const val defaultTransitionTime = 5

        private const val minimumActionCount = 20
        private const val lastReviewRequestVersionKey = "lastReviewRequestVersion"
        private const val reviewActionCountKey = "reviewActionCount"

        fun requestReviewIfAppropriate(activity: Activity) {
            val sharedPrefs = SharedPreferencesHandler.getPrefs().sharedPreferences
            val versionCode = BuildConfig.VERSION_CODE
            val lastVersionCode = sharedPrefs.getInt(lastReviewRequestVersionKey, -1)

            if (lastVersionCode != -1 && lastVersionCode == versionCode) return

            val actionCount = sharedPrefs.getInt(reviewActionCountKey, 0) + 1
            sharedPrefs.edit {
                putInt(reviewActionCountKey, actionCount)
            }

            if (actionCount < minimumActionCount) return

            val manager = ReviewManagerFactory.create(activity.applicationContext)

            val request = manager.requestReviewFlow()
            request.addOnSuccessListener {
                manager.launchReviewFlow(activity, it)
            }
            request.addOnFailureListener {
                debug(it.message)
            }
            sharedPrefs.edit {
                putInt(reviewActionCountKey, 0)
                putInt(lastReviewRequestVersionKey, versionCode)
            }
        }

        fun actionSuccessful() {
            val sharedPrefs = SharedPreferencesHandler.getPrefs().sharedPreferences
            val actionCount = sharedPrefs.getInt(reviewActionCountKey, 0) + 1
            sharedPrefs.edit {
                putInt(reviewActionCountKey, actionCount)
            }
        }

        fun actionFailed() {
            val sharedPrefs = SharedPreferencesHandler.getPrefs().sharedPreferences
            val actionCount = sharedPrefs.getInt(reviewActionCountKey, 0) - 1
            sharedPrefs.edit {
                putInt(reviewActionCountKey, actionCount)
            }
        }
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