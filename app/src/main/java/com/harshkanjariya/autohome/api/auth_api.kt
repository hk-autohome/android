package com.harshkanjariya.autohome.api

import com.harshkanjariya.autohome.api.dto.ApiResponseDto
import com.harshkanjariya.autohome.api.dto.GetTokenResponse
import com.harshkanjariya.autohome.api.dto.getResponseType
import com.harshkanjariya.autohome.utils.ApiUrl

fun getAuthToken(googleIdToken: String): String? {
    val responseType = getResponseType<ApiResponseDto<GetTokenResponse>>()
    return Api.getInstance()
        .getSync<ApiResponseDto<GetTokenResponse>>(
            ApiUrl.GET_AUTH_TOKEN,
            responseType,
            mapOf("token" to googleIdToken)
        )?.data?.token
}
