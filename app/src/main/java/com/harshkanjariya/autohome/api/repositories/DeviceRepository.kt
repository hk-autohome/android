package com.harshkanjariya.autohome.api.repositories

import android.content.Context
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
    }
}