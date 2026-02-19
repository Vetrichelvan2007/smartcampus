package com.example.demo.models;

public class CourseData {

    private String courseName;
    private String courseCode;
    private String courseType;
    private Integer courseSem;

    public CourseData(String courseName, String courseCode, String courseType, Integer courseSem){
        this.courseName=courseName;
        this.courseCode=courseCode;
        this.courseType=courseType;
        this.courseSem=courseSem;
    }
    public String getCourseName(){return courseName;}
    public String getCourseCode(){return courseCode;}
    public String getCourseType(){return courseType;}
    public Integer getCourseSem(){return courseSem;}
}
