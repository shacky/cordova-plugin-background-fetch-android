package org.nypr.cordova.wakeupplugin;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class WakeupPlugin extends CordovaPlugin {

    private static final String LOG_TAG = "WakeupPlugin";

    private static final int ID_ONETIME_OFFSET = 10000;

    public static CallbackContext connectionCallbackContext;

    @Override
    public void onReset() {
        // app startup
        Log.d(LOG_TAG, "Wakeup Plugin onReset");
        Bundle extras = cordova.getActivity().getIntent().getExtras();
        if (extras != null && !extras.getBoolean("wakeup", false)) {
            setAlarmsFromPrefs(cordova.getActivity().getApplicationContext());
        }
        super.onReset();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        boolean ret = true;
        try {
            if (action.equalsIgnoreCase("wakeup")) {
                JSONObject options = args.getJSONObject(0);

                JSONArray alarms;
                if (options.has("alarms")) {
                    alarms = options.getJSONArray("alarms");
                } else {
                    alarms = new JSONArray(); // default to empty array
                }

                saveToPrefs(cordova.getActivity().getApplicationContext(), alarms);
                setAlarms(cordova.getActivity().getApplicationContext(), alarms, true);

                WakeupPlugin.connectionCallbackContext = callbackContext;
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            } else {
                PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, LOG_TAG + " error: invalid action (" + action + ")");
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
                ret = false;
            }
        } catch (JSONException e) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, LOG_TAG + " error: invalid json");
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            ret = false;
        } catch (Exception e) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, LOG_TAG + " error: " + e.getMessage());
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            ret = false;
        }
        return ret;
    }

    private void setAlarmsFromPrefs(Context context) {
        try {
            SharedPreferences prefs;
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String a = prefs.getString("alarms", "[]");
            Log.d(LOG_TAG, "setting alarms:\n" + a);
            JSONArray alarms = new JSONArray(a);
            setAlarms(context, alarms, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint({"SimpleDateFormat", "NewApi"})
    private void setAlarms(Context context, JSONArray alarms, boolean cancelAlarms) throws JSONException {
        if (cancelAlarms) {
            cancelAlarms(context);
        }

        for (int i = 0; i < alarms.length(); i++) {
            JSONObject alarm = alarms.getJSONObject(i);

            String type = "onetime";
            if (alarm.has("type")) {
                type = alarm.getString("type");
            }

            if (!alarm.has("time")) {
                throw new JSONException("alarm missing time: " + alarm.toString());
            }

            JSONObject time = alarm.getJSONObject("time");

            if (type.equals("onetime")) {
                Calendar alarmDate = getOneTimeAlarmDate(time);
                Intent intent = new Intent(context, WakeupReceiver.class);
                if (alarm.has("extra")) {
                    intent.putExtra("extra", alarm.getJSONObject("extra").toString());
                    intent.putExtra("type", type);
                }

                setNotification(context, type, alarmDate, intent, ID_ONETIME_OFFSET);
            }
        }
    }

    private void setNotification(Context context, String type, Calendar alarmDate, Intent intent, int id) throws JSONException {
        if (alarmDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Log.d(LOG_TAG, "setting alarm at " + sdf.format(alarmDate.getTime()) + "; id " + id);

            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent sender = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= 19) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmDate.getTimeInMillis(), sender);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarmDate.getTimeInMillis(), sender);
            }

            if (WakeupPlugin.connectionCallbackContext != null) {
                JSONObject o = new JSONObject();
                o.put("type", "set");
                o.put("alarm_type", type);
                o.put("alarm_date", alarmDate.getTimeInMillis());

                Log.d(LOG_TAG, "alarm time in millis: " + alarmDate.getTimeInMillis());

                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, o);
                pluginResult.setKeepCallback(true);
                WakeupPlugin.connectionCallbackContext.sendPluginResult(pluginResult);
            }
        }
    }

    private void cancelAlarms(Context context) {
        Log.d(LOG_TAG, "canceling alarms");
        Intent intent = new Intent(context, WakeupReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, ID_ONETIME_OFFSET, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Log.d(LOG_TAG, "cancelling alarm id " + ID_ONETIME_OFFSET);
        alarmManager.cancel(sender);
    }

    private Calendar getOneTimeAlarmDate(JSONObject time) throws JSONException {
        TimeZone defaultz = TimeZone.getDefault();
        Calendar calendar = new GregorianCalendar(defaultz);
        Calendar now = new GregorianCalendar(defaultz);
        now.setTime(new Date());
        calendar.setTime(new Date());

        int hour = (time.has("hour")) ? time.getInt("hour") : -1;
        int minute = (time.has("minute")) ? time.getInt("minute") : 0;

        if (hour >= 0) {
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.before(now)) {
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
            }
        } else {
            calendar = null;
        }

        return calendar;
    }

    private void saveToPrefs(Context context, JSONArray alarms) {
        SharedPreferences prefs;
        SharedPreferences.Editor editor;

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        editor = prefs.edit();
        editor.putString("alarms", alarms.toString());
        editor.commit();

    }

}
