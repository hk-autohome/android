package com.harshkanjariya.autohome.api.dto

data class EspDeviceInfoDto(
    val deviceId: String,
    val version: Int,
    val isRegistered: Boolean,
    val pinCode: String,
)