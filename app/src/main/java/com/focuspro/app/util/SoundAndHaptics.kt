package com.focuspro.app.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundAndHaptics {

    fun playCompletionChime(context: Context, soundEnabled: Boolean) {
        if (soundEnabled) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            }
        }
        vibrateSuccess(context)
    }

    fun vibrateSuccess(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(
                VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }
}
