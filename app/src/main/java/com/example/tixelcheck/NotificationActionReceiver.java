package com.example.tixelcheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

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
            Log.e(TAG, "Received intent with null action");
            return;
        }
        
        Log.d(TAG, "Received action: " + action);
        
        switch (action) {
            case "com.example.tixelcheck.OPEN_URL":
                String url = intent.getStringExtra("url");
                Log.d(TAG, "URL from intent: " + url);
                
                if (url != null && !url.isEmpty()) {
                    // Ensure URL has proper scheme
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                        Log.d(TAG, "Added https scheme to URL: " + url);
                    }
                    
                    try {
                        // Create browser intent with proper flags
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                        browserIntent.setData(Uri.parse(url));
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        
                        // Log the URL that will be opened
                        Log.d(TAG, "Opening URL in browser: " + url);
                        
                        // Start activity
                        context.startActivity(browserIntent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error opening URL: " + url, e);
                        // Show toast to user if there's an error
                        Toast.makeText(context, "Could not open URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "URL is null or empty");
                    Toast.makeText(context, "No URL provided to open", Toast.LENGTH_SHORT).show();
                }
                
                // Always stop the alarm when action is taken
                TicketCheckerAlarm.stopAlarmSound();
                break;
                
            case "com.example.tixelcheck.STOP_ALARM":
                Log.d(TAG, "Stopping alarm sound");
                // Just stop the alarm sound
                TicketCheckerAlarm.stopAlarmSound();
                break;
                
            default:
                Log.d(TAG, "Unknown action: " + action);
                break;
        }
    }
}