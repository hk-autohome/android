package com.harshkanjariya.autohome.api

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.harshkanjariya.autohome.BuildConfig
import com.harshkanjariya.autohome.api.dto.ApiResponseDto
import com.harshkanjariya.autohome.utils.DataStoreKeys
import com.pluto.plugins.network.okhttp.PlutoOkhttpInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.lang.reflect.Type
import javax.inject.Singleton

@Singleton
class Api private constructor() {
    private lateinit var jwtToken: String
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(PlutoOkhttpInterceptor)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $jwtToken")
                .build()
            chain.proceed(request)
        }
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
                        instance?.jwtToken = token
                    }
                }
            }
        }

        fun getInstance(): Api {
            return instance ?: throw IllegalStateException("Api must be initialized first by calling init()")
        }
    }


    fun get(endpoint: String, callback: ApiResponseCallback) {
        val request = Request.Builder()
            .url(BASE_URL + endpoint)
            .addHeader("Authorization", "Bearer $jwtToken")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                callback.onSuccess(response)
            }
        })
    }

    // POST Request
    fun post(endpoint: String, jsonBody: String, callback: ApiResponseCallback? = null) {
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(BASE_URL + endpoint)
            .addHeader("Authorization", "Bearer $jwtToken")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback?.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                callback?.onSuccess(response)
            }
        })
    }

    // PUT Request
    fun put(endpoint: String, jsonBody: String, callback: ApiResponseCallback) {
        val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(BASE_URL + endpoint)
            .put(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                callback.onSuccess(response)
            }
        })
    }

    // DELETE Request
    fun delete(endpoint: String, callback: ApiResponseCallback) {
        val request = Request.Builder()
            .url(BASE_URL + endpoint)
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                callback.onSuccess(response)
            }
        })
    }


    fun getRaw(endpoint: String): String? {
        val request = Request.Builder()
            .url(BASE_URL + endpoint)
            .get()
            .build()

        val response = client.newCall(request).execute()

        return response.body?.string()
    }

    fun <T> getSync(endpoint: String, responseType: Type): ApiResponseDto<T>? {
        val request = Request.Builder()
            .url(BASE_URL + endpoint)
            .get()
            .build()

        val response = client.newCall(request).execute()

        // Convert the response body to string
        val stringResponse = response.body?.string()

        // Deserialize using Gson
        return stringResponse?.let {
            Gson().fromJson(it, responseType)
        }
    }
    interface ApiResponseCallback {
        fun onSuccess(response: Response)
        fun onFailure(e: IOException)
    }
}

