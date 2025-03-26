package com.example.tixelcheck;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class TicketCheckerAlarm extends BroadcastReceiver {
    private static final String TAG = "TicketCheckerAlarm";
    private static final String CHANNEL_ID = "tixel_check_channel";
    
    // Add static MediaPlayer reference to handle alarm sounds
    private static MediaPlayer activeAlarmPlayer;
    public static long activeAlarmUrlId;
    
    // For exponential backoff
    private static final int MAX_RETRY_COUNT = 5; 
    private static final int BASE_RETRY_DELAY = 5000; // 5 seconds
    private static Map<Long, Integer> retryCountMap = new HashMap<>();
    
    // Method to stop an active alarm sound
    public static void stopAlarmSound() {
        synchronized (TicketCheckerAlarm.class) {
            if (activeAlarmPlayer != null) {
                if (activeAlarmPlayer.isPlaying()) {
                    try {
                        activeAlarmPlayer.stop();
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Error stopping alarm sound", e);
                    }
                }
                activeAlarmPlayer.release();
                activeAlarmPlayer = null;
            }
        }
    }

    public static void setAlarm(Context context, MonitoredUrl url) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TicketCheckerAlarm.class);
        intent.putExtra("url_id", url.getId());
        intent.putExtra("url", url.getUrl());
        
        // Create a unique pending intent ID based on URL ID
        int requestCode = (int) url.getId();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Convert frequency from minutes to milliseconds
        long intervalMillis = url.getFrequency() * 60 * 1000;
        long triggerAtMillis = SystemClock.elapsedRealtime() + intervalMillis;
        
        // Schedule alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent);
        } else {
            alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent);
        }
        
        Log.d(TAG, "Alarm set for URL: " + url.getUrl() + " with frequency: " + url.getFrequency() + " minutes");
    }
    
    /**
     * Set an immediate retry alarm with exponential backoff
     */
    private static void setRetryAlarm(Context context, MonitoredUrl url) {
        // Get current retry count or initialize to 0
        int retryCount = retryCountMap.getOrDefault(url.getId(), 0);
        
        // Check if we've reached the maximum retry count
        if (retryCount >= MAX_RETRY_COUNT) {
            Log.d(TAG, "Maximum retry count reached for URL ID " + url.getId() + ", going back to normal schedule");
            retryCountMap.remove(url.getId());
            setAlarm(context, url);
            return;
        }
        
        // Calculate exponential backoff delay
        long delay = BASE_RETRY_DELAY * (long) Math.pow(2, retryCount);
        
        // Increment and store retry count
        retryCount++;
        retryCountMap.put(url.getId(), retryCount);
        
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TicketCheckerAlarm.class);
        intent.putExtra("url_id", url.getId());
        intent.putExtra("url", url.getUrl());
        intent.putExtra("retry", true);
        
        // Create a unique pending intent ID based on URL ID and retry count
        int requestCode = (int) url.getId() * 1000 + retryCount;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Set alarm with the calculated delay
        long triggerAtMillis = SystemClock.elapsedRealtime() + delay;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent);
        } else {
            alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis,
                    pendingIntent);
        }
        
        Log.d(TAG, "Retry alarm #" + retryCount + " set for URL ID " + url.getId() + " with delay " + (delay / 1000) + " seconds");
    }

    public static void cancelAlarm(Context context, long urlId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, TicketCheckerAlarm.class);
        int requestCode = (int) urlId;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        alarmManager.cancel(pendingIntent);
        
        // Also clear any retry alarms
        retryCountMap.remove(urlId);
        for (int i = 1; i <= MAX_RETRY_COUNT; i++) {
            int retryRequestCode = (int) urlId * 1000 + i;
            Intent retryIntent = new Intent(context, TicketCheckerAlarm.class);
            PendingIntent retryPendingIntent = PendingIntent.getBroadcast(
                    context, 
                    retryRequestCode, 
                    retryIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            alarmManager.cancel(retryPendingIntent);
        }
        
        Log.d(TAG, "Alarm canceled for URL ID: " + urlId);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm triggered");
        
        if (intent.hasExtra("url")) {
            String url = intent.getStringExtra("url");
            long urlId = intent.getLongExtra("url_id", -1);
            boolean isRetry = intent.getBooleanExtra("retry", false);
            
            Executors.newSingleThreadExecutor().execute(() -> {
                checkTicketAvailability(context, url, urlId, isRetry);
            });
        } else if (intent.getAction() != null && intent.getAction().equals("com.example.tixelcheck.TEST_ALERT")) {
            // Handle test alert broadcast
            sendTestNotification(context);
        }
    }
    
    private void sendTestNotification(Context context) {
        Log.d(TAG, "Sending test notification");
        
        // Use sample event details for the test notification to show how real notifications will look
        String sampleEventName = "Sample Event";
        String sampleEventDate = "March 30, 2025";
        
        // Create a notification that includes the sample event details
        String notificationTitle = "TEST: Tickets for " + sampleEventName + "!";
        String notificationMessage = "This is a test alert. A real notification will include event details like \"" + 
            sampleEventName + " on " + sampleEventDate + "\". Tap to open the event page.";
        
        sendNotification(context, notificationTitle, notificationMessage, 9999, "https://tixel.com");
        
        // Play an additional alarm sound for the test
        try {
            Uri alarmSound = Settings.System.DEFAULT_ALARM_ALERT_URI;
            MediaPlayer mediaPlayer = MediaPlayer.create(context, alarmSound);
            mediaPlayer.setLooping(false);
            mediaPlayer.start();
            
            // Stop after 3 seconds
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            }, 3000);
        } catch (Exception e) {
            Log.e(TAG, "Error playing test alarm", e);
        }
    }

    private void checkTicketAvailability(Context context, String url, long urlId, boolean isRetry) {
        // Get the full URL object from the database
        MonitoredUrl monitoredUrl = UrlDatabase.getInstance(context).getUrlById(urlId);
        if (monitoredUrl == null) {
            Log.e(TAG, "URL with ID " + urlId + " not found in database");
            return;
        }
        
        if (!monitoredUrl.isActive()) {
            Log.d(TAG, "URL is inactive, canceling alarm");
            cancelAlarm(context, urlId);
            return;
        }
        
        // Update last checked timestamp
        long now = System.currentTimeMillis();
        monitoredUrl.setLastCheckedTimestamp(now);
        
        boolean hasTickets = false;
        boolean networkError = false;
        
        try {
            Log.d(TAG, "Checking URL: " + url + (isRetry ? " (retry)" : ""));
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(15000)
                    .get();
            
            // Extract event details if not already set
            if (!monitoredUrl.hasEventDetails()) {
                String eventName = extractEventName(doc);
                String eventDate = extractEventDate(doc);
                
                if (!eventName.isEmpty() || !eventDate.isEmpty()) {
                    monitoredUrl.setEventName(eventName);
                    monitoredUrl.setEventDate(eventDate);
                    monitoredUrl.detectEventType();
                    Log.d(TAG, "Extracted event details - Name: " + eventName + ", Date: " + eventDate);
                }
            }
            
            // Simplified ticket detection logic that specifically looks for "Listing available" or "Listings available"
            String pageText = doc.text().toLowerCase();
            hasTickets = pageText.contains("listing available") || pageText.contains("listings available");
            
            // Log the detection result and the relevant text for debugging
            Log.d(TAG, "Page text contains 'listing available': " + pageText.contains("listing available"));
            Log.d(TAG, "Page text contains 'listings available': " + pageText.contains("listings available"));
            
            // Reset retry count on successful check
            retryCountMap.remove(urlId);
            
        } catch (IOException e) {
            Log.e(TAG, "Error checking URL: " + url, e);
            networkError = true;
        }
        
        // Update the URL in the database with last checked time and tickets found status
        monitoredUrl.setHasTicketsFound(hasTickets);
        UrlDatabase.getInstance(context).updateLastChecked(urlId, now, hasTickets);
        
        if (hasTickets) {
            // Add a history entry for this ticket finding
            String note = "Tickets found";
            if (monitoredUrl.hasEventDetails()) {
                note += " for " + monitoredUrl.getEventName();
                if (!monitoredUrl.getEventDate().isEmpty()) {
                    note += " on " + monitoredUrl.getEventDate();
                }
            }
            UrlDatabase.getInstance(context).addTicketHistory(urlId, now, note);
            
            // Broadcast update to refresh UI
            Intent refreshIntent = new Intent("com.example.tixelcheck.URL_UPDATED");
            LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);
            
            // Create a notification with a better title and message based on event details
            String notificationTitle;
            String notificationMessage;
            
            if (!monitoredUrl.getEventName().isEmpty()) {
                notificationTitle = "🎟️ Tickets for " + monitoredUrl.getEventName() + "!";
            } else {
                notificationTitle = "🎟️ Tickets Available!";
            }
            
            if (!monitoredUrl.getEventName().isEmpty() && !monitoredUrl.getEventDate().isEmpty()) {
                notificationMessage = "Tickets have been found for " + monitoredUrl.getEventName() + " on " + monitoredUrl.getEventDate() + ". Tap to view the listing now!";
            } else if (!monitoredUrl.getEventName().isEmpty()) {
                notificationMessage = "Tickets have been found for " + monitoredUrl.getEventName() + ". Tap to view the listing now!";
            } else if (!monitoredUrl.getEventDate().isEmpty()) {
                notificationMessage = "Tickets have been found for your monitored event on " + monitoredUrl.getEventDate() + ". Tap to view the listing now!";
            } else {
                notificationMessage = "Tickets have been found for your monitored event. Tap to view the listing now!";
            }
            
            // Add URL ID to intent for event detail extraction when notification is clicked
            sendNotification(context, notificationTitle, notificationMessage, urlId, url);
            Log.d(TAG, "Tickets found for URL: " + url);
            
            // Also play an additional alarm sound to ensure user is alerted
            try {
                Uri alarmSound = Settings.System.DEFAULT_ALARM_ALERT_URI;
                MediaPlayer mediaPlayer = MediaPlayer.create(context, alarmSound);
                mediaPlayer.setLooping(true); // Loop the alarm until user interacts with notification
                mediaPlayer.start();
                
                // Store reference to MediaPlayer to stop it when the notification is tapped
                synchronized (TicketCheckerAlarm.class) {
                    if (activeAlarmPlayer != null) {
                        activeAlarmPlayer.release();
                    }
                    activeAlarmPlayer = mediaPlayer;
                    activeAlarmUrlId = urlId;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error playing alarm", e);
            }
        } else {
            Log.d(TAG, "No tickets available for URL: " + url);
        }
        
        // Check if we need to retry due to network error
        if (networkError) {
            Log.d(TAG, "Network error occurred, setting up retry with exponential backoff");
            setRetryAlarm(context, monitoredUrl);
        } else {
            // Re-schedule the normal alarm for the next check
            setAlarm(context, monitoredUrl);
        }
    }
    
    /**
     * Extract the event name from the Tixel webpage
     */
    public static String extractEventName(Document doc) {
        try {
            // Try different selectors that might contain the event name
            // For primary event title
            Elements titleElements = doc.select("h1");
            if (!titleElements.isEmpty()) {
                return titleElements.first().text().trim();
            }
            
            // Try event title that might be in other heading levels
            titleElements = doc.select("h2, h3");
            for (Element element : titleElements) {
                if (element.text().length() > 5 && !element.text().toLowerCase().contains("faq") && 
                    !element.text().toLowerCase().contains("similar")) {
                    return element.text().trim();
                }
            }
            
            // Try schema.org metadata
            Elements metadata = doc.select("script[type=application/ld+json]");
            if (!metadata.isEmpty()) {
                String json = metadata.first().html();
                if (json.contains("\"name\":")) {
                    int nameIndex = json.indexOf("\"name\":");
                    if (nameIndex > 0) {
                        int startQuote = json.indexOf("\"", nameIndex + 7) + 1;
                        int endQuote = json.indexOf("\"", startQuote);
                        if (startQuote > 0 && endQuote > startQuote) {
                            return json.substring(startQuote, endQuote).trim();
                        }
                    }
                }
            }
            
            // Try meta tags
            Element ogTitle = doc.select("meta[property=og:title]").first();
            if (ogTitle != null) {
                return ogTitle.attr("content").trim();
            }
            
            return "";
        } catch (Exception e) {
            Log.e(TAG, "Error extracting event name", e);
            return "";
        }
    }
    
    /**
     * Extract the event date from the Tixel webpage
     */
    public static String extractEventDate(Document doc) {
        try {
            // Look for date information in specific elements
            Elements dateElements = doc.select(".event-date, .date, time");
            if (!dateElements.isEmpty()) {
                return dateElements.first().text().trim();
            }
            
            // Try looking for date patterns in text
            Elements paragraphs = doc.select("p, span, div");
            for (Element element : paragraphs) {
                String text = element.text().trim();
                // Check for common date formats
                if ((text.matches(".*\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4}.*") || 
                     text.matches(".*[A-Za-z]+\\s+\\d{1,2}(st|nd|rd|th)?\\s+\\d{4}.*") ||
                     text.matches(".*\\d{1,2}/\\d{1,2}/\\d{2,4}.*") ||
                     text.matches(".*\\d{1,2}-\\d{1,2}-\\d{2,4}.*")) && 
                     text.length() < 40) { // Keep it short to avoid capturing paragraphs
                    return text;
                }
            }
            
            // Try schema.org metadata
            Elements metadata = doc.select("script[type=application/ld+json]");
            if (!metadata.isEmpty()) {
                String json = metadata.first().html();
                if (json.contains("\"startDate\":")) {
                    int dateIndex = json.indexOf("\"startDate\":");
                    if (dateIndex > 0) {
                        int startQuote = json.indexOf("\"", dateIndex + 12) + 1;
                        int endQuote = json.indexOf("\"", startQuote);
                        if (startQuote > 0 && endQuote > startQuote) {
                            return json.substring(startQuote, endQuote).trim();
                        }
                    }
                }
            }
            
            return "";
        } catch (Exception e) {
            Log.e(TAG, "Error extracting event date", e);
            return "";
        }
    }
    
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            
            // Check if channel already exists
            if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
                return;
            }
            
            // Create a high-priority notification channel for alerts
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Tixel Check Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for ticket availability");
            
            // Configure the notification channel
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});
            
            // Set up alarm sound for notifications
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            channel.setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, audioAttributes);
            
            // Create the channel
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(Context context, String title, String message, long urlId, String urlToOpen) {
        createNotificationChannel(context);
        
        // Set up the main click intent (open app)
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(
            context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);
            
        // Set up open URL intent with direct URL
        Intent openUrlIntent = new Intent(context, NotificationActionReceiver.class);
        openUrlIntent.setAction("com.example.tixelcheck.OPEN_URL");
        if (urlToOpen != null && !urlToOpen.isEmpty()) {
            openUrlIntent.putExtra("url", urlToOpen);
            openUrlIntent.putExtra("url_id", urlId); // Include URL ID for event details extraction
            Log.d(TAG, "Added URL to open: " + urlToOpen + " with ID: " + urlId);
        }
        PendingIntent openUrlPendingIntent = PendingIntent.getBroadcast(
            context, (int)urlId * 100 + 1, openUrlIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            
        // Set up stop alarm intent
        Intent stopAlarmIntent = new Intent(context, NotificationActionReceiver.class);
        stopAlarmIntent.setAction("com.example.tixelcheck.STOP_ALARM");
        PendingIntent stopAlarmPendingIntent = PendingIntent.getBroadcast(
            context, (int)urlId * 100 + 2, stopAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create a deleteIntent to stop alarm when notification is dismissed
        Intent deleteIntent = new Intent(context, NotificationActionReceiver.class);
        deleteIntent.setAction("com.example.tixelcheck.STOP_ALARM");
        PendingIntent deleteIntentPending = PendingIntent.getBroadcast(
            context, (int)urlId * 100 + 3, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Create a more attention-grabbing notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Highest priority
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Treat as alarm
                .setContentIntent(mainPendingIntent)
                .setDeleteIntent(deleteIntentPending) // Stop alarm when notification is dismissed
                .setAutoCancel(true)
                .setVibrate(new long[] { 0, 500, 200, 500, 200, 500 }) // Vibration pattern
                .setSound(Settings.System.DEFAULT_ALARM_ALERT_URI) // Use alarm sound
                .setLights(Color.RED, 1000, 500) // Flash LED if available
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        
        // Add full screen intent for heads-up display
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            builder.setFullScreenIntent(mainPendingIntent, true);
        }
                
        // Add action buttons to the notification
        if (urlToOpen != null && !urlToOpen.isEmpty()) {
            builder.addAction(R.drawable.ic_open, "Open Website", openUrlPendingIntent);
        }
        builder.addAction(R.drawable.ic_stop, "Stop Alarm", stopAlarmPendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) urlId, builder.build());
        
        // Also trigger vibration separately for maximum attention
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[] { 0, 1000, 500, 1000, 500, 1000 }, -1));
            } else {
                vibrator.vibrate(new long[] { 0, 1000, 500, 1000, 500, 1000 }, -1);
            }
        }
    }
}