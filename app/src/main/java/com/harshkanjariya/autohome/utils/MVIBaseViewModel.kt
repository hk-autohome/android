package com.harshkanjariya.autohome.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class MVIBaseViewModel<State : MVIState, Event : MVIEvent, Effect : MVIEffect> :
    ViewModel() {
    private val initialState by lazy { createInitialState() }

    val currentState: State
        get() = _state.value

    private val _state: MutableStateFlow<State> = MutableStateFlow(initialState)
    val state = _state.asStateFlow()

    private val _event: MutableSharedFlow<Event> = MutableSharedFlow()

    private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
    val effect = _effect.asSharedFlow()

    init {
        subscribeEvent()
        subscribeEffect()
    }

    private fun subscribeEffect() {
        viewModelScope.launch {
            _effect.collect {
                handleEffect(it)
            }
        }
    }

    private fun subscribeEvent() {
        viewModelScope.launch {
            _event.collect {
                handleEvent(it)
            }
        }
    }

    abstract fun createInitialState(): State

    abstract fun handleEffect(effect: Effect)

    abstract fun handleEvent(event: Event)

    protected fun setState(reduce: State.() -> State) {
        val newState = currentState.reduce()
        _state.value = newState
    }

    protected fun sendEvent(event: Event) {
        viewModelScope.launch { _event.emit(event) }
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { _effect.emit(effect) }
    }
}

interface MVIState

interface MVIEvent

interface MVIEffect
