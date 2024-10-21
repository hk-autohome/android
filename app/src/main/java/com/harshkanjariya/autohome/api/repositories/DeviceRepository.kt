package com.harshkanjariya.autohome.api.repositories

import android.content.Context
import android.widget.Button
import com.google.gson.Gson
import com.harshkanjariya.autohome.api.Api
import com.harshkanjariya.autohome.api.dto.getResponseType
import com.harshkanjariya.autohome.db.entity.ButtonEntity
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import com.harshkanjariya.autohome.utils.ApiUrl
import okhttp3.Response
import java.io.IOException

class DeviceRepository {
    companion object {
        val api = Api.getInstance()

        fun getDevices(page: Int = 1, limit: Int = 10): List<DeviceEntity> {
            val responseType = getResponseType<List<DeviceEntity>>()

            try {
                return api.getSync<List<DeviceEntity>>(
                    ApiUrl.USER_DEVICES,
                    responseType,
                    mapOf(
                        "page" to "$page",
                        "limit" to "$limit"
                    ),
                    true,
                ) ?: return emptyList()
            } catch (error: Exception) {
                return emptyList()
            }
        }

        fun addDevice(device: DeviceEntity, onSuccess: () -> Unit) {
            api.post(ApiUrl.USER_DEVICES, device.toJson(), object : Api.ApiResponseCallback {
                override fun onSuccess(response: Response) {
                    onSuccess()
                }

                override fun onFailure(e: IOException) {}
            })
        }

        fun getButtonsForDevice(id: String, context: Context): List<ButtonEntity> {
            // TODO: complete this
            return emptyList()
        }

        fun addButtonForDevice(id: String, pinNumber: Int, name: String, context: Context) {
            // TODO: complete this
        }

        fun updateDevice(deviceId: String, name: String, buttons: List<ButtonEntity>) {
            // Create a mutable map to hold the JSON body
            val requestBody = mutableMapOf<String, Any>()

            // Add 'name' to the request body if it's not empty
            if (name.isNotEmpty()) {
                requestBody["name"] = name
            }

            // Add 'buttons' to the request body if the list is not empty
            if (buttons.isNotEmpty()) {
                requestBody["buttons"] = buttons
            }

            // Convert the map to JSON string
            val jsonBody = Gson().toJson(requestBody)

            // Make the API call with the dynamically generated JSON body
            Api.getInstance().put(ApiUrl.USER_DEVICES + "/$deviceId", jsonBody, object : Api.ApiResponseCallback {
                override fun onSuccess(response: Response) {
                    // Handle success
                }

                override fun onFailure(e: IOException) {
                    // Handle failure
                }
            }, true)
        }

    }
}