package com.vetri.smartcampus.controllers;

import com.vetri.smartcampus.models.AssignedCourses;
import com.vetri.smartcampus.models.AssignmentStudentStatusDTO;
import com.vetri.smartcampus.models.CourseAssignmentDTO;
import com.vetri.smartcampus.models.CourseMaterialDTO;
import com.vetri.smartcampus.models.DataBaseConnection;
import com.vetri.smartcampus.models.FeedbackFormDTO;
import com.vetri.smartcampus.models.FeedbackQuestionDTO;
import com.vetri.smartcampus.models.FeedbackStudentStatusDTO;
import com.vetri.smartcampus.models.QuizDTO;
import com.vetri.smartcampus.models.QuizQuestionDTO;
import com.vetri.smartcampus.models.QuizStudentStatusDTO;
import com.vetri.smartcampus.models.TeacherDTO;
import com.vetri.smartcampus.models.TeacherQualificationDTO;
import com.vetri.smartcampus.models.ViewStudentDTO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Controller
public class TeacherController {

    private static final DateTimeFormatter UI_DTF = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a", Locale.ENGLISH);

    private static String fmt(LocalDateTime dt) {
        return dt == null ? "-" : dt.format(UI_DTF);
    }

    private static final int ASSIGNMENT_TEXT_WORD_LIMIT = 500;


    private static int parseLeadingInt(String raw, int defaultValue) {
        if (raw == null) return defaultValue;
        Matcher m = Pattern.compile("(\\d+)").matcher(raw);
        if (!m.find()) return defaultValue;
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static List<String> splitNonEmptyLines(String raw) {
        if (raw == null) return List.of();
        String[] parts = raw.split("\\r?\\n");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static int countWords(String raw) {
        if (raw == null) return 0;
        String text = raw.trim();
        if (text.isEmpty()) return 0;
        return text.split("\\s+").length;
    }

    private static boolean feedbackTablesExist(Connection con) throws Exception {
        PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) AS cnt " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() " +
                        "AND table_name IN ('feedback_form','feedback_question','feedback_submission','feedback_answer')"
        );
        ResultSet rs = ps.executeQuery();
        int cnt = 0;
        if (rs.next()) cnt = rs.getInt("cnt");
        rs.close();
        ps.close();
        return cnt == 4;
    }

