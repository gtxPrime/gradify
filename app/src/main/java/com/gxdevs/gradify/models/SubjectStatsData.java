package com.gxdevs.gradify.models;

import android.graphics.Color; // Import for Color

public class SubjectStatsData {
    private String subjectName;
    private long pyqTimeMillis;
    private long lectureTimeMillis;
    private int pyqProgress; // 0-100
    private int lectureProgress; // 0-100
    private int subjectColor; // Added field for subject color

    public SubjectStatsData(String subjectName, long pyqTimeMillis, long lectureTimeMillis, int pyqProgress, int lectureProgress) {
        this.subjectName = subjectName;
        this.pyqTimeMillis = pyqTimeMillis;
        this.lectureTimeMillis = lectureTimeMillis;
        this.pyqProgress = pyqProgress;
        this.lectureProgress = lectureProgress;
        this.subjectColor = Color.TRANSPARENT; // Default color
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public long getPyqTimeMillis() {
        return pyqTimeMillis;
    }

    public void setPyqTimeMillis(long pyqTimeMillis) {
        this.pyqTimeMillis = pyqTimeMillis;
    }

    public long getLectureTimeMillis() {
        return lectureTimeMillis;
    }

    public void setLectureTimeMillis(long lectureTimeMillis) {
        this.lectureTimeMillis = lectureTimeMillis;
    }

    public int getPyqProgress() {
        return pyqProgress;
    }

    public void setPyqProgress(int pyqProgress) {
        this.pyqProgress = pyqProgress;
    }

    public int getLectureProgress() {
        return lectureProgress;
    }

    public void setLectureProgress(int lectureProgress) {
        this.lectureProgress = lectureProgress;
    }

    public int getSubjectColor() {
        return subjectColor;
    }

    public void setSubjectColor(int subjectColor) {
        this.subjectColor = subjectColor;
    }

    public String getFormattedPyqTime() {
        return formatMillisToHoursMinutes(pyqTimeMillis);
    }

    public String getFormattedLectureTime() {
        return formatMillisToHoursMinutes(lectureTimeMillis);
    }

    private String formatMillisToHoursMinutes(long millis) {
        long hours = millis / (1000 * 60 * 60);
        long minutes = (millis / (1000 * 60)) % 60;
        if (hours == 0 && minutes == 0 && millis > 0) { // Show at least 1 min for small durations > 0
            return "<1m";
        }
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
} 