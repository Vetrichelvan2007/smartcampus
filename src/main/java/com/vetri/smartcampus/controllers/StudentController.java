package com.vetri.smartcampus.controllers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;

import com.vetri.smartcampus.models.CourseData;
import com.vetri.smartcampus.models.CourseAssignmentDTO;
import com.vetri.smartcampus.models.CourseDTO;
import com.vetri.smartcampus.models.CourseMaterialDTO;
import com.vetri.smartcampus.models.CourseRegistration;
import com.vetri.smartcampus.models.DataBaseConnection;
import com.vetri.smartcampus.models.FeedbackQuestionDTO;
import com.vetri.smartcampus.models.StudentFeedbackFormListDTO;
import com.vetri.smartcampus.models.QuizQuestionDTO;
import com.vetri.smartcampus.models.StudentQuizListDTO;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class StudentController {

    private static final DateTimeFormatter UI_DTF = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a", Locale.ENGLISH);

    private static String fmt(LocalDateTime dt) {
        return dt == null ? "-" : dt.format(UI_DTF);
    }

    private static String quizStatus(LocalDateTime now, LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && now.isBefore(startAt)) return "UPCOMING";
        if (endAt != null && now.isAfter(endAt)) return "CLOSED";
        return "ACTIVE";
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
                        "AND table_name IN ('course_assignment','course_assignment_submission')"
        );
        ResultSet rs = ps.executeQuery();
        int cnt = 0;
        if (rs.next()) cnt = rs.getInt("cnt");
        rs.close();
        ps.close();
        return cnt == 2;
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

    private static List<CourseAssignmentDTO> loadAssignmentsForCourse(Connection con, long studentId, long courseId) throws Exception {
        List<CourseAssignmentDTO> assignments = new ArrayList<>();
        if (!assignmentTablesExist(con)) return assignments;

        PreparedStatement psA = con.prepareStatement(
                "SELECT ca.id, ca.title, ca.assignment_mode, ca.question_text, ca.instructions, ca.original_filename, " +
                        "ca.file_size, ca.due_at, ca.max_marks, ca.download_allowed, ca.created_at, " +
                        "cas.id AS submission_id, cas.original_filename AS submission_original_filename, cas.submitted_at " +
                        "FROM course_assignment ca " +
                        "JOIN student_course_teacher sct ON sct.course_id = ca.course_id AND sct.teacher_id = ca.teacher_id AND sct.student_id = ? " +
                        "JOIN student st ON st.id = sct.student_id " +
                        "LEFT JOIN course_assignment_submission cas ON cas.assignment_id = ca.id AND cas.student_id = st.id " +
                        "WHERE ca.course_id = ? AND ca.is_published = 1 " +
                        "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                        "AND st.current_semester = sct.semester " +
                        "ORDER BY ca.created_at DESC"
        );
        psA.setLong(1, studentId);
        psA.setLong(2, courseId);
        ResultSet rsA = psA.executeQuery();
        LocalDateTime now = LocalDateTime.now();
        while (rsA.next()) {
            CourseAssignmentDTO a = new CourseAssignmentDTO();
            a.setId(rsA.getLong("id"));
            a.setTitle(rsA.getString("title"));
            a.setAssignmentMode(rsA.getString("assignment_mode"));
            a.setQuestionText(rsA.getString("question_text"));
            a.setInstructions(rsA.getString("instructions"));
            a.setOriginalFileName(rsA.getString("original_filename"));
            long fileSize = rsA.getLong("file_size");
            a.setFileSize(rsA.wasNull() ? null : fileSize);
            java.sql.Timestamp due = rsA.getTimestamp("due_at");
            LocalDateTime dueAt = due == null ? null : due.toLocalDateTime();
            a.setDueAt(dueAt);
            a.setDueAtText(dueAt == null ? "-" : fmt(dueAt));
            int maxMarks = rsA.getInt("max_marks");
            a.setMaxMarks(rsA.wasNull() ? null : maxMarks);
            a.setDownloadAllowed(rsA.getInt("download_allowed") == 1);
            a.setCreatedAt(rsA.getTimestamp("created_at"));
            long submissionId = rsA.getLong("submission_id");
            boolean submitted = !rsA.wasNull();
            a.setSubmitted(submitted);
            a.setSubmissionOriginalFileName(rsA.getString("submission_original_filename"));
            java.sql.Timestamp submittedAt = rsA.getTimestamp("submitted_at");
            a.setSubmittedAtText(submittedAt == null ? "-" : fmt(submittedAt.toLocalDateTime()));
            a.setSubmissionClosed(dueAt != null && now.isAfter(dueAt));
            assignments.add(a);
        }
        rsA.close();
        psA.close();
        return assignments;
    }

    @GetMapping("/student-dashboard")
    public String studentDashboard(HttpSession session) {

        Object sid = session.getAttribute("studentId");
        if (sid == null) {
            return "redirect:/login";
        }
        return "Student/StudentDashboard";
    }

    @GetMapping("/student-profile")
    public String studentprofile(HttpSession session, Model model) {

        Object sidObj = session.getAttribute("studentId");

        if (sidObj == null) {
            return "redirect:/login";
        }

        long studentId = Long.parseLong(sidObj.toString());

        String url = "jdbc:mysql://localhost:3306/smartcampus";
        String dbUser = "root";
        String dbPass = "root";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(url, dbUser, dbPass);

            PreparedStatement student_ps = con.prepareStatement("SELECT s.id, s.roll_number, s.name, s.dob, s.gender, s.blood_group, s.mother_tongue, s.nationality, s.address, s.dept_id, d.dept_name, s.current_year, s.current_semester, s.email, s.phone FROM student s JOIN department d ON s.dept_id = d.id WHERE s.id = ?;");
            student_ps.setLong(1, studentId);
            ResultSet student_rs = student_ps.executeQuery();

            if(student_rs.next()){
                model.addAttribute("studentName",student_rs.getString("name"));
                model.addAttribute("studentRollNumber",student_rs.getString("roll_number"));
                model.addAttribute("studentEmail",student_rs.getString("email"));
                model.addAttribute("studentDepartmet",student_rs.getString("dept_name"));
                model.addAttribute("studentYear",student_rs.getString("current_year"));
                model.addAttribute("studentSem",student_rs.getString("current_semester"));
                model.addAttribute("studentDob",student_rs.getString("dob"));
                model.addAttribute("studentGender",student_rs.getString("gender"));
                model.addAttribute("studentNationality",student_rs.getString("nationality"));
                model.addAttribute("studentBloodGroup",student_rs.getString("blood_group"));
                model.addAttribute("studentMotherTongue",student_rs.getString("mother_tongue"));
                model.addAttribute("studentAddress",student_rs.getString("address"));
                model.addAttribute("studentPhone",student_rs.getString("phone"));
            }


            PreparedStatement father_ps = con.prepareStatement("SELECT * FROM father_details WHERE student_id = ?");
            father_ps.setLong(1, studentId);
            ResultSet father_rs = father_ps.executeQuery();

            if (father_rs.next()) {
                model.addAttribute("fatherName",father_rs.getString("name"));
                model.addAttribute("fatherPhone",father_rs.getString("phone"));
                model.addAttribute("fatherEmail",father_rs.getString("email"));
                model.addAttribute("fatherOccupation",father_rs.getString("occupation"));
                model.addAttribute("fatherAnnual_income",father_rs.getString("annual_income"));
                model.addAttribute("fatherAddress",father_rs.getString("address"));
            }

            PreparedStatement mother_ps = con.prepareStatement("SELECT * FROM mother_details WHERE student_id = ?");
            mother_ps.setLong(1, studentId);
            ResultSet mother_rs = mother_ps.executeQuery();

            if (mother_rs.next()) {
                model.addAttribute("motherName",mother_rs.getString("name"));
                model.addAttribute("motherPhone",mother_rs.getString("phone"));
                model.addAttribute("motherEmail",mother_rs.getString("email"));
                model.addAttribute("motherOccupation",mother_rs.getString("occupation"));
                model.addAttribute("motherAnnual_income",mother_rs.getString("annual_income"));
                model.addAttribute("motherAddress",mother_rs.getString("address"));
            }

            PreparedStatement identity_ps = con.prepareStatement("SELECT * from identity_details where student_id=?");
            identity_ps.setLong(1,studentId);
            ResultSet indentity_rs = identity_ps.executeQuery();

            if(indentity_rs.next()){
                model.addAttribute("aadharNumber",indentity_rs.getString("aadhar_number"));
                model.addAttribute("panNumber",indentity_rs.getString("pan_number"));
                model.addAttribute("passportNumber",indentity_rs.getString("passport_number"));
            }

            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Student/StudentProfile";
    }

    @GetMapping("/student-classroom")
    public String classroom(Model model, HttpSession session) {

        List<CourseData> courseDatas = new ArrayList<>();

        Object deptIdObj = session.getAttribute("department_id");
        Object sidObj = session.getAttribute("studentId");

        if (deptIdObj == null || sidObj == null) {
            return "redirect:/login";
        }

        int deptId = Integer.parseInt(deptIdObj.toString());
        long studentId = Long.parseLong(sidObj.toString());

        int current_sem = 0;

        try {
            Connection con1 = DataBaseConnection.getConnection();
            PreparedStatement ps1 = DataBaseConnection.getPreparedStatement(con1,
                    "SELECT current_semester FROM student WHERE id=?"
            );
            ps1.setLong(1, studentId);

            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                current_sem = rs1.getInt("current_semester");
            }

            rs1.close();
            ps1.close();
            con1.close();

            Connection con2 = DataBaseConnection.getConnection();
            PreparedStatement ps2 = DataBaseConnection.getPreparedStatement(con2,
                    "SELECT d.dept_name, cd.sem, c.course_name, c.course_type, c.course_code " +
                            "FROM course_for_depts cd " +
                            "JOIN course c ON c.id = cd.course_id " +
                            "JOIN department d ON d.id = cd.dept_id " +
                            "WHERE cd.dept_id = ? AND cd.sem <= ? ORDER BY cd.sem"
            );

            ps2.setInt(1, deptId);
            ps2.setInt(2, current_sem);

            ResultSet rs2 = ps2.executeQuery();

            while (rs2.next()) {
                int sem = rs2.getInt("sem");
                    courseDatas.add(new CourseData(
                            rs2.getString("course_name"),
                            rs2.getString("course_code"),
                            rs2.getString("course_type"),
                            sem
                    ));
            }

            model.addAttribute("courseDatas", courseDatas);
            model.addAttribute("current_sem",current_sem);
            rs2.close();
            ps2.close();
            con2.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Student/Classroom";
    }

    @GetMapping("/course/{courseCode}")
    public String course(@PathVariable String courseCode, Model model, HttpSession session) {
        Object sid = session.getAttribute("studentId");
        if (sid == null) {
            return "redirect:/login";
        }
        long studentId = Long.parseLong(sid.toString());

        model.addAttribute("courseId", courseCode); // UI uses this as "Course Code"
        model.addAttribute("resources", List.of());
        model.addAttribute("assignments", List.of());

        try {
            Connection con = DataBaseConnection.getConnection();

            long courseId;
            PreparedStatement ps = DataBaseConnection.getPreparedStatement(con,
                    "SELECT id, course_name, course_code FROM course WHERE course_code=?"
            );
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

            courseId = rs.getLong("id");
            model.addAttribute("courseName", rs.getString("course_name"));
            model.addAttribute("courseDbId", courseId);
            rs.close();
            ps.close();

            List<StudentQuizListDTO> quizzes = new ArrayList<>();
            if (quizTablesExist(con)) {
                PreparedStatement psQ = con.prepareStatement(
                        "SELECT q.id AS quiz_id, q.title, q.start_at, q.end_at, q.is_score_published, " +
                                "CASE WHEN qs.id IS NULL THEN 0 ELSE 1 END AS submitted, qs.score " +
                                "FROM quiz q " +
                                "JOIN student_course_teacher sct ON sct.course_id = q.course_id AND sct.teacher_id = q.teacher_id AND sct.student_id = ? " +
                                "JOIN student st ON st.id = sct.student_id " +
                                "LEFT JOIN quiz_submission qs ON qs.quiz_id = q.id AND qs.student_id = ? " +
                                "WHERE q.course_id = ? " +
                                "AND q.is_published = 1 " +
                                "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                                "AND st.current_semester = sct.semester " +
                                "ORDER BY q.start_at DESC"
                );
                psQ.setLong(1, studentId);
                psQ.setLong(2, studentId);
                psQ.setLong(3, courseId);
                ResultSet rsQ = psQ.executeQuery();

                LocalDateTime now = LocalDateTime.now();
                while (rsQ.next()) {
                    StudentQuizListDTO dto = new StudentQuizListDTO();
                    dto.setQuizId(rsQ.getLong("quiz_id"));
                    dto.setTitle(rsQ.getString("title"));
                    LocalDateTime startAt = rsQ.getTimestamp("start_at").toLocalDateTime();
                    LocalDateTime endAt = rsQ.getTimestamp("end_at").toLocalDateTime();
                    dto.setStartAt(startAt);
                    dto.setEndAt(endAt);
                    dto.setStartAtText(fmt(startAt));
                    dto.setEndAtText(fmt(endAt));
                    dto.setStatus(quizStatus(now, startAt, endAt));
                    dto.setScorePublished(rsQ.getInt("is_score_published") == 1);
                    dto.setSubmitted(rsQ.getInt("submitted") == 1);
                    int sc = rsQ.getInt("score");
                    dto.setScore(rsQ.wasNull() ? null : sc);
                    quizzes.add(dto);
                }
                rsQ.close();
                psQ.close();
            }

            // Course materials (resources) uploaded by the teacher the student selected for this course.
            List<CourseMaterialDTO> resources = new ArrayList<>();
            if (materialTablesExist(con)) {
                PreparedStatement psM = con.prepareStatement(
                        "SELECT cm.id, cm.title, cm.module, cm.material_type, cm.original_filename, cm.file_size, cm.download_allowed, cm.created_at " +
                                "FROM course_material cm " +
                                "JOIN student_course_teacher sct ON sct.course_id = cm.course_id AND sct.teacher_id = cm.teacher_id AND sct.student_id = ? " +
                                "JOIN student st ON st.id = sct.student_id " +
                                "WHERE cm.course_id = ? " +
                                "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                                "AND st.current_semester = sct.semester " +
                                "ORDER BY cm.created_at DESC"
                );
                psM.setLong(1, studentId);
                psM.setLong(2, courseId);
                ResultSet rsM = psM.executeQuery();
                while (rsM.next()) {
                    CourseMaterialDTO r = new CourseMaterialDTO();
                    r.setId(rsM.getLong("id"));
                    r.setTitle(rsM.getString("title"));
                    r.setModule(rsM.getString("module"));
                    r.setType(rsM.getString("material_type"));
                    r.setOriginalFileName(rsM.getString("original_filename"));
                    long sz = rsM.getLong("file_size");
                    r.setFileSize(rsM.wasNull() ? null : sz);
                    r.setDownloadAllowed(rsM.getInt("download_allowed") == 1);
                    java.sql.Timestamp up = rsM.getTimestamp("created_at");
                    r.setUploadedAt(up);
                    r.setUploadedAtText(up == null ? "-" : fmt(up.toLocalDateTime()));
                    resources.add(r);
                }
                rsM.close();
                psM.close();
            }
            model.addAttribute("resources", resources);
            model.addAttribute("assignments", loadAssignmentsForCourse(con, studentId, courseId));

            model.addAttribute("quizzes", quizzes);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("quizzes", List.of());
        }

        return "Student/Course";
    }

    @GetMapping("/course/{id}/quizzes")
    public String quizzes(@PathVariable("id") long courseId, HttpSession session, Model model) {
        Object sid = session.getAttribute("studentId");
        if (sid == null) {
            return "redirect:/login";
        }
        long studentId = Long.parseLong(sid.toString());

        model.addAttribute("courseId", String.valueOf(courseId));
        model.addAttribute("activeTab", "quizzes");
        model.addAttribute("resources", List.of());
        model.addAttribute("assignments", List.of());

        try {
            Connection con = DataBaseConnection.getConnection();

            PreparedStatement psCourse = con.prepareStatement(
                    "SELECT course_name, course_code FROM course WHERE id = ?"
            );
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

            List<StudentQuizListDTO> quizzes = new ArrayList<>();
            if (quizTablesExist(con)) {
                PreparedStatement ps = con.prepareStatement(
                        "SELECT q.id AS quiz_id, q.title, q.start_at, q.end_at, q.is_score_published, " +
                                "CASE WHEN qs.id IS NULL THEN 0 ELSE 1 END AS submitted, qs.score " +
                                "FROM quiz q " +
                                "JOIN student_course_teacher sct ON sct.course_id = q.course_id AND sct.teacher_id = q.teacher_id AND sct.student_id = ? " +
                                "JOIN student st ON st.id = sct.student_id " +
                                "LEFT JOIN quiz_submission qs ON qs.quiz_id = q.id AND qs.student_id = ? " +
                                "WHERE q.course_id = ? " +
                                "AND q.is_published = 1 " +
                                "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                                "AND st.current_semester = sct.semester " +
                                "ORDER BY q.start_at DESC"
                );
                ps.setLong(1, studentId);
                ps.setLong(2, studentId);
                ps.setLong(3, courseId);
                ResultSet rs = ps.executeQuery();
                LocalDateTime now = LocalDateTime.now();
                while (rs.next()) {
                    StudentQuizListDTO dto = new StudentQuizListDTO();
                    dto.setQuizId(rs.getLong("quiz_id"));
                    dto.setTitle(rs.getString("title"));
                    LocalDateTime startAt = rs.getTimestamp("start_at").toLocalDateTime();
                    LocalDateTime endAt = rs.getTimestamp("end_at").toLocalDateTime();
                    dto.setStartAt(startAt);
                    dto.setEndAt(endAt);
                    dto.setStartAtText(fmt(startAt));
                    dto.setEndAtText(fmt(endAt));
                    dto.setStatus(quizStatus(now, startAt, endAt));
                    dto.setScorePublished(rs.getInt("is_score_published") == 1);
                    dto.setSubmitted(rs.getInt("submitted") == 1);
                    int sc = rs.getInt("score");
                    dto.setScore(rs.wasNull() ? null : sc);
                    quizzes.add(dto);
                }
                rs.close();
                ps.close();
            }

            // Course materials (resources) uploaded by the teacher the student selected for this course.
            List<CourseMaterialDTO> resources = new ArrayList<>();
            if (materialTablesExist(con)) {
                PreparedStatement psM = con.prepareStatement(
                        "SELECT cm.id, cm.title, cm.module, cm.material_type, cm.original_filename, cm.file_size, cm.download_allowed, cm.created_at " +
                                "FROM course_material cm " +
                                "JOIN student_course_teacher sct ON sct.course_id = cm.course_id AND sct.teacher_id = cm.teacher_id AND sct.student_id = ? " +
                                "JOIN student st ON st.id = sct.student_id " +
                                "WHERE cm.course_id = ? " +
                                "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                                "AND st.current_semester = sct.semester " +
                                "ORDER BY cm.created_at DESC"
                );
                psM.setLong(1, studentId);
                psM.setLong(2, courseId);
                ResultSet rsM = psM.executeQuery();
                while (rsM.next()) {
                    CourseMaterialDTO r = new CourseMaterialDTO();
                    r.setId(rsM.getLong("id"));
                    r.setTitle(rsM.getString("title"));
                    r.setModule(rsM.getString("module"));
                    r.setType(rsM.getString("material_type"));
                    r.setOriginalFileName(rsM.getString("original_filename"));
                    long sz = rsM.getLong("file_size");
                    r.setFileSize(rsM.wasNull() ? null : sz);
                    r.setDownloadAllowed(rsM.getInt("download_allowed") == 1);
                    java.sql.Timestamp up = rsM.getTimestamp("created_at");
                    r.setUploadedAt(up);
                    r.setUploadedAtText(up == null ? "-" : fmt(up.toLocalDateTime()));
                    resources.add(r);
                }
                rsM.close();
                psM.close();
            }
            model.addAttribute("resources", resources);
            model.addAttribute("assignments", loadAssignmentsForCourse(con, studentId, courseId));

            model.addAttribute("quizzes", quizzes);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("quizzes", List.of());
        }

        return "Student/Course";
    }

    @GetMapping("/quiz/{quizId}")
    public String takeQuiz(@PathVariable("quizId") long quizId, HttpSession session, Model model) {
        Object sid = session.getAttribute("studentId");
        if (sid == null) return "redirect:/login";
        long studentId = Long.parseLong(sid.toString());

        try {
            Connection con = DataBaseConnection.getConnection();
            if (!quizTablesExist(con)) {
                con.close();
                return "redirect:/student-dashboard";
            }

            PreparedStatement psQuiz = con.prepareStatement(
                    "SELECT q.id, q.title, q.instructions, q.duration_minutes, q.total_marks, q.start_at, q.end_at, q.is_score_published, " +
                            "q.course_id, c.course_name, c.course_code, t.name AS teacher_name, " +
                            "qs.id AS submission_id, qs.score " +
                            "FROM quiz q " +
                            "JOIN course c ON c.id = q.course_id " +
                            "JOIN teacher t ON t.id = q.teacher_id " +
                            "JOIN student_course_teacher sct ON sct.course_id = q.course_id AND sct.teacher_id = q.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "LEFT JOIN quiz_submission qs ON qs.quiz_id = q.id AND qs.student_id = ? " +
                            "WHERE q.id = ? AND q.is_published = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            );
            psQuiz.setLong(1, studentId);
            psQuiz.setLong(2, studentId);
            psQuiz.setLong(3, quizId);
            ResultSet rsQuiz = psQuiz.executeQuery();
            if (!rsQuiz.next()) {
                rsQuiz.close();
                psQuiz.close();
                con.close();
                return "redirect:/student-dashboard";
            }

            model.addAttribute("quizId", quizId);
            model.addAttribute("quizTitle", rsQuiz.getString("title"));
            model.addAttribute("instructions", rsQuiz.getString("instructions"));
            model.addAttribute("durationMinutes", rsQuiz.getInt("duration_minutes"));
            model.addAttribute("totalMarks", rsQuiz.getInt("total_marks"));
            boolean scorePublished = rsQuiz.getInt("is_score_published") == 1;
            model.addAttribute("scorePublished", scorePublished);
            LocalDateTime startAt = rsQuiz.getTimestamp("start_at").toLocalDateTime();
            LocalDateTime endAt = rsQuiz.getTimestamp("end_at").toLocalDateTime();
            model.addAttribute("startAt", startAt);
            model.addAttribute("endAt", endAt);
            model.addAttribute("startAtText", fmt(startAt));
            model.addAttribute("endAtText", fmt(endAt));
            model.addAttribute("courseName", rsQuiz.getString("course_name"));
            model.addAttribute("courseCode", rsQuiz.getString("course_code"));
            model.addAttribute("teacherName", rsQuiz.getString("teacher_name"));

            long courseId = rsQuiz.getLong("course_id");
            model.addAttribute("courseId", courseId);

            long submissionId = rsQuiz.getLong("submission_id");
            boolean alreadySubmitted = !rsQuiz.wasNull();
            model.addAttribute("alreadySubmitted", alreadySubmitted);
            int score = rsQuiz.getInt("score");
            model.addAttribute("score", rsQuiz.wasNull() ? null : score);

            rsQuiz.close();
            psQuiz.close();

            LocalDateTime now = LocalDateTime.now();
            boolean inWindow = (now.isAfter(startAt) || now.isEqual(startAt)) && (now.isBefore(endAt) || now.isEqual(endAt));
            model.addAttribute("inWindow", inWindow);

            List<QuizQuestionDTO> questions = new ArrayList<>();
            if (!alreadySubmitted && inWindow) {
                PreparedStatement psQ = con.prepareStatement(
                        "SELECT id, question_order, question_text, question_type, marks " +
                                "FROM quiz_question WHERE quiz_id = ? ORDER BY question_order"
                );
                psQ.setLong(1, quizId);
                ResultSet rsQ = psQ.executeQuery();
                while (rsQ.next()) {
                    QuizQuestionDTO q = new QuizQuestionDTO();
                    q.setId(rsQ.getLong("id"));
                    q.setOrder(rsQ.getInt("question_order"));
                    q.setText(rsQ.getString("question_text"));
                    q.setType(rsQ.getString("question_type"));
                    q.setMarks(rsQ.getInt("marks"));

                    if ("MCQ".equalsIgnoreCase(q.getType())) {
                        PreparedStatement psOpt = con.prepareStatement(
                                "SELECT option_index, option_text FROM quiz_option WHERE question_id = ? ORDER BY option_index"
                        );
                        psOpt.setLong(1, q.getId());
                        ResultSet rsOpt = psOpt.executeQuery();
                        List<String> opts = new ArrayList<>();
                        while (rsOpt.next()) {
                            opts.add(rsOpt.getString("option_text"));
                        }
                        rsOpt.close();
                        psOpt.close();
                        q.setOptions(opts);
                    }

                    questions.add(q);
                }
                rsQ.close();
                psQ.close();
            }

            model.addAttribute("questions", questions);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/student-dashboard";
        }

        return "Student/TakeQuiz";
    }

    @PostMapping("/quiz/{quizId}/submit")
    public String submitQuiz(@PathVariable("quizId") long quizId, HttpSession session, @RequestParam Map<String, String> params) {
        Object sid = session.getAttribute("studentId");
        if (sid == null) return "redirect:/login";
        long studentId = Long.parseLong(sid.toString());

        try {
            Connection con = DataBaseConnection.getConnection();
            con.setAutoCommit(false);

            if (!quizTablesExist(con)) {
                con.rollback();
                con.close();
                return "redirect:/student-dashboard";
            }

            PreparedStatement psCheck = con.prepareStatement(
                    "SELECT q.course_id, q.teacher_id, q.start_at, q.end_at " +
                            "FROM quiz q " +
                            "JOIN student_course_teacher sct ON sct.course_id = q.course_id AND sct.teacher_id = q.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE q.id = ? AND q.is_published = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            );
            psCheck.setLong(1, studentId);
            psCheck.setLong(2, quizId);
            ResultSet rsCheck = psCheck.executeQuery();
            if (!rsCheck.next()) {
                rsCheck.close();
                psCheck.close();
                con.rollback();
                con.close();
                return "redirect:/student-dashboard";
            }
            LocalDateTime startAt = rsCheck.getTimestamp("start_at").toLocalDateTime();
            LocalDateTime endAt = rsCheck.getTimestamp("end_at").toLocalDateTime();
            rsCheck.close();
            psCheck.close();

            LocalDateTime now = LocalDateTime.now();
            boolean inWindow = (now.isAfter(startAt) || now.isEqual(startAt)) && (now.isBefore(endAt) || now.isEqual(endAt));
            if (!inWindow) {
                con.rollback();
                con.close();
                return "redirect:/quiz/" + quizId;
            }

            PreparedStatement psAlready = con.prepareStatement(
                    "SELECT 1 FROM quiz_submission WHERE quiz_id = ? AND student_id = ? LIMIT 1"
            );
            psAlready.setLong(1, quizId);
            psAlready.setLong(2, studentId);
            ResultSet rsAlready = psAlready.executeQuery();
            boolean already = rsAlready.next();
            rsAlready.close();
            psAlready.close();
            if (already) {
                con.rollback();
                con.close();
                return "redirect:/quiz/" + quizId;
            }

            PreparedStatement psSub = con.prepareStatement(
                    "INSERT INTO quiz_submission (quiz_id, student_id, score) VALUES (?, ?, 0)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
            );
            psSub.setLong(1, quizId);
            psSub.setLong(2, studentId);
            psSub.executeUpdate();
            ResultSet keys = psSub.getGeneratedKeys();
            if (!keys.next()) {
                keys.close();
                psSub.close();
                con.rollback();
                con.close();
                return "redirect:/quiz/" + quizId;
            }
            long submissionId = keys.getLong(1);
            keys.close();
            psSub.close();

            PreparedStatement psQ = con.prepareStatement(
                    "SELECT id, question_type, marks, correct_option_index " +
                            "FROM quiz_question WHERE quiz_id = ? ORDER BY question_order"
            );
            psQ.setLong(1, quizId);
            ResultSet rsQ = psQ.executeQuery();

            PreparedStatement psAns = con.prepareStatement(
                    "INSERT INTO quiz_answer (submission_id, question_id, selected_option_index, answer_text, marks_awarded) VALUES (?, ?, ?, ?, ?)"
            );

            int totalScore = 0;
            while (rsQ.next()) {
                long qid = rsQ.getLong("id");
                String type = rsQ.getString("question_type");
                int marks = rsQ.getInt("marks");
                int corr = rsQ.getInt("correct_option_index");
                Integer correct = rsQ.wasNull() ? null : corr;

                String key = "q_" + qid;
                String raw = params.get(key);

                psAns.setLong(1, submissionId);
                psAns.setLong(2, qid);

                if (type != null && type.equalsIgnoreCase("MCQ")) {
                    Integer selected = null;
                    try {
                        if (raw != null && !raw.trim().isEmpty()) selected = Integer.parseInt(raw.trim());
                    } catch (Exception ignored) {}
                    int awarded = (selected != null && correct != null && selected.intValue() == correct.intValue()) ? marks : 0;
                    totalScore += awarded;

                    if (selected == null) psAns.setObject(3, null);
                    else psAns.setInt(3, selected);
                    psAns.setString(4, null);
                    psAns.setInt(5, awarded);
                } else {
                    String text = raw == null ? null : raw.trim();
                    psAns.setObject(3, null);
                    psAns.setString(4, text);
                    psAns.setObject(5, null);
                }
                psAns.addBatch();
            }

            rsQ.close();
            psQ.close();

            psAns.executeBatch();
            psAns.close();

            PreparedStatement psUpd = con.prepareStatement(
                    "UPDATE quiz_submission SET score = ? WHERE id = ?"
            );
            psUpd.setInt(1, totalScore);
            psUpd.setLong(2, submissionId);
            psUpd.executeUpdate();
            psUpd.close();

            con.commit();
            con.close();

            return "redirect:/quiz/" + quizId;
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/quiz/" + quizId;
        }
    }

    @GetMapping("/material/{id}/download")
    public void downloadMaterial(@PathVariable("id") long materialId, HttpSession session, HttpServletResponse response) {
        serveMaterial(materialId, session, response, true, true);
    }

    @GetMapping("/material/{id}/view")
    public void viewMaterial(@PathVariable("id") long materialId, HttpSession session, HttpServletResponse response) {
        serveMaterial(materialId, session, response, false, false);
    }

    private void serveMaterial(long materialId,
                               HttpSession session,
                               HttpServletResponse response,
                               boolean asAttachment,
                               boolean requireDownloadAllowed) {
        Object sid = session.getAttribute("studentId");
        if (sid == null) {
            try { response.sendRedirect("/login"); } catch (Exception ignored) {}
            return;
        }
        long studentId = Long.parseLong(sid.toString());

        try {
            Connection con = DataBaseConnection.getConnection();
            if (!materialTablesExist(con)) {
                con.close();
                response.sendError(404);
                return;
            }

            PreparedStatement ps = con.prepareStatement(
                    "SELECT cm.stored_path, cm.original_filename, cm.mime_type, cm.download_allowed " +
                            "FROM course_material cm " +
                            "JOIN student_course_teacher sct ON sct.course_id = cm.course_id AND sct.teacher_id = cm.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE cm.id = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            );
            ps.setLong(1, studentId);
            ps.setLong(2, materialId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                con.close();
                response.sendError(404);
                return;
            }

            boolean allowed = rs.getInt("download_allowed") == 1;
            String storedPath = rs.getString("stored_path");
            String original = rs.getString("original_filename");
            String mime = rs.getString("mime_type");
            rs.close();
            ps.close();
            con.close();

            if (requireDownloadAllowed && !allowed) {
                response.sendError(403);
                return;
            }
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
            response.setHeader("Content-Disposition", (asAttachment ? "attachment" : "inline") + "; filename=\"" + safe + "\"");
            Files.copy(file, response.getOutputStream());
        } catch (Exception e) {
            try { response.sendError(500); } catch (Exception ignored) {}
        }
    }

    @GetMapping("/assignment/{id}/download")
    public void downloadAssignment(@PathVariable("id") long assignmentId, HttpSession session, HttpServletResponse response) {
        serveAssignmentQuestionFile(assignmentId, session, response, true, true);
    }

    @GetMapping("/assignment/{id}/view")
    public void viewAssignment(@PathVariable("id") long assignmentId, HttpSession session, HttpServletResponse response) {
        serveAssignmentQuestionFile(assignmentId, session, response, false, false);
    }

    @PostMapping("/assignment/{id}/submit")
    public String submitAssignment(@PathVariable("id") long assignmentId,
                                   HttpSession session,
                                   @RequestParam("submissionFile") MultipartFile submissionFile) {
        Object sid = session.getAttribute("studentId");
        if (sid == null) return "redirect:/login";
        long studentId = Long.parseLong(sid.toString());

        try {
            Connection con = DataBaseConnection.getConnection();
            if (!assignmentTablesExist(con)) {
                con.close();
                return "redirect:/student-dashboard";
            }

            PreparedStatement ps = con.prepareStatement(
                    "SELECT ca.title, ca.due_at, c.course_code, s.roll_number " +
                            "FROM course_assignment ca " +
                            "JOIN course c ON c.id = ca.course_id " +
                            "JOIN student_course_teacher sct ON sct.course_id = ca.course_id AND sct.teacher_id = ca.teacher_id AND sct.student_id = ? " +
                            "JOIN student s ON s.id = sct.student_id " +
                            "WHERE ca.id = ? AND ca.is_published = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND s.current_semester = sct.semester"
            );
            ps.setLong(1, studentId);
            ps.setLong(2, assignmentId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                con.close();
                return "redirect:/student-dashboard";
            }
            String assignmentTitle = rs.getString("title");
            java.sql.Timestamp dueTs = rs.getTimestamp("due_at");
            String courseCode = rs.getString("course_code");
            String rollNumber = rs.getString("roll_number");
            rs.close();
            ps.close();

            LocalDateTime dueAt = dueTs == null ? null : dueTs.toLocalDateTime();
            if (dueAt != null && LocalDateTime.now().isAfter(dueAt)) {
                con.close();
                return "redirect:/course/" + courseCode;
            }
            if (submissionFile == null || submissionFile.isEmpty()) {
                con.close();
                return "redirect:/course/" + courseCode;
            }

            String original = submissionFile.getOriginalFilename();
            String extension = "";
            if (original != null) {
                int dot = original.lastIndexOf('.');
                if (dot >= 0) extension = original.substring(dot);
            }
            String safeTitle = (assignmentTitle == null ? "assignment" : assignmentTitle).replaceAll("[^a-zA-Z0-9._-]", "_");
            String safeRoll = (rollNumber == null ? "student" : rollNumber).replaceAll("[^a-zA-Z0-9._-]", "_");
            String stored = safeTitle + "_" + safeRoll + extension;
            Path base = Paths.get("Assignment", "Submissions").toAbsolutePath().normalize();
            Files.createDirectories(base);
            Path dest = base.resolve(stored).normalize();
            if (!dest.startsWith(base)) {
                con.close();
                return "redirect:/course/" + courseCode;
            }

            PreparedStatement psExisting = con.prepareStatement(
                    "SELECT id, stored_path FROM course_assignment_submission WHERE assignment_id = ? AND student_id = ?"
            );
            psExisting.setLong(1, assignmentId);
            psExisting.setLong(2, studentId);
            ResultSet rsExisting = psExisting.executeQuery();
            Long submissionId = null;
            String oldPath = null;
            if (rsExisting.next()) {
                submissionId = rsExisting.getLong("id");
                oldPath = rsExisting.getString("stored_path");
            }
            rsExisting.close();
            psExisting.close();

            Files.copy(submissionFile.getInputStream(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            if (submissionId == null) {
                PreparedStatement psIns = con.prepareStatement(
                        "INSERT INTO course_assignment_submission (assignment_id, student_id, original_filename, stored_filename, stored_path, mime_type, file_size) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)"
                );
                psIns.setLong(1, assignmentId);
                psIns.setLong(2, studentId);
                psIns.setString(3, original == null ? stored : original);
                psIns.setString(4, stored);
                psIns.setString(5, dest.toString());
                psIns.setString(6, submissionFile.getContentType());
                psIns.setLong(7, submissionFile.getSize());
                psIns.executeUpdate();
                psIns.close();
            } else {
                PreparedStatement psUpd = con.prepareStatement(
                        "UPDATE course_assignment_submission " +
                                "SET original_filename = ?, stored_filename = ?, stored_path = ?, mime_type = ?, file_size = ?, submitted_at = CURRENT_TIMESTAMP " +
                                "WHERE id = ?"
                );
                psUpd.setString(1, original == null ? stored : original);
                psUpd.setString(2, stored);
                psUpd.setString(3, dest.toString());
                psUpd.setString(4, submissionFile.getContentType());
                psUpd.setLong(5, submissionFile.getSize());
                psUpd.setLong(6, submissionId);
                psUpd.executeUpdate();
                psUpd.close();
                if (oldPath != null && !oldPath.equals(dest.toString())) {
                    try { Files.deleteIfExists(Paths.get(oldPath)); } catch (Exception ignored) {}
                }
            }

            con.close();
            return "redirect:/course/" + courseCode;
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/student-dashboard";
        }
    }

    private void serveAssignmentQuestionFile(long assignmentId,
                                             HttpSession session,
                                             HttpServletResponse response,
                                             boolean asAttachment,
                                             boolean requireDownloadAllowed) {
        Object sid = session.getAttribute("studentId");
        if (sid == null) {
            try { response.sendRedirect("/login"); } catch (Exception ignored) {}
            return;
        }
        long studentId = Long.parseLong(sid.toString());

        try {
            Connection con = DataBaseConnection.getConnection();
            if (!assignmentTablesExist(con)) {
                con.close();
                response.sendError(404);
                return;
            }

            PreparedStatement ps = con.prepareStatement(
                    "SELECT ca.stored_path, ca.original_filename, ca.mime_type, ca.download_allowed " +
                            "FROM course_assignment ca " +
                            "JOIN student_course_teacher sct ON sct.course_id = ca.course_id AND sct.teacher_id = ca.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE ca.id = ? AND ca.is_published = 1 " +
                            "AND ca.assignment_mode = 'FILE' " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            );
            ps.setLong(1, studentId);
            ps.setLong(2, assignmentId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                con.close();
                response.sendError(404);
                return;
            }

            boolean allowed = rs.getInt("download_allowed") == 1;
            String storedPath = rs.getString("stored_path");
            String original = rs.getString("original_filename");
            String mime = rs.getString("mime_type");
            rs.close();
            ps.close();
            con.close();

            if (requireDownloadAllowed && !allowed) {
                response.sendError(403);
                return;
            }
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
            response.setHeader("Content-Disposition", (asAttachment ? "attachment" : "inline") + "; filename=\"" + safe + "\"");
            Files.copy(file, response.getOutputStream());
        } catch (Exception e) {
            try { response.sendError(500); } catch (Exception ignored) {}
        }
    }

    @GetMapping("/courseregistration")
    public String courseregistration(HttpSession session, Model model){

        try{
            Connection con = DataBaseConnection.getConnection();

            Object sid = session.getAttribute("studentId");
            if (sid == null) {
                return "redirect:/login";
            }

            Long studentId = Long.parseLong(sid.toString());

            PreparedStatement ps1 = DataBaseConnection.getPreparedStatement(con, "SELECT dept_id, current_semester FROM student WHERE id=?");
            ps1.setLong(1, studentId);
            ResultSet rs1 = ps1.executeQuery();

            Long deptId = null;
            int currentSemester = 1;

            if (rs1.next()) {
                deptId = rs1.getLong("dept_id");
                currentSemester = rs1.getInt("current_semester");
            } else {
                return "redirect:/login";
            }

            PreparedStatement ps0 = DataBaseConnection.getPreparedStatement(con,"select student_id from student_course_teacher where student_id =? and semester = ?");
            ps0.setLong(1,studentId);
            ps0.setInt(2, currentSemester);

            ResultSet rs0 = ps0.executeQuery();

            if(rs0.next()) {
                return "redirect:/registered-course";
            }

            model.addAttribute("current_semester", currentSemester);

            PreparedStatement ps2 = DataBaseConnection.getPreparedStatement(con,
                    "SELECT c.id, c.course_code, c.course_name, c.course_type, cfd.sem, " +
                            "t.id, t.name " +
                            "FROM course_for_depts cfd " +
                            "JOIN course c ON c.id = cfd.course_id " +
                            "LEFT JOIN course_teacher_allocation cta ON c.id = cta.course_id " +
                            "LEFT JOIN teacher t ON cta.teacher_id = t.id " +
                            "WHERE cfd.dept_id = ? AND cfd.sem = ? " +
                            "ORDER BY c.id");

            ps2.setLong(1, deptId);
            ps2.setInt(2, currentSemester);

            ResultSet rs2 = ps2.executeQuery();

            Map<Long, CourseRegistration> courseMap = new LinkedHashMap<>();

            while(rs2.next()){

                Long courseId = rs2.getLong(1);
                String courseCode = rs2.getString(2);
                String courseName = rs2.getString(3);
                String courseType = rs2.getString(4);
                int courseSem = rs2.getInt(5);


                Long teacherId = (Long) rs2.getObject(6);
                String teacherName = rs2.getString(7);

                CourseRegistration course = courseMap.get(courseId);

                if(course == null){
                    course = new CourseRegistration(courseId, courseName, courseType);
                    course.setCourseCode(courseCode);
                    course.setCourseSem(courseSem);
                    course.setTeacherIds(new ArrayList<>());
                    course.setTeacherNames(new ArrayList<>());
                    courseMap.put(courseId, course);
                }

                if (teacherId != null && teacherName != null) {
                    course.getTeacherIds().add(teacherId);
                    course.getTeacherNames().add(teacherName);
                }
            }

            List<CourseRegistration> courses =
                    new ArrayList<>(courseMap.values());

            model.addAttribute("courses", courses);

        } catch(Exception e){
            e.printStackTrace();
        }
        return "Student/CourseRegistration";
    }

    @PostMapping("/courseregistraction-submit")
    public String handleCourseRegistration(@RequestBody List<CourseDTO> selectedCourses,HttpSession session) {

        try {
            Object sid = session.getAttribute("studentId");
            if (sid == null) return "redirect:/login";

            Long studentId = Long.parseLong(sid.toString());
            Connection con = DataBaseConnection.getConnection();
            con.setAutoCommit(false);

            PreparedStatement getStudent = DataBaseConnection.getPreparedStatement(con,"SELECT current_semester FROM student WHERE id = ?");
            getStudent.setLong(1, studentId);

            ResultSet rs = getStudent.executeQuery();

            if (!rs.next()) return "redirect:/login";

            int currentSem = rs.getInt("current_semester");

            PreparedStatement ps = DataBaseConnection.getPreparedStatement(con,
                    "INSERT INTO student_course_teacher " +
                            "(student_id, course_id, teacher_id, semester, status) " +
                            "VALUES (?, ?, ?, ?, ?)");

            for (CourseDTO dto : selectedCourses) {
                ps.setLong(1, studentId);
                ps.setLong(2, dto.getCourseId());
                ps.setLong(3, dto.getTeacherId());
                ps.setInt(4, currentSem);
                ps.setString(5, "ACTIVE");
                ps.addBatch();
            }

            ps.executeBatch();
            con.commit();

            return "redirect:/registered-course";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/courseregistration";
        }
    }

    @GetMapping("/registered-course")
    public String registeredcourse(HttpSession session, Model model) {

        try {
            Object sid = session.getAttribute("studentId");
            if (sid == null) {
                return "redirect:/login";
            }

            Long studentId = Long.parseLong(sid.toString());
            Connection con = DataBaseConnection.getConnection();


            PreparedStatement ps1 = DataBaseConnection.getPreparedStatement(con, "SELECT current_semester FROM student WHERE id = ?");
            ps1.setLong(1, studentId);
            ResultSet rs1 = ps1.executeQuery();

            int currentSemester = 1;

            if (rs1.next()) {
                currentSemester = rs1.getInt("current_semester");
            } else {
                return "redirect:/login";
            }

            model.addAttribute("current_semester", currentSemester);

            // 🔹 2️⃣ Fetch registered courses
            PreparedStatement ps2 = DataBaseConnection.getPreparedStatement(con,
                    "SELECT c.id, c.course_code, c.course_name, c.course_type, sct.semester, " +
                            "t.id, t.name " +
                            "FROM student_course_teacher sct " +
                            "JOIN course c ON sct.course_id = c.id " +
                            "LEFT JOIN teacher t ON sct.teacher_id = t.id " +
                            "WHERE sct.student_id = ? AND sct.semester = ? " +
                            "ORDER BY c.id");

            ps2.setLong(1, studentId);
            ps2.setInt(2, currentSemester);

            ResultSet rs2 = ps2.executeQuery();

            Map<Long, CourseRegistration> courseMap = new LinkedHashMap<>();

            while (rs2.next()) {

                Long courseId = rs2.getLong(1);
                String courseCode = rs2.getString(2);
                String courseName = rs2.getString(3);
                String courseType = rs2.getString(4);
                int courseSem = rs2.getInt(5);

                Long teacherId = (Long) rs2.getObject(6);
                String teacherName = rs2.getString(7);

                CourseRegistration course = courseMap.get(courseId);

                if (course == null) {
                    course = new CourseRegistration(courseId, courseName, courseType);
                    course.setCourseCode(courseCode);
                    course.setCourseSem(courseSem);
                    course.setTeacherIds(new ArrayList<>());
                    course.setTeacherNames(new ArrayList<>());
                    courseMap.put(courseId, course);
                }

                if (teacherId != null && teacherName != null) {
                    course.getTeacherIds().add(teacherId);
                    course.getTeacherNames().add(teacherName);
                }
            }

            List<CourseRegistration> courses =
                    new ArrayList<>(courseMap.values());

            model.addAttribute("courses", courses);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Student/RegisteredCourse";
    }

    @GetMapping("/feedback")
    public String feedback(HttpSession session, Model model){
        try {
            Object sid = session.getAttribute("studentId");
            if (sid == null) {
                return "redirect:/login";
            }
            long studentId = Long.parseLong(sid.toString());

            Connection con = DataBaseConnection.getConnection();
            if (!feedbackTablesExist(con)) {
                con.close();
                model.addAttribute("feedbackDbMissing", true);
                model.addAttribute("forms", List.of());
                return "Student/Feedback";
            }

            PreparedStatement ps = con.prepareStatement(
                    "SELECT ff.id AS form_id, ff.title, ff.description, ff.created_at, " +
                            "c.course_name, c.course_code, t.name AS teacher_name, " +
                            "CASE WHEN fs.id IS NULL THEN 0 ELSE 1 END AS submitted " +
                            "FROM feedback_form ff " +
                            "JOIN student_course_teacher sct ON sct.course_id = ff.course_id AND sct.teacher_id = ff.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "JOIN course c ON c.id = ff.course_id " +
                            "JOIN teacher t ON t.id = ff.teacher_id " +
                            "LEFT JOIN feedback_submission fs ON fs.form_id = ff.id AND fs.student_id = ? " +
                            "WHERE ff.is_active = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester " +
                            "ORDER BY ff.created_at DESC"
            );
            ps.setLong(1, studentId);
            ps.setLong(2, studentId);

            ResultSet rs = ps.executeQuery();
            List<StudentFeedbackFormListDTO> forms = new ArrayList<>();
            while (rs.next()) {
                StudentFeedbackFormListDTO dto = new StudentFeedbackFormListDTO();
                dto.setFormId(rs.getLong("form_id"));
                dto.setTitle(rs.getString("title"));
                dto.setDescription(rs.getString("description"));
                dto.setCourseName(rs.getString("course_name"));
                dto.setCourseCode(rs.getString("course_code"));
                dto.setTeacherName(rs.getString("teacher_name"));
                dto.setSubmitted(rs.getInt("submitted") == 1);
                dto.setCreatedAt(rs.getTimestamp("created_at"));
                forms.add(dto);
            }
            rs.close();
            ps.close();
            con.close();

            model.addAttribute("forms", forms);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Student/Feedback";
    }

    @GetMapping("/feedback/{formId}")
    public String viewFeedbackForm(@PathVariable("formId") long formId, HttpSession session, Model model) {
        try {
            Object sid = session.getAttribute("studentId");
            if (sid == null) {
                return "redirect:/login";
            }
            long studentId = Long.parseLong(sid.toString());

            Connection con = DataBaseConnection.getConnection();
            if (!feedbackTablesExist(con)) {
                con.close();
                return "redirect:/feedback";
            }

            // Ensure this form belongs to a course/teacher the student selected.
            PreparedStatement psForm = con.prepareStatement(
                    "SELECT ff.id, ff.title, ff.description, ff.course_id, ff.teacher_id, c.course_name, c.course_code, t.name AS teacher_name " +
                            "FROM feedback_form ff " +
                            "JOIN course c ON c.id = ff.course_id " +
                            "JOIN teacher t ON t.id = ff.teacher_id " +
                            "JOIN student_course_teacher sct ON sct.course_id = ff.course_id AND sct.teacher_id = ff.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE ff.id = ? AND ff.is_active = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            );
            psForm.setLong(1, studentId);
            psForm.setLong(2, formId);
            ResultSet rsForm = psForm.executeQuery();
            if (!rsForm.next()) {
                rsForm.close();
                psForm.close();
                con.close();
                return "redirect:/feedback";
            }

            model.addAttribute("formId", formId);
            model.addAttribute("formTitle", rsForm.getString("title"));
            model.addAttribute("formDescription", rsForm.getString("description"));
            model.addAttribute("courseName", rsForm.getString("course_name"));
            model.addAttribute("courseCode", rsForm.getString("course_code"));
            model.addAttribute("teacherName", rsForm.getString("teacher_name"));

            long courseId = rsForm.getLong("course_id");
            long teacherId = rsForm.getLong("teacher_id");
            rsForm.close();
            psForm.close();

            PreparedStatement psSubmitted = con.prepareStatement(
                    "SELECT 1 FROM feedback_submission WHERE form_id = ? AND student_id = ? LIMIT 1"
            );
            psSubmitted.setLong(1, formId);
            psSubmitted.setLong(2, studentId);
            ResultSet rsSubmitted = psSubmitted.executeQuery();
            boolean alreadySubmitted = rsSubmitted.next();
            rsSubmitted.close();
            psSubmitted.close();
            model.addAttribute("alreadySubmitted", alreadySubmitted);

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

            model.addAttribute("questions", questions);

            // Keep for submit verification (hidden fields not trusted, but useful for UI).
            model.addAttribute("courseId", courseId);
            model.addAttribute("teacherId", teacherId);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/feedback";
        }
        return "Student/FeedbackForm";
    }

    @PostMapping("/feedback/{formId}/submit")
    public String submitFeedbackForm(@PathVariable("formId") long formId,
                                    HttpSession session,
                                    @RequestParam Map<String, String> params) {
        try {
            Object sid = session.getAttribute("studentId");
            if (sid == null) {
                return "redirect:/login";
            }
            long studentId = Long.parseLong(sid.toString());

            Connection con = DataBaseConnection.getConnection();
            con.setAutoCommit(false);

            if (!feedbackTablesExist(con)) {
                con.rollback();
                con.close();
                return "redirect:/feedback";
            }

            // Verify student is allowed to submit this form (student selected this teacher for this course).
            PreparedStatement psCheck = con.prepareStatement(
                    "SELECT ff.course_id, ff.teacher_id " +
                            "FROM feedback_form ff " +
                            "JOIN student_course_teacher sct ON sct.course_id = ff.course_id AND sct.teacher_id = ff.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE ff.id = ? AND ff.is_active = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            );
            psCheck.setLong(1, studentId);
            psCheck.setLong(2, formId);
            ResultSet rsCheck = psCheck.executeQuery();
            if (!rsCheck.next()) {
                rsCheck.close();
                psCheck.close();
                con.rollback();
                con.close();
                return "redirect:/feedback";
            }
            rsCheck.close();
            psCheck.close();

            PreparedStatement psAlready = con.prepareStatement(
                    "SELECT 1 FROM feedback_submission WHERE form_id = ? AND student_id = ? LIMIT 1"
            );
            psAlready.setLong(1, formId);
            psAlready.setLong(2, studentId);
            ResultSet rsAlready = psAlready.executeQuery();
            boolean already = rsAlready.next();
            rsAlready.close();
            psAlready.close();
            if (already) {
                con.rollback();
                con.close();
                return "redirect:/feedback/" + formId;
            }

            PreparedStatement psSub = con.prepareStatement(
                    "INSERT INTO feedback_submission (form_id, student_id) VALUES (?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
            );
            psSub.setLong(1, formId);
            psSub.setLong(2, studentId);
            psSub.executeUpdate();

            long submissionId;
            ResultSet keys = psSub.getGeneratedKeys();
            if (!keys.next()) {
                keys.close();
                psSub.close();
                con.rollback();
                con.close();
                return "redirect:/feedback/" + formId;
            }
            submissionId = keys.getLong(1);
            keys.close();
            psSub.close();

            PreparedStatement psQ = con.prepareStatement(
                    "SELECT id, question_type FROM feedback_question WHERE form_id = ? ORDER BY question_order"
            );
            psQ.setLong(1, formId);
            ResultSet rsQ = psQ.executeQuery();

            PreparedStatement psAns = con.prepareStatement(
                    "INSERT INTO feedback_answer (submission_id, question_id, answer_text, answer_number) VALUES (?, ?, ?, ?)"
            );

            while (rsQ.next()) {
                long qid = rsQ.getLong("id");
                String type = rsQ.getString("question_type");
                String key = "q_" + qid;
                String raw = params.get(key);

                psAns.setLong(1, submissionId);
                psAns.setLong(2, qid);

                if (type != null && type.equalsIgnoreCase("RATING")) {
                    Integer val = null;
                    try {
                        if (raw != null && !raw.trim().isEmpty()) val = Integer.parseInt(raw.trim());
                    } catch (Exception ignored) {}
                    psAns.setString(3, null);
                    if (val == null) psAns.setObject(4, null);
                    else psAns.setInt(4, val);
                } else {
                    String text = (raw == null) ? null : raw.trim();
                    psAns.setString(3, text);
                    psAns.setObject(4, null);
                }

                psAns.addBatch();
            }

            rsQ.close();
            psQ.close();

            psAns.executeBatch();
            psAns.close();

            con.commit();
            con.close();

            return "redirect:/feedback";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/feedback/" + formId;
        }
    }

    @GetMapping("/online-exam")
    public String onlinexam(){
        return "Student/OnlineExam";
    }

    @GetMapping("/calendar")
    public String calendar(){
        return "Student/Calendar";
    }

    @GetMapping("/drive")
    public String drive(){
        return "Student/Drive";
    }

}
