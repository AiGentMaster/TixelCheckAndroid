package com.example.tixelcheck;

public class MonitoredUrl {
    private long id;
    private String url;
    private int frequency; // In minutes
    private boolean isActive;

    public MonitoredUrl(long id, String url, int frequency, boolean isActive) {
        this.id = id;
        this.url = url;
        this.frequency = frequency;
        this.isActive = isActive;
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
}