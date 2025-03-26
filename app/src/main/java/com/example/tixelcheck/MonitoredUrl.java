package com.example.tixelcheck;

public class MonitoredUrl {
    private long id;
    private String url;
    private int frequency; // In minutes
    private boolean isActive;
    private String eventName;
    private String eventDate;

    public MonitoredUrl(long id, String url, int frequency, boolean isActive) {
        this.id = id;
        this.url = url;
        this.frequency = frequency;
        this.isActive = isActive;
        this.eventName = "";
        this.eventDate = "";
    }

    public MonitoredUrl(long id, String url, int frequency, boolean isActive, String eventName, String eventDate) {
        this.id = id;
        this.url = url;
        this.frequency = frequency;
        this.isActive = isActive;
        this.eventName = eventName;
        this.eventDate = eventDate;
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
}