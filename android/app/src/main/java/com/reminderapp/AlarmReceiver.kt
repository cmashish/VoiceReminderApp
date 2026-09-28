package com.reminderapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != AlarmScheduler.ACTION_ALARM) {
            return
        }

        val id =
            intent.getStringExtra(AlarmScheduler.EXTRA_ID)
                ?: return

        val title =
            intent.getStringExtra(AlarmScheduler.EXTRA_TITLE)
                ?: "Reminder"

        val json =
            intent.getStringExtra(AlarmScheduler.EXTRA_JSON)
                ?: "{}"

        /*
         * Show notification immediately.
         *
         * This keeps the existing v5 lock-screen/full-screen behavior.
         */
        AlarmNotification.show(
            context,
            id,
            title,
            json
        )

        /*
         * Speak using Android ALARM audio instead of MEDIA/MUSIC audio.
         */
        speakReminder(
            context,
            id,
            title,
            json
        )
    }

    private fun speakReminder(
        context: Context,
        id: String,
        title: String,
        json: String
    ) {

        val message = try {

            org.json.JSONObject(json)
                .optString(
                    "message",
                    "It's time for your reminder."
                )

        } catch (_: Exception) {

            "It's time for your reminder."
        }

        val speech =
            if (title.isBlank()) {
                message
            } else {
                "$title. $message"
            }

        /*
         * BroadcastReceiver has limited lifetime.
         *
         * goAsync() keeps the receiver alive while TTS initializes
         * and speaks the reminder.
         */
        val pendingResult = goAsync()

        val appContext = context.applicationContext

        /*
         * Centralized alarm audio manager.
         */
        val alarmAudioManager =
            AlarmAudioManager(appContext)

        /*
         * Request alarm audio focus.
         */
        alarmAudioManager.requestAlarmAudioFocus()

        /*
         * If ALARM volume itself is zero, make it minimally audible.
         *
         * We do NOT force maximum volume.
         */
        alarmAudioManager.ensureAlarmVolumeAudible()

        var tts: TextToSpeech? = null

        tts = TextToSpeech(appContext) { status ->

            val engine = tts ?: run {

                alarmAudioManager.releaseAlarmAudioFocus()
                pendingResult.finish()

                return@TextToSpeech
            }

            if (status != TextToSpeech.SUCCESS) {

                engine.shutdown()

                alarmAudioManager.releaseAlarmAudioFocus()
                pendingResult.finish()

                return@TextToSpeech
            }

            /*
             * IMPORTANT:
             *
             * Tell Android that this TTS belongs to an ALARM.
             *
             * This is the main fix for the current problem.
             */
            try {

                engine.setAudioAttributes(
                    alarmAudioManager.getAudioAttributes()
                )

            } catch (_: Exception) {
                // Continue; TTS may still work on older engines.
            }

            /*
             * Set device default language.
             */
            val languageResult = try {

                engine.setLanguage(
                    Locale.getDefault()
                )

            } catch (_: Exception) {

                TextToSpeech.LANG_NOT_SUPPORTED
            }

            if (
                languageResult ==
                    TextToSpeech.LANG_MISSING_DATA ||
                languageResult ==
                    TextToSpeech.LANG_NOT_SUPPORTED
            ) {

                engine.shutdown()

                alarmAudioManager.releaseAlarmAudioFocus()
                pendingResult.finish()

                return@TextToSpeech
            }

            /*
             * Monitor TTS completion so that:
             *
             * 1. TTS engine is released
             * 2. Audio focus is released
             * 3. BroadcastReceiver finishes
             */
            engine.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {

                    override fun onStart(
                        utteranceId: String?
                    ) {
                        // TTS started.
                    }

                    override fun onDone(
                        utteranceId: String?
                    ) {

                        engine.shutdown()

                        alarmAudioManager
                            .releaseAlarmAudioFocus()

                        pendingResult.finish()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(
                        utteranceId: String?
                    ) {

                        engine.shutdown()

                        alarmAudioManager
                            .releaseAlarmAudioFocus()

                        pendingResult.finish()
                    }
                }
            )

            /*
             * Speak the reminder.
             *
             * AudioAttributes above force this TTS to use
             * ALARM audio usage.
             */
            val result = engine.speak(
                speech,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "REMINDER_$id"
            )

            if (result == TextToSpeech.ERROR) {

                engine.shutdown()

                alarmAudioManager
                    .releaseAlarmAudioFocus()

                pendingResult.finish()
            }
        }
    }
}