package com.reminderapp

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.widget.TimePicker
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import org.json.JSONObject

class AlarmModule(private val ctx: ReactApplicationContext) : ReactContextBaseJavaModule(ctx) {
    override fun getName() = "AlarmModule"

    @ReactMethod
    fun showTimePicker(initialHour: Int, initialMinute: Int, promise: Promise) {
        val activity: android.app.Activity? = ctx.getCurrentActivity()
        if (activity == null) {
            promise.reject("NO_ACTIVITY", "No foreground Activity is available.")
            return
        }

        activity.runOnUiThread {
            val hour = initialHour.coerceIn(0, 23)
            val minute = initialMinute.coerceIn(0, 59)
            val dialog = TimePickerDialog(
                activity,
                { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
                    promise.resolve(String.format("%02d:%02d", selectedHour, selectedMinute))
                },
                hour,
                minute,
                android.text.format.DateFormat.is24HourFormat(activity)
            )
            dialog.setOnCancelListener { promise.resolve(null) }
            dialog.show()
        }
    }

    @ReactMethod
    fun scheduleAlarm(id: String, title: String, timestamp: Double, json: String, promise: Promise) {
        try {
            val alarm = JSONObject(json).apply {
                put("id", id)
                put("title", title)
                put("timestamp", timestamp.toLong())
                put("enabled", true)
            }
            AlarmStorage.upsert(ctx, alarm)
            AlarmScheduler.schedule(ctx, id, title, timestamp.toLong(), alarm.toString())
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SCHEDULE_ERROR", e)
        }
    }

    @ReactMethod
    fun cancelAlarm(id: String, promise: Promise) {
        try {
            AlarmScheduler.cancel(ctx, id)
            AlarmStorage.remove(ctx, id)
            AlarmNotification.cancel(ctx, id)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("CANCEL_ERROR", e)
        }
    }

    @ReactMethod
    fun getAlarms(promise: Promise) {
        promise.resolve(AlarmStorage.get(ctx).toString())
    }

    @ReactMethod
    fun canScheduleExactAlarms(promise: Promise) {
        val alarmManager = ctx.getSystemService(AlarmManager::class.java)
        promise.resolve(
            if (Build.VERSION.SDK_INT >= 31) alarmManager.canScheduleExactAlarms() else true
        )
    }

    @ReactMethod
    fun openExactAlarmSettings(promise: Promise) {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                ctx.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = android.net.Uri.parse("package:${ctx.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SETTINGS_ERROR", e)
        }
    }

    @ReactMethod
    fun canUseFullScreenIntent(promise: Promise) {
        if (Build.VERSION.SDK_INT >= 34) {
            val manager = ctx.getSystemService(android.app.NotificationManager::class.java)
            promise.resolve(manager.canUseFullScreenIntent())
        } else {
            promise.resolve(true)
        }
    }

    @ReactMethod
    fun openFullScreenIntentSettings(promise: Promise) {
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = android.net.Uri.parse("package:${ctx.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("FULL_SCREEN_SETTINGS_ERROR", e)
        }
    }
}
