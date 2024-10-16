package com.harshkanjariya.autohome

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.harshkanjariya.autohome.api.Api
import com.pluto.Pluto
import com.pluto.plugins.network.PlutoNetworkPlugin
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application() {
    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Pluto.Installer(this)
                .addPlugin(PlutoNetworkPlugin())
                .install()
        }
        Api.init(dataStore)
    }
}
