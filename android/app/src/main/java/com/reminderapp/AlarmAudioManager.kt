package com.reminderapp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

class AlarmAudioManager(private val context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * IMPORTANT:
     * USAGE_ALARM makes this audio an alarm rather than normal music/media.
     */
    private val alarmAudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

    private val audioFocusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
                .setAudioAttributes(alarmAudioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .build()
        } else {
            null
        }

    /**
     * Request audio focus using the ALARM audio usage.
     */
    fun requestAlarmAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            audioFocusRequest?.let { request ->
                audioManager.requestAudioFocus(request) ==
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } ?: false

        } else {

            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    /**
     * Release alarm audio focus after TTS finishes.
     */
    fun releaseAlarmAudioFocus() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
            }

        } else {

            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    /**
     * Return current Android ALARM stream volume.
     */
    fun getAlarmVolume(): Int {
        return audioManager.getStreamVolume(
            AudioManager.STREAM_ALARM
        )
    }

    /**
     * Return maximum Android ALARM stream volume.
     */
    fun getMaxAlarmVolume(): Int {
        return audioManager.getStreamMaxVolume(
            AudioManager.STREAM_ALARM
        )
    }

    /**
     * If alarm volume is completely zero, make it minimally audible.
     *
     * We intentionally DO NOT set it to maximum volume.
     */
    fun ensureAlarmVolumeAudible() {

        val currentVolume = getAlarmVolume()

        if (currentVolume <= 0) {

            val maxVolume = getMaxAlarmVolume()

            if (maxVolume > 0) {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    1.coerceAtMost(maxVolume),
                    0
                )
            }
        }
    }

    fun getAudioAttributes(): AudioAttributes {
        return alarmAudioAttributes
    }
}