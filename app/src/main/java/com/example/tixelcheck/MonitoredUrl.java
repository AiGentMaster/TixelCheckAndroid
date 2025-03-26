package com.example.tixelcheck;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MonitoredUrl {
    // Event types
    public static final String EVENT_TYPE_CONCERT = "concert";
    public static final String EVENT_TYPE_SPORTS = "sports";
    public static final String EVENT_TYPE_THEATER = "theater";
    public static final String EVENT_TYPE_OTHER = "other";
    
    private long id;
    private String url;
    private int frequency; // In minutes
    private boolean isActive;
    private String eventName;
    private String eventDate;
    private String eventType; // concert, sports, theater, other
    private long lastCheckedTimestamp; // Unix timestamp of last check
    private boolean hasTicketsFound; // Whether tickets were found on last check

    public MonitoredUrl(long id, String url, int frequency, boolean isActive) {
        this.id = id;
        this.url = url;
        this.frequency = frequency;
        this.isActive = isActive;
        this.eventName = "";
        this.eventDate = "";
        this.eventType = EVENT_TYPE_OTHER;
        this.lastCheckedTimestamp = 0;
        this.hasTicketsFound = false;
    }

    public MonitoredUrl(long id, String url, int frequency, boolean isActive, String eventName, String eventDate) {
        this.id = id;
        this.url = url;
        this.frequency = frequency;
        this.isActive = isActive;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventType = EVENT_TYPE_OTHER; // Default
        this.lastCheckedTimestamp = 0;
        this.hasTicketsFound = false;
    }
    
    public MonitoredUrl(long id, String url, int frequency, boolean isActive, 
                       String eventName, String eventDate, String eventType,
                       long lastCheckedTimestamp, boolean hasTicketsFound) {
        this.id = id;
        this.url = url;
        this.frequency = frequency;
        this.isActive = isActive;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.lastCheckedTimestamp = lastCheckedTimestamp;
        this.hasTicketsFound = hasTicketsFound;
    }

    public long getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public int getFrequency() {
        return frequency;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
    
    public String getEventName() {
        return eventName;
    }
    
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    
    public String getEventDate() {
        return eventDate;
    }
    
    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }
    
    public boolean hasEventDetails() {
        return eventName != null && !eventName.isEmpty();
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public long getLastCheckedTimestamp() {
        return lastCheckedTimestamp;
    }
    
    public void setLastCheckedTimestamp(long lastCheckedTimestamp) {
        this.lastCheckedTimestamp = lastCheckedTimestamp;
    }
    
    public boolean hasTicketsFound() {
        return hasTicketsFound;
    }
    
    public void setHasTicketsFound(boolean hasTicketsFound) {
        this.hasTicketsFound = hasTicketsFound;
    }
    
    public void updateLastChecked() {
        this.lastCheckedTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Gets formatted last checked string in format "DD/MM/YYYY, HH:MMam/pm"
     */
    public String getFormattedLastChecked() {
        if (lastCheckedTimestamp == 0) {
            return "Never checked";
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, hh:mma", Locale.getDefault());
        return sdf.format(new Date(lastCheckedTimestamp));
    }
    
    /**
     * Determines the event type from the event name and URL
     * Uses keywords to guess the most likely type
     */
    public void detectEventType() {
        String nameAndUrl = (eventName + " " + url).toLowerCase();
        
        // Check for concert/music keywords
        if (nameAndUrl.contains("concert") || nameAndUrl.contains("music") || 
            nameAndUrl.contains("festival") || nameAndUrl.contains("band") ||
            nameAndUrl.contains("singer") || nameAndUrl.contains("tour") ||
            nameAndUrl.contains("dj")) {
            this.eventType = EVENT_TYPE_CONCERT;
            return;
        }
        
        // Check for sports keywords
        if (nameAndUrl.contains("game") || nameAndUrl.contains("match") ||
            nameAndUrl.contains("stadium") || nameAndUrl.contains("arena") ||
            nameAndUrl.contains("sport") || nameAndUrl.contains("ball") ||
            nameAndUrl.contains("team") || nameAndUrl.contains("championship")) {
            this.eventType = EVENT_TYPE_SPORTS;
            return;
        }
        
        // Check for theater keywords
        if (nameAndUrl.contains("theater") || nameAndUrl.contains("theatre") ||
            nameAndUrl.contains("show") || nameAndUrl.contains("play") ||
            nameAndUrl.contains("musical") || nameAndUrl.contains("stage") ||
            nameAndUrl.contains("comedy") || nameAndUrl.contains("performance")) {
            this.eventType = EVENT_TYPE_THEATER;
            return;
        }
        
        // Default to other
        this.eventType = EVENT_TYPE_OTHER;
    }
}