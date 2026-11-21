package com.example.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

class SwipeHaptics(
    private val context: Context,
    private val view: View,
    private val composeHaptic: HapticFeedback,
) {
    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    private fun isHapticEnabled(): Boolean {
        return runCatching {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1,
            ) == 1
        }.getOrDefault(true)
    }

    fun onThresholdCrossed(isDeleteDirection: Boolean) {
        if (isDeleteDirection) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            vibrateOneShot(durationMs = 16L, amplitude = 170)
        } else {
            composeHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun onKeepCommitted() {
        val performed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            false
        }
        if (!performed) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun onDeleteCommitted() {
        val performed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            false
        }
        if (!performed) {
            composeHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        // Distinct crisp double-tap pulse for delete actions
        vibrateWaveform(
            timings = longArrayOf(0L, 26L, 38L, 44L),
            amplitudes = intArrayOf(0, 185, 0, 255),
        )
    }

    private fun vibrateOneShot(durationMs: Long, amplitude: Int) {
        val vib = vibrator ?: return
        if (!isHapticEnabled()) return
        runCatching {
            if (!vib.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp = if (vib.hasAmplitudeControl()) amplitude else VibrationEffect.DEFAULT_AMPLITUDE
                vib.vibrate(VibrationEffect.createOneShot(durationMs, amp))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        }
    }

    private fun vibrateWaveform(timings: LongArray, amplitudes: IntArray) {
        val vib = vibrator ?: return
        if (!isHapticEnabled()) return
        runCatching {
            if (!vib.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (vib.hasAmplitudeControl()) {
                    VibrationEffect.createWaveform(timings, amplitudes, -1)
                } else {
                    VibrationEffect.createWaveform(timings, -1)
                }
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(timings, -1)
            }
        }
    }
}

@Composable
fun rememberSwipeHaptics(): SwipeHaptics {
    val context = LocalContext.current
    val view = LocalView.current
    val composeHaptic = LocalHapticFeedback.current
    return remember(context, view, composeHaptic) {
        SwipeHaptics(context = context, view = view, composeHaptic = composeHaptic)
    }
}
