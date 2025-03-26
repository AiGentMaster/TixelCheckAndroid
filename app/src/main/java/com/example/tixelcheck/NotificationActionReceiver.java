package com.example.tixelcheck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.concurrent.Executors;

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
                long urlId = intent.getLongExtra("url_id", -1);
                Log.d(TAG, "URL from intent: " + url + ", URL ID: " + urlId);
                
                if (url != null && !url.isEmpty()) {
                    // Ensure URL has proper scheme
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "https://" + url;
                        Log.d(TAG, "Added https scheme to URL: " + url);
                    }
                    
                    // Extract event details in background if URL ID is provided
                    final String finalUrl = url;
                    if (urlId > 0) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            extractEventDetails(context, finalUrl, urlId);
                        });
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
    
    /**
     * Extract event details from the URL and update database
     */
    private void extractEventDetails(Context context, String url, long urlId) {
        try {
            Log.d(TAG, "Extracting event details from URL: " + url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(15000)
                    .get();
            
            // Use the same extraction methods as in TicketCheckerAlarm
            String eventName = TicketCheckerAlarm.extractEventName(doc);
            String eventDate = TicketCheckerAlarm.extractEventDate(doc);
            
            if (!eventName.isEmpty() || !eventDate.isEmpty()) {
                Log.d(TAG, "Found event details - Name: " + eventName + ", Date: " + eventDate);
                
                // Update database with event details
                UrlDatabase.getInstance(context).updateEventDetails(urlId, eventName, eventDate);
                
                // Show confirmation toast on the main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Event details updated", Toast.LENGTH_SHORT).show();
                });
            } else {
                Log.d(TAG, "Could not extract event details from URL");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting event details", e);
        }
    }
}