package com.reminderapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {
    const val ACTION_ALARM = "com.reminderapp.ACTION_ALARM"
    const val EXTRA_ID = "alarm_id"
    const val EXTRA_TITLE = "alarm_title"
    const val EXTRA_JSON = "alarm_json"

    fun schedule(c: Context, id: String, title: String, time: Long, json: String) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(c, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_JSON, json)
        }
        val p = PendingIntent.getBroadcast(
            c,
            id.hashCode(),
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, p)
    }

    fun cancel(c: Context, id: String) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(c, AlarmReceiver::class.java).apply { action = ACTION_ALARM }
        val p = PendingIntent.getBroadcast(
            c,
            id.hashCode(),
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(p)
        p.cancel()
    }

    fun scheduleNextRepeat(c: Context, alarmJson: String): Boolean {
        val alarm = try { org.json.JSONObject(alarmJson) } catch (_: Exception) { return false }
        val repeatType = alarm.optString("repeatType", "ONCE")
        if (repeatType == "ONCE") return false

        val original = alarm.optLong("timestamp")
        if (original <= 0L) return false

        var nextTimestamp = nextOccurrence(original, repeatType)
        val now = System.currentTimeMillis()

        // If the device was off when a repeating alarm should have fired,
        // advance until the next occurrence is actually in the future.
        while (nextTimestamp <= now) {
            nextTimestamp = nextOccurrence(nextTimestamp, repeatType)
        }

        alarm.put("timestamp", nextTimestamp)
        AlarmStorage.upsert(c, alarm)
        schedule(
            c,
            alarm.optString("id"),
            alarm.optString("title", "Reminder"),
            nextTimestamp,
            alarm.toString(),
        )
        return true
    }

    private fun nextOccurrence(timestamp: Long, repeatType: String): Long {
        val next = Calendar.getInstance().apply { timeInMillis = timestamp }

        when (repeatType) {
            "DAILY" -> next.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> next.add(Calendar.WEEK_OF_YEAR, 1)
            "WEEKDAYS" -> {
                do {
                    next.add(Calendar.DAY_OF_YEAR, 1)
                } while (
                    next.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                        next.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                )
            }
            "WEEKENDS" -> {
                do {
                    next.add(Calendar.DAY_OF_YEAR, 1)
                } while (
                    next.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY &&
                        next.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
                )
            }
            else -> return timestamp
        }

        return next.timeInMillis
    }

}
