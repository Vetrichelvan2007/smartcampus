package com.vetri.smartcampus.models.teacher;

public class AssignmentStudentStatusDTO {
    private long studentId;
    private String studentName;
    private String studentRegisterNumber;
    private String studentEmail;
    private boolean submitted;
    private long submissionId;
    private String submissionOriginalFileName;
    private String submittedAtText;

    public long getStudentId() { return studentId; }
    public void setStudentId(long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentRegisterNumber() { return studentRegisterNumber; }
    public void setStudentRegisterNumber(String studentRegisterNumber) { this.studentRegisterNumber = studentRegisterNumber; }
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public boolean isSubmitted() { return submitted; }
    public void setSubmitted(boolean submitted) { this.submitted = submitted; }
    public long getSubmissionId() { return submissionId; }
    public void setSubmissionId(long submissionId) { this.submissionId = submissionId; }
    public String getSubmissionOriginalFileName() { return submissionOriginalFileName; }
    public void setSubmissionOriginalFileName(String submissionOriginalFileName) { this.submissionOriginalFileName = submissionOriginalFileName; }
    public String getSubmittedAtText() { return submittedAtText; }
    public void setSubmittedAtText(String submittedAtText) { this.submittedAtText = submittedAtText; }
}
