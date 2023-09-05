package com.msiganos.driveon.helpers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class IncidentHelper {

    private String uid, incident, timestamp;
    private double latitude, longitude, speed, altitude, bearing;

    public IncidentHelper() {
    }

    public IncidentHelper(String uid, String incident, double latitude, double longitude, double speed, double altitude, double bearing) {
        setUid(uid);
        setIncident(incident);
        setLatitude(latitude);
        setLongitude(longitude);
        setSpeed(speed);
        setAltitude(altitude);
        setBearing(bearing);
        setTimestamp();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getIncident() {
        return incident;
    }

    public void setIncident(String incident) {
        this.incident = incident;
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

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getBearing() {
        return bearing;
    }

    public void setBearing(double bearing) {
        this.bearing = bearing;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public String getTimestamp() {
        String input = timestamp;
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.getDefault());
        LocalDateTime localDateTime = LocalDateTime.parse(input, dateTimeFormatter);
        return localDateTime.format(dateTimeFormatter);
    }

    @SuppressWarnings("SpellCheckingInspection")
    public void setTimestamp() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        Date date = new Date();
        this.timestamp = dateFormat.format(date);
    }
}