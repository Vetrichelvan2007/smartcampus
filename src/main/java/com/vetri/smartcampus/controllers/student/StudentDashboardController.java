package com.vetri.smartcampus.controllers.student;

import com.vetri.smartcampus.models.student.CourseData;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Controller
public class StudentDashboardController extends StudentControllerSupport {

    @GetMapping("/old-student-dashboard")
    public String studentDashboard(HttpSession session) {
        return getStudentId(session) == null ? "redirect:/login" : "Student/StudentDashboard";
    }

    @GetMapping("/old-student-classroom")
    public String classroom(Model model, HttpSession session) {
        List<CourseData> courseDatas = new ArrayList<>();

        Object deptIdObj = session.getAttribute("department_id");
        Long studentId = getStudentId(session);
        if (deptIdObj == null || studentId == null) {
            return "redirect:/login";
        }

        int deptId = Integer.parseInt(deptIdObj.toString());
        int currentSem = 0;

        try {
            Connection con1 = openConnection();
            PreparedStatement ps1 = con1.prepareStatement("SELECT current_semester FROM student WHERE id=?");
            ps1.setLong(1, studentId);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                currentSem = rs1.getInt("current_semester");
            }
            rs1.close();
            ps1.close();
            con1.close();

            Connection con2 = openConnection();
            PreparedStatement ps2 = con2.prepareStatement(
                    "SELECT d.dept_name, cd.sem, c.course_name, c.course_type, c.course_code " +
                            "FROM course_for_depts cd " +
                            "JOIN course c ON c.id = cd.course_id " +
                            "JOIN department d ON d.id = cd.dept_id " +
                            "WHERE cd.dept_id = ? AND cd.sem <= ? ORDER BY cd.sem"
            );
            ps2.setInt(1, deptId);
            ps2.setInt(2, currentSem);
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next()) {
                courseDatas.add(new CourseData(
                        rs2.getString("course_name"),
                        rs2.getString("course_code"),
                        rs2.getString("course_type"),
                        rs2.getInt("sem")
                ));
            }
            model.addAttribute("courseDatas", courseDatas);
            model.addAttribute("current_sem", currentSem);
            rs2.close();
            ps2.close();
            con2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Student/Classroom";
    }

    @GetMapping("/online-exam")
    public String onlineExam() {
        return "Student/OnlineExam";
    }

    @GetMapping("/calendar")
    public String calendar() {
        return "Student/Calendar";
    }

    @GetMapping("/drive")
    public String drive() {
        return "Student/Drive";
    }
}
