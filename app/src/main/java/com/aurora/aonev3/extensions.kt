package com.aurora.aonev3

import com.aurora.aonev3.synthetic.*
import android.app.Activity
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.core.content.edit
import com.aurora.aonev3.data.groups.Group
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.network.handlers.NabtoHandler
import com.aurora.aonev3.network.handlers.SyncHandler
import com.aurora.aonev3.network.volley.RequestQueue
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.json.JSONArray
import java.lang.RuntimeException


fun String.replace(vararg elementsToReplace: Pair<String, Any?>): String {
    var string = this
    elementsToReplace.forEach {
        string = string.replace(it.first, it.second.toString())
    }

    return string
}

fun JSONArray.indices(): IntRange {
    return 0 until length()
}

fun JSONArray.isEmpty(): Boolean {
    return length() == 0
}

operator fun JSONArray.contains(element: Any): Boolean {
    for (i in this.indices()) {
        if (element == this.opt(i)) {
            return true
        }
    }

    ArrayList<Boolean>().any { it }
    return false
}

fun JSONArray.any(predicate: (Any) -> Boolean): Boolean {
    for (i in this.indices()) {
        val element = this.opt(i)
        if (predicate(element)) return true
    }
    return false
}

fun JSONArray.toIntArray(): IntArray {
    val destination = ArrayList<Int>()
    for (i in this.indices()) {
        this.opt(i)?.let {
            if (it is Int) {
                destination.add(it)
            }
        }
    }

    return destination.toIntArray()
}

fun String.toCapitalisedLowerCase(): String {
    return this.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun <T : Any>ArrayList<T>.replaceOrAdd(element: T) {
    val index = this.indexOf(element)
    if (index != -1) {
        this[index] = element
    } else {
        this.add(element)
    }
}

fun buildTrigger(path: String): Trigger {
    return ResourceUpdateTrigger(path)
}

fun buildCondition(
    path: String,
    value: Any
): Condition {
    return buildCondition(path, "##INVAL## == $value")
}

fun buildCondition(
    path: String,
    rule: String
): Condition {
    return ResourceValueCondition(path, rule)
}

//fun buildAction1(
//    path: String,
//    dataKey: String = "value",
//    value: Any
//): Action {
//    val data = JsonObject()
//    when (value) {
//        is Number -> data.addProperty(dataKey, value)
//        is Boolean -> data.addProperty(dataKey, value)
//        is String -> data.addProperty(dataKey, value)
//        is Array<*>,
//        is Iterable<*> -> data.add(dataKey, gson.toJsonTree(value))
//    }
//    return UpdateResourceAction(path, data)
//}

inline fun JSONArray.forEach(action: (Any) -> Unit) {
    for (i in this.indices()) action(this[i])
}

fun debug(msg: String?) = Log.d(getCallerClassName(), msg ?: "null")

fun debugError(msg: String?) = Log.e(getCallerClassName(), msg ?: "null")

private fun getCallerClassName(): String? {
    val stElements = Thread.currentThread().stackTrace
    for (i in 1 until stElements.size) {
        val ste = stElements[i]
        if (ste.className != "com.aurora.aonev3.ExtensionsKt" && ste.className.indexOf("java.lang.Thread") != 0) {
            return try {
                Class.forName(ste.className).simpleName
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }

        }
    }
    return null
}

fun Group.findGroupsNestedIn(parentGroups: ArrayList<Int>): List<Group> {
    val gateway = this.parentGateway
    val groups = SyncHandler
        .groupsList
        .filter {
            it.parentGateway == gateway
                    && (it.metadata.optJSONArray("nested_groups")?.contains(id) ?: false)
        }
        .toMutableList()

    val allGroups = emptyList<Group>().toMutableList()

    parentGroups.add(id)

    groups.forEach {
        if (!parentGroups.contains(it.id)) {
            allGroups.addAll(it.findGroupsNestedIn(parentGroups))
        }
    }

    allGroups.addAll(groups)
    return allGroups.distinctBy { it.id }
}

fun allowEditing(): Boolean {
    return (NabtoHandler.selectedGateway?.accessLevel == NabtoHandler.GatewayAccessLevel.OWNER
        || NabtoHandler.selectedGateway?.accessLevel == NabtoHandler.GatewayAccessLevel.FULL_ACCESS)
        && !SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("office_mode", false)
}

fun enableSunriseSunset() = BuildConfig.DEBUG

class TimeoutException: Exception {
    constructor(): super()
    constructor(s: String?): super(s)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}

class InsufficientSpaceException: Exception {
    constructor(): super()
    constructor(s: String?): super(s)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}

class UnknownApiException: Exception {
    constructor(): super()
    constructor(s: String?): super(s)
    constructor(message: String?, cause: Throwable?): super(message, cause)
    constructor(cause: Throwable?): super(cause)
}

fun signOut() {
    NabtoHandler.nabtoGateways.forEach {
        it.isConnected = false
    }
    NabtoHandler.signOut()
    RequestQueue.clear()

    val homeTourDone = SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("homeTourDone", false)
    val allDevicesTourDone = SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("allDevicesTourDone", false)
    val groupTourDone = SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("groupTourDone", false)
    val introDone = SharedPreferencesHandler.getPrefs().sharedPreferences.getBoolean("introDone", false)
    SharedPreferencesHandler.getPrefs().sharedPreferences.edit {
        clear()
        putBoolean("homeTourDone", homeTourDone)
        putBoolean("allDevicesTourDone", allDevicesTourDone)
        putBoolean("groupTourDone", groupTourDone)
        putBoolean("introDone", introDone)
    }
    SyncHandler.signOut()
}

fun hideSoftKeyboard(activity: Activity?) {
    val inputMethodManager = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
    if (inputMethodManager.isAcceptingText) {
        val windowToken = activity.currentFocus?.windowToken ?: return
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }
}

fun Int.isPositive() = this > 0

fun Int.isNegative() = this < 0

fun getString(resId: Int) = App.context.getString(resId)
