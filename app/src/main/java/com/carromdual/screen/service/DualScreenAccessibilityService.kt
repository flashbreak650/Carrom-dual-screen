package com.carromdual.screen.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class DualScreenAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No screen content reading needed; used solely for GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN.
    }

    override fun onInterrupt() {
        // Nothing to interrupt
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    fun triggerSplitScreen(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
        } catch (_: Throwable) {
            false
        }
    }

    companion object {
        @Volatile
        private var instance: DualScreenAccessibilityService? = null

        fun isEnabled(): Boolean = instance != null

        fun requestSplitScreenToggle(): Boolean {
            return instance?.triggerSplitScreen() ?: false
        }

        fun requestRecents(): Boolean {
            return try {
                instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) ?: false
            } catch (_: Throwable) {
                false
            }
        }
    }
}
