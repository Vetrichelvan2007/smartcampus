package com.vetri.smartcampus.models.teacher;

public class AssignedCourses {

    private Long  courseid;
    private String coursename;
    private String coursecode;
    private String coursedept;
    private Integer sem;
    private Integer Credits;

    public Long getCourseid() {
        return courseid;
    }

    public void setCourseid(Long courseid) {
        this.courseid = courseid;
    }

    public String getCoursename() {
        return coursename;
    }

    public void setCoursename(String coursename) {
        this.coursename = coursename;
    }

    public String getCoursecode() {
        return coursecode;
    }

    public void setCoursecode(String coursecode) {
        this.coursecode = coursecode;
    }

    public String getCoursedept() {
        return coursedept;
    }

    public void setCoursedept(String coursedept) {
        this.coursedept = coursedept;
    }

    public Integer getSem() {
        return sem;
    }

    public void setSem(Integer sem) {
        this.sem = sem;
    }

    public Integer getCredits() {
        return Credits;
    }

    public void setCredits(Integer credits) {
        this.Credits = credits;
    }
}
