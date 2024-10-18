package com.harshkanjariya.autohome.ui.main

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import com.harshkanjariya.autohome.api.getEspDeviceInfo
import com.harshkanjariya.autohome.models.DiscoveredDevice
import com.harshkanjariya.autohome.utils.DataStoreKeys
import com.harshkanjariya.autohome.utils.MVIBaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val dataStore: DataStore<Preferences>,
) : MVIBaseViewModel<MainContract.State, MainContract.Event, MainContract.Effect>() {
    override fun createInitialState(): MainContract.State {
        return MainContract.State()
    }

    override fun handleEvent(event: MainContract.Event) {
        when (event) {
            else -> {}
        }
    }

    override fun handleEffect(effect: MainContract.Effect) {
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            dataStore.edit {
                it.clear()
                onComplete()
            }
        }
    }

    fun verifyAuthToken(onFail: () -> Unit) {
        viewModelScope.launch {
            dataStore.data.map { preferences ->
                preferences[DataStoreKeys.TOKEN] ?: ""
            }.collect { token ->
                if (token.isEmpty()) {
                    onFail()
                } else {
                    setState {
                        copy(isAuthenticated = true)
                    }
                }
            }
        }
    }

    fun updateGatewayIp(gatewayIp: String) {
        setState {
            copy(gatewayIp = gatewayIp)
        }
    }
}