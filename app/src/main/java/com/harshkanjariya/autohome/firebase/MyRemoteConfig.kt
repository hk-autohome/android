package com.harshkanjariya.autohome.firebase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.harshkanjariya.autohome.BuildConfig
import com.harshkanjariya.autohome.utils.DataStoreKeys
import com.harshkanjariya.autohome.utils.RemoteConfigKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyRemoteConfig {
    companion object {
        fun init(dataStore: DataStore<Preferences>) {
            val settings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = BuildConfig.FIREBASE_REMOTE_CONFIG_FETCH_INTERVAL
            }
            val config = Firebase.remoteConfig
            config.setConfigSettingsAsync(settings)
            config.fetchAndActivate()

            listen(dataStore)
        }

        private fun listen(dataStore: DataStore<Preferences>) {
            Firebase.remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    if (configUpdate.updatedKeys.contains(RemoteConfigKeys.API_BASE_URL)) {
                        Firebase.remoteConfig.activate().addOnCompleteListener {
                            updateApiBaseUrl(dataStore)
                        }
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                }
            })
        }

        private fun updateApiBaseUrl(dataStore: DataStore<Preferences>) {
            CoroutineScope(Dispatchers.IO).launch {
                dataStore.edit { preferences ->
                    preferences[DataStoreKeys.API_BASE_URL] =
                        Firebase.remoteConfig.getString(RemoteConfigKeys.API_BASE_URL)
                }
            }
        }
    }
}