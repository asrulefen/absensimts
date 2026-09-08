package com.example.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SessionState(
    val isLoggedIn: Boolean = false,
    val secondsRemaining: Int = 0,
    val totalTimeoutSeconds: Int = 0,
    val showWarningDialog: Boolean = false,
    val wasAutoLoggedOut: Boolean = false
)

class SessionManager(
    private val scope: CoroutineScope,
    private val onAutoLogoutTriggered: () -> Unit = {}
) {
    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun startSession(timeoutSeconds: Int = 0) {
        // Auto-logout disabled as requested: session stays active indefinitely
        _sessionState.value = SessionState(
            isLoggedIn = true,
            secondsRemaining = 0,
            totalTimeoutSeconds = 0,
            showWarningDialog = false,
            wasAutoLoggedOut = false
        )
    }

    fun recordUserActivity() {
        // No-op: Auto logout is disabled
    }

    fun extendSession() {
        // No-op: Session is always active
    }

    fun endSession() {
        _sessionState.value = SessionState(
            isLoggedIn = false,
            secondsRemaining = 0,
            totalTimeoutSeconds = 0,
            showWarningDialog = false,
            wasAutoLoggedOut = false
        )
    }

    fun dismissAutoLogoutNotice() {
        _sessionState.update { it.copy(wasAutoLoggedOut = false) }
    }
}
