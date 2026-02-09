package com.gxdevs.gradify.models;

public class DailySubjectTotalData {
    private String subjectName;
    private long totalDurationMillis;

    public DailySubjectTotalData(String subjectName, long totalDurationMillis) {
        this.subjectName = subjectName;
        this.totalDurationMillis = totalDurationMillis;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public long getTotalDurationMillis() {
        return totalDurationMillis;
    }

    public void setTotalDurationMillis(long totalDurationMillis) {
        this.totalDurationMillis = totalDurationMillis;
    }
} 