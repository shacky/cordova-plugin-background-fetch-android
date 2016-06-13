package de.panko.wakeupplugin;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class WakeupPlugin extends CordovaPlugin {

    private static final String LOG_TAG = "WakeupPlugin";

    static CallbackContext connectionCallbackContext;

    static CordovaPlugin selfReference;

    @Override
    public void onReset() {
        selfReference = this;

        Log.d(LOG_TAG, "Wakeup Plugin onReset");

        if (wakeupEnabled()) {
            activateWakeup(preferences.getInteger("wakeup", 15));
        }

        super.onReset();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        selfReference = this;
        connectionCallbackContext = callbackContext;

        if (action.equalsIgnoreCase("enable")) {
            return enableWakeup(args, callbackContext);
        } else if (action.equalsIgnoreCase("disable")) {
            return disableWakeup(args, callbackContext);
        } else if (action.equalsIgnoreCase("executionFinished")) {
            return executionFinished(args, callbackContext);
        }

        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, LOG_TAG + " error: invalid action (" + action + ")");
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
        return false;
    }

    private boolean executionFinished(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject options = args.getJSONObject(0);

        String id = options.getString("id");

        WakeupService.executionFinished(id);

        signalSuccess(callbackContext);

        return true;
    }

    private void signalSuccess(CallbackContext callbackContext) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    private boolean disableWakeup(JSONArray args, CallbackContext callbackContext) {
        cancelWakeup();

        signalSuccess(callbackContext);

        return true;
    }

    private boolean enableWakeup(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONObject options = args.getJSONObject(0);

        int interval = options.getInt("interval");

        if (wakeupEnabled()) {
            cancelWakeup();
        }

        activateWakeup(interval);

        signalSuccess(callbackContext);

        return true;
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private void activateWakeup(int interval) {
        Date firstTrigger = new Date(System.currentTimeMillis() + (interval * 60 * 1000));

        Intent intent = new Intent(cordova.getActivity().getApplicationContext(), WakeupReceiver.class);
        PendingIntent operation = PendingIntent.getBroadcast(cordova.getActivity().getApplicationContext(), 4711, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        this.preferences.set("wakeup", interval);

        AlarmManager alarmManager = (AlarmManager) cordova.getActivity().getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, firstTrigger.getTime(), interval * 60 * 1000, operation);
    }

    private void cancelWakeup() {
        Intent intent = new Intent(cordova.getActivity().getApplicationContext(), WakeupReceiver.class);
        PendingIntent operation = PendingIntent.getBroadcast(cordova.getActivity().getApplicationContext(), 4711, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) cordova.getActivity().getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(operation);

        this.preferences.set("wakeup", "-1");
    }

    private boolean wakeupEnabled() {
        return (this.preferences.getInteger("wakeup", -1) < 0);
    }

    public static void notifyAsync(final JSONObject o) {
        if (selfReference != null && connectionCallbackContext != null) {
            selfReference.cordova.getThreadPool().submit(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "running wakeup call");
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, o);
                    pluginResult.setKeepCallback(true);
                    WakeupPlugin.connectionCallbackContext.sendPluginResult(pluginResult);
                }
            });
        }
    }
}
