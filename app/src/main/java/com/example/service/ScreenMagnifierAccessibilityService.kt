package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityService.MagnificationController
import android.view.accessibility.AccessibilityManager

class ScreenMagnifierAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ScreenMagnifierAccessibilityService? = null

        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(context.packageName)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event interception required
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    /**
     * Controls native Android magnification across all apps and screens.
     */
    fun applyMagnification(scale: Float, centerX: Float, centerY: Float, isWindowMode: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val mode = if (isWindowMode) {
                    android.accessibilityservice.MagnificationConfig.MAGNIFICATION_MODE_WINDOW
                } else {
                    android.accessibilityservice.MagnificationConfig.MAGNIFICATION_MODE_FULLSCREEN
                }
                val config = android.accessibilityservice.MagnificationConfig.Builder()
                    .setMode(mode)
                    .setScale(scale)
                    .setCenterX(centerX)
                    .setCenterY(centerY)
                    .build()
                magnificationController.setMagnificationConfig(config, true)
            } catch (e: Exception) {
                // Fallback to legacy API
                applyLegacyMagnification(scale, centerX, centerY)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            applyLegacyMagnification(scale, centerX, centerY)
        }
    }

    private fun applyLegacyMagnification(scale: Float, centerX: Float, centerY: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                magnificationController.setScale(scale, true)
                magnificationController.setCenter(centerX, centerY, true)
            } catch (_: Exception) {}
        }
    }

    fun resetMagnification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                magnificationController.reset(true)
            } catch (_: Exception) {}
        }
    }
}
