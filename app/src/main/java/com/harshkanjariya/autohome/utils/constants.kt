package com.harshkanjariya.autohome.utils

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.harshkanjariya.autohome.db.entity.ButtonEntity

object ApiUrl {
    const val FIREBASE_TOKEN = "/users/firebase/token"
    const val DELETE_USER = "/users/delete"
    const val GET_AUTH_TOKEN = "/users/google/sign-in"
    const val USER_DEVICES = "/user-devices"
}

const val DATA_STORE_NAME = "settings"

object DataStoreKeys {
    val TOKEN = stringPreferencesKey("token")
    val API_BASE_URL = stringPreferencesKey("API_BASE_URL")
    val SETTINGS_SHOW_DEVICE_DETAILS = booleanPreferencesKey("SETTINGS_SHOW_DEVICE_DETAILS")
}

object RemoteConfigKeys {
    const val API_BASE_URL = "apiBaseUrl"
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

object NavRoutes {
    const val DEVICE_LIST = "devicesList"
    const val SETTINGS = "settings"
    fun DEVICE_DETAILS(device: String = "{device}") = "deviceDetails/$device"
    const val NEW_DEVICE = "new_device"
}
