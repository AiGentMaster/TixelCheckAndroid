package com.example.tixelcheck;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class TicketMonitorService extends Service {
    private static final String TAG = "TicketMonitorService";
    private static final String CHANNEL_ID = "TixelCheckChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final int SERVICE_NOTIFICATION_ID = 9999;
    private static final int CONNECTION_TIMEOUT = 15000; // 15 seconds

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        // Create notification channel
        createNotificationChannel();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Must create a notification for foreground service
            startForeground(SERVICE_NOTIFICATION_ID, createServiceNotification());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        
        // Extract URL ID from intent
        long urlId = intent.getLongExtra("url_id", -1);
        
        if (urlId != -1) {
            // Start checking in background thread to not block main thread
            new Thread(() -> {
                checkTicketAvailability(urlId);
                // Stop service when check is complete
                stopSelf(startId);
            }).start();
        } else {
            // No valid URL ID, stop service
            stopSelf(startId);
        }
        
        return START_NOT_STICKY;
    }

    /**
     * Checks if tickets are available for the specified URL
     * 
     * @param urlId ID of the URL to check
     */
    private void checkTicketAvailability(long urlId) {
        // Get URL from database
        UrlDatabase database = UrlDatabase.getInstance(this);
        MonitoredUrl url = database.getUrlById(urlId);
        
        if (url == null || !url.isActive()) {
            Log.d(TAG, "URL is null or inactive: " + urlId);
            return;
        }
        
        boolean previousStatus = url.hasTicketsFound();
        boolean currentStatus = false;
        String statusMessage = "";
        
        try {
            Log.d(TAG, "Checking URL: " + url.getUrl());
            
            // Connect to URL and get HTML content with timeout
            Document doc = Jsoup.connect(url.getUrl())
                    .timeout(CONNECTION_TIMEOUT)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .followRedirects(true)
                    .get();
            
            // Check if tickets are available using the simple text filter
            currentStatus = isTicketAvailable(doc);
            
            if (currentStatus) {
                statusMessage = "Tickets are now available for " + 
                    (url.hasEventDetails() ? url.getEventName() : "your monitored event");
                Log.d(TAG, "Found tickets available in the page content!");
            } else {
                statusMessage = "No tickets available for " + 
                    (url.hasEventDetails() ? url.getEventName() : "your monitored event");
                Log.d(TAG, "No tickets available text found in the page content.");
            }
            
            Log.d(TAG, "Ticket status for URL " + urlId + ": " + statusMessage);
            
            // Update last checked timestamp and status
            long currentTime = System.currentTimeMillis();
            database.updateLastChecked(url.getId(), currentTime, currentStatus);
            
            // If status changed from unavailable to available, notify user
            if (currentStatus && !previousStatus) {
                // Create history entry
                database.addTicketHistory(url.getId(), currentTime, statusMessage);
                
                // Send notification
                sendTicketAvailableNotification(url, statusMessage);
            }
            
        } catch (SocketTimeoutException e) {
            // Handle timeout specifically
            Log.e(TAG, "Connection timed out for URL: " + url.getUrl(), e);
            statusMessage = "Connection timed out. Will retry later.";
            
            // Update last checked timestamp but don't change availability status
            database.updateLastChecked(url.getId(), System.currentTimeMillis(), previousStatus);
            
            // Add history entry for the timeout
            database.addTicketHistory(url.getId(), System.currentTimeMillis(), statusMessage);
        } catch (IOException e) {
            // Handle other connection errors
            Log.e(TAG, "Error checking URL: " + url.getUrl(), e);
            statusMessage = "Connection error. Will retry later.";
            
            // Update last checked timestamp but don't change availability status
            database.updateLastChecked(url.getId(), System.currentTimeMillis(), previousStatus);
            
            // Add history entry for the error
            database.addTicketHistory(url.getId(), System.currentTimeMillis(), statusMessage);
        }
    }
    
    /**
     * Check if tickets are available based on simple text filter
     * Looks specifically for the exact text "ticket available" or "tickets available"
     * 
     * @param doc The HTML document to check
     * @return true if tickets are available, false otherwise
     */
    private boolean isTicketAvailable(Document doc) {
        // Convert the entire HTML to lowercase for case-insensitive matching
        String htmlText = doc.text().toLowerCase();
        
        // Log some of the content for debugging
        Log.d(TAG, "Page content sample: " + htmlText.substring(0, Math.min(500, htmlText.length())));
        
        // Check for the exact phrases "ticket available" or "tickets available"
        boolean hasTickets = htmlText.contains("ticket available") || htmlText.contains("tickets available");
        Log.d(TAG, "Has tickets available text: " + hasTickets);
        return hasTickets;
    }
    
    /**
     * Send notification to user that tickets are available
     * 
     * @param url The URL that has tickets available
     * @param message The notification message to display
     */
    private void sendTicketAvailableNotification(MonitoredUrl url, String message) {
        // Create intent to open the app when notification is clicked
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Tickets Available!")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        // Show notification
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        notificationManager.notify((int) url.getId(), builder.build());
        Log.d(TAG, "Notification sent for URL ID: " + url.getId());
    }
    
    /**
     * Create a notification for the foreground service
     */
    private android.app.Notification createServiceNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_search)
                .setContentTitle("Checking for tickets")
                .setContentText("Tixel Check is monitoring ticket availability")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent);
        
        return builder.build();
    }
    
    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Tixel Check Notifications";
            String description = "Notifications for ticket availability";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }
}