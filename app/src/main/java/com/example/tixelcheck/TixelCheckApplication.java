package com.example.tixelcheck;

import android.app.Application;
import android.util.Log;

public class TixelCheckApplication extends Application {
    private static final String TAG = "TixelCheckApp";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize logging
        Log.i(TAG, "Application initialized");
    }
}