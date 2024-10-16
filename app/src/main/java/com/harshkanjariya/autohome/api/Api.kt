package com.harshkanjariya.autohome.api

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.harshkanjariya.autohome.BuildConfig
import com.harshkanjariya.autohome.utils.DataStoreKeys
import com.pluto.plugins.network.okhttp.PlutoOkhttpInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.reflect.Type
import javax.inject.Singleton

@Singleton
class Api private constructor() {
    private var jwtToken: String? = null

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(PlutoOkhttpInterceptor)
        .build()

    companion object {
        private const val BASE_URL = BuildConfig.API_BASE_URL
        private var instance: Api? = null

        // Initialize the Api instance with DataStore
        fun init(dataStore: DataStore<Preferences>) {
            if (instance == null) {
                instance = Api()
                // Read JWT token from DataStore and assign it to the Api instance
                CoroutineScope(Dispatchers.IO).launch {
                    dataStore.data.map { preferences ->
                        preferences[DataStoreKeys.TOKEN] ?: ""
                    }.collect { token ->
                        instance?.jwtToken = if (token.isNotEmpty()) token else null
                    }
                }
            }
        }

        fun getInstance(): Api {
            return instance ?: throw IllegalStateException("Api must be initialized first by calling init()")
        }
    }

    // Helper function to append BASE_URL if the endpoint does not have a scheme or hostname
    private fun buildUrl(endpoint: String): String {
        return if (endpoint.toHttpUrlOrNull() == null) {
            // If the endpoint is a relative URL (no scheme), append BASE_URL
            BASE_URL + endpoint
        } else {
            // Endpoint is already a complete URL
            endpoint
        }
    }

    // GET Request with optional token
    fun get(endpoint: String, callback: ApiResponseCallback, token: Boolean = true) {
        val requestBuilder = Request.Builder()
            .url(buildUrl(endpoint))
            .get()

        // Add the token header if provided
        if (token) {
            requestBuilder.addHeader("Authorization", "Bearer $jwtToken")
        }

        client.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                callback.onSuccess(response)
            }
        })
    }

    fun <T> getSync(endpoint: String, responseType: Type, queryData: Map<String, String> = mapOf()): T? {
        val baseUrl = (buildUrl(endpoint)).toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid URL")

        val urlBuilder = baseUrl.newBuilder()
            .apply {
                queryData.forEach { (key, value) ->
                    addQueryParameter(key, value)
                }
            }

        val request = Request.Builder()
            .url(urlBuilder.build()) // Use the built URL
            .get()
            .build()

        val response = client.newCall(request).execute()

        // Convert the response body to string
        val stringResponse = response.body?.string()

        // Deserialize using Gson
        return stringResponse?.let {
            Log.e("TAG", "getSync: $it")
            val tmp = Gson().fromJson<T>(it, responseType)
            Log.e("TAG", "getSync: $tmp")
            tmp
        }
    }

    // POST Request with optional token
    fun post(endpoint: String, jsonBody: String, callback: ApiResponseCallback? = null, token: Boolean = true) {
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val requestBuilder = Request.Builder()
            .url(buildUrl(endpoint))
            .post(body)

        // Add the token header if provided
        if (token) {
            requestBuilder.addHeader("Authorization", "Bearer $jwtToken")
        }

        client.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback?.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                callback?.onSuccess(response)
            }
        })
    }

    // PUT Request with optional token
    fun put(endpoint: String, jsonBody: String, callback: ApiResponseCallback, token: Boolean = true) {
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val requestBuilder = Request.Builder()
            .url(buildUrl(endpoint))
            .put(body)

        // Add the token header if provided
        if (token) {
            requestBuilder.addHeader("Authorization", "Bearer $jwtToken")
        }

        client.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                callback.onSuccess(response)
            }
        })
    }

    // DELETE Request with optional token
    fun delete(endpoint: String, callback: ApiResponseCallback, token: Boolean = true) {
        val requestBuilder = Request.Builder()
            .url(buildUrl(endpoint))
            .delete()

        // Add the token header if provided
        if (token) {
            requestBuilder.addHeader("Authorization", "Bearer $jwtToken")
        }

        client.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                callback.onSuccess(response)
            }
        })
    }

    interface ApiResponseCallback {
        fun onSuccess(response: Response)
        fun onFailure(e: IOException)
    }
}

