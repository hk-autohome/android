package com.harshkanjariya.autohome.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.harshkanjariya.autohome.api.dto.getResponseType

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val deviceId: String,
    val localIp: String,
    val name: String,
    val password: String,
    val version: Int,
    val pinCode: String,
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(jsonString: String): DeviceEntity {
            val typeToken = getResponseType<DeviceEntity>()
            return Gson().fromJson(jsonString, typeToken)
        }
    }
}
