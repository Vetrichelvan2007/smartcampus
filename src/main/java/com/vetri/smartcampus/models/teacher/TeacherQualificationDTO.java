package com.vetri.smartcampus.models.teacher;

public class TeacherQualificationDTO {

    private Long teacherId;
    private String ugDegree;
    private String pgDegree;
    private String phdStatus;
    private String specialization;
    private String universityName;
    private int yearOfPassing;

    public TeacherQualificationDTO() {}

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getUgDegree() {
        return ugDegree;
    }

    public void setUgDegree(String ugDegree) {
        this.ugDegree = ugDegree;
    }

    public String getPgDegree() {
        return pgDegree;
    }

    public void setPgDegree(String pgDegree) {
        this.pgDegree = pgDegree;
    }

    public String getPhdStatus() {
        return phdStatus;
    }

    public void setPhdStatus(String phdStatus) {
        this.phdStatus = phdStatus;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public int getYearOfPassing() {
        return yearOfPassing;
    }

    public void setYearOfPassing(int yearOfPassing) {
        this.yearOfPassing = yearOfPassing;
    }
}
