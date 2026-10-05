package com.reminderapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object AlarmStorage {
    private const val P = "reminder_alarms"
    private const val K = "alarms"

    fun get(c: Context): JSONArray {
        val source = try {
            JSONArray(c.getSharedPreferences(P, 0).getString(K, "[]") ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }

        val items = mutableListOf<JSONObject>()
        for (i in 0 until source.length()) {
            source.optJSONObject(i)?.let { items.add(it) }
        }

        items.sortBy { it.optLong("timestamp", Long.MAX_VALUE) }

        val sorted = JSONArray()
        items.forEach { sorted.put(it) }
        return sorted
    }

    fun save(c: Context, a: JSONArray) {
        // Persist in chronological order as well as returning in chronological order.
        val items = mutableListOf<JSONObject>()
        for (i in 0 until a.length()) {
            a.optJSONObject(i)?.let { items.add(it) }
        }
        items.sortBy { it.optLong("timestamp", Long.MAX_VALUE) }

        val sorted = JSONArray()
        items.forEach { sorted.put(it) }
        c.getSharedPreferences(P, 0).edit().putString(K, sorted.toString()).apply()
    }

    fun upsert(c: Context, a: JSONObject) {
        val old = get(c)
        val out = JSONArray()
        var found = false

        for (i in 0 until old.length()) {
            val x = old.optJSONObject(i) ?: continue
            if (x.optString("id") == a.optString("id")) {
                out.put(a)
                found = true
            } else {
                out.put(x)
            }
        }

        if (!found) out.put(a)
        save(c, out)
    }

    fun remove(c: Context, id: String) {
        val old = get(c)
        val out = JSONArray()
        for (i in 0 until old.length()) {
            val x = old.optJSONObject(i) ?: continue
            if (x.optString("id") != id) out.put(x)
        }
        save(c, out)
    }
}
