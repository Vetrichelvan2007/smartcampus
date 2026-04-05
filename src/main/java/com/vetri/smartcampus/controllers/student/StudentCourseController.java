package com.vetri.smartcampus.controllers.student;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

@Controller
public class StudentCourseController extends StudentControllerSupport {

    @GetMapping("/course/{courseCode}")
    public String course(@PathVariable String courseCode, Model model, HttpSession session) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return "redirect:/login";
        }

        model.addAttribute("courseId", courseCode);
        model.addAttribute("resources", List.of());
        model.addAttribute("assignments", List.of());

        try {
            Connection con = openConnection();
            PreparedStatement ps = con.prepareStatement("SELECT id, course_name, course_code FROM course WHERE course_code=?");
            ps.setString(1, courseCode);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                con.close();
                model.addAttribute("courseName", "Course");
                model.addAttribute("quizzes", List.of());
                return "Student/Course";
            }

            long courseId = rs.getLong("id");
            model.addAttribute("courseName", rs.getString("course_name"));
            model.addAttribute("courseDbId", courseId);
            rs.close();
            ps.close();

            model.addAttribute("resources", loadMaterialsForCourse(con, studentId, courseId));
            model.addAttribute("assignments", loadAssignmentsForCourse(con, studentId, courseId));
            model.addAttribute("quizzes", loadQuizzesForCourse(con, studentId, courseId));

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("quizzes", List.of());
        }

        return "Student/Course";
    }

    @GetMapping("/course/{id}/quizzes")
    public String quizzes(@PathVariable("id") long courseId, HttpSession session, Model model) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return "redirect:/login";
        }

        model.addAttribute("courseId", String.valueOf(courseId));
        model.addAttribute("activeTab", "quizzes");
        model.addAttribute("resources", List.of());
        model.addAttribute("assignments", List.of());

        try {
            Connection con = openConnection();

            PreparedStatement psCourse = con.prepareStatement("SELECT course_name, course_code FROM course WHERE id = ?");
            psCourse.setLong(1, courseId);
            ResultSet rsCourse = psCourse.executeQuery();
            if (rsCourse.next()) {
                model.addAttribute("courseName", rsCourse.getString("course_name"));
                model.addAttribute("courseCode", rsCourse.getString("course_code"));
            } else {
                model.addAttribute("courseName", "Course");
            }
            rsCourse.close();
            psCourse.close();

            model.addAttribute("resources", loadMaterialsForCourse(con, studentId, courseId));
            model.addAttribute("assignments", loadAssignmentsForCourse(con, studentId, courseId));
            model.addAttribute("quizzes", loadQuizzesForCourse(con, studentId, courseId));

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("quizzes", List.of());
        }

        return "Student/Course";
    }
}
