package com.aurora.aonev3.network.volley

import com.aurora.aonev3.synthetic.*
import android.util.Log
import androidx.annotation.WorkerThread
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request.Method.*
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import com.aurora.aonev3.debugError
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

open class Request(
    method: Int,
    url: String?,
    requestBody: String?,
    listener: Response.Listener<JSONObject>?,
    errorListener: Response.ErrorListener?,
    maxRetries: Int = 0
) : JsonRequest<JSONObject>(method, url, requestBody, listener, errorListener) {

    init {
        retryPolicy =
            DefaultRetryPolicy(6000, maxRetries, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        setShouldRetryServerErrors(true)
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
        val jsonResponse = JSONObject()

        response?.let{
            jsonResponse.put("statusCode", response.statusCode)
            jsonResponse.put("headers", response.headers)

            if (response.data.isNotEmpty()) {
                val jsonString = String(response.data)
                jsonResponse.put("statusCode", response.statusCode)

                val json = try {
                    JSONTokener(jsonString).nextValue()
                } catch (ex: JSONException) {
                    crashlytics.recordException(ex)
                    ex.printStackTrace()
                    return Response.error(ParseError(ex))
                }

                jsonResponse.put("body", json)
            }
        }

        return Response.success(jsonResponse,
            HttpHeaderParser.parseCacheHeaders(response))
    }

    companion object {
        private const val TAG = "Request"

        private val crashlytics = FirebaseCrashlytics.getInstance()

        @WorkerThread
        suspend fun get(url: String, maxRetries: Int = 2, first: Boolean = false): JSONObject {
            return suspendCoroutine { cont ->
                Log.v(
                    TAG, """request:
                            GET - $url"""
                )
                val request = Request(
                    GET,
                    url,
                    null,
                    { response ->
                        Log.v(TAG, """request response:
                            GET - $url
                                $response"""
                        )
                        try {
                            cont.resume(response)
                        } catch (ex: IllegalStateException) {
                            debugError(ex.localizedMessage)
                            ex.printStackTrace()
                        }
                    },
                    { error ->
                        val msg = """exception:
                            GET - $url
                            status code - ${error.networkResponse?.statusCode}
                            response body - ${error.networkResponse?.data?.toString(Charsets.UTF_8)}"""
                        crashlytics.log("E/$TAG: $msg")
                        Log.e(TAG, msg)
                        try {
                            cont.resumeWithException(error)
                        } catch (ex: IllegalStateException) {
                            ex.printStackTrace()
                        }
                    },
                    maxRetries
                )

                RequestQueue.addToRequestQueue(request, first)
            }
        }

        @WorkerThread
        suspend fun post(url: String, body: JSONObject, maxRetries: Int = 0, first: Boolean = false) =
            post(url, body.toString(), maxRetries, first)

        @WorkerThread
        suspend fun post(url: String, body: JSONArray, maxRetries: Int = 0, first: Boolean = false) =
            post(url, body.toString(), maxRetries, first)

        @WorkerThread
        suspend fun post(url: String, body: String?, maxRetries: Int = 0, first: Boolean = false): JSONObject {
            return suspendCoroutine { cont ->
                Log.v(TAG, """request:
                            POST - $url,
                            body - $body"""
                )
                val request = Request(
                    POST,
                    url,
                    body,
                    { response ->
                        Log.v(TAG, """request response:
                            POST - $url,
                            body - $body,
                                $response"""
                        )
                        cont.resume(response)
                    },
                    { error ->
                        val msg = """exception:
                            POST - $url
                            body - ${if (body?.contains("password") != true) { body } else { "hidden due to password" }}
                            status code - ${error.networkResponse?.statusCode}
                            response body - ${error.networkResponse?.data?.toString(Charsets.UTF_8)}"""
                        crashlytics.log("E/$TAG: $msg")
                        Log.e(TAG, msg)
                        cont.resumeWithException(error)
                    },
                    maxRetries
                )

                RequestQueue.addToRequestQueue(request, first)
            }
        }

        @WorkerThread
        suspend fun put(url: String, body: JSONObject?, maxRetries: Int = 2, first: Boolean = false) =
            put(url, body?.toString(), maxRetries, first)

        @WorkerThread
        suspend fun put(url: String, body: JSONArray, maxRetries: Int = 2, first: Boolean = false) =
            put(url, body.toString(), maxRetries, first)

        @WorkerThread
        suspend fun put(url: String, body: String?, maxRetries: Int = 2, first: Boolean = false): JSONObject {
            return suspendCoroutine { cont ->
                Log.v(TAG, """request:
                            PUT - $url,
                            body - $body"""
                )
                val request = Request(
                    PUT,
                    url,
                    body,
                    { response ->
                        Log.v(TAG, """request response:
                            PUT - $url,
                            body - $body,
                                $response"""
                        )
                        cont.resume(response)
                    },
                    { error ->
                        val msg = """exception:
                            PUT - $url
                            body - ${if (body?.contains("password") != true) { body } else { "hidden due to password" }}
                            status code - ${error.networkResponse?.statusCode}
                            response body - ${error.networkResponse?.data?.toString(Charsets.UTF_8)}"""
                        crashlytics.log("E/$TAG: $msg")
                        Log.e(TAG, """exception:
                            PUT - $url,
                            body - $body
                                ${error.networkResponse?.statusCode}
                                ${error.networkResponse?.data?.toString(Charsets.UTF_8)}"""
                        )
                        cont.resumeWithException(error)
                    },
                    maxRetries
                )

                RequestQueue.addToRequestQueue(request, first)
            }
        }

        @WorkerThread
        suspend fun delete(url: String, maxRetries: Int = 2, first: Boolean = false): JSONObject {
            return suspendCoroutine { cont ->
                Log.v(TAG, """request:
                            DELETE - $url"""
                )
                val request = Request(
                    DELETE,
                    url,
                    null,
                    { response ->
                        Log.v(TAG, """request response:
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
                        Log.e(TAG, """exception:
                            DELETE - $url
                                ${error.networkResponse?.statusCode}
                                ${error.networkResponse?.data?.toString(Charsets.UTF_8)}"""
                        )
                        cont.resumeWithException(error)
                    },
                    maxRetries
                )

                RequestQueue.addToRequestQueue(request, first)
            }
        }
    }
}
