package com.reminderapp

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import org.json.JSONObject

class AlarmActivity : Activity() {

    private lateinit var alarmAudioManager: AlarmAudioManager

    private var alarmId: String = ""

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        /*
         * Lock-screen support.
         */
        if (android.os.Build.VERSION.SDK_INT >= 27) {

            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        /*
         * Keep screen on while alarm is active.
         */
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        /*
         * Full-screen immersive alarm UI.
         */
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        /*
         * Alarm audio manager.
         */
        alarmAudioManager =
            AlarmAudioManager(this)

        /*
         * Request alarm audio focus.
         */
        alarmAudioManager.requestAlarmAudioFocus()

        /*
         * Make sure alarm stream isn't completely muted.
         */
        alarmAudioManager.ensureAlarmVolumeAudible()

        setContentView(R.layout.activity_alarm)

        alarmId =
            intent.getStringExtra(
                AlarmScheduler.EXTRA_ID
            ) ?: ""

        val json =
            intent.getStringExtra(
                AlarmScheduler.EXTRA_JSON
            ) ?: "{}"

        val title =
            intent.getStringExtra(
                AlarmScheduler.EXTRA_TITLE
            ) ?: "Reminder"

        val message = try {

            JSONObject(json)
                .optString(
                    "message",
                    "It's time for your reminder."
                )

        } catch (_: Exception) {

            "It's time for your reminder."
        }

        findViewById<TextView>(
            R.id.alarm_title
        ).text = title

        findViewById<TextView>(
            R.id.alarm_message
        ).text = message

        /*
         * STOP
         */
        findViewById<Button>(
            R.id.stop_button
        ).setOnClickListener {

            stopAlarm()

        }

        /*
         * SNOOZE
         */
        findViewById<Button>(
            R.id.snooze_button
        ).setOnClickListener {

            snoozeAlarm(
                json,
                title,
                message
            )
        }
    }

    private fun stopAlarm() {

        /*
         * Cancel notification.
         */
        AlarmNotification.cancel(
            this,
            alarmId
        )

        /*
         * Cancel scheduled alarm.
         */
        AlarmScheduler.cancel(
            this,
            alarmId
        )

        /*
         * Remove persisted alarm.
         */
        AlarmStorage.remove(
            this,
            alarmId
        )

        /*
         * Release alarm audio focus.
         */
        alarmAudioManager.releaseAlarmAudioFocus()

        /*
         * Close full-screen alarm.
         */
        finishAndRemoveTask()
    }

    private fun snoozeAlarm(
        json: String,
        title: String,
        message: String
    ) {

        /*
         * Cancel current notification.
         */
        AlarmNotification.cancel(
            this,
            alarmId
        )

        val alarm = try {

            JSONObject(json)

        } catch (_: Exception) {

            JSONObject()
        }

        /*
         * Existing app behavior:
         * Snooze for 10 minutes.
         */
        val timestamp =
            System.currentTimeMillis() + 600_000L

        alarm.put(
            "id",
            alarmId
        )

        alarm.put(
            "timestamp",
            timestamp
        )

        alarm.put(
            "enabled",
            true
        )

        alarm.put(
            "title",
            alarm.optString(
                "title",
                title
            )
        )

        alarm.put(
            "message",
            message
        )

        /*
         * Persist snoozed alarm.
         */
        AlarmStorage.upsert(
            this,
            alarm
        )

        /*
         * Schedule again.
         */
        AlarmScheduler.schedule(
            this,
            alarmId,
            alarm.optString(
                "title",
                title
            ),
            timestamp,
            alarm.toString()
        )

        /*
         * Release alarm audio focus.
         */
        alarmAudioManager.releaseAlarmAudioFocus()

        /*
         * Close alarm screen.
         */
        finishAndRemoveTask()
    }

    override fun onDestroy() {

        /*
         * Safety release.
         */
        if (::alarmAudioManager.isInitialized) {

            alarmAudioManager
                .releaseAlarmAudioFocus()
        }

        super.onDestroy()
    }
}