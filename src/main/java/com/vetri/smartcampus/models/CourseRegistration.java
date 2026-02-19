package com.vetri.smartcampus.models;

import java.util.List;

public class CourseRegistration {

    private Long courseId;
    private String courseCode;
    private String courseName;
    private String courseType;
    private int courseSem;

    private List<Long> teacherIds;
    private List<String> teacherNames;

    public CourseRegistration() {}

    public CourseRegistration(Long courseId,
                              String courseName,
                              String courseType) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseType = courseType;
    }


    public Long getCourseId() {
        return courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getCourseType() {
        return courseType;
    }

    public int getCourseSem() {
        return courseSem;
    }

    public List<Long> getTeacherIds() {
        return teacherIds;
    }

    public List<String> getTeacherNames() {
        return teacherNames;
    }


    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public void setCourseSem(int courseSem) {
        this.courseSem = courseSem;
    }

    public void setTeacherIds(List<Long> teacherIds) {
        this.teacherIds = teacherIds;
    }

    public void setTeacherNames(List<String> teacherNames) {
        this.teacherNames = teacherNames;
    }
}
