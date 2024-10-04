package com.harshkanjariya.autohome.api

import android.util.Log
import com.pluto.Pluto
import com.pluto.plugins.network.okhttp.PlutoOkhttpInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

fun triggerSwitch(deviceIp: String, pin: Int, password: String, onComplete: (String) -> Unit, onError: (String) -> Unit) {
    val url = "http://$deviceIp/control?pin=$pin&password=$password"
    val client = OkHttpClient.Builder()
        .addInterceptor(PlutoOkhttpInterceptor)
        .build()

    val request = Request.Builder()
        .url(url)
        .build()

    try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                onError("Error: ${response.code} ${response.message}")
                return
            }
            val body = response.body?.string() ?: ""
            onComplete(body)
        }
    } catch (e: IOException) {
        onError("Network error: ${e.message}")
    } catch (e: Exception) {
        onError("Unexpected error: ${e.message}")
    }
}

fun checkDeviceStatus(deviceIp: String, deviceId: String, onError: (String) -> Unit): Boolean {
    val url = "http://$deviceIp/device_id"
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(url)
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                onError("Error: ${response.code} ${response.message}")
                return false
            }
            val body = response.body?.string() ?: ""
            onError("")
            body == deviceId // Return true if the ID matches
        }
    } catch (e: IOException) {
        onError("Network error: ${e.message}")
        false
    } catch (e: Exception) {
        onError("Unexpected error: ${e.message}")
        false
    }
}

suspend fun verifyPassword(deviceIp: String, password: String, onError: (String) -> Unit): Boolean {
    val url = "http://$deviceIp/validate_password?device_password=$password"
    val client = OkHttpClient.Builder()
        .addInterceptor(PlutoOkhttpInterceptor)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    val requestBody = okhttp3.FormBody.Builder()
        .add("device_password", password)
        .build()

    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                onError("Error: ${response.code} ${response.message}")
                return false
            }
            val body = response.body?.string() ?: ""
            Log.e("TAG", "verifyPassword: $body")
            onError("")
            body == "1"
        }
    } catch (e: IOException) {
        onError("Network error: ${e.message}")
        false
    } catch (e: Exception) {
        e.printStackTrace()
        onError("Unexpected error: ${e.message}")
        false
    }
}
