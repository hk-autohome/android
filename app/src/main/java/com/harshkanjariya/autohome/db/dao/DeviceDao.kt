package com.harshkanjariya.autohome.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.harshkanjariya.autohome.db.entity.DeviceEntity

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDevice(device: DeviceEntity)

    @Query("SELECT * FROM devices")
    fun getDevices(): List<DeviceEntity>

    @Query("SELECT * FROM devices where deviceId = :id")
    fun getDeviceById(id: String): DeviceEntity

    @Query("UPDATE devices set name = :name where deviceId = :id")
    fun updateName(name: String, id: String)
}
