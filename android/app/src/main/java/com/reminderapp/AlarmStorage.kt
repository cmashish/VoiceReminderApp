package com.reminderapp
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
object AlarmStorage {
    private const val P="reminder_alarms"; private const val K="alarms"
    fun get(c:Context):JSONArray=try{JSONArray(c.getSharedPreferences(P,0).getString(K,"[]")?:"[]")}catch(_:Exception){JSONArray()}
    fun save(c:Context,a:JSONArray){c.getSharedPreferences(P,0).edit().putString(K,a.toString()).apply()}
    fun upsert(c:Context,a:JSONObject){val old=get(c);val out=JSONArray();var found=false;for(i in 0 until old.length()){val x=old.optJSONObject(i)?:continue;if(x.optString("id")==a.optString("id")){out.put(a);found=true}else out.put(x)};if(!found)out.put(a);save(c,out)}
    fun remove(c:Context,id:String){val old=get(c);val out=JSONArray();for(i in 0 until old.length()){val x=old.optJSONObject(i)?:continue;if(x.optString("id")!=id)out.put(x)};save(c,out)}
}
