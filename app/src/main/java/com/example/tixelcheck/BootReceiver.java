package com.example.tixelcheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.List;

/**
 * Receiver to restore monitoring alarms after device reboot
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Boot completed, re-scheduling alarms");
            
            // Give the system a moment to fully initialize before setting up alarms
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Restart the job service
                TicketMonitorService.scheduleJob(context);
                
                // Re-schedule individual alarms for active URLs
                List<MonitoredUrl> activeUrls = UrlDatabase.getInstance(context).getActiveUrls();
                Log.d(TAG, "Found " + activeUrls.size() + " active URLs to monitor");
                
                for (MonitoredUrl url : activeUrls) {
                    TicketCheckerAlarm.setAlarm(context, url);
                    Log.d(TAG, "Re-scheduled alarm for URL: " + url.getUrl());
                }
            }, 10000); // 10 seconds delay to ensure system is ready
        }
    }
}