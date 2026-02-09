package com.gxdevs.gradify.models;

import java.util.List;
import java.util.Map;

public class LectureData {
    private String subject;
    private Map<String, List<VideoItem>> weeks;

    // Getters
    public String getSubject() {
        return subject;
    }

    public Map<String, List<VideoItem>> getWeeks() {
        return weeks;
    }

    // Setters (optional)
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setWeeks(Map<String, List<VideoItem>> weeks) {
        this.weeks = weeks;
    }
} 