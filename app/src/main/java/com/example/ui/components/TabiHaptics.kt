package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

@Immutable
class TabiHaptics(
    private val view: View,
    private val composeHaptic: HapticFeedback,
    private val context: Context
) {
    /** Crisp micro-tap for standard buttons, nav items, chips, and cards. */
    fun tap() {
        val ok = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (!ok) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /** Tactile tick for toggles, switches, 3D card flips, and quiz choices. */
    fun toggle() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            HapticFeedbackConstants.CLOCK_TICK
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        if (!view.performHapticFeedback(constant)) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    /** Distinct affirmative confirmation pulse for budget entries, itinerary saves, and check-ins. */
    fun confirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)) return
        }
        vibrateWaveform(longArrayOf(0, 18, 36, 26), intArrayOf(0, 150, 0, 220))
    }

    /** Celebratory dual-beat pulse when completing a language module or quiz. */
    fun celebrateModule() {
        val pulsed = vibrateWaveform(
            timings = longArrayOf(0, 24, 55, 36, 55, 50),
            amplitudes = intArrayOf(0, 160, 0, 210, 0, 255)
        )
        if (!pulsed) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    /** Crisp reject feedback for invalid codes or validation errors. */
    fun error() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (view.performHapticFeedback(HapticFeedbackConstants.REJECT)) return
        }
        composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    private fun vibrateWaveform(timings: LongArray, amplitudes: IntArray): Boolean {
        return runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return false

            if (!vibrator.hasVibrator()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
            true
        }.getOrDefault(false)
    }
}

@Composable
fun rememberTabiHaptics(): TabiHaptics {
    val view = LocalView.current
    val composeHaptic = LocalHapticFeedback.current
    val context = LocalContext.current
    return remember(view, composeHaptic, context) {
        TabiHaptics(view = view, composeHaptic = composeHaptic, context = context)
    }
}
