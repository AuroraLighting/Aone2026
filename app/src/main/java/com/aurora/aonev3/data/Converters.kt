package com.aurora.aonev3.data

import com.aurora.aonev3.synthetic.*
import android.util.Log
import androidx.room.TypeConverter
import com.aurora.aonev3.gson
import com.aurora.aonev3.indices
import com.aurora.aonev3.logic.*
import com.aurora.aonev3.data.devices.Device
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

//class Converters {
//    val TAG = this::class.simpleName
//    @TypeConverter
//    fun stringArrayListFromString(value: String?): ArrayList<String>? {
//        return value?.let {
//            try {
//                val json = JSONArray(it)
//                val arrayList: ArrayList<String> = ArrayList()
//                for (i in json.indices()) {
//                    arrayList.add(json.optString(i))
//                }
//
//                arrayList
//            } catch (ex: JSONException) {
//                Log.e(TAG, ex.message ?: "")
//                null
//            }
//        }
//    }
//
//    @TypeConverter
//    fun stringArrayListToString(list: ArrayList<String>?): String? {
//        val json = JSONArray()
//
//        list?.forEach {
//            json.put(it)
//        } ?: return null
//
//        return json.toString()
//    }
//
//    @TypeConverter
//    fun stringArrayFromString(value: String?): Array<String>? {
//        return value?.let {
//            try {
//                val json = JSONArray(it)
//                val array: Array<String> = Array(size = json.length()) { i ->
//                    json.optString(i)
//                }
//
//                array
//            } catch (ex: JSONException) {
//                Log.e(TAG, ex.message ?: "")
//                null
//            }
//        }
//    }
//
//    @TypeConverter
//    fun stringArrayToString(list: Array<String>?): String? {
//        val json = JSONArray()
//
//        list?.forEach {
//            json.put(it)
//        } ?: return null
//
//        return json.toString()
//    }
//
//    @TypeConverter
//    fun jsonObjectFromString(value: String?): JSONObject? {
//        return value?.let {
//            try {
//                JSONObject(value)
//            } catch (ex: JSONException) {
//                Log.e(TAG, ex.message ?: "")
//                null
//            }
//        }
//    }
//
//    @TypeConverter
//    fun jsonObjectToString(json: JSONObject?): String? {
//        return json?.toString()
//    }
//
//    @TypeConverter
//    fun jsonArrayFromString(value: String?): JSONArray? {
//        return value?.let {
//            try {
//                JSONArray(value)
//            } catch (ex: JSONException) {
//                Log.e(TAG, ex.message ?: "")
//                null
//            }
//        }
//    }
//
//    @TypeConverter
//    fun collectionMetadataToString(data: CollectionMetadata?): String? {
//        return gson.toJson(data)
//    }
//
//    @TypeConverter
//    fun collectionMetadataFromString(value: String?): CollectionMetadata? {
//        return value?.let {
//            try {
//                gson.fromJson(it, CollectionMetadata::class.java)
//            } catch (ex: Exception) {
//                Log.e(TAG, ex.message ?: "")
//                null
//            }
//        }
//    }
//
//    @TypeConverter
//    fun ruleMetadataToString(data: RuleMetadata?): String? {
//        return gson.toJson(data)
//    }
//
//    @TypeConverter
//    fun ruleMetadataFromString(value: String?): RuleMetadata? {
//        return value?.let {
//            try {
//                gson.fromJson(it, RuleMetadata::class.java)
//            } catch (ex: Exception) {
//                Log.e(TAG, ex.message ?: "")
//                null
//            }
//        }
//    }
//
//    @TypeConverter
//    fun jsonArrayToString(json: JSONArray?): String? {
//        return json?.toString()
//    }
//
//    @TypeConverter
//    fun triggersFromString(value: String?): Array<Trigger>? {
//        return value?.let {
//            try {
//                val json = JSONArray(value)
//                Trigger.fromJson(json)
//            } catch (ex: Exception) {
//                Log.e(TAG, ex.message ?: "")
//                null
//            }
//        }
//    }
//
//    @TypeConverter
//    fun conditionsToString(data: Array<Condition>?): String? {
//        return gson.toJson(data)
//    }
//
//    @TypeConverter
//    fun conditionsFromString(value: String?): Array<Condition>? {
//        return value?.let {
//            try {
//                val json = JSONArray(value)
//                Condition.fromJson(json)
//            } catch (ex: Exception) {
//                Log.e(TAG, ex.message ?: "")
//                null
//            }
//        }
//    }
//
//    @TypeConverter
//    fun actionsToString(data: Array<Action>?): String? {
//        return gson.toJson(data)
//    }
//
//    @TypeConverter
//    fun actionsFromString(value: String?): Array<Action>? {
//        return value?.let {
//            try {
//                val json = JSONArray(value)
//                Action.fromJson(json)
//            } catch (ex: Exception) {
//                Log.e(TAG, ex.message ?: "")
//                null
//            }
//        }
//    }
//
//    @TypeConverter
//    fun triggersToString(data: Array<Trigger>?): String? {
//        return gson.toJson(data)
//    }
//
//    @TypeConverter
//    fun deviceClassEnumFromString(value: String?): Device.DeviceClass? {
//        return value?.let {
//            Device.DeviceClass.valueOf(it)
//        }
//    }
//
//    @TypeConverter
//    fun deviceClassEnumToString(deviceClass: Device.DeviceClass?): String? {
//        return deviceClass?.name
//    }
//
//    @TypeConverter
//    fun anyFromString(value: String?): Any? {
//        return if (value?.toIntOrNull() != null) {
//            value.toInt()
//        } else if (value?.toLowerCase() == "true" || value?.toLowerCase() == "false") {
//            value.toBoolean()
//        } else if (value?.toFloatOrNull() != null) {
//            value.toFloat()
//        } else if (value?.toDoubleOrNull() != null) {
//            value.toDouble()
//        } else {
//            value
//        }
//    }
//
//    @TypeConverter
//    fun anyToString(value: Any?): String? {
//        return value?.toString()
//    }
//}
