package com.example.tixelcheck;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "TixelCheckPrefs";
    private static final String THEME_KEY = "app_theme";
    
    private RecyclerView recyclerView;
    private UrlAdapter adapter;
    private List<MonitoredUrl> urlList;
    
    // Define a broadcast receiver for event detail updates
    private BroadcastReceiver eventDetailsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Refresh the URL list to show updated event details
            refreshUrlList();
        }
    };
    
    // Define a broadcast receiver for URL updates
    private BroadcastReceiver urlUpdatesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Refresh the URL list to show updated URLs
            refreshUrlList();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before setting content view
        applyTheme();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Set action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Tixel Check");
        }
        
        // Stop any active alarm when the app is opened
        stopActiveAlarm();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        urlList = UrlDatabase.getInstance(this).getAllUrls();
        adapter = new UrlAdapter(urlList, this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_url);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddUrlDialog dialog = new AddUrlDialog(MainActivity.this, new AddUrlDialog.UrlDialogListener() {
                    @Override
                    public void onUrlAdded(MonitoredUrl url) {
                        UrlDatabase.getInstance(MainActivity.this).addUrl(url);
                        urlList.add(url);
                        adapter.notifyItemInserted(urlList.size() - 1);
                        
                        // Start monitoring this URL
                        TicketCheckerAlarm.setAlarm(MainActivity.this, url);
                    }
                });
                dialog.show();
            }
        });
        
        // Add Test Alert button functionality
        Button testAlertButton = findViewById(R.id.button_test_alert);
        testAlertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerTestAlert();
            }
        });

        // Create notification channel for Android 8.0 and higher
        createNotificationChannel();
        
        // Start the monitoring service
        TicketMonitorService.scheduleJob(this);
        
        // Ensure all active URLs have alarms set
        resetActiveAlarms();
        
        // Register for event details update broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(
            eventDetailsReceiver, new IntentFilter("com.example.tixelcheck.EVENT_DETAILS_UPDATED"));
        
        // Register for URL updates broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(
            urlUpdatesReceiver, new IntentFilter("com.example.tixelcheck.URL_UPDATED"));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_theme) {
            // Toggle dark mode
            toggleDarkMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Toggles between light and dark mode
     */
    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int currentTheme = prefs.getInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        // Toggle between light and dark
        int newTheme;
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            newTheme = AppCompatDelegate.MODE_NIGHT_NO;
            Toast.makeText(this, "Light Mode Enabled", Toast.LENGTH_SHORT).show();
        } else {
            newTheme = AppCompatDelegate.MODE_NIGHT_YES;
            Toast.makeText(this, "Dark Mode Enabled", Toast.LENGTH_SHORT).show();
        }
        
        // Save the new theme setting
        prefs.edit().putInt(THEME_KEY, newTheme).apply();
        
        // Apply the new theme
        AppCompatDelegate.setDefaultNightMode(newTheme);
        
        // Recreate activity to apply the theme
        recreate();
    }
    
    /**
     * Apply the saved theme on app start
     */
    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int themeMode = prefs.getInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }
    
    /**
     * Get the current UI mode (dark or light)
     */
    private boolean isDarkModeActive() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Also stop any active alarm when the app resumes
        stopActiveAlarm();
        
        // Refresh URL list to get latest event details
        refreshUrlList();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Stop active alarm when app is brought to front by a notification click
        stopActiveAlarm();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister broadcast receivers
        LocalBroadcastManager.getInstance(this).unregisterReceiver(eventDetailsReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(urlUpdatesReceiver);
    }
    
    /**
     * Refreshes the URL list from the database
     */
    private void refreshUrlList() {
        List<MonitoredUrl> freshUrls = UrlDatabase.getInstance(this).getAllUrls();
        urlList.clear();
        urlList.addAll(freshUrls);
        adapter.notifyDataSetChanged();
    }
    
    /**
     * Stops any active alarm sound
     */
    private void stopActiveAlarm() {
        Log.d(TAG, "Stopping active alarm from MainActivity");
        TicketCheckerAlarm.stopAlarmSound();
        
        // Also clear the notification for the active alarm
        if (TicketCheckerAlarm.activeAlarmUrlId > 0) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel((int) TicketCheckerAlarm.activeAlarmUrlId);
            TicketCheckerAlarm.activeAlarmUrlId = 0;
        }
    }
    
    /**
     * Creates the notification channel for high-priority alerts
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            
            // Check if channel already exists
            if (notificationManager.getNotificationChannel("tixel_check_channel") != null) {
                return;
            }
            
            NotificationChannel channel = new NotificationChannel(
                    "tixel_check_channel",
                    "Tixel Check Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for ticket availability");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000, 500, 1000});
            
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build();
            channel.setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, audioAttributes);
            
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Makes sure all active URLs have corresponding alarms set
     */
    private void resetActiveAlarms() {
        List<MonitoredUrl> activeUrls = UrlDatabase.getInstance(this).getActiveUrls();
        for (MonitoredUrl url : activeUrls) {
            // Cancel any existing alarm first to avoid duplicates
            TicketCheckerAlarm.cancelAlarm(this, url.getId());
            // Then set up a new alarm
            TicketCheckerAlarm.setAlarm(this, url);
        }
    }
    
    /**
     * Triggers a test alert to demonstrate what users will experience when tickets become available
     */
    private void triggerTestAlert() {
        // Display toast message
        Toast.makeText(this, "Testing alert notification", Toast.LENGTH_SHORT).show();
        
        // Create test notification with sample event details
        String sampleEventName = "Sample Concert";
        String sampleEventDate = "April 15, 2025";
        
        String notificationTitle = "🎟️ TEST: Tickets for " + sampleEventName + "!";
        String notificationMessage = "This is a test alert showing how notifications will look. The real alert for " + 
            sampleEventName + " on " + sampleEventDate + " will include the event details like this.";
        
        // Create notification with same parameters as the real alert
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1001, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "tixel_check_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notificationTitle)
                .setContentText(notificationMessage)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[] { 0, 500, 200, 500, 200, 500 })
                .setSound(Settings.System.DEFAULT_ALARM_ALERT_URI)
                .setLights(Color.RED, 1000, 500)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationMessage))
                .setAutoCancel(true);
                
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(9999, builder.build());
        
        // Trigger vibration
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[] { 0, 1000, 500, 1000 }, -1));
            } else {
                vibrator.vibrate(new long[] { 0, 1000, 500, 1000 }, -1);
            }
        }
        
        // Play alarm sound (in addition to notification sound)
        try {
            Uri alarmSound = Settings.System.DEFAULT_ALARM_ALERT_URI;
            MediaPlayer mediaPlayer = MediaPlayer.create(this, alarmSound);
            mediaPlayer.setLooping(false);
            mediaPlayer.start();
            
            // Stop the sound after 3 seconds (it's just a test)
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.release();
                }
            }, 3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}