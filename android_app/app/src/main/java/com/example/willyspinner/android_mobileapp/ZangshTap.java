package com.example.willyspinner.android_mobileapp;

public class ZangshTap {
    private double lat,lng;
    private long timestamp;

    public ZangshTap(double lat, double lng, long timestamp){
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
