package com.vetri.smartcampus.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TeacherController {

    @GetMapping("/teacher-dashboard")
    public String teacerdashboard(){
        return "Teacher/Dashboard";
    }

    @GetMapping("/teacher-mysubjects")
    public String teachermysubjects(){
        return "Teacher/MySubjects";
    }

    @GetMapping("/view-subject")
    public String viewsubject(){
        return "Teacher/ViewSubject";
    }

    @GetMapping("/teacher-profile")
    public String profile(){
        return "Teacher/Profile";
    }
}
