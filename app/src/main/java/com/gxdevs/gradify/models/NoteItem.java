package com.gxdevs.gradify.models;

public class NoteItem {
    private final String link;
    private final String helper;
    private final String week;

    public NoteItem(String link, String helper, String week) {
        this.link = link;
        this.helper = helper;
        this.week = week;
    }

    public String getLink() {
        return link;
    }

    public String getHelper() {
        return helper;
    }

    public String getWeek() {
        return week;
    }
} 