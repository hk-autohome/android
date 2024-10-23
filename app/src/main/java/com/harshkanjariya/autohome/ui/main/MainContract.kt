package com.harshkanjariya.autohome.ui.main

import com.harshkanjariya.autohome.models.DiscoveredDevice
import com.harshkanjariya.autohome.utils.MVIEffect
import com.harshkanjariya.autohome.utils.MVIEvent
import com.harshkanjariya.autohome.utils.MVIState

class MainContract {
    data class State(
        val isAuthenticated: Boolean = false,
        val gatewayIp: String = "",
        val showDeviceDetails: Boolean = false,
    ) : MVIState

    sealed class Event : MVIEvent

    sealed class Effect : MVIEffect
}