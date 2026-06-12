package com.aurora.aonev3

import com.aurora.aonev3.synthetic.*
import androidx.lifecycle.MutableLiveData
import java.util.*

class MutableLiveDataArrayList<T>(c: Collection<T?>) : MutableLiveData<ArrayList<T>>(ArrayList(c)) {

    constructor(vararg element: T) : this(element.asList())

    constructor(): this(emptyList())

    private var mDataLock = Object()

    fun add(element: T) {
        if (value?.contains(element) != true) {
            synchronized(mDataLock) {
                value?.add(element)
                postValue(value)
            }
        } else {
            replace(element)
        }
    }

    fun add(collection: Collection<T>) {
        collection.forEach { element ->
            if (value?.contains(element) != true) {
                synchronized(mDataLock) {
                    value?.add(element)
                }
            } else {
                replace(element, false)
            }
        }
        postValue(value)
    }

    private fun replace(element: T, postValue: Boolean) {
        synchronized(mDataLock) {
            val index = value?.indexOf(element) ?: -1
            if (index != -1) {
                value?.set(index, element)
                if (postValue) {
                    postValue(value)
                }
            }
        }
    }

    fun replace(element: T) {
        replace(element, true)
    }

    fun replace(collection: Collection<T>) {
        synchronized(mDataLock) {
            collection.forEach { element ->
                val index = value?.indexOf(element) ?: -1
                if (index != -1) {
                    value?.set(index, element)
                }
            }
        }
        postValue(value)
    }

    fun remove(element: T) {
        synchronized(mDataLock) {
            value?.remove(element)
            postValue(value)
        }
    }

    fun remove(collection: Collection<T>) {
        collection.forEach { element ->
            synchronized(mDataLock) {
                value?.remove(element)
            }
        }
        postValue(value)
    }

    fun removeAll(predicate: (T) -> Boolean): Boolean {
        return value?.removeAll(predicate) ?: false
    }

    fun addRemove(toAdd: Collection<T>, toRemove: Collection<T>) {
        synchronized(mDataLock) {
            toAdd.forEach { element ->
                if (value?.contains(element) != true) {
                        value?.add(element)
                } else {
                    val index = value?.indexOf(element) ?: -1
                    if (index != -1) {
                        value?.set(index, element)
                    }
                }
            }

            toRemove.forEach { element ->
                value?.remove(element)
            }
        }
        postValue(value)
    }

    fun clear() {
        synchronized(mDataLock) {
            value?.clear()
            postValue(value)
        }
    }
}
