package com.vetri.smartcampus.controllers.student;

import com.vetri.smartcampus.models.student.CourseDTO;
import com.vetri.smartcampus.models.student.CourseRegistration;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class StudentRegistrationController extends StudentControllerSupport {

    @GetMapping("/old-courseregistration")
    public String courseRegistration(HttpSession session, Model model) {
        try {
            Long studentId = getStudentId(session);
            if (studentId == null) {
                return "redirect:/login";
            }

            Connection con = openConnection();
            PreparedStatement ps1 = con.prepareStatement("SELECT dept_id, current_semester FROM student WHERE id=?");
            ps1.setLong(1, studentId);
            ResultSet rs1 = ps1.executeQuery();

            Long deptId = null;
            int currentSemester = 1;
            if (rs1.next()) {
                deptId = rs1.getLong("dept_id");
                currentSemester = rs1.getInt("current_semester");
            } else {
                rs1.close();
                ps1.close();
                con.close();
                return "redirect:/login";
            }
            rs1.close();
            ps1.close();

            PreparedStatement ps0 = con.prepareStatement(
                    "SELECT student_id FROM student_course_teacher WHERE student_id = ? AND semester = ?"
            );
            ps0.setLong(1, studentId);
            ps0.setInt(2, currentSemester);
            ResultSet rs0 = ps0.executeQuery();
            if (rs0.next()) {
                rs0.close();
                ps0.close();
                con.close();
                return "redirect:/registered-course";
            }
            rs0.close();
            ps0.close();

            model.addAttribute("current_semester", currentSemester);

            PreparedStatement ps2 = con.prepareStatement(
                    "SELECT c.id, c.course_code, c.course_name, c.course_type, cfd.sem, " +
                            "t.id, t.name " +
                            "FROM course_for_depts cfd " +
                            "JOIN course c ON c.id = cfd.course_id " +
                            "LEFT JOIN course_teacher_allocation cta ON c.id = cta.course_id " +
                            "LEFT JOIN teacher t ON cta.teacher_id = t.id " +
                            "WHERE cfd.dept_id = ? AND cfd.sem = ? " +
                            "ORDER BY c.id"
            );
            ps2.setLong(1, deptId);
            ps2.setInt(2, currentSemester);
            ResultSet rs2 = ps2.executeQuery();

            Map<Long, CourseRegistration> courseMap = new LinkedHashMap<>();
            while (rs2.next()) {
                Long courseId = rs2.getLong(1);
                CourseRegistration course = courseMap.get(courseId);
                if (course == null) {
                    course = new CourseRegistration(courseId, rs2.getString(3), rs2.getString(4));
                    course.setCourseCode(rs2.getString(2));
                    course.setCourseSem(rs2.getInt(5));
                    course.setTeacherIds(new ArrayList<>());
                    course.setTeacherNames(new ArrayList<>());
                    courseMap.put(courseId, course);
                }

                Long teacherId = (Long) rs2.getObject(6);
                String teacherName = rs2.getString(7);
                if (teacherId != null && teacherName != null) {
                    course.getTeacherIds().add(teacherId);
                    course.getTeacherNames().add(teacherName);
                }
            }
            rs2.close();
            ps2.close();
            con.close();

            model.addAttribute("courses", new ArrayList<>(courseMap.values()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Student/CourseRegistration";
    }

    @PostMapping("/old-courseregistraction-submit")
    public String handleCourseRegistration(@RequestBody List<CourseDTO> selectedCourses, HttpSession session) {
        try {
            Long studentId = getStudentId(session);
            if (studentId == null) return "redirect:/login";

            Connection con = openConnection();
            con.setAutoCommit(false);

            PreparedStatement getStudent = con.prepareStatement("SELECT current_semester FROM student WHERE id = ?");
            getStudent.setLong(1, studentId);
            ResultSet rs = getStudent.executeQuery();
            if (!rs.next()) {
                rs.close();
                getStudent.close();
                con.close();
                return "redirect:/login";
            }
            int currentSem = rs.getInt("current_semester");
            rs.close();
            getStudent.close();

            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO student_course_teacher (student_id, course_id, teacher_id, semester, status) VALUES (?, ?, ?, ?, ?)"
            );
            for (CourseDTO dto : selectedCourses) {
                ps.setLong(1, studentId);
                ps.setLong(2, dto.getCourseId());
                ps.setLong(3, dto.getTeacherId());
                ps.setInt(4, currentSem);
                ps.setString(5, "ACTIVE");
                ps.addBatch();
            }

            ps.executeBatch();
            ps.close();
            con.commit();
            con.close();
            return "redirect:/registered-course";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/courseregistration";
        }
    }

    @GetMapping("/old-registered-course")
    public String registeredCourse(HttpSession session, Model model) {
        try {
            Long studentId = getStudentId(session);
            if (studentId == null) {
                return "redirect:/login";
            }

            Connection con = openConnection();
            PreparedStatement ps1 = con.prepareStatement("SELECT current_semester FROM student WHERE id = ?");
            ps1.setLong(1, studentId);
            ResultSet rs1 = ps1.executeQuery();

            int currentSemester = 1;
            if (rs1.next()) {
                currentSemester = rs1.getInt("current_semester");
            } else {
                rs1.close();
                ps1.close();
                con.close();
                return "redirect:/login";
            }
            rs1.close();
            ps1.close();
            model.addAttribute("current_semester", currentSemester);

            PreparedStatement ps2 = con.prepareStatement(
                    "SELECT c.id, c.course_code, c.course_name, c.course_type, sct.semester, " +
                            "t.id, t.name " +
                            "FROM student_course_teacher sct " +
                            "JOIN course c ON sct.course_id = c.id " +
                            "LEFT JOIN teacher t ON sct.teacher_id = t.id " +
                            "WHERE sct.student_id = ? AND sct.semester = ? " +
                            "ORDER BY c.id"
            );
            ps2.setLong(1, studentId);
            ps2.setInt(2, currentSemester);
            ResultSet rs2 = ps2.executeQuery();

            Map<Long, CourseRegistration> courseMap = new LinkedHashMap<>();
            while (rs2.next()) {
                Long courseId = rs2.getLong(1);
                CourseRegistration course = courseMap.get(courseId);
                if (course == null) {
                    course = new CourseRegistration(courseId, rs2.getString(3), rs2.getString(4));
                    course.setCourseCode(rs2.getString(2));
                    course.setCourseSem(rs2.getInt(5));
                    course.setTeacherIds(new ArrayList<>());
                    course.setTeacherNames(new ArrayList<>());
                    courseMap.put(courseId, course);
                }

                Long teacherId = (Long) rs2.getObject(6);
                String teacherName = rs2.getString(7);
                if (teacherId != null && teacherName != null) {
                    course.getTeacherIds().add(teacherId);
                    course.getTeacherNames().add(teacherName);
                }
            }
            rs2.close();
            ps2.close();
            con.close();

            model.addAttribute("courses", new ArrayList<>(courseMap.values()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Student/RegisteredCourse";
    }
}
