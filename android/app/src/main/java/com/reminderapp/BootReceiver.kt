package com.reminderapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action != Intent.ACTION_BOOT_COMPLETED && i.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

        val alarms = AlarmStorage.get(c)
        val now = System.currentTimeMillis()

        for (n in 0 until alarms.length()) {
            val alarm = alarms.optJSONObject(n) ?: continue
            if (!alarm.optBoolean("enabled", true)) continue

            val timestamp = alarm.optLong("timestamp")
            if (timestamp > now) {
                AlarmScheduler.schedule(
                    c,
                    alarm.optString("id"),
                    alarm.optString("title", "Reminder"),
                    timestamp,
                    alarm.toString(),
                )
            } else {
                // If a repeating alarm's stored timestamp is already in the past
                // because the device was powered off, calculate its next valid run.
                AlarmScheduler.scheduleNextRepeat(c, alarm.toString())
            }
        }
    }
}
