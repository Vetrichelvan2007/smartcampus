package com.vetri.smartcampus.models.teacher;

public class ViewStudentDTO {

    private String studentName;
    private String studentRegisterNumber;
    private String studentEmail;
    private String department;
    private int semester;
    private String status;

    public ViewStudentDTO() {}

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentRegisterNumber() {
        return studentRegisterNumber;
    }

    public void setStudentRegisterNumber(String studentRegisterNumber) {
        this.studentRegisterNumber = studentRegisterNumber;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
