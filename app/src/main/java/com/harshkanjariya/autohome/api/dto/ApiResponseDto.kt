package com.harshkanjariya.autohome.api.dto

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

data class ApiResponseDto<T>(
    val statusCode: Int,
    val data: T,
)

inline fun <reified T> getResponseType(): Type {
    return object : TypeToken<T>() {}.type
}

data class MqttPayloadDto(
    val id: String,
    val pin: Int,
) {
    fun toJson(): String {
        return "$id,$pin"
    }
}