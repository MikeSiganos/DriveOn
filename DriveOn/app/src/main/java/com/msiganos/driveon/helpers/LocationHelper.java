package com.msiganos.driveon.helpers;

public class LocationHelper {

    private String uid, timestamp;
    private double latitude, longitude, lastSpeed, speed, acceleration, deceleration, lastBearing, bearing;
    private boolean usingAccelerometer;

    public LocationHelper() {
    }

    public LocationHelper(String uid, String timestamp, double latitude, double longitude, double lastSpeed, double speed, double acceleration, double deceleration, double lastBearing, double bearing, boolean usingAccelerometer) {
        setUid(uid);
        setTimestamp(timestamp);
        setLatitude(latitude);
        setLongitude(longitude);
        setLastSpeed(lastSpeed);
        setSpeed(speed);
        setAcceleration(acceleration);
        setDeceleration(deceleration);
        setLastBearing(lastBearing);
        setBearing(bearing);
        setUsingAccelerometer(usingAccelerometer);
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

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLastSpeed() {
        return lastSpeed;
    }

    public void setLastSpeed(double lastSpeed) {
        this.lastSpeed = lastSpeed;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(double acceleration) {
        this.acceleration = acceleration;
    }

    public double getDeceleration() {
        return deceleration;
    }

    public void setDeceleration(double deceleration) {
        this.deceleration = deceleration;
    }

    public double getLastBearing() {
        return lastBearing;
    }

    public void setLastBearing(double lastBearing) {
        this.lastBearing = lastBearing;
    }

    public double getBearing() {
        return bearing;
    }

    public void setBearing(double bearing) {
        this.bearing = bearing;
    }

    public boolean isUsingAccelerometer() {
        return usingAccelerometer;
    }

    public void setUsingAccelerometer(boolean usingAccelerometer) {
        this.usingAccelerometer = usingAccelerometer;
    }
}