package com.msiganos.driveon.helpers;

public class UserHelper {

    private String uid, nickname, device, locales;
    private double averageSpeed;
    private int drivingStatus, accelerations, decelerations, incidents;
    private DateTimeHelper firstRegister, lastLogin;

    public UserHelper() {
    }

    public UserHelper(String uid, String nickname, String device, String locales) {
        setUid(uid);
        setNickname(nickname);
        setDevice(device);
        setLocales(locales);
        setDrivingStatus(0);
        setAverageSpeed(0);
        setAccelerations(0);
        setDecelerations(0);
        setIncidents(0);
        setFirstRegister(new DateTimeHelper());
        setLastLogin(new DateTimeHelper());
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getLocales() {
        return locales;
    }

    public void setLocales(String locales) {
        this.locales = locales;
    }

    public int getDrivingStatus() {
        return drivingStatus;
    }

    public void setDrivingStatus(int drivingStatus) {
        this.drivingStatus = drivingStatus;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public int getAccelerations() {
        return accelerations;
    }

    public void setAccelerations(int accelerations) {
        this.accelerations = accelerations;
    }

    public int getDecelerations() {
        return decelerations;
    }

    public void setDecelerations(int decelerations) {
        this.decelerations = decelerations;
    }

    public int getIncidents() {
        return incidents;
    }

    public void setIncidents(int incidents) {
        this.incidents = incidents;
    }

    public DateTimeHelper getFirstRegister() {
        return firstRegister;
    }

    public void setFirstRegister(DateTimeHelper firstRegister) {
        this.firstRegister = firstRegister;
    }

    public DateTimeHelper getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(DateTimeHelper lastLogin) {
        this.lastLogin = lastLogin;
    }
}