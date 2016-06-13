package de.panko.wakeupplugin;

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

    private static final Map<String, Semaphore> semaphores = new HashMap<String, Semaphore>();

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
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);

            String id = UUID.randomUUID().toString();

            JSONObject o = new JSONObject();
            o.put("type", "wakeup");
            o.put("id", id);

            Semaphore semaphore = new Semaphore(0);
            semaphores.put(id, semaphore);

            WakeupPlugin.notifyAsync(o);

            boolean released = semaphore.tryAcquire(30, TimeUnit.SECONDS);

            if (!released) {
                Log.e(LOG_TAG, "timeout occurred while waiting for background task to finish");
            }

            semaphores.remove(id);
        } catch (Exception e) {
            Log.e(LOG_TAG, "exception while trying to wakeup", e);
        }
    }

    public static void executionFinished(String id) {
        Semaphore semaphore = semaphores.get(id);

        if (semaphore != null) {
            semaphore.release(1);
        } else {
            Log.e(LOG_TAG, "no running background task found while signalling execution finished for id " + id);
        }
    }
}
