package com.vetri.smartcampus.controllers.teacher;

import com.vetri.smartcampus.models.teacher.AssignedCourses;
import com.vetri.smartcampus.models.teacher.ViewStudentDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Controller
public class TeacherStudentController extends TeacherControllerSupport {

    @GetMapping("/teacher-view-student")
    public String teacherViewStudentLanding(HttpSession session, Model model) {
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
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }

        return "Teacher/ViewStudentsByCourse";
    }

    @GetMapping("/teacher-view-student/{teacherId}/{courseId}")
    public String teacherViewStudentByCourse(@PathVariable("teacherId") long teacherId,
                                             @PathVariable("courseId") long courseId,
                                             HttpSession session,
                                             Model model) {
        try {
            Long sessionTeacherId = getTeacherId(session);
            if (sessionTeacherId == null) {
                return "redirect:/login";
            }

            if (sessionTeacherId != teacherId) {
                return "redirect:/teacher-view-student/" + sessionTeacherId + "/" + courseId;
            }

            Connection con = openConnection();

            String courseName = null;
            String courseCode = null;
            PreparedStatement psCourse = con.prepareStatement(
                    "SELECT course_name, course_code FROM course WHERE id = ?"
            );
            psCourse.setLong(1, courseId);
            ResultSet rsCourse = psCourse.executeQuery();
            if (rsCourse.next()) {
                courseName = rsCourse.getString("course_name");
                courseCode = rsCourse.getString("course_code");
            }
            rsCourse.close();
            psCourse.close();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT s.name, s.roll_number, s.email, d.dept_name, sct.semester, sct.status " +
                            "FROM student_course_teacher sct " +
                            "JOIN student s ON sct.student_id = s.id " +
                            "JOIN department d ON s.dept_id = d.id " +
                            "JOIN course_for_depts cfd ON cfd.course_id = sct.course_id AND cfd.dept_id = s.dept_id AND cfd.sem = sct.semester " +
                            "WHERE sct.teacher_id = ? " +
                            "AND sct.course_id = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND s.current_semester = sct.semester " +
                            "ORDER BY s.roll_number"
            );

            ps.setLong(1, teacherId);
            ps.setLong(2, courseId);

            ResultSet rs = ps.executeQuery();
            List<ViewStudentDTO> students = new ArrayList<>();

            while (rs.next()) {
                ViewStudentDTO dto = new ViewStudentDTO();
                dto.setStudentName(rs.getString("name"));
                dto.setStudentRegisterNumber(rs.getString("roll_number"));
                dto.setStudentEmail(rs.getString("email"));
                dto.setDepartment(rs.getString("dept_name"));
                dto.setSemester(rs.getInt("semester"));
                dto.setStatus(rs.getString("status"));
                students.add(dto);
            }

            model.addAttribute("teacherId", teacherId);
            model.addAttribute("courseId", courseId);
            model.addAttribute("courseName", courseName);
            model.addAttribute("courseCode", courseCode);
            model.addAttribute("students", students);

            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Teacher/ViewStudent";
    }
}
