package com.vetri.smartcampus.models;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class QuizDTO {
    private long id;
    private long courseId;
    private long teacherId;
    private String title;
    private String instructions;
    private int totalMarks;
    private int durationMinutes;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    // Pre-formatted UI text (avoid LocalDateTime default toString)
    private String startAtText;
    private String endAtText;

    private boolean published;

    // Teacher-controlled toggle for students
    private boolean scorePublished;

    private Timestamp createdAt;

    private int questionCount;
    private int eligibleCount;
    private int submittedCount;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCourseId() {
        return courseId;
    }

    public void setCourseId(long courseId) {
        this.courseId = courseId;
    }

    public long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(long teacherId) {
        this.teacherId = teacherId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
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

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public boolean isScorePublished() {
        return scorePublished;
    }

    public void setScorePublished(boolean scorePublished) {
        this.scorePublished = scorePublished;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public int getEligibleCount() {
        return eligibleCount;
    }

    public void setEligibleCount(int eligibleCount) {
        this.eligibleCount = eligibleCount;
    }

    public int getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(int submittedCount) {
        this.submittedCount = submittedCount;
    }
}
