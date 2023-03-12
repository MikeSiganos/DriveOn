package com.msiganos.driveon.helpers;

import android.location.Location;

public class TrafficHelper {

    private String uid, timestamp;
    private double latitudeStart, longitudeStart, speedStart, latitudeEnd, longitudeEnd, speedEnd;

    public TrafficHelper() {
    }

    public TrafficHelper(String uid, String timestamp, Location trafficStart, Location trafficEnd) {
        setUid(uid);
        setTimestamp(timestamp);
        setLatitudeStart(trafficStart.getLatitude());
        setLongitudeStart(trafficStart.getLongitude());
        setSpeedStart(trafficStart.getSpeed());
        setLatitudeEnd(trafficEnd.getLatitude());
        setLongitudeEnd(trafficEnd.getLongitude());
        setSpeedEnd(trafficEnd.getSpeed());
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatitudeStart() {
        return latitudeStart;
    }

    public void setLatitudeStart(double latitudeStart) {
        this.latitudeStart = latitudeStart;
    }

    public double getLongitudeStart() {
        return longitudeStart;
    }

    public void setLongitudeStart(double longitudeStart) {
        this.longitudeStart = longitudeStart;
    }

    public double getSpeedStart() {
        return speedStart;
    }

    public void setSpeedStart(double speedStart) {
        this.speedStart = speedStart;
    }

    public double getLatitudeEnd() {
        return latitudeEnd;
    }

    public void setLatitudeEnd(double latitudeEnd) {
        this.latitudeEnd = latitudeEnd;
    }

    public double getLongitudeEnd() {
        return longitudeEnd;
    }

    public void setLongitudeEnd(double longitudeEnd) {
        this.longitudeEnd = longitudeEnd;
    }

    public double getSpeedEnd() {
        return speedEnd;
    }

    public void setSpeedEnd(double speedEnd) {
        this.speedEnd = speedEnd;
    }
}