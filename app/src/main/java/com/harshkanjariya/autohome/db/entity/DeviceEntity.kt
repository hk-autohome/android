package com.harshkanjariya.autohome.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val ip: String,
    val name: String,
    val password: String
)
