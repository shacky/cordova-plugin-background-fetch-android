package de.panko.wakeupplugin;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author Sven Panko (sp@intuitiveminds.de)
 */
public class WakeupService extends IntentService {

    private static final Map<String, Semaphore> semaphores = new HashMap<String, Semaphore>();
    private static final String LOG_TAG = "WakeupService";


    public WakeupService() {
        super("WakeupService");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        try {
            String id = UUID.randomUUID().toString();

            JSONObject o = new JSONObject();
            o.put("type", "wakeup");
            o.put("id", id);

            Semaphore semaphore = new Semaphore(0);
            semaphores.put(id, semaphore);

            WakeupPlugin.notifyAsync(o);

            int counter = 0;
            boolean released = false;

            while (!released && counter < 120) {
                released = semaphore.tryAcquire(250, TimeUnit.MILLISECONDS);
                counter++;
            }

            if (!released) {
                Log.e(LOG_TAG, "timeout occurred while waiting for background task to finish");
            }

            semaphores.remove(id);
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "exception while trying to wakeup in service", e);
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