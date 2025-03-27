package com.example.tixelcheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Boot completed, restoring alarms");
            
            // Get all active URLs from database
            UrlDatabase database = UrlDatabase.getInstance(context);
            List<MonitoredUrl> activeUrls = database.getActiveUrls();
            
            // Set alarms for all active URLs
            for (MonitoredUrl url : activeUrls) {
                TicketCheckerAlarm.setAlarm(context, url);
                Log.d(TAG, "Restored alarm for URL ID: " + url.getId());
            }
            
            Log.d(TAG, "Restored " + activeUrls.size() + " alarms");
        }
    }
}