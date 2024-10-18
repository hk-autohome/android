package com.harshkanjariya.autohome.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycleWithValue(
    lifecycleOwner: LifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext
): T = collectAsStateWithLifecycle(
    initialValue = this.value,
    lifecycle = lifecycleOwner.lifecycle,
    minActiveState = minActiveState,
    context = context
).value

@Composable
fun <T> MutableStateFlow<T>.collectAsStateWithLifecycleWithValue(
    lifecycleOwner: LifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext
): T = collectAsStateWithLifecycle(
    initialValue = this.value,
    lifecycle = lifecycleOwner.lifecycle,
    minActiveState = minActiveState,
    context = context
).value
