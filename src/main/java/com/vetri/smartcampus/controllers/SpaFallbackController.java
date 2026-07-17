package com.vetri.smartcampus.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaFallbackController {

    @GetMapping(value = {
        "/",
        "/login",
        "/student-dashboard",
        "/student-classroom",
        "/student-classroom/{courseCode}",
        "/student-quiz/{quizId}",
        "/student-calendar",
        "/student-courses",
        "/student-feedback",
        "/student-profile",
        "/teacher-dashboard",
        "/teacher-classroom/{courseId}",
        "/teacher-profile",
        "/admin-dashboard"
    })
    public String fallback() {
        return "forward:/index.html";
    }
}
