package com.example.travelapp;

public class Question {

    public enum Type { TEXT, NUMBER, DATE, CHOICE }

    private final Type type;
    private final String title;
    private final String description;
    private final String[] options;

    public Question(Type type, String title, String description) {
        this(type, title, description, new String[0]);
    }

    public Question(Type type, String title, String description, String[] options) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.options = options;
    }

    public Type getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String[] getOptions() {
        return options;
    }
}
