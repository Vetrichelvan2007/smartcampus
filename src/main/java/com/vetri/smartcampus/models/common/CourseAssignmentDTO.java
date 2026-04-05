package com.vetri.smartcampus.models.common;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class CourseAssignmentDTO {
    private long id;
    private String title;
    private String assignmentMode;
    private String questionText;
    private String instructions;
    private String originalFileName;
    private Long fileSize;
    private LocalDateTime dueAt;
    private String dueAtText;
    private Integer maxMarks;
    private boolean downloadAllowed;
    private Timestamp createdAt;
    private boolean submitted;
    private String submittedAtText;
    private String submissionOriginalFileName;
    private boolean submissionClosed;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAssignmentMode() { return assignmentMode; }
    public void setAssignmentMode(String assignmentMode) { this.assignmentMode = assignmentMode; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public String getDueAtText() { return dueAtText; }
    public void setDueAtText(String dueAtText) { this.dueAtText = dueAtText; }
    public Integer getMaxMarks() { return maxMarks; }
    public void setMaxMarks(Integer maxMarks) { this.maxMarks = maxMarks; }
    public boolean isDownloadAllowed() { return downloadAllowed; }
    public void setDownloadAllowed(boolean downloadAllowed) { this.downloadAllowed = downloadAllowed; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public boolean isSubmitted() { return submitted; }
    public void setSubmitted(boolean submitted) { this.submitted = submitted; }
    public String getSubmittedAtText() { return submittedAtText; }
    public void setSubmittedAtText(String submittedAtText) { this.submittedAtText = submittedAtText; }
    public String getSubmissionOriginalFileName() { return submissionOriginalFileName; }
    public void setSubmissionOriginalFileName(String submissionOriginalFileName) { this.submissionOriginalFileName = submissionOriginalFileName; }
    public boolean isSubmissionClosed() { return submissionClosed; }
    public void setSubmissionClosed(boolean submissionClosed) { this.submissionClosed = submissionClosed; }
}
