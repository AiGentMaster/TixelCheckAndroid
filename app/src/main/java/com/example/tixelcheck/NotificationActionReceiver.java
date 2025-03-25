package com.example.tixelcheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * Handles actions performed on notifications, such as opening URLs
 * or stopping alarm sounds.
 */
public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationActionRcvr";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (action == null) {
            return;
        }
        
        switch (action) {
            case "com.example.tixelcheck.OPEN_URL":
                String url = intent.getStringExtra("url");
                if (url != null && !url.isEmpty()) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(browserIntent);
                }
                // Always stop the alarm when action is taken
                TicketCheckerAlarm.stopAlarmSound();
                break;
                
            case "com.example.tixelcheck.STOP_ALARM":
                // Just stop the alarm sound
                TicketCheckerAlarm.stopAlarmSound();
                break;
        }
    }
}