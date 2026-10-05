package com.reminderapp

import android.content.BroadcastReceiver
import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: android.content.Intent) {
        if (intent.action != AlarmScheduler.ACTION_ALARM) return

        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Reminder"
        val json = intent.getStringExtra(AlarmScheduler.EXTRA_JSON) ?: "{}"

        AlarmNotification.show(context, id, title, json)
        AlarmScheduler.scheduleNextRepeat(context.applicationContext, json)
        speakReminder(context, id, title, json)
    }

    private fun speakReminder(context: Context, id: String, title: String, json: String) {
        val message = try {
            org.json.JSONObject(json).optString("message", "It's time for your reminder.")
        } catch (_: Exception) {
            "It's time for your reminder."
        }

        val speech = if (title.isBlank()) message else "$title. $message"
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val alarmAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        if (audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM) <= 0) {
            val max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
            if (max > 0) {
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, 1, 0)
            }
        }

        var tts: TextToSpeech? = null
        tts = TextToSpeech(appContext) { status ->
            val engine = tts ?: run {
                pendingResult.finish()
                return@TextToSpeech
            }

            if (status != TextToSpeech.SUCCESS) {
                engine.shutdown()
                pendingResult.finish()
                return@TextToSpeech
            }

            try {
                engine.setAudioAttributes(alarmAttributes)
            } catch (_: Exception) {
                // Older/third-party TTS engines may not support the attribute.
            }

            val languageResult = try {
                engine.setLanguage(Locale.getDefault())
            } catch (_: Exception) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }

            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                engine.shutdown()
                pendingResult.finish()
                return@TextToSpeech
            }

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    engine.shutdown()
                    pendingResult.finish()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    engine.shutdown()
                    pendingResult.finish()
                }
            })

            // Explicitly request normal TTS gain (1.0) while routing through
            // the Android ALARM audio usage/stream.
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }

            val result = engine.speak(
                speech,
                TextToSpeech.QUEUE_FLUSH,
                params,
                "REMINDER_$id",
            )

            if (result == TextToSpeech.ERROR) {
                engine.shutdown()
                pendingResult.finish()
            }
        }
    }
}
