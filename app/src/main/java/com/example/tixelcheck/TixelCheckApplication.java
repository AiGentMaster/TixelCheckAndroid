package com.example.tixelcheck;

import android.app.Application;

import timber.log.Timber;

public class TixelCheckApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Timber logging
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(new Timber.DebugTree());
        } else {
            // For release builds, use a crash reporting tree that only logs errors
            Timber.plant(new CrashReportingTree());
        }
    }

    /**
     * A tree which logs important information for crash reporting.
     * In a real app, this would send logs to Crashlytics or another crash reporting service.
     */
    private static class CrashReportingTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == android.util.Log.ERROR || priority == android.util.Log.WARN) {
                // In a real app, this would send logs to Crashlytics or similar
                android.util.Log.e(tag, message, t);
            }
        }
    }
}