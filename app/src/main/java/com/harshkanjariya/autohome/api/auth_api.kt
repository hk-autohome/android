package com.harshkanjariya.autohome.api

import com.harshkanjariya.autohome.api.dto.ApiResponseDto
import com.harshkanjariya.autohome.api.dto.GetTokenResponse
import com.harshkanjariya.autohome.api.dto.getResponseType
import com.harshkanjariya.autohome.utils.ApiUrl
import okhttp3.Response
import java.io.IOException

fun getAuthToken(googleIdToken: String): String? {
    val responseType = getResponseType<GetTokenResponse>()
    return Api.getInstance()
        .getSync<GetTokenResponse>(
            ApiUrl.GET_AUTH_TOKEN,
            responseType,
            mapOf("token" to googleIdToken)
        )?.token
}

fun deleteAccountPermanently(onComplete: () -> Unit) {
    Api.getInstance().delete(
        ApiUrl.DELETE_USER,
        object: Api.ApiResponseCallback {
            override fun onSuccess(response: Response) {
                onComplete()
            }
            override fun onFailure(e: IOException) {}
        }, true)
}

