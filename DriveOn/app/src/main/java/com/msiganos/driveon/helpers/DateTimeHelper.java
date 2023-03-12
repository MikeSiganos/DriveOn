package com.msiganos.driveon.helpers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeHelper {

    private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private String date, time;

    public DateTimeHelper() {
        Date today = new Date();
        setDate(dateFormat.format(today));
        setTime(timeFormat.format(today));
    }

    public DateTimeHelper(Date date) {
        setDate(dateFormat.format(date));
        setTime(timeFormat.format(date));
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {

        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}