package com.example.tixelcheck;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

public class TicketCheckerAlarm extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract URL ID from intent
        long urlId = intent.getLongExtra("url_id", -1);
        
        if (urlId != -1) {
            // Get the URL from database
            MonitoredUrl url = UrlDatabase.getInstance(context).getUrlById(urlId);
            
            if (url != null && url.isActive()) {
                // Start service to check URL
                Intent serviceIntent = new Intent(context, TicketMonitorService.class);
                serviceIntent.putExtra("url_id", urlId);
                context.startService(serviceIntent);
                
                // Reset alarm for next check
                setAlarm(context, url);
            }
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
        
        // Schedule the alarm to repeat
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
    }
    
    /**
     * Immediately checks all active URLs
     * 
     * @param context Application context
     */
    public static void checkNow(Context context) {
        // Get all active URLs
        List<MonitoredUrl> activeUrls = UrlDatabase.getInstance(context).getActiveUrls();
        
        // Start service for each URL
        for (MonitoredUrl url : activeUrls) {
            Intent serviceIntent = new Intent(context, TicketMonitorService.class);
            serviceIntent.putExtra("url_id", url.getId());
            context.startService(serviceIntent);
        }
    }
}