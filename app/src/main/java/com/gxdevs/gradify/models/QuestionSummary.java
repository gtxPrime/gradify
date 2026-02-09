package com.gxdevs.gradify.models;

public class QuestionSummary {
    private int actualPosition; // Real index in JSON array
    private int displayPosition; // Display number (excluding extra info)
    private double totalMarks;
    private double userScore;
    private boolean isExtraInfo;

    public QuestionSummary(int actualPosition, int displayPosition, double totalMarks, double userScore, boolean isExtraInfo) {
        this.actualPosition = actualPosition;
        this.displayPosition = displayPosition;
        this.totalMarks = totalMarks;
        this.userScore = userScore;
        this.isExtraInfo = isExtraInfo;
    }

    public int getActualPosition() {
        return actualPosition;
    }

    public int getDisplayPosition() {
        return displayPosition;
    }

    public double getTotalMarks() {
        return totalMarks;
    }

    public double getUserScore() {
        return userScore;
    }

    public boolean isExtraInfo() {
        return isExtraInfo;
    }

    public boolean isCorrect() {
        return userScore == totalMarks && totalMarks > 0;
    }

    public boolean isPartiallyCorrect() {
        return userScore > 0 && userScore < totalMarks;
    }
} 