package com.harshkanjariya.autohome.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson

@Entity(tableName = "buttons")
data class ButtonEntity(
    @PrimaryKey val id: String = "",
    val pinNumber: Int,
    val name: String,
    var on: Boolean,
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
}
