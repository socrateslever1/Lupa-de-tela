package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

class HapticFeedbackHelper(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var wasAtMaxZoom = false

    /**
     * Called whenever zoom level changes. If zoom reaches maximum, produces a strong tactile buzz.
     */
    fun onZoomChanged(currentZoom: Float, maxZoom: Float, composeHaptics: HapticFeedback? = null) {
        val isAtMax = currentZoom >= (maxZoom - 0.05f)

        if (isAtMax && !wasAtMaxZoom) {
            // Edge-triggered: just entered maximum zoom!
            performMaxZoomHaptic(composeHaptics)
            wasAtMaxZoom = true
        } else if (!isAtMax && wasAtMaxZoom) {
            wasAtMaxZoom = false
        }
    }

    /**
     * Distinct tactile pattern for maximum optical/digital zoom limit.
     */
    fun performMaxZoomHaptic(composeHaptics: HapticFeedback? = null) {
        try {
            composeHaptics?.performHapticFeedback(HapticFeedbackType.LongPress)

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Double pulse punch to indicate limit reached
                    val timings = longArrayOf(0, 70, 50, 90)
                    val amplitudes = intArrayOf(0, 200, 0, 255)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(120)
                }
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    /**
     * Subtle click for filter switches or zoom stop steps
     */
    fun performStepClick(composeHaptics: HapticFeedback? = null) {
        try {
            composeHaptics?.performHapticFeedback(HapticFeedbackType.TextHandleMove)

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(25, 80))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(25)
                }
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
    }
}
