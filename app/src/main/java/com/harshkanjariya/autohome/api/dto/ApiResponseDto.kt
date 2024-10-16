package com.harshkanjariya.autohome.api.dto

import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

data class ApiResponseDto<T>(
    val statusCode: Int,
    val data: T,
)

inline fun <reified T> getResponseType(): Type {
    return object : TypeToken<T>() {}.type
}
