package com.aurora.aonev3

import com.aurora.aonev3.synthetic.*
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

/**
 * Created by Jack Mills on 09/05/2018.
 */
//object SharedPreferencesHandler {
//    val sharedPreferences: SharedPreferences by lazy {
//        // applicationContext is key, it keeps you from leaking the
//        // Activity or BroadcastReceiver if someone passes one in.
//        val preferences = App.context.getSharedPreferences("aone_prefs", MODE_PRIVATE)
//
//        FirebaseCrashlytics.getInstance().log("E/SharedPreferences:$preferences")
//        Log.d("SharedPreferences", "$preferences")
//
//        preferences
//    }
//}

class SharedPreferencesHandler {
    val sharedPreferences: SharedPreferences = App.context.getSharedPreferences("aone_prefs", MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: SharedPreferencesHandler? = null

        fun getPrefs(force: Boolean = false): SharedPreferencesHandler {
            if (force) INSTANCE = null

            return INSTANCE ?: synchronized(this) {
                val instance = SharedPreferencesHandler()
                INSTANCE = instance
                return instance
            }
        }
    }
}
