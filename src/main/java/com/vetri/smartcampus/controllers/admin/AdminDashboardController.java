package com.vetri.smartcampus.controllers.admin;

import com.vetri.smartcampus.models.common.DataBaseConnection;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AdminDashboardController {

    @GetMapping({"/admin", "/admin-dashboard"})
    public String adminDashboard(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            model.addAttribute("totalStudents", countRecords(con, "student"));
            model.addAttribute("totalTeachers", countRecords(con, "teacher"));
            model.addAttribute("pendingPromotions", countPendingPromotions(con));
            model.addAttribute("departments", loadDepartments(con));
            model.addAttribute("students", loadStudents(con));
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Unable to load admin dashboard data.");
        }

        return "Admin/Dashboard";
    }

    @PostMapping("/admin/add-student")
    public String addStudent(@RequestParam String rollNumber,
                             @RequestParam String name,
                             @RequestParam String dob,
                             @RequestParam String gender,
                             @RequestParam Long departmentId,
                             @RequestParam Integer currentYear,
                             @RequestParam Integer currentSemester,
                             @RequestParam String email,
                             @RequestParam String phone,
                             @RequestParam String address,
                             @RequestParam String password,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String studentSql = "INSERT INTO student " +
                "(roll_number, name, dob, gender, address, dept_id, current_year, current_semester, email, phone) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String userSql = "INSERT INTO users (email, password, role) VALUES (?, ?, 'STUDENT')";

        try (Connection con = DataBaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement studentPs = con.prepareStatement(studentSql);
                 PreparedStatement userPs = con.prepareStatement(userSql)) {
                studentPs.setString(1, rollNumber);
                studentPs.setString(2, name);
                studentPs.setDate(3, Date.valueOf(dob));
                studentPs.setString(4, gender);
                studentPs.setString(5, address);
                studentPs.setLong(6, departmentId);
                studentPs.setInt(7, currentYear);
                studentPs.setInt(8, currentSemester);
                studentPs.setString(9, email);
                studentPs.setString(10, phone);
                studentPs.executeUpdate();

                userPs.setString(1, email);
                userPs.setString(2, password);
                userPs.executeUpdate();

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Student added successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to add student. Check for duplicate email or roll number.");
        }

        return "redirect:/admin-dashboard";
    }

    @PostMapping("/admin/add-teacher")
    public String addTeacher(@RequestParam String teacherCollegeId,
                             @RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String password,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String teacherSql = "INSERT INTO teacher (teacher_clg_id, name, email) VALUES (?, ?, ?)";
        String userSql = "INSERT INTO users (email, password, role) VALUES (?, ?, 'TEACHER')";

        try (Connection con = DataBaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement teacherPs = con.prepareStatement(teacherSql);
                 PreparedStatement userPs = con.prepareStatement(userSql)) {
                teacherPs.setString(1, teacherCollegeId);
                teacherPs.setString(2, name);
                teacherPs.setString(3, email);
                teacherPs.executeUpdate();

                userPs.setString(1, email);
                userPs.setString(2, password);
                userPs.executeUpdate();

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Teacher added successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to add teacher. Check for duplicate college ID or email.");
        }

        return "redirect:/admin-dashboard";
    }

    @PostMapping("/admin/promote-student")
    public String promoteStudent(@RequestParam Long studentId,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String loadSql = "SELECT name, current_year, current_semester FROM student WHERE id = ?";
        String updateSql = "UPDATE student SET current_year = ?, current_semester = ? WHERE id = ?";

        try (Connection con = DataBaseConnection.getConnection();
             PreparedStatement loadPs = con.prepareStatement(loadSql)) {
            loadPs.setLong(1, studentId);

            try (ResultSet rs = loadPs.executeQuery()) {
                if (!rs.next()) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Student not found.");
                    return "redirect:/admin-dashboard";
                }

                String studentName = rs.getString("name");
                int currentYear = rs.getInt("current_year");
                int currentSemester = rs.getInt("current_semester");

                if (currentSemester >= 8) {
                    redirectAttributes.addFlashAttribute("errorMessage", studentName + " has already reached the final semester.");
                    return "redirect:/admin-dashboard";
                }

                int nextSemester = currentSemester + 1;
                int nextYear = currentYear + (currentSemester % 2 == 0 ? 1 : 0);
                if (nextYear > 4) {
                    nextYear = 4;
                }

                try (PreparedStatement updatePs = con.prepareStatement(updateSql)) {
                    updatePs.setInt(1, nextYear);
                    updatePs.setInt(2, nextSemester);
                    updatePs.setLong(3, studentId);
                    updatePs.executeUpdate();
                }

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        studentName + " promoted to year " + nextYear + ", semester " + nextSemester + "."
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to promote student right now.");
        }

        return "redirect:/admin-dashboard";
    }

    private boolean isAdmin(HttpSession session) {
        Object role = session.getAttribute("role");
        return role != null && "admin".equalsIgnoreCase(role.toString());
    }

    private int countRecords(Connection con, String tableName) throws Exception {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM " + tableName);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int countPendingPromotions(Connection con) throws Exception {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM student WHERE current_semester < 8");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private List<DepartmentOption> loadDepartments(Connection con) throws Exception {
        List<DepartmentOption> departments = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement("SELECT id, dept_name FROM department ORDER BY dept_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                departments.add(new DepartmentOption(
                        rs.getLong("id"),
                        rs.getString("dept_name")
                ));
            }
        }

        return departments;
    }

    private List<StudentPromotionCandidate> loadStudents(Connection con) throws Exception {
        List<StudentPromotionCandidate> students = new ArrayList<>();
        String sql = "SELECT s.id, s.roll_number, s.name, s.current_year, s.current_semester, d.dept_name " +
                "FROM student s " +
                "JOIN department d ON d.id = s.dept_id " +
                "ORDER BY s.current_year, s.current_semester, s.name";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                students.add(new StudentPromotionCandidate(
                        rs.getLong("id"),
                        rs.getString("roll_number"),
                        rs.getString("name"),
                        rs.getInt("current_year"),
                        rs.getInt("current_semester"),
                        rs.getString("dept_name")
                ));
            }
        }

        return students;
    }

    public record DepartmentOption(Long id, String name) { }

    public record StudentPromotionCandidate(
            Long id,
            String rollNumber,
            String name,
            Integer currentYear,
            Integer currentSemester,
            String departmentName
    ) { }
}
