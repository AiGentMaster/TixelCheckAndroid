package com.example.tixelcheck;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class TicketCheckerAlarm extends BroadcastReceiver {
    private static final String TAG = "TicketCheckerAlarm";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received - checking for tickets");
        
        // Extract URL ID from intent
        long urlId = intent.getLongExtra("url_id", -1);
        
        if (urlId != -1) {
            // Start the TicketMonitorService to check for tickets
            Intent serviceIntent = new Intent(context, TicketMonitorService.class);
            serviceIntent.putExtra("url_id", urlId);
            
            // Start the service to check for tickets
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            // Re-schedule the alarm for the next check
            UrlDatabase database = UrlDatabase.getInstance(context);
            MonitoredUrl url = database.getUrlById(urlId);
            
            if (url != null && url.isActive()) {
                setAlarm(context, url);
                Log.d(TAG, "Rescheduled alarm for URL ID: " + urlId);
            }
        } else {
            Log.e(TAG, "Received alarm with invalid URL ID");
        }
    }
    
    /**
     * Sets an alarm to check the given URL at the specified frequency
     * 
     * @param context Application context
     * @param url The MonitoredUrl to check
     */
    public static void setAlarm(Context context, MonitoredUrl url) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        
        Intent intent = new Intent(context, TicketCheckerAlarm.class);
        intent.putExtra("url_id", url.getId());
        
        // Generate a unique ID for this alarm based on the URL ID
        int requestCode = (int) url.getId();
        
        // Create a pending intent that will trigger our BroadcastReceiver
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            requestCode, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Convert frequency from minutes to milliseconds
        long intervalMillis = url.getFrequency() * 60 * 1000;
        
        // Schedule the alarm to trigger
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                pendingIntent
            );
        }
        
        Log.d(TAG, "Alarm set for URL ID: " + url.getId() + " to trigger in " + url.getFrequency() + " minutes");
    }
    
    /**
     * Cancels the alarm for the given URL
     * 
     * @param context Application context
     * @param url The MonitoredUrl to cancel
     */
    public static void cancelAlarm(Context context, MonitoredUrl url) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        
        Intent intent = new Intent(context, TicketCheckerAlarm.class);
        int requestCode = (int) url.getId();
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            requestCode, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Alarm canceled for URL ID: " + url.getId());
    }
    
    /**
     * Cancels the alarm with the given URL ID
     * 
     * @param context Application context
     * @param urlId The ID of the MonitoredUrl to cancel
     */
    public static void cancelAlarm(Context context, long urlId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        
        Intent intent = new Intent(context, TicketCheckerAlarm.class);
        int requestCode = (int) urlId;
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            requestCode, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Alarm canceled for URL ID: " + urlId);
    }
}