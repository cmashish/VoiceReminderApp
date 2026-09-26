package com.reminderapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_ALARM) return

        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: return
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Reminder"
        val json = intent.getStringExtra(AlarmScheduler.EXTRA_JSON) ?: "{}"

        AlarmNotification.show(context, id, title, json)
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

            val languageResult = try {
                engine.setLanguage(Locale.getDefault())
            } catch (_: Exception) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }

            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
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

            val result = engine.speak(
                speech,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "REMINDER_$id"
            )

            if (result == TextToSpeech.ERROR) {
                engine.shutdown()
                pendingResult.finish()
            }
        }
    }
}
