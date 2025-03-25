package com.example.tixelcheck;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private UrlAdapter adapter;
    private List<MonitoredUrl> urlList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        
        // Create notification with same parameters as the real alert
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1001, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "tixel_check_channel")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("TEST ALERT: Tickets Available!")
                .setContentText("This is a test alert. A real alert will look like this when tickets are found.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[] { 0, 500, 200, 500, 200, 500 })
                .setSound(Settings.System.DEFAULT_ALARM_ALERT_URI)
                .setLights(Color.RED, 1000, 500)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("This is a test alert. When tickets are found for an event you're monitoring, you'll receive a notification like this one."))
                .setAutoCancel(true);
                
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(9999, builder.build());
        
        // Trigger vibration
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
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