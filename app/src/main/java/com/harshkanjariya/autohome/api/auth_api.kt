package com.harshkanjariya.autohome.api

import com.harshkanjariya.autohome.api.dto.GetTokenResponse
import com.harshkanjariya.autohome.api.dto.getResponseType
import com.harshkanjariya.autohome.utils.ApiUrl

fun getAuthToken(): String? {
    val responseType = getResponseType<GetTokenResponse>()
    return Api.getInstance()
        .getSync<GetTokenResponse>(ApiUrl.GET_AUTH_TOKEN, responseType)?.data?.token
}
