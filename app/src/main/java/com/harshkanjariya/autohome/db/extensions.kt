package com.harshkanjariya.autohome.db

import android.content.Context
import com.harshkanjariya.autohome.db.entity.DeviceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun AppDatabase.Companion.addDeviceToDatabase(device: DeviceEntity, context: Context) {
    val db = getDatabase(context)
    CoroutineScope(Dispatchers.IO).launch {
        db.deviceDao().insertDevice(device)
    }
}
