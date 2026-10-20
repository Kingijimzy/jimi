package com.jimi.app.agent

import com.jimi.app.ui.OrbState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the Orb visual state.
 * The Agent and UI both observe / update this.
 */
object OrbStateManager {
    private val _state = MutableStateFlow(OrbState.IDLE)
    val state: StateFlow<OrbState> = _state.asStateFlow()

    fun set(newState: OrbState) {
        _state.value = newState
    }

    fun idle() = set(OrbState.IDLE)
    fun listening() = set(OrbState.LISTENING)
    fun thinking() = set(OrbState.THINKING)
    fun speaking() = set(OrbState.SPEAKING)
    fun acting() = set(OrbState.ACTING)
    fun error() = set(OrbState.ERROR)
}
