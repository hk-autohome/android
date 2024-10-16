package com.harshkanjariya.autohome.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.harshkanjariya.autohome.utils.ApiUrl
import com.harshkanjariya.autohome.api.Api
import javax.inject.Inject

class MyFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var api: Api

    companion object {
        private const val TAG = "MyFirebaseMessagingService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        Api.getInstance().post(
            ApiUrl.FIREBASE_TOKEN,
            """{
                "token": $token
            }""".trimIndent()
        )
    }
}