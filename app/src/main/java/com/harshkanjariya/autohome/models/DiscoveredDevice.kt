package com.harshkanjariya.autohome.models

data class DiscoveredDevice(
    val ip: String,
    val id: String,
    val isRegistered: Boolean,
    val version: Int,
    val pinCode: String,
    var password: String? = null,
)