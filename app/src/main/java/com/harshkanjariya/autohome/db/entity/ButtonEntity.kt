package com.harshkanjariya.autohome.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buttons")
data class ButtonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val buttonNumber: Int,
    val name: String
)
