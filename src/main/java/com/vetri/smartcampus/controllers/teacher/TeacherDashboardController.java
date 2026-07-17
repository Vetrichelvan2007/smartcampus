package com.vetri.smartcampus.controllers.teacher;

import com.vetri.smartcampus.models.teacher.AssignedCourses;
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
public class TeacherDashboardController extends TeacherControllerSupport {

    @GetMapping("/old-teacher-dashboard")
    public String teacherDashboard(HttpSession session, Model model) {
        try {
            Long teacherId = getTeacherId(session);
            if (teacherId == null) {
                return "redirect:/login";
            }

            Connection con = openConnection();
            List<AssignedCourses> courses = loadAssignedCourses(con, teacherId);
            model.addAttribute("courses", courses);
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }

        return "Teacher/Dashboard";
    }

    @GetMapping("/teacher-mysubjects")
    public String teachermysubjects(HttpSession session, Model model) {
        try {
            Long teacherId = getTeacherId(session);
            if (teacherId == null) {
                return "redirect:/login";
            }

            model.addAttribute("teacherId", teacherId);

            Connection con = openConnection();
            List<AssignedCourses> courses = loadAssignedCourses(con, teacherId);
            con.close();

            model.addAttribute("courses", courses);
            model.addAttribute("subjectCount", courses.size());
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }

        return "Teacher/MySubjects";
    }

    @GetMapping("/view-subject/{courseid}")
    public String viewsubject(@PathVariable("courseid") int courseid, Model model, HttpSession session) {
        try {
            Long teacherId = getTeacherId(session);
            if (teacherId == null) {
                return "redirect:/login";
            }

            model.addAttribute("teacherId", teacherId);

            Connection con = openConnection();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT c.id, c.course_name, c.course_code, c.credit, cfd.sem, d.dept_name " +
                            "FROM course c " +
                            "JOIN course_for_depts cfd ON c.id = cfd.course_id " +
                            "JOIN department d ON cfd.dept_id = d.id " +
                            "WHERE c.id = ?"
            );

            ps.setInt(1, courseid);

            ResultSet rs = ps.executeQuery();

            AssignedCourses course = new AssignedCourses();

            if (rs.next()) {
                course.setCourseid(rs.getLong("id"));
                course.setCoursename(rs.getString("course_name"));
                course.setCoursecode(rs.getString("course_code"));
                course.setCredits(rs.getInt("credit"));
                course.setSem(rs.getInt("sem"));
                course.setCoursedept(rs.getString("dept_name"));
            }

            rs.close();
            ps.close();
            con.close();

            model.addAttribute("course", course);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Teacher/ViewSubject";
    }
}
