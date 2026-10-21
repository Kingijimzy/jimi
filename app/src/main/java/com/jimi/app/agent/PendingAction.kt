package com.jimi.app.agent

/**
 * Holds a sensitive action until the user confirms in the UI.
 */
data class PendingAction(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val message: String,
    val action: () -> String
)

object PendingActionBus {
    @Volatile
    var current: PendingAction? = null
        private set

    private var onChange: ((PendingAction?) -> Unit)? = null

    fun setListener(listener: (PendingAction?) -> Unit) {
        onChange = listener
        listener(current)
    }

    fun request(action: PendingAction) {
        current = action
        onChange?.invoke(action)
    }

    fun confirm(): String? {
        val a = current ?: return null
        current = null
        onChange?.invoke(null)
        return try {
            a.action()
        } catch (e: Exception) {
            "Action failed: ${e.message}"
        }
    }

    fun cancel() {
        current = null
        onChange?.invoke(null)
    }
}
