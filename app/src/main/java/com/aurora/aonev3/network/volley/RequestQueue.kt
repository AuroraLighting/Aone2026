package com.aurora.aonev3.network.volley

import com.android.volley.RequestQueue.RequestEvent.REQUEST_FINISHED
import com.android.volley.toolbox.Volley
import com.aurora.aonev3.App
import com.jackmills.queue.Queue
import com.android.volley.RequestQueue as rq

object RequestQueue {
    private var requestQueueCount = 0
    private val requestQueue: rq by lazy {
        Volley.newRequestQueue(App.context.applicationContext)
    }.apply {
        value.addRequestEventListener { _, event ->
            if (event != REQUEST_FINISHED) return@addRequestEventListener
            if (requestQueueCount > 0) {
                requestQueueCount--
            }
        }
    }

    private val queueLock = Object()
    private var mIsClearing = false
    private val queue = Queue<Request>().apply {
        setQueueCallback(object: Queue.QueueCallback<Request> {
            override fun onProcess(item: Request) {
                if (mIsClearing) return
                if (requestQueueCount < 4) {
                    requestQueueCount++
                    requestQueue.add(item)
                } else {
                    this@apply.pushHead(item)
                }
            }
        })

        delay = 200
    }

    fun addToRequestQueue(request: Request, first: Boolean = false) {
        if (mIsClearing) return
        synchronized(queueLock) {
            queue.apply {
                if (!first) {
                    pushTail(request)
                } else {
                    pushHead(request)
                }
                if (!isProcessing) {
                    start()
                }
            }
        }
    }

    fun clear() {
        mIsClearing = true
        synchronized(queueLock) {
            queue.clear()
            mIsClearing = false
        }
    }
}