package com.harshkanjariya.autohome.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harshkanjariya.autohome.db.entity.ButtonEntity

@Dao
interface ButtonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertButton(button: ButtonEntity)

    @Query("SELECT buttonNumber FROM buttons WHERE deviceId = :deviceId")
    fun getButtonsForDevice(deviceId: String): List<Int>
}
