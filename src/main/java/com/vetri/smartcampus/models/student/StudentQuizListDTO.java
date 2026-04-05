package com.vetri.smartcampus.models.student;

import java.time.LocalDateTime;

public class StudentQuizListDTO {
    private long quizId;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    // Pre-formatted strings for UI so templates don't render LocalDateTime as 2026-03-29T15:30
    private String startAtText;
    private String endAtText;

    // UPCOMING / ACTIVE / CLOSED
    private String status;

    private boolean submitted;
    private Integer score;

    // Teacher-controlled toggle
    private boolean scorePublished;

    public long getQuizId() {
        return quizId;
    }

    public void setQuizId(long quizId) {
        this.quizId = quizId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getStartAtText() {
        return startAtText;
    }

    public void setStartAtText(String startAtText) {
        this.startAtText = startAtText;
    }

    public String getEndAtText() {
        return endAtText;
    }

    public void setEndAtText(String endAtText) {
        this.endAtText = endAtText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public boolean isScorePublished() {
        return scorePublished;
    }

    public void setScorePublished(boolean scorePublished) {
        this.scorePublished = scorePublished;
    }
}
