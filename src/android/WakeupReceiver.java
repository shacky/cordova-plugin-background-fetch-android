package org.nypr.cordova.wakeupplugin;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class WakeupReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "WakeupReceiver";

    @SuppressLint({"SimpleDateFormat", "NewApi"})
    @Override
    public void onReceive(Context context, Intent intent) {
        if (WakeupPlugin.connectionCallbackContext == null) {
            Log.d(LOG_TAG, "got wakeup from timer, bot don't have a callback context");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Log.d(LOG_TAG, "wakeuptimer expired at " + sdf.format(new Date().getTime()));

        try {
            String packageName = context.getPackageName();
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            String className = launchIntent.getComponent().getClassName();
            Log.d(LOG_TAG, "launching activity for class " + className);

            @SuppressWarnings("rawtypes")
            Class c = Class.forName(className);

            Intent i = new Intent(context, c);
            i.putExtra("wakeup", true);
            Bundle extrasBundle = intent.getExtras();
            String extras = null;
            if (extrasBundle != null && extrasBundle.get("extra") != null) {
                extras = extrasBundle.get("extra").toString();
                i.putExtra("extra", extras);
            }

            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);

            JSONObject o = new JSONObject();
            o.put("type", "wakeup");
            if (extras != null) {
                o.put("extra", extras);
            }
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, o);
            pluginResult.setKeepCallback(true);
            WakeupPlugin.connectionCallbackContext.sendPluginResult(pluginResult);
        } catch (Exception e) {
            PluginResult pluginResult;
            JSONObject o = new JSONObject();
            try {
                o.put("error", e.getMessage());
                pluginResult = new PluginResult(PluginResult.Status.ERROR, o);
            } catch (JSONException e1) {
                pluginResult = new PluginResult(PluginResult.Status.JSON_EXCEPTION);
            }
            pluginResult.setKeepCallback(true);
            WakeupPlugin.connectionCallbackContext.sendPluginResult(pluginResult);
        }
    }
}
