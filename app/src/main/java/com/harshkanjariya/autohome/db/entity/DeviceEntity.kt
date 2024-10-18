package com.harshkanjariya.autohome.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val deviceId: String,
    val localIp: String,
    val name: String,
    val password: String,
    val version: Int,
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
}
