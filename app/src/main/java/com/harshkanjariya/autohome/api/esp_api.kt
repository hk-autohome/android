package com.harshkanjariya.autohome.api

import android.util.Log
import com.harshkanjariya.autohome.api.dto.EspDeviceInfoDto
import com.harshkanjariya.autohome.api.dto.getResponseType
import com.pluto.plugins.network.okhttp.PlutoOkhttpInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
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
    val url = "http://$deviceIp/device_info"
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
            body == deviceId
        }
    } catch (e: IOException) {
        onError("Network error: ${e.message}")
        false
    } catch (e: Exception) {
        onError("Unexpected error: ${e.message}")
        false
    }
}

fun verifyPassword(deviceIp: String, password: String, onError: (String) -> Unit): Boolean {
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

fun getEspDeviceInfo(gatewayIp: String): EspDeviceInfoDto {
    val responseType = getResponseType<EspDeviceInfoDto>()
    val response = Api.getInstance().getSync<EspDeviceInfoDto>("http://$gatewayIp/device_info", responseType)
        ?: throw Exception("Invalid response")

    // Check if the response length is 24
    if (response.deviceId.length != 24) {
        throw Exception("Invalid response")
    }

    // Validate if the response is a hexadecimal string
    if (!response.deviceId.matches(Regex("^[0-9a-fA-F]+$"))) {
        throw Exception("Response is not a valid hexadecimal string")
    }

    return response
}

fun getDeviceSsidList(gatewayIp: String): List<String> {
    val responseType = getResponseType<List<String>>()
    return Api.getInstance().getSync<List<String>>("http://$gatewayIp/ssids", responseType) ?: listOf()
}

fun updateEspWifiConfig(gatewayIp: String, ssid: String, password: String, devicePassword: String, onComplete: () -> Unit) {
    val payload = mutableMapOf(
        "ssid" to ssid,
        "password" to password
    )

    if (devicePassword.isNotEmpty()) {
        payload["device_password"] = devicePassword
    }

    val jsonPayload = payload.map { "\"${it.key}\": \"${it.value}\"" }.joinToString(prefix = "{", postfix = "}")

    Api.getInstance().post(
        "http://$gatewayIp/wifi_config",
        jsonPayload,
        callback = object: Api.ApiResponseCallback {
            override fun onSuccess(response: Response) {
                onComplete()
            }
            override fun onFailure(e: IOException) {
            }
        }
    )
}
