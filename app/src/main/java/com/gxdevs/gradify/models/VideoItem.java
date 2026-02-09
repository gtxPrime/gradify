package com.gxdevs.gradify.models;

public class VideoItem {
    private String id;
    private String title;
    private String link;

    // Getters
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    // Setters (optional, but good for flexibility)
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setLink(String link) {
        this.link = link;
    }
} 