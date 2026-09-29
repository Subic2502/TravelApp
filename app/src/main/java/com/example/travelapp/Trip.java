package com.example.travelapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class Trip {

    private static final String DATE_PATTERN = "dd.MM.yyyy";

    private final long id;
    private final String destination;
    private final int days;
    private final long startDate;
    private final String travelStyle;
    private final String budget;
    private final String summary;
    private final long createdAt;

    public Trip(long id, String destination, int days, long startDate, String travelStyle,
                String budget, String summary, long createdAt) {
        this.id = id;
        this.destination = destination;
        this.days = days;
        this.startDate = startDate;
        this.travelStyle = travelStyle;
        this.budget = budget;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getDestination() {
        return destination;
    }

    public int getDays() {
        return days;
    }

    public long getStartDate() {
        return startDate;
    }

    public String getTravelStyle() {
        return travelStyle;
    }

    public String getBudget() {
        return budget;
    }

    public String getSummary() {
        return summary;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Poslednji dan putovanja (putovanje od 3 dana traje od dana 1 do dana 3).
    public long getEndDate() {
        return startDate + TimeUnit.DAYS.toMillis(days - 1);
    }

    // Datumi se čuvaju kao UTC ponoć (tako ih vraća MaterialDatePicker).
    public static String formatDate(long millis) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(millis));
    }
}
