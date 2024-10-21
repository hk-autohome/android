package com.harshkanjariya.autohome.utils

import androidx.datastore.preferences.core.stringPreferencesKey
import com.harshkanjariya.autohome.db.entity.ButtonEntity

object ApiUrl {
    const val FIREBASE_TOKEN = "/users/firebase/token"
    const val GET_AUTH_TOKEN = "/auth/google/sign-in"
    const val USER_DEVICES = "/user-devices"
}

const val DATA_STORE_NAME = "settings"
object DataStoreKeys {
    val TOKEN = stringPreferencesKey("token")
}

fun getPinNumbers() = listOf(4, 13, 16, 17, 18, 19, 21, 22, 23)

fun getPinIndex(number: Int) = getPinNumbers().indexOf(number)

fun getDefaultButtons(): List<ButtonEntity> {
    return getPinNumbers().mapIndexed { index, _ ->
        ButtonEntity(
            pinNumber = index,
            name = "",
            on = false
        )
    }
}