    private static boolean materialTablesExist(Connection con) throws Exception {
        PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) AS cnt " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() " +
                        "AND table_name = 'course_material'"
        );
        ResultSet rs = ps.executeQuery();
        int cnt = 0;
        if (rs.next()) cnt = rs.getInt("cnt");
        rs.close();
        ps.close();
        return cnt == 1;
    }

    private static boolean assignmentTablesExist(Connection con) throws Exception {
        PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) AS cnt " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() " +
                        "AND table_name = 'course_assignment'"
        );
        ResultSet rs = ps.executeQuery();
        int cnt = 0;
        if (rs.next()) cnt = rs.getInt("cnt");
        rs.close();
        ps.close();
        return cnt == 1;
    }

    private static boolean quizTablesExist(Connection con) throws Exception {
        PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) AS cnt " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() " +
                        "AND table_name IN ('quiz','quiz_question','quiz_option','quiz_submission','quiz_answer')"
        );
        ResultSet rs = ps.executeQuery();
        int cnt = 0;
        if (rs.next()) cnt = rs.getInt("cnt");
        rs.close();
        ps.close();
        return cnt == 5;
    }

    @GetMapping("/teacher-dashboard")
    public String teacherDashboard(HttpSession session, Model model){

        try{

            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }

            Long teacherId = Long.parseLong(tid.toString());

            Connection con = DataBaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT DISTINCT\n" +
                            "c.id as course_id,\n"+
                            "c.course_name,\n" +
                            "c.course_code,\n" +
                            "d.dept_name,\n" +
                            "cfd.sem,\n" +
                            "c.credit\n" +
                            "FROM course_teacher_allocation cta\n" +
                            "JOIN course c ON c.id = cta.course_id\n" +
                            "JOIN course_for_depts cfd ON cfd.course_id = c.id\n" +
                            "JOIN department d ON d.id = cfd.dept_id\n" +
                            "WHERE cta.teacher_id = ?"
            );

            ps.setLong(1, teacherId);

            ResultSet rs = ps.executeQuery();

            List<AssignedCourses> courses = new ArrayList<>();

            while(rs.next()){

                AssignedCourses ac = new AssignedCourses();
                ac.setCourseid(rs.getLong("course_id"));
                ac.setCoursename(rs.getString("course_name"));
                ac.setCoursecode(rs.getString("course_code"));
                ac.setCoursedept(rs.getString("dept_name"));
                ac.setSem(rs.getInt("sem"));
                ac.setCredits(rs.getInt("credit"));

                courses.add(ac);
            }

            model.addAttribute("courses", courses);

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }

        return "Teacher/Dashboard";
    }

    @GetMapping("/teacher-view-student")
    public String teacherViewStudentLanding(HttpSession session, Model model) {
        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }

            Long teacherId = Long.parseLong(tid.toString());
            model.addAttribute("teacherId", teacherId);

            Connection con = DataBaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT DISTINCT " +
                            "c.id as course_id, " +
                            "c.course_name, " +
                            "c.course_code, " +
                            "d.dept_name, " +
                            "cfd.sem, " +
                            "c.credit " +
                            "FROM course_teacher_allocation cta " +
                            "JOIN course c ON c.id = cta.course_id " +
                            "JOIN course_for_depts cfd ON cfd.course_id = c.id " +
                            "JOIN department d ON d.id = cfd.dept_id " +
                            "WHERE cta.teacher_id = ?"
            );

            ps.setLong(1, teacherId);

            ResultSet rs = ps.executeQuery();
            List<AssignedCourses> courses = new ArrayList<>();

            while (rs.next()) {
                AssignedCourses ac = new AssignedCourses();
                ac.setCourseid(rs.getLong("course_id"));
                ac.setCoursename(rs.getString("course_name"));
                ac.setCoursecode(rs.getString("course_code"));
                ac.setCoursedept(rs.getString("dept_name"));
                ac.setSem(rs.getInt("sem"));
                ac.setCredits(rs.getInt("credit"));
                courses.add(ac);
            }

            model.addAttribute("courses", courses);

            rs.close();
            ps.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }

        return "Teacher/ViewStudentsByCourse";
    }

    @GetMapping("/teacher-mysubjects")
    public String teachermysubjects(HttpSession session, Model model){
        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }

            long teacherId = Long.parseLong(tid.toString());
            model.addAttribute("teacherId", teacherId);

            Connection con = DataBaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                    "SELECT DISTINCT " +
                            "c.id AS course_id, " +
                            "c.course_name, " +
                            "c.course_code, " +
                            "d.dept_name, " +
                            "cfd.sem, " +
                            "c.credit " +
                            "FROM course_teacher_allocation cta " +
                            "JOIN course c ON c.id = cta.course_id " +
                            "JOIN course_for_depts cfd ON cfd.course_id = c.id " +
                            "JOIN department d ON d.id = cfd.dept_id " +
                            "WHERE cta.teacher_id = ?"
            );
            ps.setLong(1, teacherId);

            ResultSet rs = ps.executeQuery();
            List<AssignedCourses> courses = new ArrayList<>();

            while (rs.next()) {
                AssignedCourses course = new AssignedCourses();
                course.setCourseid(rs.getLong("course_id"));
                course.setCoursename(rs.getString("course_name"));
                course.setCoursecode(rs.getString("course_code"));
                course.setCoursedept(rs.getString("dept_name"));
                course.setSem(rs.getInt("sem"));
                course.setCredits(rs.getInt("credit"));
                courses.add(course);
            }

            rs.close();
            ps.close();
            con.close();

            model.addAttribute("courses", courses);
            model.addAttribute("subjectCount", courses.size());
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }

        return "Teacher/MySubjects";
    }

    @GetMapping("/teacher-create-quiz")
    public String teacherCreateQuiz() {
        return "redirect:/teacher-assign-quiz";
    }

    @GetMapping("/teacher-view-student/{teacherId}/{courseId}")
    public String teacherViewStudentByCourse(@PathVariable("teacherId") long teacherId,
                                             @PathVariable("courseId") long courseId,
                                             HttpSession session,
                                             Model model) {
        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }

            long sessionTeacherId = Long.parseLong(tid.toString());
            if (sessionTeacherId != teacherId) {
                // Don’t allow teachers to view other teachers’ students by URL tampering.
                return "redirect:/teacher-view-student/" + sessionTeacherId + "/" + courseId;
            }

            Connection con = DataBaseConnection.getConnection();

            // Course meta (for header context in the page)
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

    @GetMapping("/teacher-upload-material")
    public String teacherUploadMaterial(HttpSession session,
                                       @RequestParam(value = "courseId", required = false) Long courseId,
                                       Model model) {
        Object tid = session.getAttribute("teacherId");
        if (tid == null) {
            return "redirect:/login";
        }
        long teacherId = Long.parseLong(tid.toString());
        model.addAttribute("teacherId", teacherId);

        try {
            Connection con = DataBaseConnection.getConnection();

            PreparedStatement psCourses = con.prepareStatement(
                    "SELECT DISTINCT " +
                            "c.id as course_id, " +
                            "c.course_name, " +
                            "c.course_code, " +
                            "d.dept_name, " +
                            "cfd.sem, " +
                            "c.credit " +
                            "FROM course_teacher_allocation cta " +
                            "JOIN course c ON c.id = cta.course_id " +
                            "JOIN course_for_depts cfd ON cfd.course_id = c.id " +
                            "JOIN department d ON d.id = cfd.dept_id " +
                            "WHERE cta.teacher_id = ? " +
                            "ORDER BY c.course_name"
            );
            psCourses.setLong(1, teacherId);
            ResultSet rsCourses = psCourses.executeQuery();
            List<AssignedCourses> courses = new ArrayList<>();
            while (rsCourses.next()) {
                AssignedCourses ac = new AssignedCourses();
                ac.setCourseid(rsCourses.getLong("course_id"));
                ac.setCoursename(rsCourses.getString("course_name"));
                ac.setCoursecode(rsCourses.getString("course_code"));
                ac.setCoursedept(rsCourses.getString("dept_name"));
                ac.setSem(rsCourses.getInt("sem"));
                ac.setCredits(rsCourses.getInt("credit"));
                courses.add(ac);
            }
            rsCourses.close();
            psCourses.close();
            model.addAttribute("courses", courses);

            if (courseId != null) {
                PreparedStatement psAlloc = con.prepareStatement(
                        "SELECT 1 FROM course_teacher_allocation WHERE teacher_id = ? AND course_id = ? LIMIT 1"
                );
                psAlloc.setLong(1, teacherId);
                psAlloc.setLong(2, courseId);
                ResultSet rsAlloc = psAlloc.executeQuery();
                boolean allowed = rsAlloc.next();
                rsAlloc.close();
                psAlloc.close();
                if (!allowed) {
                    con.close();
                    return "redirect:/teacher-upload-material";
                }

                AssignedCourses selected = null;
                for (AssignedCourses c : courses) {
                    if (c.getCourseid() == courseId) {
                        selected = c;
                        break;
                    }
                }
                model.addAttribute("selectedCourse", selected);

                boolean ready = materialTablesExist(con);
                model.addAttribute("materialDbMissing", !ready);
                if (ready) {
                    PreparedStatement psM = con.prepareStatement(
                            "SELECT id, title, module, material_type, original_filename, file_size, download_allowed, created_at " +
                                    "FROM course_material WHERE teacher_id = ? AND course_id = ? " +
                                    "ORDER BY created_at DESC"
                    );
                    psM.setLong(1, teacherId);
                    psM.setLong(2, courseId);
                    ResultSet rsM = psM.executeQuery();
                    List<CourseMaterialDTO> materials = new ArrayList<>();
                    while (rsM.next()) {
                        CourseMaterialDTO m = new CourseMaterialDTO();
                        m.setId(rsM.getLong("id"));
                        m.setTitle(rsM.getString("title"));
                        m.setModule(rsM.getString("module"));
                        m.setType(rsM.getString("material_type"));
                        m.setOriginalFileName(rsM.getString("original_filename"));
                        long sz = rsM.getLong("file_size");
                        m.setFileSize(rsM.wasNull() ? null : sz);
                        m.setDownloadAllowed(rsM.getInt("download_allowed") == 1);
                        Timestamp up = rsM.getTimestamp("created_at");
                        m.setUploadedAt(up);
                        m.setUploadedAtText(up == null ? "-" : fmt(up.toLocalDateTime()));
                        materials.add(m);
                    }
                    rsM.close();
                    psM.close();
                    model.addAttribute("materials", materials);
                }
            }

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Teacher/UploadMaterial";
    }

    @PostMapping("/teacher-upload-material")
    public String teacherUploadMaterialSubmit(HttpSession session,
                                             @RequestParam("courseId") long courseId,
                                             @RequestParam("materialTitle") String materialTitle,
                                             @RequestParam(value = "module", required = false) String module,
                                             @RequestParam("materialType") String materialType,
                                             @RequestParam(value = "description", required = false) String description,
                                             @RequestParam(value = "publishDate", required = false) String publishDate,
                                             @RequestParam(value = "access", required = false) String[] access,
                                             @RequestParam("materialFile") MultipartFile materialFile) {
        Object tid = session.getAttribute("teacherId");
        if (tid == null) {
            return "redirect:/login";
        }
        long teacherId = Long.parseLong(tid.toString());

        if (materialTitle == null || materialTitle.trim().isEmpty() || materialFile == null || materialFile.isEmpty()) {
            return "redirect:/teacher-upload-material?courseId=" + courseId;
        }

        boolean downloadAllowed = true;
        if (access != null) {
            downloadAllowed = false;
            for (String a : access) {
                if (a != null && a.equalsIgnoreCase("DOWNLOAD")) {
                    downloadAllowed = true;
                    break;
                }
            }
        }

        Timestamp publishAt = null;
        try {
            if (publishDate != null && !publishDate.trim().isEmpty()) {
                publishAt = Timestamp.valueOf(LocalDate.parse(publishDate.trim()).atStartOfDay());
            }
        } catch (Exception ignored) {
        }

        try {
            Connection con = DataBaseConnection.getConnection();

            PreparedStatement psAlloc = con.prepareStatement(
                    "SELECT 1 FROM course_teacher_allocation WHERE teacher_id = ? AND course_id = ? LIMIT 1"
            );
            psAlloc.setLong(1, teacherId);
            psAlloc.setLong(2, courseId);
            ResultSet rsAlloc = psAlloc.executeQuery();
            boolean allowed = rsAlloc.next();
            rsAlloc.close();
            psAlloc.close();
            if (!allowed) {
                con.close();
                return "redirect:/teacher-upload-material";
            }

            if (!materialTablesExist(con)) {
                con.close();
                return "redirect:/teacher-upload-material?courseId=" + courseId;
            }

            Path base = Paths.get("material").toAbsolutePath().normalize();
            Files.createDirectories(base);

            String original = materialFile.getOriginalFilename();
            if (original == null || original.isBlank()) original = "material";
            String safe = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String stored = java.util.UUID.randomUUID() + "_" + safe;
            Path dest = base.resolve(stored).normalize();
            if (!dest.startsWith(base)) {
                con.close();
                return "redirect:/teacher-upload-material?courseId=" + courseId;
            }
            Files.copy(materialFile.getInputStream(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String storedPath = dest.toString();
            String mime = materialFile.getContentType();
            long size = materialFile.getSize();

            PreparedStatement psIns = con.prepareStatement(
                    "INSERT INTO course_material (course_id, teacher_id, title, module, material_type, description, original_filename, stored_filename, stored_path, mime_type, file_size, publish_at, expiry_at, download_allowed) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            );
            psIns.setLong(1, courseId);
            psIns.setLong(2, teacherId);
            psIns.setString(3, materialTitle.trim());
            psIns.setString(4, (module == null || module.trim().isEmpty()) ? null : module.trim());
            psIns.setString(5, materialType);
            psIns.setString(6, description);
            psIns.setString(7, original);
            psIns.setString(8, stored);
            psIns.setString(9, storedPath);
            psIns.setString(10, mime);
            psIns.setLong(11, size);
            psIns.setTimestamp(12, publishAt);
            psIns.setTimestamp(13, null);
            psIns.setInt(14, downloadAllowed ? 1 : 0);
            psIns.executeUpdate();
            psIns.close();

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/teacher-upload-material?courseId=" + courseId;
    }

    @GetMapping("/teacher/material/{id}/download")
    public void teacherDownloadMaterial(@PathVariable("id") long materialId, HttpSession session, HttpServletResponse response) {
        Object tid = session.getAttribute("teacherId");
        if (tid == null) {
            try { response.sendRedirect("/login"); } catch (Exception ignored) {}
            return;
        }
        long teacherId = Long.parseLong(tid.toString());

        try {
            Connection con = DataBaseConnection.getConnection();
            if (!materialTablesExist(con)) {
                con.close();
                response.sendError(404);
                return;
            }

            PreparedStatement ps = con.prepareStatement(
                    "SELECT stored_path, original_filename, mime_type " +
                            "FROM course_material WHERE id = ? AND teacher_id = ?"
            );
            ps.setLong(1, materialId);
            ps.setLong(2, teacherId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                con.close();
                response.sendError(404);
                return;
            }
            String storedPath = rs.getString("stored_path");
            String original = rs.getString("original_filename");
            String mime = rs.getString("mime_type");
            rs.close();
            ps.close();
            con.close();

            if (storedPath == null || storedPath.contains("..")) {
                response.sendError(404);
                return;
            }
            Path base = Paths.get("material").toAbsolutePath().normalize();
            Path file = Paths.get(storedPath).toAbsolutePath().normalize();
            if (!file.startsWith(base) || !Files.exists(file)) {
                response.sendError(404);
                return;
            }

            response.setContentType((mime == null || mime.isBlank()) ? "application/octet-stream" : mime);
            String safe = (original == null || original.isBlank()) ? ("material_" + materialId) : original.replace("\"", "");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + safe + "\"");
            Files.copy(file, response.getOutputStream());
        } catch (Exception e) {
            try { response.sendError(500); } catch (Exception ignored) {}
        }
    }

    @GetMapping("/teacher-upload-assignment")
    public String teacherUploadAssignment(HttpSession session,
                                          @RequestParam(value = "courseId", required = false) Long courseId,
                                          Model model) {
        Object tid = session.getAttribute("teacherId");
        if (tid == null) return "redirect:/login";
        long teacherId = Long.parseLong(tid.toString());
        model.addAttribute("teacherId", teacherId);

        try {
            Connection con = DataBaseConnection.getConnection();
            PreparedStatement psCourses = con.prepareStatement(
                    "SELECT DISTINCT c.id as course_id, c.course_name, c.course_code, d.dept_name, cfd.sem, c.credit " +
                            "FROM course_teacher_allocation cta " +
                            "JOIN course c ON c.id = cta.course_id " +
                            "JOIN course_for_depts cfd ON cfd.course_id = c.id " +
                            "JOIN department d ON d.id = cfd.dept_id " +
                            "WHERE cta.teacher_id = ? ORDER BY c.course_name"
            );
            psCourses.setLong(1, teacherId);
            ResultSet rsCourses = psCourses.executeQuery();
            List<AssignedCourses> courses = new ArrayList<>();
            while (rsCourses.next()) {
                AssignedCourses ac = new AssignedCourses();
                ac.setCourseid(rsCourses.getLong("course_id"));
                ac.setCoursename(rsCourses.getString("course_name"));
                ac.setCoursecode(rsCourses.getString("course_code"));
                ac.setCoursedept(rsCourses.getString("dept_name"));
                ac.setSem(rsCourses.getInt("sem"));
                ac.setCredits(rsCourses.getInt("credit"));
                courses.add(ac);
            }
            rsCourses.close();
            psCourses.close();
            model.addAttribute("courses", courses);

            if (courseId != null) {
                PreparedStatement psAlloc = con.prepareStatement(
                        "SELECT 1 FROM course_teacher_allocation WHERE teacher_id = ? AND course_id = ? LIMIT 1"
                );
                psAlloc.setLong(1, teacherId);
                psAlloc.setLong(2, courseId);
                ResultSet rsAlloc = psAlloc.executeQuery();
                boolean allowed = rsAlloc.next();
                rsAlloc.close();
                psAlloc.close();
                if (!allowed) {
                    con.close();
                    return "redirect:/teacher-upload-assignment";
                }

                AssignedCourses selected = null;
                for (AssignedCourses c : courses) {
                    if (c.getCourseid() == courseId) {
                        selected = c;
                        break;
                    }
                }
                model.addAttribute("selectedCourse", selected);

                boolean ready = assignmentTablesExist(con);
                model.addAttribute("assignmentDbMissing", !ready);
                if (ready) {
                    PreparedStatement psA = con.prepareStatement(
                            "SELECT id, title, assignment_mode, question_text, original_filename, file_size, due_at, max_marks, download_allowed, created_at " +
                                    "FROM course_assignment WHERE teacher_id = ? AND course_id = ? ORDER BY created_at DESC"
                    );
                    psA.setLong(1, teacherId);
                    psA.setLong(2, courseId);
                    ResultSet rsA = psA.executeQuery();
                    List<CourseAssignmentDTO> assignments = new ArrayList<>();
                    while (rsA.next()) {
                        CourseAssignmentDTO a = new CourseAssignmentDTO();
                        a.setId(rsA.getLong("id"));
                        a.setTitle(rsA.getString("title"));
                        a.setAssignmentMode(rsA.getString("assignment_mode"));
                        a.setQuestionText(rsA.getString("question_text"));
                        a.setOriginalFileName(rsA.getString("original_filename"));
                        long sz = rsA.getLong("file_size");
                        a.setFileSize(rsA.wasNull() ? null : sz);
                        Timestamp due = rsA.getTimestamp("due_at");
                        a.setDueAt(due == null ? null : due.toLocalDateTime());
                        a.setDueAtText(due == null ? "-" : fmt(due.toLocalDateTime()));
                        int mm = rsA.getInt("max_marks");
                        a.setMaxMarks(rsA.wasNull() ? null : mm);
                        a.setDownloadAllowed(rsA.getInt("download_allowed") == 1);
                        a.setCreatedAt(rsA.getTimestamp("created_at"));
                        assignments.add(a);
                    }
                    rsA.close();
                    psA.close();
                    model.addAttribute("assignments", assignments);
                }
            }

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Teacher/UploadAssignment";
    }

    @PostMapping("/teacher-upload-assignment")
    public String teacherUploadAssignmentSubmit(HttpSession session,
                                                @RequestParam("courseId") long courseId,
                                                @RequestParam("assignmentTitle") String assignmentTitle,
                                                @RequestParam("questionMode") String questionMode,
                                                @RequestParam(value = "questionText", required = false) String questionText,
                                                @RequestParam(value = "instructions", required = false) String instructions,
                                                @RequestParam(value = "dueDate", required = false) String dueDate,
                                                @RequestParam(value = "dueTime", required = false) String dueTime,
                                                @RequestParam(value = "maxMarks", required = false) String maxMarks,
                                                @RequestParam(value = "access", required = false) String[] access,
                                                @RequestParam(value = "assignmentFile", required = false) MultipartFile assignmentFile) {
        Object tid = session.getAttribute("teacherId");
        if (tid == null) return "redirect:/login";
        long teacherId = Long.parseLong(tid.toString());

        String mode = questionMode == null ? "" : questionMode.trim().toUpperCase(Locale.ENGLISH);
        boolean fileMode = "FILE".equals(mode);
        boolean textMode = "TEXT".equals(mode);
        if ((assignmentTitle == null || assignmentTitle.trim().isEmpty()) || (!fileMode && !textMode)) {
            return "redirect:/teacher-upload-assignment?courseId=" + courseId;
        }
        if (textMode && (questionText == null || questionText.trim().isEmpty())) {
            return "redirect:/teacher-upload-assignment?courseId=" + courseId;
        }
        if (textMode && countWords(questionText) > ASSIGNMENT_TEXT_WORD_LIMIT) {
            return "redirect:/teacher-upload-assignment?courseId=" + courseId + "&uploadError=Direct+question+must+be+within+" + ASSIGNMENT_TEXT_WORD_LIMIT + "+words";
        }
        if (fileMode && (assignmentFile == null || assignmentFile.isEmpty())) {
            return "redirect:/teacher-upload-assignment?courseId=" + courseId;
        }

        boolean downloadAllowed = fileMode;
        if (fileMode && access != null) {
            downloadAllowed = false;
            for (String a : access) {
                if (a != null && a.equalsIgnoreCase("DOWNLOAD")) {
                    downloadAllowed = true;
                    break;
                }
            }
        }

        Timestamp dueAt = null;
        try {
            if (dueDate != null && !dueDate.trim().isEmpty() && dueTime != null && !dueTime.trim().isEmpty()) {
                dueAt = Timestamp.valueOf(LocalDateTime.parse(dueDate.trim() + "T" + dueTime.trim()));
            }
        } catch (Exception ignored) {
        }

        Integer maxMarksValue = null;
        try {
            if (maxMarks != null && !maxMarks.trim().isEmpty()) maxMarksValue = Integer.parseInt(maxMarks.trim());
        } catch (Exception ignored) {
        }

        try {
            Connection con = DataBaseConnection.getConnection();
            PreparedStatement psAlloc = con.prepareStatement(
                    "SELECT 1 FROM course_teacher_allocation WHERE teacher_id = ? AND course_id = ? LIMIT 1"
            );
            psAlloc.setLong(1, teacherId);
            psAlloc.setLong(2, courseId);
            ResultSet rsAlloc = psAlloc.executeQuery();
            boolean allowed = rsAlloc.next();
            rsAlloc.close();
            psAlloc.close();
            if (!allowed) {
                con.close();
                return "redirect:/teacher-upload-assignment";
            }
            if (!assignmentTablesExist(con)) {
                con.close();
                return "redirect:/teacher-upload-assignment?courseId=" + courseId;
            }

            String original = null;
            String stored = null;
            String storedPath = null;
            String mime = null;
            Long size = null;
            if (fileMode) {
                original = assignmentFile.getOriginalFilename();
                if (original == null || original.isBlank()) {
                    con.close();
                    return "redirect:/teacher-upload-assignment?courseId=" + courseId;
                }
                String lower = original.toLowerCase(Locale.ENGLISH);
                if (!(lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx"))) {
                    con.close();
                    return "redirect:/teacher-upload-assignment?courseId=" + courseId;
                }
                Path base = Paths.get("Assignment", "Questions").toAbsolutePath().normalize();
                Files.createDirectories(base);
                String safe = original.replaceAll("[^a-zA-Z0-9._-]", "_");
                stored = java.util.UUID.randomUUID() + "_" + safe;
                Path dest = base.resolve(stored).normalize();
                if (!dest.startsWith(base)) {
                    con.close();
                    return "redirect:/teacher-upload-assignment?courseId=" + courseId;
                }
                Files.copy(assignmentFile.getInputStream(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                storedPath = dest.toString();
                mime = assignmentFile.getContentType();
                size = assignmentFile.getSize();
            }

            PreparedStatement psIns = con.prepareStatement(
                    "INSERT INTO course_assignment (course_id, teacher_id, title, assignment_mode, question_text, instructions, original_filename, stored_filename, stored_path, mime_type, file_size, due_at, max_marks, download_allowed) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            );
            psIns.setLong(1, courseId);
            psIns.setLong(2, teacherId);
            psIns.setString(3, assignmentTitle.trim());
            psIns.setString(4, mode);
            psIns.setString(5, textMode ? questionText.trim() : null);
            psIns.setString(6, instructions);
            psIns.setString(7, original);
            psIns.setString(8, stored);
            psIns.setString(9, storedPath);
            psIns.setString(10, mime);
            if (size == null) psIns.setObject(11, null); else psIns.setLong(11, size);
            psIns.setTimestamp(12, dueAt);
            if (maxMarksValue == null) psIns.setObject(13, null); else psIns.setInt(13, maxMarksValue);
            psIns.setInt(14, downloadAllowed ? 1 : 0);
            psIns.executeUpdate();
            psIns.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/teacher-upload-assignment?courseId=" + courseId;
    }

    @GetMapping("/teacher/assignment/{id}/download")
    public void teacherDownloadAssignment(@PathVariable("id") long assignmentId, HttpSession session, HttpServletResponse response) {
        Object tid = session.getAttribute("teacherId");
        if (tid == null) {
            try { response.sendRedirect("/login"); } catch (Exception ignored) {}
            return;
        }
        long teacherId = Long.parseLong(tid.toString());
        try {
            Connection con = DataBaseConnection.getConnection();
            if (!assignmentTablesExist(con)) {
                con.close();
                response.sendError(404);
                return;
            }
            PreparedStatement ps = con.prepareStatement(
                    "SELECT stored_path, original_filename, mime_type FROM course_assignment WHERE id = ? AND teacher_id = ?"
            );
            ps.setLong(1, assignmentId);
            ps.setLong(2, teacherId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                con.close();
                response.sendError(404);
                return;
            }
            String storedPath = rs.getString("stored_path");
            String original = rs.getString("original_filename");
            String mime = rs.getString("mime_type");
            rs.close();
            ps.close();
            con.close();

            if (storedPath == null || storedPath.contains("..")) {
                response.sendError(404);
                return;
            }
            Path base = Paths.get("Assignment", "Questions").toAbsolutePath().normalize();
            Path file = Paths.get(storedPath).toAbsolutePath().normalize();
            if (!file.startsWith(base) || !Files.exists(file)) {
                response.sendError(404);
                return;
            }
            response.setContentType((mime == null || mime.isBlank()) ? "application/octet-stream" : mime);
            String safe = (original == null || original.isBlank()) ? ("assignment_" + assignmentId) : original.replace("\"", "");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + safe + "\"");
            Files.copy(file, response.getOutputStream());
        } catch (Exception e) {
            try { response.sendError(500); } catch (Exception ignored) {}
        }
    }

    @GetMapping("/teacher-assignment/{assignmentId}")
    public String teacherAssignmentStatus(@PathVariable("assignmentId") long assignmentId,
                                          HttpSession session,
                                          Model model) {
        Object tid = session.getAttribute("teacherId");
        if (tid == null) return "redirect:/login";
        long teacherId = Long.parseLong(tid.toString());

        try {
            Connection con = DataBaseConnection.getConnection();
            if (!assignmentTablesExist(con)) {
                con.close();
                return "redirect:/teacher-upload-assignment";
            }

            PreparedStatement psAssignment = con.prepareStatement(
                    "SELECT ca.id, ca.course_id, ca.title, ca.assignment_mode, ca.question_text, ca.original_filename, ca.due_at, ca.max_marks, " +
                            "c.course_name, c.course_code " +
                            "FROM course_assignment ca " +
                            "JOIN course c ON c.id = ca.course_id " +
                            "WHERE ca.id = ? AND ca.teacher_id = ?"
            );
            psAssignment.setLong(1, assignmentId);
            psAssignment.setLong(2, teacherId);
            ResultSet rsAssignment = psAssignment.executeQuery();
            if (!rsAssignment.next()) {
                rsAssignment.close();
                psAssignment.close();
                con.close();
                return "redirect:/teacher-upload-assignment";
            }

            CourseAssignmentDTO assignment = new CourseAssignmentDTO();
            assignment.setId(rsAssignment.getLong("id"));
            assignment.setTitle(rsAssignment.getString("title"));
            assignment.setAssignmentMode(rsAssignment.getString("assignment_mode"));
            assignment.setQuestionText(rsAssignment.getString("question_text"));
            assignment.setOriginalFileName(rsAssignment.getString("original_filename"));
            assignment.setInstructions(null);
            assignment.setMaxMarks(rsAssignment.getObject("max_marks") == null ? null : rsAssignment.getInt("max_marks"));
            Timestamp due = rsAssignment.getTimestamp("due_at");
            assignment.setDueAt(due == null ? null : due.toLocalDateTime());
            assignment.setDueAtText(due == null ? "-" : fmt(due.toLocalDateTime()));
            long courseId = rsAssignment.getLong("course_id");
            model.addAttribute("courseId", courseId);
            model.addAttribute("courseName", rsAssignment.getString("course_name"));
            model.addAttribute("courseCode", rsAssignment.getString("course_code"));
            model.addAttribute("assignment", assignment);
            rsAssignment.close();
            psAssignment.close();

            PreparedStatement psStudents = con.prepareStatement(
                    "SELECT s.id AS student_id, s.name, s.roll_number, s.email, " +
                            "cas.id AS submission_id, cas.original_filename AS submission_original_filename, cas.submitted_at " +
                            "FROM student_course_teacher sct " +
                            "JOIN student s ON s.id = sct.student_id " +
                            "LEFT JOIN course_assignment_submission cas ON cas.assignment_id = ? AND cas.student_id = s.id " +
                            "WHERE sct.teacher_id = ? AND sct.course_id = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND s.current_semester = sct.semester " +
                            "ORDER BY s.roll_number"
            );
            psStudents.setLong(1, assignmentId);
            psStudents.setLong(2, teacherId);
            psStudents.setLong(3, courseId);
            ResultSet rsStudents = psStudents.executeQuery();
            List<AssignmentStudentStatusDTO> students = new ArrayList<>();
            int submittedCount = 0;
            while (rsStudents.next()) {
                AssignmentStudentStatusDTO dto = new AssignmentStudentStatusDTO();
                dto.setStudentId(rsStudents.getLong("student_id"));
                dto.setStudentName(rsStudents.getString("name"));
                dto.setStudentRegisterNumber(rsStudents.getString("roll_number"));
                dto.setStudentEmail(rsStudents.getString("email"));
                long submissionId = rsStudents.getLong("submission_id");
                boolean submitted = !rsStudents.wasNull();
                dto.setSubmitted(submitted);
                if (submitted) {
                    submittedCount++;
                    dto.setSubmissionId(submissionId);
                    dto.setSubmissionOriginalFileName(rsStudents.getString("submission_original_filename"));
                    Timestamp submittedAt = rsStudents.getTimestamp("submitted_at");
                    dto.setSubmittedAtText(submittedAt == null ? "-" : fmt(submittedAt.toLocalDateTime()));
                } else {
                    dto.setSubmittedAtText("-");
                }
                students.add(dto);
            }
            rsStudents.close();
            psStudents.close();

            model.addAttribute("students", students);
            model.addAttribute("submittedCount", submittedCount);
            model.addAttribute("pendingCount", Math.max(0, students.size() - submittedCount));

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/teacher-upload-assignment";
        }

        return "Teacher/ViewAssignmentStatus";
    }

    @GetMapping("/teacher/assignment-submission/{submissionId}/download")
    public void teacherDownloadAssignmentSubmission(@PathVariable("submissionId") long submissionId,
                                                    HttpSession session,
                                                    HttpServletResponse response) {
        serveTeacherAssignmentSubmission(submissionId, session, response, true);
    }

    @GetMapping("/teacher/assignment-submission/{submissionId}/view")
    public void teacherViewAssignmentSubmission(@PathVariable("submissionId") long submissionId,
                                                HttpSession session,
                                                HttpServletResponse response) {
        serveTeacherAssignmentSubmission(submissionId, session, response, false);
    }

    private void serveTeacherAssignmentSubmission(long submissionId,
                                                  HttpSession session,
                                                  HttpServletResponse response,
                                                  boolean asAttachment) {
        Object tid = session.getAttribute("teacherId");
        if (tid == null) {
            try { response.sendRedirect("/login"); } catch (Exception ignored) {}
            return;
        }
        long teacherId = Long.parseLong(tid.toString());

        try {
            Connection con = DataBaseConnection.getConnection();
            if (!assignmentTablesExist(con)) {
                con.close();
                response.sendError(404);
                return;
            }

            PreparedStatement ps = con.prepareStatement(
                    "SELECT cas.stored_path, cas.original_filename, cas.mime_type " +
                            "FROM course_assignment_submission cas " +
                            "JOIN course_assignment ca ON ca.id = cas.assignment_id " +
                            "WHERE cas.id = ? AND ca.teacher_id = ?"
            );
            ps.setLong(1, submissionId);
            ps.setLong(2, teacherId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                con.close();
                response.sendError(404);
                return;
            }
            String storedPath = rs.getString("stored_path");
            String original = rs.getString("original_filename");
            String mime = rs.getString("mime_type");
            rs.close();
            ps.close();
            con.close();

            if (storedPath == null || storedPath.contains("..")) {
                response.sendError(404);
                return;
            }

            Path base = Paths.get("Assignment", "Submissions").toAbsolutePath().normalize();
            Path file = Paths.get(storedPath).toAbsolutePath().normalize();
            if (!file.startsWith(base) || !Files.exists(file)) {
                response.sendError(404);
                return;
            }

            response.setContentType((mime == null || mime.isBlank()) ? "application/octet-stream" : mime);
            String safe = (original == null || original.isBlank()) ? ("submission_" + submissionId) : original.replace("\"", "");
            response.setHeader("Content-Disposition", (asAttachment ? "attachment" : "inline") + "; filename=\"" + safe + "\"");
            Files.copy(file, response.getOutputStream());
        } catch (Exception e) {
            try { response.sendError(500); } catch (Exception ignored) {}
        }
    }

    @GetMapping("/teacher-assign-feedback")
    public String teacherAssignFeedback(HttpSession session,
                                        @RequestParam(value = "courseId", required = false) Long courseId,
                                        Model model) {
        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }

            long teacherId = Long.parseLong(tid.toString());
            model.addAttribute("teacherId", teacherId);

            Connection con = DataBaseConnection.getConnection();

            // Courses allocated to this teacher (for dropdown / landing).
            PreparedStatement psCourses = con.prepareStatement(
                    "SELECT DISTINCT " +
                            "c.id as course_id, " +
                            "c.course_name, " +
                            "c.course_code, " +
                            "d.dept_name, " +
                            "cfd.sem, " +
                            "c.credit " +
                            "FROM course_teacher_allocation cta " +
                            "JOIN course c ON c.id = cta.course_id " +
                            "JOIN course_for_depts cfd ON cfd.course_id = c.id " +
                            "JOIN department d ON d.id = cfd.dept_id " +
                            "WHERE cta.teacher_id = ? " +
                            "ORDER BY c.course_name"
            );
            psCourses.setLong(1, teacherId);
            ResultSet rsCourses = psCourses.executeQuery();
            List<AssignedCourses> courses = new ArrayList<>();
            while (rsCourses.next()) {
                AssignedCourses ac = new AssignedCourses();
                ac.setCourseid(rsCourses.getLong("course_id"));
                ac.setCoursename(rsCourses.getString("course_name"));
                ac.setCoursecode(rsCourses.getString("course_code"));
                ac.setCoursedept(rsCourses.getString("dept_name"));
                ac.setSem(rsCourses.getInt("sem"));
                ac.setCredits(rsCourses.getInt("credit"));
                courses.add(ac);
            }
            rsCourses.close();
            psCourses.close();
            model.addAttribute("courses", courses);

            // If courseId is present, load builder context + existing forms.
            if (courseId != null) {
                // Verify teacher-course allocation.
                PreparedStatement psAlloc = con.prepareStatement(
                        "SELECT 1 FROM course_teacher_allocation WHERE teacher_id = ? AND course_id = ? LIMIT 1"
                );
                psAlloc.setLong(1, teacherId);
                psAlloc.setLong(2, courseId);
                ResultSet rsAlloc = psAlloc.executeQuery();
                boolean allowed = rsAlloc.next();
                rsAlloc.close();
                psAlloc.close();
                if (!allowed) {
                    con.close();
                    return "redirect:/teacher-assign-feedback";
                }

                AssignedCourses selected = null;
                for (AssignedCourses c : courses) {
                    if (c.getCourseid() == courseId) {
                        selected = c;
                        break;
                    }
                }
                model.addAttribute("selectedCourse", selected);

                boolean feedbackReady = feedbackTablesExist(con);
                model.addAttribute("feedbackDbMissing", !feedbackReady);
                if (!feedbackReady) {
                    con.close();
                    return "Teacher/AssignFeedback";
                }

                int eligibleCount = 0;
                PreparedStatement psEligible = con.prepareStatement(
                        "SELECT COUNT(DISTINCT sct.student_id) AS cnt " +
                                "FROM student_course_teacher sct " +
                                "JOIN student s ON s.id = sct.student_id " +
                                "WHERE sct.teacher_id = ? " +
                                "AND sct.course_id = ? " +
                                "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                                "AND s.current_semester = sct.semester"
                );
                psEligible.setLong(1, teacherId);
                psEligible.setLong(2, courseId);
                ResultSet rsEligible = psEligible.executeQuery();
                if (rsEligible.next()) eligibleCount = rsEligible.getInt("cnt");
                rsEligible.close();
                psEligible.close();
                model.addAttribute("eligibleCount", eligibleCount);

                PreparedStatement psForms = con.prepareStatement(
                        "SELECT ff.id, ff.course_id, ff.teacher_id, ff.title, ff.description, ff.is_active, ff.created_at, " +
                                "(SELECT COUNT(*) FROM feedback_question fq WHERE fq.form_id = ff.id) AS question_count, " +
                                "(SELECT COUNT(*) FROM feedback_submission fs WHERE fs.form_id = ff.id) AS submitted_count " +
                                "FROM feedback_form ff " +
                                "WHERE ff.teacher_id = ? AND ff.course_id = ? " +
                                "ORDER BY ff.created_at DESC"
                );
                psForms.setLong(1, teacherId);
                psForms.setLong(2, courseId);
                ResultSet rsForms = psForms.executeQuery();
                List<FeedbackFormDTO> forms = new ArrayList<>();
                while (rsForms.next()) {
                    FeedbackFormDTO f = new FeedbackFormDTO();
                    f.setId(rsForms.getLong("id"));
                    f.setCourseId(rsForms.getLong("course_id"));
                    f.setTeacherId(rsForms.getLong("teacher_id"));
                    f.setTitle(rsForms.getString("title"));
                    f.setDescription(rsForms.getString("description"));
                    f.setActive(rsForms.getInt("is_active") == 1);
                    f.setCreatedAt(rsForms.getTimestamp("created_at"));
                    f.setQuestionCount(rsForms.getInt("question_count"));
                    f.setSubmittedCount(rsForms.getInt("submitted_count"));
                    f.setEligibleCount(eligibleCount);
                    forms.add(f);
                }
                rsForms.close();
                psForms.close();
                model.addAttribute("forms", forms);
            }

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }
        return "Teacher/AssignFeedback";
    }

    @PostMapping("/teacher-assign-feedback/create")
    public String teacherCreateFeedbackForm(HttpSession session, @RequestParam("courseId") long courseId, @RequestParam("title") String title, @RequestParam(value = "description", required = false) String description, @RequestParam("questionText") String[] questionText, @RequestParam("questionType") String[] questionType, @RequestParam(value = "questionOptions", required = false) String[] questionOptions, @RequestParam(value = "ratingMax", required = false) String[] ratingMax) {
        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }
            long teacherId = Long.parseLong(tid.toString());

            if (title == null || title.trim().isEmpty()) {
                return "redirect:/teacher-assign-feedback?courseId=" + courseId;
            }
            if (questionText == null || questionText.length == 0) {
                return "redirect:/teacher-assign-feedback?courseId=" + courseId;
            }

            Connection con = DataBaseConnection.getConnection();
            con.setAutoCommit(false);

            if (!feedbackTablesExist(con)) {
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-feedback?courseId=" + courseId;
            }

            PreparedStatement psAlloc = con.prepareStatement(
                    "SELECT 1 FROM course_teacher_allocation WHERE teacher_id = ? AND course_id = ? LIMIT 1"
            );
            psAlloc.setLong(1, teacherId);
            psAlloc.setLong(2, courseId);
            ResultSet rsAlloc = psAlloc.executeQuery();
            boolean allowed = rsAlloc.next();
            rsAlloc.close();
            psAlloc.close();
            if (!allowed) {
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-feedback";
            }

            PreparedStatement psForm = con.prepareStatement(
                    "INSERT INTO feedback_form (course_id, teacher_id, title, description, is_active) VALUES (?, ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS
            );
            psForm.setLong(1, courseId);
            psForm.setLong(2, teacherId);
            psForm.setString(3, title.trim());
            psForm.setString(4, description);
            psForm.executeUpdate();

            long formId;
            ResultSet keys = psForm.getGeneratedKeys();
            if (!keys.next()) {
                keys.close();
                psForm.close();
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-feedback?courseId=" + courseId;
            }
            formId = keys.getLong(1);
            keys.close();
            psForm.close();

            PreparedStatement psQ = con.prepareStatement(
                    "INSERT INTO feedback_question (form_id, question_order, question_text, question_type, options_text, rating_max, required) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 1)"
            );

            int added = 0;
            for (int i = 0; i < questionText.length; i++) {
                String qt = questionText[i] == null ? "" : questionText[i].trim();
                String type = (questionType != null && questionType.length > i && questionType[i] != null) ? questionType[i].trim() : "TEXT";

                if (qt.isEmpty()) {
                    continue; // skip empty rows
                }

                String options = (questionOptions != null && questionOptions.length > i) ? questionOptions[i] : null;
                String rMaxRaw = (ratingMax != null && ratingMax.length > i) ? ratingMax[i] : null;
                Integer rMax = null;
                if ("RATING".equalsIgnoreCase(type)) {
                    int parsed = parseLeadingInt(rMaxRaw, 5);
                    rMax = parsed <= 0 ? 5 : parsed;
                    options = null;
                } else if ("MCQ".equalsIgnoreCase(type)) {
                    rMax = null;
                    options = (options == null) ? "" : options.trim();
                } else {
                    rMax = null;
                    options = null;
                    type = "TEXT";
                }

                psQ.setLong(1, formId);
                psQ.setInt(2, i + 1);
                psQ.setString(3, qt);
                psQ.setString(4, type.toUpperCase(Locale.ROOT));
                psQ.setString(5, options);
                if (rMax == null) {
                    psQ.setObject(6, null);
                } else {
                    psQ.setInt(6, rMax);
                }
                psQ.addBatch();
                added++;
            }

            if (added == 0) {
                psQ.close();
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-feedback?courseId=" + courseId;
            }

            psQ.executeBatch();
            psQ.close();

            con.commit();
            con.close();

            return "redirect:/teacher-assign-feedback?courseId=" + courseId;
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/teacher-assign-feedback?courseId=" + courseId;
        }
    }

    @GetMapping("/teacher-feedback/{formId}")
    public String teacherViewFeedbackStatus(@PathVariable("formId") long formId, HttpSession session, Model model) {
        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }
            long teacherId = Long.parseLong(tid.toString());

            Connection con = DataBaseConnection.getConnection();
            if (!feedbackTablesExist(con)) {
                con.close();
                return "redirect:/teacher-assign-feedback";
            }

            PreparedStatement psForm = con.prepareStatement(
                    "SELECT ff.id, ff.course_id, ff.teacher_id, ff.title, ff.description, ff.is_active, ff.created_at, " +
                            "c.course_name, c.course_code " +
                            "FROM feedback_form ff " +
                            "JOIN course c ON c.id = ff.course_id " +
                            "WHERE ff.id = ? AND ff.teacher_id = ?"
            );
            psForm.setLong(1, formId);
            psForm.setLong(2, teacherId);
            ResultSet rsForm = psForm.executeQuery();
            if (!rsForm.next()) {
                rsForm.close();
                psForm.close();
                con.close();
                return "redirect:/teacher-assign-feedback";
            }

            FeedbackFormDTO form = new FeedbackFormDTO();
            form.setId(rsForm.getLong("id"));
            form.setCourseId(rsForm.getLong("course_id"));
            form.setTeacherId(rsForm.getLong("teacher_id"));
            form.setTitle(rsForm.getString("title"));
            form.setDescription(rsForm.getString("description"));
            form.setActive(rsForm.getInt("is_active") == 1);
            form.setCreatedAt(rsForm.getTimestamp("created_at"));
            form.setCourseName(rsForm.getString("course_name"));
            form.setCourseCode(rsForm.getString("course_code"));
            rsForm.close();
            psForm.close();

            // Questions (for preview)
            PreparedStatement psQ = con.prepareStatement(
                    "SELECT id, question_order, question_text, question_type, options_text, rating_max, required " +
                            "FROM feedback_question WHERE form_id = ? ORDER BY question_order"
            );
            psQ.setLong(1, formId);
            ResultSet rsQ = psQ.executeQuery();
            List<FeedbackQuestionDTO> questions = new ArrayList<>();
            while (rsQ.next()) {
                FeedbackQuestionDTO q = new FeedbackQuestionDTO();
                q.setId(rsQ.getLong("id"));
                q.setOrder(rsQ.getInt("question_order"));
                q.setText(rsQ.getString("question_text"));
                q.setType(rsQ.getString("question_type"));
                q.setOptionsText(rsQ.getString("options_text"));
                int rm = rsQ.getInt("rating_max");
                q.setRatingMax(rsQ.wasNull() ? null : rm);
                q.setRequired(rsQ.getInt("required") == 1);
                if (q.getOptionsText() != null) {
                    q.setOptions(splitNonEmptyLines(q.getOptionsText()));
                }
                questions.add(q);
            }
            rsQ.close();
            psQ.close();

            // Eligible students and submission status.
            PreparedStatement psStatus = con.prepareStatement(
                    "SELECT s.id AS student_id, s.name, s.roll_number, d.dept_name, sct.semester, fs.submitted_at " +
                            "FROM student_course_teacher sct " +
                            "JOIN student s ON s.id = sct.student_id " +
                            "JOIN department d ON d.id = s.dept_id " +
                            "LEFT JOIN feedback_submission fs ON fs.form_id = ? AND fs.student_id = s.id " +
                            "WHERE sct.teacher_id = ? AND sct.course_id = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND s.current_semester = sct.semester " +
                            "ORDER BY s.roll_number"
            );
            psStatus.setLong(1, formId);
            psStatus.setLong(2, teacherId);
            psStatus.setLong(3, form.getCourseId());
            ResultSet rsStatus = psStatus.executeQuery();
            List<FeedbackStudentStatusDTO> studentStatuses = new ArrayList<>();
            int submittedCount = 0;
            while (rsStatus.next()) {
                FeedbackStudentStatusDTO st = new FeedbackStudentStatusDTO();
                st.setStudentId(rsStatus.getLong("student_id"));
                st.setName(rsStatus.getString("name"));
                st.setRollNumber(rsStatus.getString("roll_number"));
                st.setDepartment(rsStatus.getString("dept_name"));
                st.setSemester(rsStatus.getInt("semester"));
                Timestamp submittedAt = rsStatus.getTimestamp("submitted_at");
                st.setSubmittedAt(submittedAt);
                st.setSubmitted(submittedAt != null);
                if (submittedAt != null) submittedCount++;
                studentStatuses.add(st);
            }
            rsStatus.close();
            psStatus.close();

            form.setQuestionCount(questions.size());
            form.setEligibleCount(studentStatuses.size());
            form.setSubmittedCount(submittedCount);

            model.addAttribute("form", form);
            model.addAttribute("questions", questions);
            model.addAttribute("studentStatuses", studentStatuses);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/teacher-assign-feedback";
        }
        return "Teacher/ViewFeedbackStatus";
    }

    @GetMapping("/teacher-feedback/{formId}/student/{studentId}")
    public String teacherViewFeedbackSubmissionForStudent(@PathVariable("formId") long formId, @PathVariable("studentId") long studentId, HttpSession session, Model model) {
        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }
            long teacherId = Long.parseLong(tid.toString());

            Connection con = DataBaseConnection.getConnection();
            if (!feedbackTablesExist(con)) {
                con.close();
                return "redirect:/teacher-assign-feedback";
            }

            PreparedStatement psSub = con.prepareStatement(
                    "SELECT fs.id AS submission_id, fs.submitted_at, " +
                            "s.name AS student_name, s.roll_number, " +
                            "ff.title AS form_title, c.course_name, c.course_code " +
                            "FROM feedback_submission fs " +
                            "JOIN feedback_form ff ON ff.id = fs.form_id " +
                            "JOIN course c ON c.id = ff.course_id " +
                            "JOIN student s ON s.id = fs.student_id " +
                            "WHERE fs.form_id = ? AND fs.student_id = ? AND ff.teacher_id = ?"
            );
            psSub.setLong(1, formId);
            psSub.setLong(2, studentId);
            psSub.setLong(3, teacherId);
            ResultSet rsSub = psSub.executeQuery();
            if (!rsSub.next()) {
                rsSub.close();
                psSub.close();
                con.close();
                return "redirect:/teacher-feedback/" + formId;
            }
            long submissionId = rsSub.getLong("submission_id");
            model.addAttribute("submittedAt", rsSub.getTimestamp("submitted_at"));
            model.addAttribute("studentName", rsSub.getString("student_name"));
            model.addAttribute("rollNumber", rsSub.getString("roll_number"));
            model.addAttribute("formTitle", rsSub.getString("form_title"));
            model.addAttribute("courseName", rsSub.getString("course_name"));
            model.addAttribute("courseCode", rsSub.getString("course_code"));
            rsSub.close();
            psSub.close();

            PreparedStatement psAns = con.prepareStatement(
                    "SELECT fq.question_order, fq.question_text, fq.question_type, fq.options_text, fq.rating_max, " +
                            "fa.answer_text, fa.answer_number " +
                            "FROM feedback_question fq " +
                            "LEFT JOIN feedback_answer fa ON fa.question_id = fq.id AND fa.submission_id = ? " +
                            "WHERE fq.form_id = ? " +
                            "ORDER BY fq.question_order"
            );
            psAns.setLong(1, submissionId);
            psAns.setLong(2, formId);
            ResultSet rsAns = psAns.executeQuery();
            List<Map<String, Object>> answers = new ArrayList<>();
            while (rsAns.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("order", rsAns.getInt("question_order"));
                row.put("text", rsAns.getString("question_text"));
                row.put("type", rsAns.getString("question_type"));
                row.put("answerText", rsAns.getString("answer_text"));
                int num = rsAns.getInt("answer_number");
                row.put("answerNumber", rsAns.wasNull() ? null : num);
                answers.add(row);
            }
            rsAns.close();
            psAns.close();

            model.addAttribute("answers", answers);
            model.addAttribute("formId", formId);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/teacher-feedback/" + formId;
        }
        return "Teacher/ViewFeedbackSubmission";
    }

    @GetMapping("/teacher-assign-quiz")
    public String teacherAssignQuiz(HttpSession session,
                                    @RequestParam(value = "courseId", required = false) Long courseId,
                                    Model model) {
        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }
            long teacherId = Long.parseLong(tid.toString());
            model.addAttribute("teacherId", teacherId);

            Connection con = DataBaseConnection.getConnection();

            PreparedStatement psCourses = con.prepareStatement(
                    "SELECT DISTINCT " +
                            "c.id as course_id, " +
                            "c.course_name, " +
                            "c.course_code, " +
                            "d.dept_name, " +
                            "cfd.sem, " +
                            "c.credit " +
                            "FROM course_teacher_allocation cta " +
                            "JOIN course c ON c.id = cta.course_id " +
                            "JOIN course_for_depts cfd ON cfd.course_id = c.id " +
                            "JOIN department d ON d.id = cfd.dept_id " +
                            "WHERE cta.teacher_id = ? " +
                            "ORDER BY c.course_name"
            );
            psCourses.setLong(1, teacherId);
            ResultSet rsCourses = psCourses.executeQuery();
            List<AssignedCourses> courses = new ArrayList<>();
            while (rsCourses.next()) {
                AssignedCourses ac = new AssignedCourses();
                ac.setCourseid(rsCourses.getLong("course_id"));
                ac.setCoursename(rsCourses.getString("course_name"));
                ac.setCoursecode(rsCourses.getString("course_code"));
                ac.setCoursedept(rsCourses.getString("dept_name"));
                ac.setSem(rsCourses.getInt("sem"));
                ac.setCredits(rsCourses.getInt("credit"));
                courses.add(ac);
            }
            rsCourses.close();
            psCourses.close();
            model.addAttribute("courses", courses);

            if (courseId != null) {
                PreparedStatement psAlloc = con.prepareStatement(
                        "SELECT 1 FROM course_teacher_allocation WHERE teacher_id = ? AND course_id = ? LIMIT 1"
                );
                psAlloc.setLong(1, teacherId);
                psAlloc.setLong(2, courseId);
                ResultSet rsAlloc = psAlloc.executeQuery();
                boolean allowed = rsAlloc.next();
                rsAlloc.close();
                psAlloc.close();
                if (!allowed) {
                    con.close();
                    return "redirect:/teacher-assign-quiz";
                }

                AssignedCourses selected = null;
                for (AssignedCourses c : courses) {
                    if (c.getCourseid() == courseId) {
                        selected = c;
                        break;
                    }
                }
                model.addAttribute("selectedCourse", selected);

                boolean quizReady = quizTablesExist(con);
                model.addAttribute("quizDbMissing", !quizReady);
                if (!quizReady) {
                    con.close();
                    return "Teacher/AssignQuiz";
                }

                int eligibleCount = 0;
                PreparedStatement psEligible = con.prepareStatement(
                        "SELECT COUNT(DISTINCT sct.student_id) AS cnt " +
                                "FROM student_course_teacher sct " +
                                "JOIN student s ON s.id = sct.student_id " +
                                "WHERE sct.teacher_id = ? " +
                                "AND sct.course_id = ? " +
                                "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                                "AND s.current_semester = sct.semester"
                );
                psEligible.setLong(1, teacherId);
                psEligible.setLong(2, courseId);
                ResultSet rsEligible = psEligible.executeQuery();
                if (rsEligible.next()) eligibleCount = rsEligible.getInt("cnt");
                rsEligible.close();
                psEligible.close();
                model.addAttribute("eligibleCount", eligibleCount);

                PreparedStatement psQuizzes = con.prepareStatement(
                        "SELECT q.id, q.course_id, q.teacher_id, q.title, q.total_marks, q.duration_minutes, q.start_at, q.end_at, q.is_published, q.is_score_published, q.created_at, " +
                                "(SELECT COUNT(*) FROM quiz_question qq WHERE qq.quiz_id = q.id) AS question_count, " +
                                "(SELECT COUNT(*) FROM quiz_submission qs WHERE qs.quiz_id = q.id) AS submitted_count " +
                                "FROM quiz q " +
                                "WHERE q.teacher_id = ? AND q.course_id = ? " +
                                "ORDER BY q.created_at DESC"
                );
                psQuizzes.setLong(1, teacherId);
                psQuizzes.setLong(2, courseId);
                ResultSet rsQuizzes = psQuizzes.executeQuery();
                List<QuizDTO> quizzes = new ArrayList<>();
                while (rsQuizzes.next()) {
                    QuizDTO q = new QuizDTO();
                    q.setId(rsQuizzes.getLong("id"));
                    q.setCourseId(rsQuizzes.getLong("course_id"));
                    q.setTeacherId(rsQuizzes.getLong("teacher_id"));
                    q.setTitle(rsQuizzes.getString("title"));
                    q.setTotalMarks(rsQuizzes.getInt("total_marks"));
                    q.setDurationMinutes(rsQuizzes.getInt("duration_minutes"));
                    q.setStartAt(rsQuizzes.getTimestamp("start_at").toLocalDateTime());
                    q.setEndAt(rsQuizzes.getTimestamp("end_at").toLocalDateTime());
                    q.setStartAtText(fmt(q.getStartAt()));
                    q.setEndAtText(fmt(q.getEndAt()));
                    q.setPublished(rsQuizzes.getInt("is_published") == 1);
                    q.setScorePublished(rsQuizzes.getInt("is_score_published") == 1);
                    q.setCreatedAt(rsQuizzes.getTimestamp("created_at"));
                    q.setQuestionCount(rsQuizzes.getInt("question_count"));
                    q.setSubmittedCount(rsQuizzes.getInt("submitted_count"));
                    q.setEligibleCount(eligibleCount);
                    quizzes.add(q);
                }
                rsQuizzes.close();
                psQuizzes.close();
                model.addAttribute("quizzes", quizzes);
            }

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }
        return "Teacher/AssignQuiz";
    }

    @PostMapping("/teacher-assign-quiz/create")
    public String teacherCreateQuizForCourse(HttpSession session,
                                             @RequestParam("courseId") long courseId,
                                             @RequestParam("title") String title,
                                             @RequestParam(value = "instructions", required = false) String instructions,
                                             @RequestParam("durationMinutes") int durationMinutes,
                                             @RequestParam("startDate") String startDate,
                                             @RequestParam("startTime") String startTime,
                                             @RequestParam("endDate") String endDate,
                                             @RequestParam("endTime") String endTime,
                                             @RequestParam("questionText") String[] questionText,
                                             @RequestParam("questionType") String[] questionType,
                                             @RequestParam("marks") String[] marks,
                                             @RequestParam(value = "opt1", required = false) String[] opt1,
                                             @RequestParam(value = "opt2", required = false) String[] opt2,
                                             @RequestParam(value = "opt3", required = false) String[] opt3,
                                             @RequestParam(value = "opt4", required = false) String[] opt4,
                                             @RequestParam(value = "correctIndex", required = false) String[] correctIndex) {
        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }
            long teacherId = Long.parseLong(tid.toString());

            if (title == null || title.trim().isEmpty()) {
                return "redirect:/teacher-assign-quiz?courseId=" + courseId;
            }
            if (questionText == null || questionText.length == 0) {
                return "redirect:/teacher-assign-quiz?courseId=" + courseId;
            }

            LocalDateTime startAt;
            LocalDateTime endAt;
            try {
                startAt = LocalDateTime.parse(startDate + "T" + startTime);
                endAt = LocalDateTime.parse(endDate + "T" + endTime);
            } catch (DateTimeParseException ex) {
                return "redirect:/teacher-assign-quiz?courseId=" + courseId;
            }
            if (!startAt.isBefore(endAt)) {
                return "redirect:/teacher-assign-quiz?courseId=" + courseId;
            }

            Connection con = DataBaseConnection.getConnection();
            con.setAutoCommit(false);

            if (!quizTablesExist(con)) {
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-quiz?courseId=" + courseId;
            }

            PreparedStatement psAlloc = con.prepareStatement(
                    "SELECT 1 FROM course_teacher_allocation WHERE teacher_id = ? AND course_id = ? LIMIT 1"
            );
            psAlloc.setLong(1, teacherId);
            psAlloc.setLong(2, courseId);
            ResultSet rsAlloc = psAlloc.executeQuery();
            boolean allowed = rsAlloc.next();
            rsAlloc.close();
            psAlloc.close();
            if (!allowed) {
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-quiz";
            }

            int totalMarks = 0;
            if (marks != null) {
                for (String m : marks) totalMarks += parseLeadingInt(m, 0);
            }
            if (totalMarks <= 0) totalMarks = 1;

            PreparedStatement psQuiz = con.prepareStatement(
                    "INSERT INTO quiz (course_id, teacher_id, title, instructions, total_marks, duration_minutes, start_at, end_at, is_published) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS
            );
            psQuiz.setLong(1, courseId);
            psQuiz.setLong(2, teacherId);
            psQuiz.setString(3, title.trim());
            psQuiz.setString(4, instructions);
            psQuiz.setInt(5, totalMarks);
            psQuiz.setInt(6, durationMinutes <= 0 ? 1 : durationMinutes);
            psQuiz.setTimestamp(7, java.sql.Timestamp.valueOf(startAt));
            psQuiz.setTimestamp(8, java.sql.Timestamp.valueOf(endAt));
            psQuiz.executeUpdate();

            ResultSet keys = psQuiz.getGeneratedKeys();
            if (!keys.next()) {
                keys.close();
                psQuiz.close();
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-quiz?courseId=" + courseId;
            }
            long quizId = keys.getLong(1);
            keys.close();
            psQuiz.close();

            PreparedStatement psQ = con.prepareStatement(
                    "INSERT INTO quiz_question (quiz_id, question_order, question_text, question_type, marks, correct_option_index) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            PreparedStatement psOpt = con.prepareStatement(
                    "INSERT INTO quiz_option (question_id, option_index, option_text) VALUES (?, ?, ?)"
            );

            int added = 0;
            for (int i = 0; i < questionText.length; i++) {
                String qt = questionText[i] == null ? "" : questionText[i].trim();
                if (qt.isEmpty()) continue;

                String type = (questionType != null && questionType.length > i && questionType[i] != null) ? questionType[i].trim() : "MCQ";
                int m = (marks != null && marks.length > i) ? parseLeadingInt(marks[i], 1) : 1;
                if (m <= 0) m = 1;

                Integer corr = null;
                if ("MCQ".equalsIgnoreCase(type)) {
                    corr = (correctIndex != null && correctIndex.length > i) ? parseLeadingInt(correctIndex[i], 0) : 0;
                    if (corr == null || corr < 1 || corr > 4) corr = 1;
                } else {
                    type = "DESCRIPTIVE";
                }

                psQ.setLong(1, quizId);
                psQ.setInt(2, i + 1);
                psQ.setString(3, qt);
                psQ.setString(4, type.toUpperCase(Locale.ROOT));
                psQ.setInt(5, m);
                if (corr == null) psQ.setObject(6, null);
                else psQ.setInt(6, corr);
                psQ.executeUpdate();

                ResultSet qKeys = psQ.getGeneratedKeys();
                if (!qKeys.next()) {
                    qKeys.close();
                    continue;
                }
                long qid = qKeys.getLong(1);
                qKeys.close();

                if ("MCQ".equalsIgnoreCase(type)) {
                    String o1 = (opt1 != null && opt1.length > i) ? opt1[i] : "";
                    String o2 = (opt2 != null && opt2.length > i) ? opt2[i] : "";
                    String o3 = (opt3 != null && opt3.length > i) ? opt3[i] : "";
                    String o4 = (opt4 != null && opt4.length > i) ? opt4[i] : "";

                    psOpt.setLong(1, qid); psOpt.setInt(2, 1); psOpt.setString(3, o1 == null ? "" : o1.trim()); psOpt.addBatch();
                    psOpt.setLong(1, qid); psOpt.setInt(2, 2); psOpt.setString(3, o2 == null ? "" : o2.trim()); psOpt.addBatch();
                    psOpt.setLong(1, qid); psOpt.setInt(2, 3); psOpt.setString(3, o3 == null ? "" : o3.trim()); psOpt.addBatch();
                    psOpt.setLong(1, qid); psOpt.setInt(2, 4); psOpt.setString(3, o4 == null ? "" : o4.trim()); psOpt.addBatch();
                }

                added++;
            }

            if (added == 0) {
                psOpt.close();
                psQ.close();
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-quiz?courseId=" + courseId;
            }

            psOpt.executeBatch();
            psOpt.close();
            psQ.close();

            con.commit();
            con.close();

            return "redirect:/teacher-assign-quiz?courseId=" + courseId;
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/teacher-assign-quiz?courseId=" + courseId;
        }
    }

    @GetMapping("/teacher-quiz/{quizId}")
    public String teacherViewQuizStatus(@PathVariable("quizId") long quizId, HttpSession session, Model model) {
        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }
            long teacherId = Long.parseLong(tid.toString());

            Connection con = DataBaseConnection.getConnection();
            if (!quizTablesExist(con)) {
                con.close();
                return "redirect:/teacher-assign-quiz";
            }

            PreparedStatement psQuiz = con.prepareStatement(
                    "SELECT q.id, q.course_id, q.teacher_id, q.title, q.instructions, q.total_marks, q.duration_minutes, q.start_at, q.end_at, q.is_published, q.is_score_published, q.created_at, " +
                            "c.course_name, c.course_code " +
                            "FROM quiz q " +
                            "JOIN course c ON c.id = q.course_id " +
                            "WHERE q.id = ? AND q.teacher_id = ?"
            );
            psQuiz.setLong(1, quizId);
            psQuiz.setLong(2, teacherId);
            ResultSet rsQuiz = psQuiz.executeQuery();
            if (!rsQuiz.next()) {
                rsQuiz.close();
                psQuiz.close();
                con.close();
                return "redirect:/teacher-assign-quiz";
            }

            QuizDTO quiz = new QuizDTO();
            quiz.setId(rsQuiz.getLong("id"));
            quiz.setCourseId(rsQuiz.getLong("course_id"));
            quiz.setTeacherId(rsQuiz.getLong("teacher_id"));
            quiz.setTitle(rsQuiz.getString("title"));
            quiz.setInstructions(rsQuiz.getString("instructions"));
            quiz.setTotalMarks(rsQuiz.getInt("total_marks"));
            quiz.setDurationMinutes(rsQuiz.getInt("duration_minutes"));
            quiz.setStartAt(rsQuiz.getTimestamp("start_at").toLocalDateTime());
            quiz.setEndAt(rsQuiz.getTimestamp("end_at").toLocalDateTime());
            quiz.setStartAtText(fmt(quiz.getStartAt()));
            quiz.setEndAtText(fmt(quiz.getEndAt()));
            quiz.setPublished(rsQuiz.getInt("is_published") == 1);
            quiz.setScorePublished(rsQuiz.getInt("is_score_published") == 1);
            quiz.setCreatedAt(rsQuiz.getTimestamp("created_at"));
            model.addAttribute("courseName", rsQuiz.getString("course_name"));
            model.addAttribute("courseCode", rsQuiz.getString("course_code"));
            rsQuiz.close();
            psQuiz.close();

            PreparedStatement psQ = con.prepareStatement(
                    "SELECT qq.id, qq.question_order, qq.question_text, qq.question_type, qq.marks, qq.correct_option_index " +
                            "FROM quiz_question qq WHERE qq.quiz_id = ? ORDER BY qq.question_order"
            );
            psQ.setLong(1, quizId);
            ResultSet rsQ = psQ.executeQuery();
            List<QuizQuestionDTO> questions = new ArrayList<>();
            while (rsQ.next()) {
                QuizQuestionDTO q = new QuizQuestionDTO();
                q.setId(rsQ.getLong("id"));
                q.setOrder(rsQ.getInt("question_order"));
                q.setText(rsQ.getString("question_text"));
                q.setType(rsQ.getString("question_type"));
                q.setMarks(rsQ.getInt("marks"));
                int ci = rsQ.getInt("correct_option_index");
                q.setCorrectOptionIndex(rsQ.wasNull() ? null : ci);
                questions.add(q);
            }
            rsQ.close();
            psQ.close();
            quiz.setQuestionCount(questions.size());

            PreparedStatement psStatus = con.prepareStatement(
                    "SELECT s.id AS student_id, s.name, s.roll_number, d.dept_name, sct.semester, qs.submitted_at, qs.score " +
                            "FROM student_course_teacher sct " +
                            "JOIN student s ON s.id = sct.student_id " +
                            "JOIN department d ON d.id = s.dept_id " +
                            "LEFT JOIN quiz_submission qs ON qs.quiz_id = ? AND qs.student_id = s.id " +
                            "WHERE sct.teacher_id = ? AND sct.course_id = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND s.current_semester = sct.semester " +
                            "ORDER BY s.roll_number"
            );
            psStatus.setLong(1, quizId);
            psStatus.setLong(2, teacherId);
            psStatus.setLong(3, quiz.getCourseId());
            ResultSet rsStatus = psStatus.executeQuery();
            List<QuizStudentStatusDTO> statuses = new ArrayList<>();
            int submittedCount = 0;
            while (rsStatus.next()) {
                QuizStudentStatusDTO st = new QuizStudentStatusDTO();
                st.setStudentId(rsStatus.getLong("student_id"));
                st.setName(rsStatus.getString("name"));
                st.setRollNumber(rsStatus.getString("roll_number"));
                st.setDepartment(rsStatus.getString("dept_name"));
                st.setSemester(rsStatus.getInt("semester"));
                Timestamp subAt = rsStatus.getTimestamp("submitted_at");
                st.setSubmittedAt(subAt);
                st.setSubmitted(subAt != null);
                int sc = rsStatus.getInt("score");
                st.setScore(rsStatus.wasNull() ? null : sc);
                if (subAt != null) submittedCount++;
                statuses.add(st);
            }
            rsStatus.close();
            psStatus.close();

            quiz.setEligibleCount(statuses.size());
            quiz.setSubmittedCount(submittedCount);

            model.addAttribute("quiz", quiz);
            model.addAttribute("questions", questions);
            model.addAttribute("studentStatuses", statuses);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/teacher-assign-quiz";
        }
        return "Teacher/ViewQuizStatus";
    }

    @PostMapping("/teacher-quiz/{quizId}/publish-score")
    public String teacherPublishQuizScore(@PathVariable("quizId") long quizId,
                                          @RequestParam("published") boolean published,
                                          HttpSession session) {
        Object tid = session.getAttribute("teacherId");
        if (tid == null) return "redirect:/login";
        long teacherId = Long.parseLong(tid.toString());

        try {
            Connection con = DataBaseConnection.getConnection();
            if (!quizTablesExist(con)) {
                con.close();
                return "redirect:/teacher-assign-quiz";
            }

            PreparedStatement ps = con.prepareStatement(
                    "UPDATE quiz SET is_score_published = ? WHERE id = ? AND teacher_id = ?"
            );
            ps.setInt(1, published ? 1 : 0);
            ps.setLong(2, quizId);
            ps.setLong(3, teacherId);
            int updated = ps.executeUpdate();
            ps.close();
            con.close();

            if (updated == 0) {
                return "redirect:/teacher-assign-quiz";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/teacher-quiz/" + quizId;
    }

    @GetMapping("/teacher-quiz/{quizId}/student/{studentId}")
    public String teacherViewQuizSubmission(@PathVariable("quizId") long quizId,
                                           @PathVariable("studentId") long studentId,
                                           HttpSession session,
                                           Model model) {
        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) return "redirect:/login";
            long teacherId = Long.parseLong(tid.toString());

            Connection con = DataBaseConnection.getConnection();
            if (!quizTablesExist(con)) {
                con.close();
                return "redirect:/teacher-assign-quiz";
            }

            PreparedStatement psSub = con.prepareStatement(
                    "SELECT qs.id AS submission_id, qs.submitted_at, qs.score, " +
                            "s.name AS student_name, s.roll_number, " +
                            "q.title AS quiz_title, c.course_name, c.course_code " +
                            "FROM quiz_submission qs " +
                            "JOIN quiz q ON q.id = qs.quiz_id " +
                            "JOIN course c ON c.id = q.course_id " +
                            "JOIN student s ON s.id = qs.student_id " +
                            "WHERE qs.quiz_id = ? AND qs.student_id = ? AND q.teacher_id = ?"
            );
            psSub.setLong(1, quizId);
            psSub.setLong(2, studentId);
            psSub.setLong(3, teacherId);
            ResultSet rsSub = psSub.executeQuery();
            if (!rsSub.next()) {
                rsSub.close();
                psSub.close();
                con.close();
                return "redirect:/teacher-quiz/" + quizId;
            }
            long submissionId = rsSub.getLong("submission_id");
            model.addAttribute("submittedAt", rsSub.getTimestamp("submitted_at"));
            int sc = rsSub.getInt("score");
            model.addAttribute("score", rsSub.wasNull() ? null : sc);
            model.addAttribute("studentName", rsSub.getString("student_name"));
            model.addAttribute("rollNumber", rsSub.getString("roll_number"));
            model.addAttribute("quizTitle", rsSub.getString("quiz_title"));
            model.addAttribute("courseName", rsSub.getString("course_name"));
            model.addAttribute("courseCode", rsSub.getString("course_code"));
            rsSub.close();
            psSub.close();

            PreparedStatement psAns = con.prepareStatement(
                    "SELECT qq.question_order, qq.question_text, qq.question_type, qq.marks, qq.correct_option_index, " +
                            "qa.selected_option_index, qa.answer_text, qa.marks_awarded " +
                            "FROM quiz_question qq " +
                            "LEFT JOIN quiz_answer qa ON qa.question_id = qq.id AND qa.submission_id = ? " +
                            "WHERE qq.quiz_id = ? " +
                            "ORDER BY qq.question_order"
            );
            psAns.setLong(1, submissionId);
            psAns.setLong(2, quizId);
            ResultSet rsAns = psAns.executeQuery();
            List<Map<String, Object>> answers = new ArrayList<>();
            while (rsAns.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("order", rsAns.getInt("question_order"));
                row.put("text", rsAns.getString("question_text"));
                row.put("type", rsAns.getString("question_type"));
                row.put("marks", rsAns.getInt("marks"));
                int ci = rsAns.getInt("correct_option_index");
                row.put("correct", rsAns.wasNull() ? null : ci);
                int sel = rsAns.getInt("selected_option_index");
                row.put("selected", rsAns.wasNull() ? null : sel);
                row.put("answerText", rsAns.getString("answer_text"));
                int ma = rsAns.getInt("marks_awarded");
                row.put("awarded", rsAns.wasNull() ? null : ma);
                answers.add(row);
            }
            rsAns.close();
            psAns.close();

            model.addAttribute("answers", answers);
            model.addAttribute("quizId", quizId);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/teacher-quiz/" + quizId;
        }
        return "Teacher/ViewQuizSubmission";
    }

    @GetMapping("/view-subject/{courseid}")
    public String viewsubject(@PathVariable("courseid") int courseid, Model model, HttpSession session){

        try{
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }

            Long teacherId = Long.parseLong(tid.toString());
            model.addAttribute("teacherId", teacherId);

            Connection con = DataBaseConnection.getConnection();

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

            if(rs.next()){
                course.setCourseid(rs.getLong("id"));
                course.setCoursename(rs.getString("course_name"));
                course.setCoursecode(rs.getString("course_code"));
                course.setCredits(rs.getInt("credit"));
                course.setSem(rs.getInt("sem"));
                course.setCoursedept(rs.getString("dept_name"));
            }

            model.addAttribute("course", course);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Teacher/ViewSubject";
    }

    @GetMapping("/profile")
    public String teacherProfile(HttpSession session, Model model) {

        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }

            Long teacherId = Long.parseLong(tid.toString());
            Connection con = DataBaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(

                    "SELECT t.*, " +
                            "p.gender, p.date_of_birth, p.blood_group, p.address, " +
                            "e.department_id, e.designation, e.employment_type, e.joining_date, e.experience_years, e.office_location, e.staff_type, " +
                            "q.phd_status, q.specialization, q.university_name, q.year_of_passing, " +
                            "r.papers_published, r.conferences_attended, r.workshops_attended, r.patents, r.funded_projects, " +
                            "l.casual_leave_balance, l.medical_leave_balance, l.earned_leave_balance " +

                            "FROM teacher t " +
                            "LEFT JOIN teacher_personal_details p ON t.id = p.teacher_id " +
                            "LEFT JOIN teacher_employment_details e ON t.id = e.teacher_id " +
                            "LEFT JOIN teacher_qualification_details q ON t.id = q.teacher_id " +
                            "LEFT JOIN teacher_research_publications r ON t.id = r.teacher_id " +
                            "LEFT JOIN teacher_leave_balance l ON t.id = l.teacher_id " +
                            "WHERE t.id = ?"

            );

            ps.setLong(1, teacherId);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return "redirect:/login";
            }

            TeacherDTO teacher = new TeacherDTO();

            teacher.setId(rs.getLong("id"));
            teacher.setTeacherClgId(rs.getString("teacher_clg_id"));
            teacher.setName(rs.getString("name"));
            teacher.setEmail(rs.getString("email"));
            teacher.setPhone(rs.getString("phone"));

            teacher.setGender(rs.getString("gender"));
            teacher.setDateOfBirth(rs.getDate("date_of_birth"));
            teacher.setBloodGroup(rs.getString("blood_group"));
            teacher.setAddress(rs.getString("address"));

            teacher.setDesignation(rs.getString("designation"));
            teacher.setEmploymentType(rs.getString("employment_type"));
            teacher.setJoiningDate(rs.getDate("joining_date"));
            // DB sometimes stores values like "7 years" instead of a pure number.
            teacher.setExperienceYears(parseLeadingInt(rs.getString("experience_years"), 0));
            teacher.setOfficeLocation(rs.getString("office_location"));
            teacher.setStaffType(rs.getString("staff_type"));

            teacher.setPapersPublished(rs.getInt("papers_published"));
            teacher.setConferencesAttended(rs.getInt("conferences_attended"));
            teacher.setWorkshopsAttended(rs.getInt("workshops_attended"));
            teacher.setPatents(rs.getInt("patents"));
            teacher.setFundedProjects(rs.getInt("funded_projects"));

            teacher.setCasualLeaveBalance(rs.getInt("casual_leave_balance"));
            teacher.setMedicalLeaveBalance(rs.getInt("medical_leave_balance"));
            teacher.setEarnedLeaveBalance(rs.getInt("earned_leave_balance"));

            teacher.setUsername(rs.getString("username"));
            teacher.setAccountStatus(rs.getString("account_status"));
            teacher.setLastLogin(rs.getTimestamp("last_login"));

            teacher.setPhdStatus(rs.getString("phd_status"));
            teacher.setSpecialization(rs.getString("specialization"));
            teacher.setUniversityName(rs.getString("university_name"));
            teacher.setYearOfPassing(rs.getInt("year_of_passing"));

            model.addAttribute("teacher", teacher);

            rs.close();
            ps.close();

            PreparedStatement psQual = con.prepareStatement(
                    "SELECT teacher_id, ug_degree, pg_degree, teacher_qualification_details.phd_status, " +
                            "specialization, university_name, year_of_passing " +
                            "FROM teacher_qualification_details WHERE teacher_id = ?"
            );

            psQual.setLong(1, teacherId);
            ResultSet rsQual = psQual.executeQuery();
            List<TeacherQualificationDTO> qualificationDetails = new ArrayList<>();

            while (rsQual.next()) {
                TeacherQualificationDTO qualification = new TeacherQualificationDTO();
                qualification.setTeacherId(rsQual.getLong("teacher_id"));
                qualification.setUgDegree(rsQual.getString("ug_degree"));
                qualification.setPgDegree(rsQual.getString("pg_degree"));
                qualification.setPhdStatus(rsQual.getString("phd_status"));
                qualification.setSpecialization(rsQual.getString("specialization"));
                qualification.setUniversityName(rsQual.getString("university_name"));
                qualification.setYearOfPassing(rsQual.getInt("year_of_passing"));
                qualificationDetails.add(qualification);
            }

            model.addAttribute("qualificationDetails", qualificationDetails);

            rsQual.close();
            psQual.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }

        return "teacher/profile";
    }



}
