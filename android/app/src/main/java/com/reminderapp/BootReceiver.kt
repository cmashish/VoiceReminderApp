package com.reminderapp
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
class BootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){if(i.action!=Intent.ACTION_BOOT_COMPLETED&&i.action!=Intent.ACTION_LOCKED_BOOT_COMPLETED)return;val a=AlarmStorage.get(c);val now=System.currentTimeMillis();for(n in 0 until a.length()){val x=a.optJSONObject(n)?:continue;if(x.optBoolean("enabled",true)&&x.optLong("timestamp")>now)AlarmScheduler.schedule(c,x.optString("id"),x.optString("title","Reminder"),x.optLong("timestamp"),x.toString())}}}
