package com.harshkanjariya.autohome.utils

import androidx.datastore.preferences.core.stringPreferencesKey

object ApiUrl {
    const val FIREBASE_TOKEN = "/users/firebase/token"
    const val GET_AUTH_TOKEN = "/auth/token"
}

const val DATA_STORE_NAME = "settings"
object DataStoreKeys {
    val TOKEN = stringPreferencesKey("token")
}