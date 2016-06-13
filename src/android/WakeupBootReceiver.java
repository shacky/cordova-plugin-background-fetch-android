package de.panko.wakeupplugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.DateFormat;
import java.util.Date;

public class WakeupBootReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "WakeupBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        DateFormat sdf = DateFormat.getDateTimeInstance();
        Log.d(LOG_TAG, "wakeup boot receiver fired at " + sdf.format(new Date()));
    }
}
