package com.example.travelapp;

public class PlanItem {

    private final long id;
    private final int day;
    private final String time;
    private final String type;
    private final String title;
    private final String description;
    private boolean done;
    private String note;

    public PlanItem(long id, int day, String time, String type, String title,
                    String description, boolean done, String note) {
        this.id = id;
        this.day = day;
        this.time = time;
        this.type = type;
        this.title = title;
        this.description = description;
        this.done = done;
        this.note = note;
    }

    public long getId() {
        return id;
    }

    public int getDay() {
        return day;
    }

    public String getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
