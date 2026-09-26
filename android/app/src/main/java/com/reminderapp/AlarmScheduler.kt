package com.reminderapp
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
object AlarmScheduler {
    const val ACTION_ALARM="com.reminderapp.ACTION_ALARM"; const val EXTRA_ID="alarm_id"; const val EXTRA_TITLE="alarm_title"; const val EXTRA_JSON="alarm_json"
    fun schedule(c:Context,id:String,title:String,time:Long,json:String){
        val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i=Intent(c,AlarmReceiver::class.java).apply{action=ACTION_ALARM;putExtra(EXTRA_ID,id);putExtra(EXTRA_TITLE,title);putExtra(EXTRA_JSON,json)}
        val p=PendingIntent.getBroadcast(c,id.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,p)
    }
    fun cancel(c:Context,id:String){
        val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i=Intent(c,AlarmReceiver::class.java).apply{action=ACTION_ALARM}
        val p=PendingIntent.getBroadcast(c,id.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.cancel(p);p.cancel()
    }
}
