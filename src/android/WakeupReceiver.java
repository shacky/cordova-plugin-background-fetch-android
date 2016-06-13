package de.panko.wakeupplugin;

import android.WakeupService;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class WakeupReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "WakeupReceiver";

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @Override
    public void onReceive(Context context, Intent intent) {
        if (WakeupPlugin.connectionCallbackContext == null) {
            Log.d(LOG_TAG, "got wakeup from timer, bot don't have a callback context");
            return;
        }

        DateFormat sdf = DateFormat.getDateTimeInstance();
        Log.d(LOG_TAG, "wakeuptimer expired at " + sdf.format(new Date().getTime()));

        try {
            String packageName = context.getPackageName();
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            String className = launchIntent.getComponent().getClassName();
            Log.d(LOG_TAG, "launching activity for class " + className);

            @SuppressWarnings("rawtypes")
            Class c = Class.forName(className);

            Intent i = new Intent(context, c);
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            context.startActivity(i);

            Class service = WakeupService.class;

            Intent serviceIntent = new Intent(context, service);
            context.startService(serviceIntent);
        } catch (Exception e) {
            Log.e(LOG_TAG, "exception while trying to wakeup", e);
        }
    }
}
