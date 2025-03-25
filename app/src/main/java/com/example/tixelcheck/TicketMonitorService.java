package com.example.tixelcheck;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.util.List;

/**
 * Background service responsible for ensuring that monitoring continues
 * even if individual alarms fail.
 */
public class TicketMonitorService extends JobService {
    private static final String TAG = "TicketMonitorService";
    private static final int JOB_ID = 1000;
    
    /**
     * Schedule the monitoring job to run periodically
     */
    public static void scheduleJob(Context context) {
        ComponentName componentName = new ComponentName(context, TicketMonitorService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName)
                .setPersisted(true)  // Service persists after reboot
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        
        // Set job to run at least once per hour
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // For Android 7.0 and higher, use setMinimumLatency
            builder.setMinimumLatency(60 * 60 * 1000); // 1 hour
        } else {
            // For earlier versions, use setPeriodic
            builder.setPeriodic(60 * 60 * 1000); // 1 hour
        }
        
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.schedule(builder.build());
            Log.d(TAG, "Ticket monitor job scheduled");
        }
    }
    
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Monitor service job started");
        
        // Reset all active alarms for active URLs in case any were missed
        new Thread(() -> {
            List<MonitoredUrl> activeUrls = UrlDatabase.getInstance(getApplicationContext()).getActiveUrls();
            Log.d(TAG, "Resetting alarms for " + activeUrls.size() + " active URLs");
            
            for (MonitoredUrl url : activeUrls) {
                TicketCheckerAlarm.setAlarm(getApplicationContext(), url);
            }
            
            // Reschedule the job after executing
            jobFinished(params, false);
            scheduleJob(getApplicationContext());
        }).start();
        
        return true; // Job is being handled on a separate thread
    }
    
    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Monitor service job stopped");
        return true; // Reschedule the job if it gets stopped prematurely
    }
}