package com.aurora.aonev3.network.volley

import com.aurora.aonev3.synthetic.*
import android.util.Log
import androidx.annotation.WorkerThread
import com.android.volley.*
import com.android.volley.Request.Method.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class JwtTokenRequest(
    method: Int,
    url: String?,
    requestBody: String?,
    private val token: String,
    listener: Response.Listener<JSONObject>?,
    errorListener: Response.ErrorListener?,
    maxRetries: Int = 0
) : Request(method, url, requestBody, listener, errorListener) {

    init {
        retryPolicy =
            DefaultRetryPolicy(60 * 1000, maxRetries, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        setShouldRetryServerErrors(true)
    }

    //map headers to string request
    override fun getHeaders(): Map<String, String> {
        val headers = HashMap<String, String>()
        headers["Accept"] = "application/json"
        headers["Authorization"] = "Token $token"
        return headers
    }

    companion object {
        private const val TAG = "JwtTokenRequest"

        private val crashlytics = FirebaseCrashlytics.getInstance()

        @WorkerThread
        suspend fun get(url: String, token: String): JSONObject {
            return suspendCoroutine { cont ->
                Log.v(
                    TAG, """request:
                            GET - $url"""
                )
                val request = JwtTokenRequest(
                    GET,
                    url,
                    null,
                    token,
                    { response ->
                        Log.v(
                            TAG, """request response:
                            GET - $url
                                $response"""
                        )
                        cont.resume(response)
                    },
                    { error ->
                        val msg = """exception:
                            GET - $url
                            status code - ${error.networkResponse?.statusCode}
                            response body - ${error.networkResponse?.data?.toString(Charsets.UTF_8)}"""
                        crashlytics.log("E/$TAG: $msg")
                        Log.e(TAG, msg)
                        cont.resumeWithException(error)
                    }
                )

                RequestQueue.addToRequestQueue(request)
            }
        }

        @WorkerThread
        suspend fun post(url: String, body: JSONObject?, token: String) =
            post(url, body?.let { body.toString() }, token)

        @WorkerThread
        suspend fun post(url: String, body: JSONArray, token: String) =
            post(url, body.toString(), token)

        @WorkerThread
        private suspend fun post(url: String, body: String?, token: String): JSONObject {
            return suspendCoroutine { cont ->
                Log.v(
                    TAG, """request:
                            POST - $url,
                            body - $body"""
                )
                val request = JwtTokenRequest(
                    POST,
                    url,
                    body,
                    token,
                    { response ->
                        Log.v(
                            TAG, """request response:
                            POST - $url,
                            body - $body,
                                $response"""
                        )
                        cont.resume(response)
                    },
                    { error ->
                        val msg = """exception:
                            POST - $url
                            body - $body
                            status code - ${error.networkResponse?.statusCode}
                            response body - ${error.networkResponse?.data?.toString(Charsets.UTF_8)}"""
                        crashlytics.log("E/$TAG: $msg")
                        Log.e(TAG, msg)
                        cont.resumeWithException(error)
                    }
                )

                RequestQueue.addToRequestQueue(request)
            }
        }

        @WorkerThread
        suspend fun put(url: String, body: JSONObject, token: String, maxRetries: Int = 2) =
            put(url, body.let { body.toString() }, token, maxRetries)

        @WorkerThread
        suspend fun put(url: String, body: JSONArray, token: String, maxRetries: Int = 2) =
            put(url, body.toString(), token, maxRetries)

        @WorkerThread
        private suspend fun put(
            url: String,
            body: String?,
            token: String,
            maxRetries: Int = 2
        ): JSONObject {
            return suspendCoroutine { cont ->
                Log.v(
                    TAG, """request:
                            PUT - $url,
                            body - $body"""
                )
                val request = JwtTokenRequest(
                    PUT,
                    url,
                    body,
                    token,
                    { response ->
                        Log.v(
                            TAG, """request response:
                            PUT - $url,
                            body - $body,
                                $response"""
                        )
                        cont.resume(response)
                    },
                    { error ->
                        val msg = """exception:
                            PUT - $url
                            body - $body
                            status code - ${error.networkResponse?.statusCode}
                            response body - ${error.networkResponse?.data?.toString(Charsets.UTF_8)}"""
                        crashlytics.log("E/$TAG: $msg")
                        Log.e(TAG, msg)
                        cont.resumeWithException(error)
                    },
                    maxRetries
                )

                RequestQueue.addToRequestQueue(request)
            }
        }


        @WorkerThread
        suspend fun patch(url: String, body: JSONObject, token: String) =
            patch(url, body.let { body.toString() }, token)

        @WorkerThread
        private suspend fun patch(url: String, body: String?, token: String): JSONObject {
            return suspendCoroutine { cont ->
                Log.v(
                    TAG, """request:
                            PATCH - $url,
                            body - $body"""
                )
                val request = JwtTokenRequest(
                    PATCH,
                    url,
                    body,
                    token,
                    { response ->
                        Log.v(
                            TAG, """request response:
                            PATCH - $url,
                            body - $body,
                                $response"""
                        )
                        cont.resume(response)
                    },
                    { error ->
                        val msg = """exception:
                            PATCH - $url
                            body - $body
                            status code - ${error.networkResponse?.statusCode}
                            response body - ${error.networkResponse?.data?.toString(Charsets.UTF_8)}"""
                        crashlytics.log("E/$TAG: $msg")
                        Log.e(TAG, msg)
                        cont.resumeWithException(error)
                    })

                RequestQueue.addToRequestQueue(request)
            }
        }

        @WorkerThread
        suspend fun delete(url: String, token: String): JSONObject {
            return suspendCoroutine { cont ->
                Log.v(
                    TAG, """request:
                            DELETE - $url"""
                )
                val request = JwtTokenRequest(
                    DELETE,
                    url,
                    null,
                    token,
                    { response ->
                        Log.v(
                            TAG, """request response:
                            DELETE - $url,
                                $response"""
                        )
                        cont.resume(response)
                    },
                    { error ->
                        val msg = """exception:
                            DELETE - $url
                            status code - ${error.networkResponse?.statusCode}
                            response body - ${error.networkResponse?.data?.toString(Charsets.UTF_8)}"""
                        crashlytics.log("E/$TAG: $msg")
                        Log.e(TAG, msg)
                        cont.resumeWithException(error)
                    }
                )

                RequestQueue.addToRequestQueue(request)
            }
        }
    }
}
