package com.reminderapp

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import org.json.JSONObject

class AlarmActivity : Activity() {
    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        setContentView(R.layout.activity_alarm)

        val id = intent.getStringExtra(AlarmScheduler.EXTRA_ID) ?: ""
        val json = intent.getStringExtra(AlarmScheduler.EXTRA_JSON) ?: "{}"
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE) ?: "Reminder"
        val message = try {
            JSONObject(json).optString("message", "It's time for your reminder.")
        } catch (_: Exception) {
            "It's time for your reminder."
        }

        findViewById<TextView>(R.id.alarm_title).text = title
        findViewById<TextView>(R.id.alarm_message).text = message

        findViewById<Button>(R.id.stop_button).setOnClickListener {
            AlarmNotification.cancel(this, id)
            AlarmScheduler.cancel(this, id)
            AlarmStorage.remove(this, id)
            finishAndRemoveTask()
        }

        findViewById<Button>(R.id.snooze_button).setOnClickListener {
            AlarmNotification.cancel(this, id)
            val alarm = try { JSONObject(json) } catch (_: Exception) { JSONObject() }
            val timestamp = System.currentTimeMillis() + 600_000L
            alarm.put("id", id)
            alarm.put("timestamp", timestamp)
            alarm.put("enabled", true)
            alarm.put("title", alarm.optString("title", title))
            alarm.put("message", message)
            AlarmStorage.upsert(this, alarm)
            AlarmScheduler.schedule(this, id, alarm.optString("title", title), timestamp, alarm.toString())
            finishAndRemoveTask()
        }
    }
}
