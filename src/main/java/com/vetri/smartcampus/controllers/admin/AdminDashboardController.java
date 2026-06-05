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
            model.addAttribute("totalAllocations", countRecords(con, "course_teacher_allocation"));
            model.addAttribute("pendingPromotions", countPendingPromotions(con));
            model.addAttribute("departments", loadDepartments(con));
            model.addAttribute("batches", loadBatches(con));
            model.addAttribute("promotionStudents", loadPromotionCandidates(con));
            model.addAttribute("studentDirectory", loadStudentDirectory(con));
            model.addAttribute("teacherDirectory", loadTeacherDirectory(con));
            model.addAttribute("courseOfferings", loadCourseOfferings(con));
            model.addAttribute("allocationRows", loadAllocationRows(con));
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
                             @RequestParam Long batchId,
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
                "(roll_number, name, dob, gender, address, dept_id, batch_id, batch_year, current_year, current_semester, email, phone) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String userSql = "INSERT INTO users (email, password, role) VALUES (?, ?, 'STUDENT')";
        String batchSql = "SELECT id, dept_id, YEAR(batch_year) AS batch_year, batch_name FROM batch WHERE id = ?";

        try (Connection con = DataBaseConnection.getConnection()) {
            con.setAutoCommit(false);

            BatchOption batch = loadBatchById(con, batchSql, batchId);
            if (batch == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected batch was not found.");
                return "redirect:/admin-dashboard";
            }
            if (!departmentId.equals(batch.departmentId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected department and batch do not match.");
                return "redirect:/admin-dashboard";
            }

            try (PreparedStatement studentPs = con.prepareStatement(studentSql);
                 PreparedStatement userPs = con.prepareStatement(userSql)) {
                studentPs.setString(1, rollNumber);
                studentPs.setString(2, name);
                studentPs.setDate(3, Date.valueOf(dob));
                studentPs.setString(4, gender);
                studentPs.setString(5, address);
                studentPs.setLong(6, departmentId);
                studentPs.setLong(7, batchId);
                studentPs.setInt(8, batch.batchYear());
                studentPs.setInt(9, currentYear);
                studentPs.setInt(10, currentSemester);
                studentPs.setString(11, email);
                studentPs.setString(12, phone);
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
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to add student. Check the batch, email, and roll number details.");
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

    @PostMapping("/admin/assign-subject")
    public String assignSubject(@RequestParam Long teacherId,
                                @RequestParam Long courseOfferingId,
                                RedirectAttributes redirectAttributes,
                                HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        String teacherSql = "SELECT id, teacher_clg_id, name FROM teacher WHERE id = ?";
        String offeringSql = "SELECT cfd.course_id, cfd.sem, c.course_code, c.course_name, d.dept_name " +
                "FROM course_for_depts cfd " +
                "JOIN course c ON c.id = cfd.course_id " +
                "JOIN department d ON d.id = cfd.dept_id " +
                "WHERE cfd.id = ?";
        String checkSql = "SELECT 1 FROM course_teacher_allocation WHERE teacher_id = ? AND course_id = ? LIMIT 1";
        String insertSql = "INSERT INTO course_teacher_allocation (course_id, teacher_id) VALUES (?, ?)";

        try (Connection con = DataBaseConnection.getConnection()) {
            TeacherDirectoryRow teacher = loadTeacherById(con, teacherSql, teacherId);
            if (teacher == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected teacher was not found.");
                return "redirect:/admin-dashboard";
            }

            CourseOfferingSelection offering = loadCourseOfferingById(con, offeringSql, courseOfferingId);
            if (offering == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected subject offering was not found.");
                return "redirect:/admin-dashboard";
            }

            try (PreparedStatement checkPs = con.prepareStatement(checkSql)) {
                checkPs.setLong(1, teacherId);
                checkPs.setLong(2, offering.courseId());

                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        redirectAttributes.addFlashAttribute(
                                "errorMessage",
                                "This teacher is already assigned to " + offering.courseCode() + " - " + offering.courseName() + "."
                        );
                        return "redirect:/admin-dashboard";
                    }
                }
            }

            try (PreparedStatement insertPs = con.prepareStatement(insertSql)) {
                insertPs.setLong(1, offering.courseId());
                insertPs.setLong(2, teacherId);
                insertPs.executeUpdate();
            }

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    teacher.name() + " assigned to " + offering.courseCode() + " for " + offering.departmentName() +
                            " semester " + offering.semester() + "."
            );
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to assign subject right now.");
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

    private List<BatchOption> loadBatches(Connection con) throws Exception {
        List<BatchOption> batches = new ArrayList<>();
        String sql = "SELECT b.id, b.dept_id, YEAR(b.batch_year) AS batch_year, b.batch_name, d.dept_name " +
                "FROM batch b " +
                "JOIN department d ON d.id = b.dept_id " +
                "ORDER BY d.dept_name, b.batch_year DESC, b.batch_name";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                batches.add(new BatchOption(
                        rs.getLong("id"),
                        rs.getLong("dept_id"),
                        rs.getInt("batch_year"),
                        rs.getString("batch_name"),
                        rs.getString("dept_name")
                ));
            }
        }

        return batches;
    }

    private List<StudentPromotionCandidate> loadPromotionCandidates(Connection con) throws Exception {
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

    private List<StudentDirectoryRow> loadStudentDirectory(Connection con) throws Exception {
        List<StudentDirectoryRow> students = new ArrayList<>();
        String sql = "SELECT s.id, s.roll_number, s.name, s.email, s.phone, s.current_year, s.current_semester, " +
                "d.dept_name, COUNT(DISTINCT sct.course_id) AS registered_subjects " +
                "FROM student s " +
                "JOIN department d ON d.id = s.dept_id " +
                "LEFT JOIN student_course_teacher sct ON sct.student_id = s.id " +
                "GROUP BY s.id, s.roll_number, s.name, s.email, s.phone, s.current_year, s.current_semester, d.dept_name " +
                "ORDER BY s.name";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                students.add(new StudentDirectoryRow(
                        rs.getLong("id"),
                        rs.getString("roll_number"),
                        rs.getString("name"),
                        rs.getString("dept_name"),
                        rs.getInt("current_year"),
                        rs.getInt("current_semester"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getInt("registered_subjects")
                ));
            }
        }

        return students;
    }

    private List<TeacherDirectoryRow> loadTeacherDirectory(Connection con) throws Exception {
        List<TeacherDirectoryRow> teachers = new ArrayList<>();
        String sql = "SELECT t.id, t.teacher_clg_id, t.name, t.email, COUNT(DISTINCT cta.course_id) AS assigned_subjects " +
                "FROM teacher t " +
                "LEFT JOIN course_teacher_allocation cta ON cta.teacher_id = t.id " +
                "GROUP BY t.id, t.teacher_clg_id, t.name, t.email " +
                "ORDER BY t.name";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                teachers.add(new TeacherDirectoryRow(
                        rs.getLong("id"),
                        rs.getString("teacher_clg_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getInt("assigned_subjects")
                ));
            }
        }

        return teachers;
    }

    private List<CourseOfferingOption> loadCourseOfferings(Connection con) throws Exception {
        List<CourseOfferingOption> offerings = new ArrayList<>();
        String sql = "SELECT cfd.id, cfd.course_id, cfd.dept_id, cfd.sem, c.course_code, c.course_name, c.credit, c.course_type, d.dept_name " +
                "FROM course_for_depts cfd " +
                "JOIN course c ON c.id = cfd.course_id " +
                "JOIN department d ON d.id = cfd.dept_id " +
                "ORDER BY d.dept_name, cfd.sem, c.course_name";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                offerings.add(new CourseOfferingOption(
                        rs.getLong("id"),
                        rs.getLong("course_id"),
                        rs.getLong("dept_id"),
                        rs.getInt("sem"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("dept_name"),
                        rs.getInt("credit"),
                        rs.getString("course_type")
                ));
            }
        }

        return offerings;
    }

    private List<AllocationRow> loadAllocationRows(Connection con) throws Exception {
        List<AllocationRow> rows = new ArrayList<>();
        String sql = "SELECT cta.id, t.teacher_clg_id, t.name AS teacher_name, t.email, " +
                "c.course_code, c.course_name, c.credit, c.course_type, d.dept_name, cfd.sem " +
                "FROM course_teacher_allocation cta " +
                "JOIN teacher t ON t.id = cta.teacher_id " +
                "JOIN course c ON c.id = cta.course_id " +
                "JOIN course_for_depts cfd ON cfd.course_id = c.id " +
                "JOIN department d ON d.id = cfd.dept_id " +
                "ORDER BY d.dept_name, cfd.sem, c.course_name, t.name";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new AllocationRow(
                        rs.getLong("id"),
                        rs.getString("teacher_clg_id"),
                        rs.getString("teacher_name"),
                        rs.getString("email"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("dept_name"),
                        rs.getInt("sem"),
                        rs.getInt("credit"),
                        rs.getString("course_type")
                ));
            }
        }

        return rows;
    }

    private TeacherDirectoryRow loadTeacherById(Connection con, String sql, Long teacherId) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return new TeacherDirectoryRow(
                        rs.getLong("id"),
                        rs.getString("teacher_clg_id"),
                        rs.getString("name"),
                        null,
                        0
                );
            }
        }
    }

    private CourseOfferingSelection loadCourseOfferingById(Connection con, String sql, Long courseOfferingId) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, courseOfferingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return new CourseOfferingSelection(
                        rs.getLong("course_id"),
                        rs.getInt("sem"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("dept_name")
                );
            }
        }
    }

    private BatchOption loadBatchById(Connection con, String sql, Long batchId) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, batchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return new BatchOption(
                        rs.getLong("id"),
                        rs.getLong("dept_id"),
                        rs.getInt("batch_year"),
                        rs.getString("batch_name"),
                        null
                );
            }
        }
    }

    public record DepartmentOption(Long id, String name) { }

    public record BatchOption(
            Long id,
            Long departmentId,
            Integer batchYear,
            String batchName,
            String departmentName
    ) { }

    public record StudentPromotionCandidate(
            Long id,
            String rollNumber,
            String name,
            Integer currentYear,
            Integer currentSemester,
            String departmentName
    ) { }

    public record StudentDirectoryRow(
            Long id,
            String rollNumber,
            String name,
            String departmentName,
            Integer currentYear,
            Integer currentSemester,
            String email,
            String phone,
            Integer registeredSubjects
    ) { }

    public record TeacherDirectoryRow(
            Long id,
            String teacherCollegeId,
            String name,
            String email,
            Integer assignedSubjects
    ) { }

    public record CourseOfferingOption(
            Long id,
            Long courseId,
            Long departmentId,
            Integer semester,
            String courseCode,
            String courseName,
            String departmentName,
            Integer credits,
            String courseType
    ) { }

    public record AllocationRow(
            Long id,
            String teacherCollegeId,
            String teacherName,
            String teacherEmail,
            String courseCode,
            String courseName,
            String departmentName,
            Integer semester,
            Integer credits,
            String courseType
    ) { }

    public record CourseOfferingSelection(
            Long courseId,
            Integer semester,
            String courseCode,
            String courseName,
            String departmentName
    ) { }
}
