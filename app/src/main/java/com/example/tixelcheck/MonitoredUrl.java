package com.example.tixelcheck;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MonitoredUrl {
    private long id;
    private String url;
    private int frequency; // In minutes
    private boolean isActive;
    private String eventName;
    private String eventDate;
    private String eventType; // "concert", "sports", "theater", "other"
    private long lastChecked; // Timestamp of last check
    private int consecutiveErrors; // For exponential backoff
    private boolean ticketsFound; // Flag to indicate if tickets were found

    public MonitoredUrl(long id, String url, int frequency, boolean isActive) {
        this.id = id;
        this.url = url;
        this.frequency = frequency;
        this.isActive = isActive;
        this.eventName = "";
        this.eventDate = "";
        this.eventType = "other";
        this.lastChecked = 0;
        this.consecutiveErrors = 0;
        this.ticketsFound = false;
    }

    public MonitoredUrl(long id, String url, int frequency, boolean isActive, String eventName, String eventDate) {
        this.id = id;
        this.url = url;
        this.frequency = frequency;
        this.isActive = isActive;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventType = determineEventType(eventName);
        this.lastChecked = 0;
        this.consecutiveErrors = 0;
        this.ticketsFound = false;
    }
    
    public MonitoredUrl(long id, String url, int frequency, boolean isActive, 
                       String eventName, String eventDate, String eventType, 
                       long lastChecked, int consecutiveErrors, boolean ticketsFound) {
        this.id = id;
        this.url = url;
        this.frequency = frequency;
        this.isActive = isActive;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventType = eventType;
        this.lastChecked = lastChecked;
        this.consecutiveErrors = consecutiveErrors;
        this.ticketsFound = ticketsFound;
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
        this.eventType = determineEventType(eventName);
    }
    
    public String getEventDate() {
        return eventDate;
    }
    
    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public long getLastChecked() {
        return lastChecked;
    }
    
    public void setLastChecked(long lastChecked) {
        this.lastChecked = lastChecked;
    }
    
    public int getConsecutiveErrors() {
        return consecutiveErrors;
    }
    
    public void setConsecutiveErrors(int consecutiveErrors) {
        this.consecutiveErrors = consecutiveErrors;
    }
    
    public void incrementConsecutiveErrors() {
        this.consecutiveErrors++;
    }
    
    public void resetConsecutiveErrors() {
        this.consecutiveErrors = 0;
    }
    
    public boolean isTicketsFound() {
        return ticketsFound;
    }
    
    public void setTicketsFound(boolean ticketsFound) {
        this.ticketsFound = ticketsFound;
    }
    
    public boolean hasEventDetails() {
        return eventName != null && !eventName.isEmpty();
    }
    
    /**
     * Gets a formatted string representation of when this URL was last checked
     */
    public String getLastCheckedFormatted() {
        if (lastChecked <= 0) {
            return "Never checked";
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy, h:mm a", Locale.getDefault());
        return "Last checked: " + dateFormat.format(new Date(lastChecked));
    }
    
    /**
     * Determine event type based on event name
     */
    private String determineEventType(String name) {
        if (name == null || name.isEmpty()) {
            return "other";
        }
        
        String lowerName = name.toLowerCase();
        
        // Check for concert keywords
        if (lowerName.contains("concert") || lowerName.contains("festival") || 
            lowerName.contains("music") || lowerName.contains("band") || 
            lowerName.contains("tour") || lowerName.contains("live") ||
            lowerName.contains("dj") || lowerName.contains("gig")) {
            return "concert";
        }
        
        // Check for sports keywords
        if (lowerName.contains("game") || lowerName.contains("match") || 
            lowerName.contains("sport") || lowerName.contains("league") || 
            lowerName.contains("cup") || lowerName.contains("championship") ||
            lowerName.contains("football") || lowerName.contains("soccer") ||
            lowerName.contains("rugby") || lowerName.contains("cricket") ||
            lowerName.contains("tennis") || lowerName.contains("basketball") ||
            lowerName.contains("golf") || lowerName.contains("racing")) {
            return "sports";
        }
        
        // Check for theater keywords
        if (lowerName.contains("theater") || lowerName.contains("theatre") || 
            lowerName.contains("play") || lowerName.contains("musical") || 
            lowerName.contains("stage") || lowerName.contains("performance") ||
            lowerName.contains("comedy") || lowerName.contains("drama") ||
            lowerName.contains("show") || lowerName.contains("act")) {
            return "theater";
        }
        
        // Default
        return "other";
    }
}