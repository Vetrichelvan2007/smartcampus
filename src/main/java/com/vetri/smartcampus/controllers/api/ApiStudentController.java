package com.vetri.smartcampus.controllers.api;

import com.vetri.smartcampus.models.common.DataBaseConnection;
import com.vetri.smartcampus.models.student.CourseData;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class ApiStudentController {

    private Long getStudentId(HttpSession session) {
        Object sid = session.getAttribute("studentId");
        if (sid == null) {
            return null;
        }
        return Long.parseLong(sid.toString());
    }

    @GetMapping("/classroom")
    public ResponseEntity<?> getClassroom(HttpSession session) {
        Long studentId = getStudentId(session);
        Object deptIdObj = session.getAttribute("department_id");

        if (studentId == null || deptIdObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a student."));
        }

        int deptId = Integer.parseInt(deptIdObj.toString());
        int currentSem = 0;
        List<CourseData> courseDatas = new ArrayList<>();

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            // Get current semester
            try (PreparedStatement ps1 = con.prepareStatement("SELECT current_semester FROM student WHERE id=?")) {
                ps1.setLong(1, studentId);
                try (ResultSet rs1 = ps1.executeQuery()) {
                    if (rs1.next()) {
                        currentSem = rs1.getInt("current_semester");
                    }
                }
            }

            // Get course data
            try (PreparedStatement ps2 = con.prepareStatement(
                    "SELECT d.dept_name, cd.sem, c.course_name, c.course_type, c.course_code " +
                            "FROM course_for_depts cd " +
                            "JOIN course c ON c.id = cd.course_id " +
                            "JOIN department d ON d.id = cd.dept_id " +
                            "WHERE cd.dept_id = ? AND cd.sem <= ? ORDER BY cd.sem"
            )) {
                ps2.setInt(1, deptId);
                ps2.setInt(2, currentSem);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) {
                        courseDatas.add(new CourseData(
                                rs2.getString("course_name"),
                                rs2.getString("course_code"),
                                rs2.getString("course_type"),
                                rs2.getInt("sem")
                        ));
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("currentSem", currentSem);
            response.put("courseDatas", courseDatas);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a student."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            Map<String, Object> profileData = new HashMap<>();

            // Student details
            try (PreparedStatement studentPs = con.prepareStatement(
                    "SELECT s.id, s.roll_number, s.name, s.dob, s.gender, s.blood_group, s.mother_tongue, s.nationality, s.address, s.dept_id, d.dept_name, s.current_year, s.current_semester, s.email, s.phone " +
                            "FROM student s JOIN department d ON s.dept_id = d.id WHERE s.id = ?"
            )) {
                studentPs.setLong(1, studentId);
                try (ResultSet studentRs = studentPs.executeQuery()) {
                    if (studentRs.next()) {
                        Map<String, Object> student = new HashMap<>();
                        student.put("name", studentRs.getString("name"));
                        student.put("rollNumber", studentRs.getString("roll_number"));
                        student.put("email", studentRs.getString("email"));
                        student.put("department", studentRs.getString("dept_name"));
                        student.put("year", studentRs.getString("current_year"));
                        student.put("sem", studentRs.getString("current_semester"));
                        student.put("dob", studentRs.getString("dob"));
                        student.put("gender", studentRs.getString("gender"));
                        student.put("nationality", studentRs.getString("nationality"));
                        student.put("bloodGroup", studentRs.getString("blood_group"));
                        student.put("motherTongue", studentRs.getString("mother_tongue"));
                        student.put("address", studentRs.getString("address"));
                        student.put("phone", studentRs.getString("phone"));
                        profileData.put("student", student);
                    }
                }
            }

            // Father details
            try (PreparedStatement fatherPs = con.prepareStatement("SELECT * FROM father_details WHERE student_id = ?")) {
                fatherPs.setLong(1, studentId);
                try (ResultSet fatherRs = fatherPs.executeQuery()) {
                    if (fatherRs.next()) {
                        Map<String, Object> father = new HashMap<>();
                        father.put("name", fatherRs.getString("name"));
                        father.put("phone", fatherRs.getString("phone"));
                        father.put("email", fatherRs.getString("email"));
                        father.put("occupation", fatherRs.getString("occupation"));
                        father.put("annualIncome", fatherRs.getString("annual_income"));
                        father.put("address", fatherRs.getString("address"));
                        profileData.put("father", father);
                    }
                }
            }

            // Mother details
            try (PreparedStatement motherPs = con.prepareStatement("SELECT * FROM mother_details WHERE student_id = ?")) {
                motherPs.setLong(1, studentId);
                try (ResultSet motherRs = motherPs.executeQuery()) {
                    if (motherRs.next()) {
                        Map<String, Object> mother = new HashMap<>();
                        mother.put("name", motherRs.getString("name"));
                        mother.put("phone", motherRs.getString("phone"));
                        mother.put("email", motherRs.getString("email"));
                        mother.put("occupation", motherRs.getString("occupation"));
                        mother.put("annualIncome", motherRs.getString("annual_income"));
                        mother.put("address", motherRs.getString("address"));
                        profileData.put("mother", mother);
                    }
                }
            }

            // Identity details
            try (PreparedStatement identityPs = con.prepareStatement("SELECT * FROM identity_details WHERE student_id = ?")) {
                identityPs.setLong(1, studentId);
                try (ResultSet identityRs = identityPs.executeQuery()) {
                    if (identityRs.next()) {
                        Map<String, Object> identity = new HashMap<>();
                        identity.put("aadharNumber", identityRs.getString("aadhar_number"));
                        identity.put("panNumber", identityRs.getString("pan_number"));
                        identity.put("passportNumber", identityRs.getString("passport_number"));
                        profileData.put("identity", identity);
                    }
                }
            }

            return ResponseEntity.ok(profileData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/course/{courseCode}")
    public ResponseEntity<?> getCourseDetails(@PathVariable String courseCode, HttpSession session) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a student."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            Map<String, Object> courseInfo = new HashMap<>();
            long courseId = 0;

            // Resolve course
            try (PreparedStatement ps = con.prepareStatement("SELECT id, course_name, course_code FROM course WHERE course_code=?")) {
                ps.setString(1, courseCode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        courseId = rs.getLong("id");
                        courseInfo.put("courseDbId", courseId);
                        courseInfo.put("courseName", rs.getString("course_name"));
                        courseInfo.put("courseCode", rs.getString("course_code"));
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("message", "Course not found."));
                    }
                }
            }

            // Load materials
            List<Map<String, Object>> materials = new ArrayList<>();
            try (PreparedStatement psM = con.prepareStatement(
                    "SELECT cm.id, cm.title, cm.module, cm.material_type, cm.original_filename, cm.file_size, cm.download_allowed, cm.created_at " +
                            "FROM course_material cm " +
                            "JOIN student_course_teacher sct ON sct.course_id = cm.course_id AND sct.teacher_id = cm.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE cm.course_id = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester " +
                            "ORDER BY cm.created_at DESC"
            )) {
                psM.setLong(1, studentId);
                psM.setLong(2, courseId);
                try (ResultSet rsM = psM.executeQuery()) {
                    while (rsM.next()) {
                        Map<String, Object> material = new HashMap<>();
                        material.put("id", rsM.getLong("id"));
                        material.put("title", rsM.getString("title"));
                        material.put("module", rsM.getString("module"));
                        material.put("type", rsM.getString("material_type"));
                        material.put("originalFileName", rsM.getString("original_filename"));
                        material.put("fileSize", rsM.getLong("file_size"));
                        material.put("downloadAllowed", rsM.getInt("download_allowed") == 1);
                        material.put("uploadedAt", rsM.getTimestamp("created_at"));
                        materials.add(material);
                    }
                }
            }
            courseInfo.put("materials", materials);

            // Load assignments
            List<Map<String, Object>> assignments = new ArrayList<>();
            try (PreparedStatement psA = con.prepareStatement(
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
            )) {
                psA.setLong(1, studentId);
                psA.setLong(2, courseId);
                try (ResultSet rsA = psA.executeQuery()) {
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    while (rsA.next()) {
                        Map<String, Object> assignment = new HashMap<>();
                        assignment.put("id", rsA.getLong("id"));
                        assignment.put("title", rsA.getString("title"));
                        assignment.put("assignmentMode", rsA.getString("assignment_mode"));
                        assignment.put("questionText", rsA.getString("question_text"));
                        assignment.put("instructions", rsA.getString("instructions"));
                        assignment.put("originalFileName", rsA.getString("original_filename"));
                        assignment.put("fileSize", rsA.getLong("file_size"));
                        java.sql.Timestamp due = rsA.getTimestamp("due_at");
                        assignment.put("dueAt", due);
                        assignment.put("maxMarks", rsA.getInt("max_marks"));
                        assignment.put("downloadAllowed", rsA.getInt("download_allowed") == 1);
                        assignment.put("createdAt", rsA.getTimestamp("created_at"));
                        long submissionId = rsA.getLong("submission_id");
                        boolean submitted = !rsA.wasNull();
                        assignment.put("submitted", submitted);
                        assignment.put("submissionOriginalFileName", rsA.getString("submission_original_filename"));
                        assignment.put("submittedAt", rsA.getTimestamp("submitted_at"));
                        assignment.put("submissionClosed", due != null && now.isAfter(due.toLocalDateTime()));
                        assignments.add(assignment);
                    }
                }
            }
            courseInfo.put("assignments", assignments);

            // Load quizzes
            List<Map<String, Object>> quizzes = new ArrayList<>();
            try (PreparedStatement psQ = con.prepareStatement(
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
            )) {
                psQ.setLong(1, studentId);
                psQ.setLong(2, studentId);
                psQ.setLong(3, courseId);
                try (ResultSet rsQ = psQ.executeQuery()) {
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    while (rsQ.next()) {
                        Map<String, Object> quiz = new HashMap<>();
                        quiz.put("quizId", rsQ.getLong("quiz_id"));
                        quiz.put("title", rsQ.getString("title"));
                        java.sql.Timestamp startAt = rsQ.getTimestamp("start_at");
                        java.sql.Timestamp endAt = rsQ.getTimestamp("end_at");
                        quiz.put("startAt", startAt);
                        quiz.put("endAt", endAt);
                        
                        String status = "ACTIVE";
                        if (startAt != null && now.isBefore(startAt.toLocalDateTime())) status = "UPCOMING";
                        else if (endAt != null && now.isAfter(endAt.toLocalDateTime())) status = "CLOSED";
                        quiz.put("status", status);

                        quiz.put("scorePublished", rsQ.getInt("is_score_published") == 1);
                        quiz.put("submitted", rsQ.getInt("submitted") == 1);
                        int score = rsQ.getInt("score");
                        quiz.put("score", rsQ.wasNull() ? null : score);
                        quizzes.add(quiz);
                    }
                }
            }
            courseInfo.put("quizzes", quizzes);

            return ResponseEntity.ok(courseInfo);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<?> getQuizDetails(@PathVariable long quizId, HttpSession session) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a student."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            Map<String, Object> quizInfo = new HashMap<>();

            try (PreparedStatement psQuiz = con.prepareStatement(
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
            )) {
                psQuiz.setLong(1, studentId);
                psQuiz.setLong(2, studentId);
                psQuiz.setLong(3, quizId);

                try (ResultSet rsQuiz = psQuiz.executeQuery()) {
                    if (!rsQuiz.next()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("message", "Quiz not found or not published."));
                    }

                    quizInfo.put("quizId", quizId);
                    quizInfo.put("title", rsQuiz.getString("title"));
                    quizInfo.put("instructions", rsQuiz.getString("instructions"));
                    quizInfo.put("durationMinutes", rsQuiz.getInt("duration_minutes"));
                    quizInfo.put("totalMarks", rsQuiz.getInt("total_marks"));
                    quizInfo.put("scorePublished", rsQuiz.getInt("is_score_published") == 1);
                    
                    java.sql.Timestamp startAt = rsQuiz.getTimestamp("start_at");
                    java.sql.Timestamp endAt = rsQuiz.getTimestamp("end_at");
                    quizInfo.put("startAt", startAt);
                    quizInfo.put("endAt", endAt);
                    quizInfo.put("courseName", rsQuiz.getString("course_name"));
                    quizInfo.put("courseCode", rsQuiz.getString("course_code"));
                    quizInfo.put("teacherName", rsQuiz.getString("teacher_name"));

                    long subId = rsQuiz.getLong("submission_id");
                    boolean alreadySubmitted = !rsQuiz.wasNull();
                    quizInfo.put("alreadySubmitted", alreadySubmitted);
                    int score = rsQuiz.getInt("score");
                    quizInfo.put("score", rsQuiz.wasNull() ? null : score);

                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    boolean inWindow = startAt != null && endAt != null &&
                            (now.isAfter(startAt.toLocalDateTime()) || now.isEqual(startAt.toLocalDateTime())) &&
                            (now.isBefore(endAt.toLocalDateTime()) || now.isEqual(endAt.toLocalDateTime()));
                    quizInfo.put("inWindow", inWindow);

                    List<Map<String, Object>> questions = new ArrayList<>();
                    if (!alreadySubmitted && inWindow) {
                        try (PreparedStatement psQ = con.prepareStatement(
                                "SELECT id, question_order, question_text, question_type, marks " +
                                        "FROM quiz_question WHERE quiz_id = ? ORDER BY question_order"
                        )) {
                            psQ.setLong(1, quizId);
                            try (ResultSet rsQ = psQ.executeQuery()) {
                                while (rsQ.next()) {
                                    Map<String, Object> q = new HashMap<>();
                                    long qid = rsQ.getLong("id");
                                    q.put("id", qid);
                                    q.put("order", rsQ.getInt("question_order"));
                                    q.put("text", rsQ.getString("question_text"));
                                    q.put("type", rsQ.getString("question_type"));
                                    q.put("marks", rsQ.getInt("marks"));

                                    if ("MCQ".equalsIgnoreCase(rsQ.getString("question_type"))) {
                                        try (PreparedStatement psOpt = con.prepareStatement(
                                                "SELECT option_index, option_text FROM quiz_option WHERE question_id = ? ORDER BY option_index"
                                        )) {
                                            psOpt.setLong(1, qid);
                                            try (ResultSet rsOpt = psOpt.executeQuery()) {
                                                List<String> opts = new ArrayList<>();
                                                while (rsOpt.next()) {
                                                    opts.add(rsOpt.getString("option_text"));
                                                }
                                                q.put("options", opts);
                                            }
                                        }
                                    }
                                    questions.add(q);
                                }
                            }
                        }
                    }
                    quizInfo.put("questions", questions);
                }
            }

            return ResponseEntity.ok(quizInfo);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/quiz/{quizId}/submit")
    public ResponseEntity<?> submitQuizAnswers(@PathVariable long quizId, @RequestBody Map<String, String> answers, HttpSession session) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a student."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            con.setAutoCommit(false);

            // Time window and session validation
            java.sql.Timestamp startAt = null;
            java.sql.Timestamp endAt = null;
            try (PreparedStatement psCheck = con.prepareStatement(
                    "SELECT q.start_at, q.end_at " +
                            "FROM quiz q " +
                            "JOIN student_course_teacher sct ON sct.course_id = q.course_id AND sct.teacher_id = q.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE q.id = ? AND q.is_published = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            )) {
                psCheck.setLong(1, studentId);
                psCheck.setLong(2, quizId);
                try (ResultSet rsCheck = psCheck.executeQuery()) {
                    if (!rsCheck.next()) {
                        con.rollback();
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", "Quiz unavailable or student not enrolled."));
                    }
                    startAt = rsCheck.getTimestamp("start_at");
                    endAt = rsCheck.getTimestamp("end_at");
                }
            }

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            boolean inWindow = startAt != null && endAt != null &&
                    (now.isAfter(startAt.toLocalDateTime()) || now.isEqual(startAt.toLocalDateTime())) &&
                    (now.isBefore(endAt.toLocalDateTime()) || now.isEqual(endAt.toLocalDateTime()));
            if (!inWindow) {
                con.rollback();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Quiz window has closed or not yet started."));
            }

            // Already submitted validation
            try (PreparedStatement psAlready = con.prepareStatement(
                    "SELECT 1 FROM quiz_submission WHERE quiz_id = ? AND student_id = ? LIMIT 1"
            )) {
                psAlready.setLong(1, quizId);
                psAlready.setLong(2, studentId);
                try (ResultSet rsAlready = psAlready.executeQuery()) {
                    if (rsAlready.next()) {
                        con.rollback();
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", "You have already submitted this quiz."));
                    }
                }
            }

            // Insert submission
            long submissionId = 0;
            try (PreparedStatement psSub = con.prepareStatement(
                    "INSERT INTO quiz_submission (quiz_id, student_id, score) VALUES (?, ?, 0)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
            )) {
                psSub.setLong(1, quizId);
                psSub.setLong(2, studentId);
                psSub.executeUpdate();
                try (ResultSet keys = psSub.getGeneratedKeys()) {
                    if (keys.next()) {
                        submissionId = keys.getLong(1);
                    } else {
                        con.rollback();
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("message", "Failed to create quiz submission."));
                    }
                }
            }

            // Grade questions
            int totalScore = 0;
            try (PreparedStatement psQ = con.prepareStatement(
                    "SELECT id, question_type, marks, correct_option_index " +
                            "FROM quiz_question WHERE quiz_id = ? ORDER BY question_order"
            )) {
                psQ.setLong(1, quizId);
                try (ResultSet rsQ = psQ.executeQuery()) {
                    try (PreparedStatement psAns = con.prepareStatement(
                            "INSERT INTO quiz_answer (submission_id, question_id, selected_option_index, answer_text, marks_awarded) VALUES (?, ?, ?, ?, ?)"
                    )) {
                        while (rsQ.next()) {
                            long qid = rsQ.getLong("id");
                            String type = rsQ.getString("question_type");
                            int marks = rsQ.getInt("marks");
                            int corr = rsQ.getInt("correct_option_index");
                            Integer correct = rsQ.wasNull() ? null : corr;
                            String raw = answers.get("q_" + qid);

                            psAns.setLong(1, submissionId);
                            psAns.setLong(2, qid);

                            if ("MCQ".equalsIgnoreCase(type)) {
                                Integer selected = null;
                                try {
                                    if (raw != null && !raw.trim().isEmpty()) {
                                        selected = Integer.parseInt(raw.trim());
                                    }
                                } catch (Exception ignored) {}

                                int awarded = (selected != null && correct != null && selected.intValue() == correct.intValue()) ? marks : 0;
                                totalScore += awarded;

                                if (selected == null) psAns.setObject(3, null);
                                else psAns.setInt(3, selected);
                                psAns.setString(4, null);
                                psAns.setInt(5, awarded);
                            } else {
                                psAns.setObject(3, null);
                                psAns.setString(4, raw == null ? null : raw.trim());
                                psAns.setObject(5, 0); // Open questions graded by teacher later
                            }
                            psAns.addBatch();
                        }
                        psAns.executeBatch();
                    }
                }
            }

            // Update score
            try (PreparedStatement psUpd = con.prepareStatement("UPDATE quiz_submission SET score = ? WHERE id = ?")) {
                psUpd.setInt(1, totalScore);
                psUpd.setLong(2, submissionId);
                psUpd.executeUpdate();
            }

            con.commit();
            return ResponseEntity.ok(Map.of("score", totalScore, "submissionId", submissionId));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/registration/status")
    public ResponseEntity<?> getRegistrationStatus(HttpSession session) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a student."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            // Resolve department and current semester
            long deptId = 0;
            int currentSemester = 1;
            try (PreparedStatement ps = con.prepareStatement("SELECT dept_id, current_semester FROM student WHERE id = ?")) {
                ps.setLong(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        deptId = rs.getLong("dept_id");
                        currentSemester = rs.getInt("current_semester");
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("message", "Student profile not found."));
                    }
                }
            }

            // Check if already registered
            boolean alreadyRegistered = false;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT 1 FROM student_course_teacher WHERE student_id = ? AND semester = ? LIMIT 1"
            )) {
                ps.setLong(1, studentId);
                ps.setInt(2, currentSemester);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        alreadyRegistered = true;
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("alreadyRegistered", alreadyRegistered);
            response.put("currentSemester", currentSemester);

            List<Map<String, Object>> coursesList = new ArrayList<>();
            if (alreadyRegistered) {
                // Return registered courses
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT c.id AS course_id, c.course_code, c.course_name, c.course_type, sct.semester, " +
                                "t.id AS teacher_id, t.name AS teacher_name " +
                                "FROM student_course_teacher sct " +
                                "JOIN course c ON sct.course_id = c.id " +
                                "LEFT JOIN teacher t ON sct.teacher_id = t.id " +
                                "WHERE sct.student_id = ? AND sct.semester = ? " +
                                "ORDER BY c.id"
                )) {
                    ps.setLong(1, studentId);
                    ps.setInt(2, currentSemester);
                    try (ResultSet rs = ps.executeQuery()) {
                        Map<Long, Map<String, Object>> courseMap = new HashMap<>();
                        while (rs.next()) {
                            long cid = rs.getLong("course_id");
                            Map<String, Object> c = courseMap.get(cid);
                            if (c == null) {
                                c = new HashMap<>();
                                c.put("courseId", cid);
                                c.put("courseCode", rs.getString("course_code"));
                                c.put("courseName", rs.getString("course_name"));
                                c.put("courseType", rs.getString("course_type"));
                                c.put("teachers", new ArrayList<Map<String, Object>>());
                                courseMap.put(cid, c);
                                coursesList.add(c);
                            }
                            long tid = rs.getLong("teacher_id");
                            String tname = rs.getString("teacher_name");
                            if (tid > 0 && tname != null) {
                                List<Map<String, Object>> tList = (List<Map<String, Object>>) c.get("teachers");
                                tList.add(Map.of("teacherId", tid, "teacherName", tname));
                            }
                        }
                    }
                }
            } else {
                // Return available courses for registration
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT c.id AS course_id, c.course_code, c.course_name, c.course_type, cfd.sem, " +
                                "t.id AS teacher_id, t.name AS teacher_name " +
                                "FROM course_for_depts cfd " +
                                "JOIN course c ON c.id = cfd.course_id " +
                                "LEFT JOIN course_teacher_allocation cta ON c.id = cta.course_id " +
                                "LEFT JOIN teacher t ON cta.teacher_id = t.id " +
                                "WHERE cfd.dept_id = ? AND cfd.sem = ? " +
                                "ORDER BY c.id"
                )) {
                    ps.setLong(1, deptId);
                    ps.setInt(2, currentSemester);
                    try (ResultSet rs = ps.executeQuery()) {
                        Map<Long, Map<String, Object>> courseMap = new HashMap<>();
                        while (rs.next()) {
                            long cid = rs.getLong("course_id");
                            Map<String, Object> c = courseMap.get(cid);
                            if (c == null) {
                                c = new HashMap<>();
                                c.put("courseId", cid);
                                c.put("courseCode", rs.getString("course_code"));
                                c.put("courseName", rs.getString("course_name"));
                                c.put("courseType", rs.getString("course_type"));
                                c.put("teachers", new ArrayList<Map<String, Object>>());
                                courseMap.put(cid, c);
                                coursesList.add(c);
                            }
                            long tid = rs.getLong("teacher_id");
                            String tname = rs.getString("teacher_name");
                            if (tid > 0 && tname != null) {
                                List<Map<String, Object>> tList = (List<Map<String, Object>>) c.get("teachers");
                                tList.add(Map.of("teacherId", tid, "teacherName", tname));
                            }
                        }
                    }
                }
            }

            response.put("courses", coursesList);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/registration/submit")
    public ResponseEntity<?> submitRegistration(@RequestBody List<Map<String, Object>> selectedCourses, HttpSession session) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a student."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            con.setAutoCommit(false);

            // Fetch current semester
            int currentSem = 1;
            try (PreparedStatement ps = con.prepareStatement("SELECT current_semester FROM student WHERE id = ?")) {
                ps.setLong(1, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentSem = rs.getInt("current_semester");
                    }
                }
            }

            // Insert each registration item
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO student_course_teacher (student_id, course_id, teacher_id, semester, status) VALUES (?, ?, ?, ?, ?)"
            )) {
                for (Map<String, Object> c : selectedCourses) {
                    long courseId = Long.parseLong(c.get("courseId").toString());
                    long teacherId = Long.parseLong(c.get("teacherId").toString());
                    ps.setLong(1, studentId);
                    ps.setLong(2, courseId);
                    ps.setLong(3, teacherId);
                    ps.setInt(4, currentSem);
                    ps.setString(5, "ACTIVE");
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();
            return ResponseEntity.ok(Map.of("message", "Courses registered successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Registration failed: " + e.getMessage()));
        }
    }

    @GetMapping("/feedback")
    public ResponseEntity<?> getFeedbackForms(HttpSession session) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a student."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            List<Map<String, Object>> forms = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
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
            )) {
                ps.setLong(1, studentId);
                ps.setLong(2, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> f = new HashMap<>();
                        f.put("formId", rs.getLong("form_id"));
                        f.put("title", rs.getString("title"));
                        f.put("description", rs.getString("description"));
                        f.put("courseName", rs.getString("course_name"));
                        f.put("courseCode", rs.getString("course_code"));
                        f.put("teacherName", rs.getString("teacher_name"));
                        f.put("submitted", rs.getInt("submitted") == 1);
                        f.put("createdAt", rs.getTimestamp("created_at"));
                        forms.add(f);
                    }
                }
            }

            return ResponseEntity.ok(forms);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/feedback/{formId}")
    public ResponseEntity<?> getFeedbackFormDetails(@PathVariable long formId, HttpSession session) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a student."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            Map<String, Object> formDetails = new HashMap<>();

            try (PreparedStatement psForm = con.prepareStatement(
                    "SELECT ff.id, ff.title, ff.description, ff.course_id, ff.teacher_id, c.course_name, c.course_code, t.name AS teacher_name " +
                            "FROM feedback_form ff " +
                            "JOIN course c ON c.id = ff.course_id " +
                            "JOIN teacher t ON t.id = ff.teacher_id " +
                            "JOIN student_course_teacher sct ON sct.course_id = ff.course_id AND sct.teacher_id = ff.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE ff.id = ? AND ff.is_active = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            )) {
                psForm.setLong(1, studentId);
                psForm.setLong(2, formId);
                try (ResultSet rsForm = psForm.executeQuery()) {
                    if (!rsForm.next()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("message", "Feedback form not found or student not enrolled."));
                    }

                    formDetails.put("formId", formId);
                    formDetails.put("title", rsForm.getString("title"));
                    formDetails.put("description", rsForm.getString("description"));
                    formDetails.put("courseName", rsForm.getString("course_name"));
                    formDetails.put("courseCode", rsForm.getString("course_code"));
                    formDetails.put("teacherName", rsForm.getString("teacher_name"));
                }
            }

            // Check if already submitted
            boolean alreadySubmitted = false;
            try (PreparedStatement psSubmitted = con.prepareStatement(
                    "SELECT 1 FROM feedback_submission WHERE form_id = ? AND student_id = ? LIMIT 1"
            )) {
                psSubmitted.setLong(1, formId);
                psSubmitted.setLong(2, studentId);
                try (ResultSet rsSubmitted = psSubmitted.executeQuery()) {
                    alreadySubmitted = rsSubmitted.next();
                }
            }
            formDetails.put("alreadySubmitted", alreadySubmitted);

            // Fetch questions
            List<Map<String, Object>> questions = new ArrayList<>();
            try (PreparedStatement psQ = con.prepareStatement(
                    "SELECT id, question_order, question_text, question_type, options_text, rating_max, required " +
                            "FROM feedback_question WHERE form_id = ? ORDER BY question_order"
            )) {
                psQ.setLong(1, formId);
                try (ResultSet rsQ = psQ.executeQuery()) {
                    while (rsQ.next()) {
                        Map<String, Object> q = new HashMap<>();
                        q.put("id", rsQ.getLong("id"));
                        q.put("order", rsQ.getInt("question_order"));
                        q.put("text", rsQ.getString("question_text"));
                        q.put("type", rsQ.getString("question_type"));
                        q.put("ratingMax", rsQ.getInt("rating_max"));
                        q.put("required", rsQ.getInt("required") == 1);
                        
                        String optText = rsQ.getString("options_text");
                        if (optText != null) {
                            String[] parts = optText.split("\\r?\\n");
                            List<String> options = new ArrayList<>();
                            for (String p : parts) {
                                if (p != null && !p.trim().isEmpty()) {
                                    options.add(p.trim());
                                }
                            }
                            q.put("options", options);
                        }
                        questions.add(q);
                    }
                }
            }
            formDetails.put("questions", questions);

            return ResponseEntity.ok(formDetails);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/feedback/{formId}/submit")
    public ResponseEntity<?> submitFeedbackAnswers(@PathVariable long formId, @RequestBody Map<String, String> answers, HttpSession session) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a student."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            con.setAutoCommit(false);

            // Enrollment check
            try (PreparedStatement psCheck = con.prepareStatement(
                    "SELECT 1 FROM feedback_form ff " +
                            "JOIN student_course_teacher sct ON sct.course_id = ff.course_id AND sct.teacher_id = ff.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE ff.id = ? AND ff.is_active = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            )) {
                psCheck.setLong(1, studentId);
                psCheck.setLong(2, formId);
                try (ResultSet rsCheck = psCheck.executeQuery()) {
                    if (!rsCheck.next()) {
                        con.rollback();
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", "Feedback form unavailable or student not enrolled."));
                    }
                }
            }

            // Already submitted check
            try (PreparedStatement psAlready = con.prepareStatement(
                    "SELECT 1 FROM feedback_submission WHERE form_id = ? AND student_id = ? LIMIT 1"
            )) {
                psAlready.setLong(1, formId);
                psAlready.setLong(2, studentId);
                try (ResultSet rsAlready = psAlready.executeQuery()) {
                    if (rsAlready.next()) {
                        con.rollback();
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", "Feedback already submitted."));
                    }
                }
            }

            // Insert submission
            long submissionId = 0;
            try (PreparedStatement psSub = con.prepareStatement(
                    "INSERT INTO feedback_submission (form_id, student_id) VALUES (?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
            )) {
                psSub.setLong(1, formId);
                psSub.setLong(2, studentId);
                psSub.executeUpdate();
                try (ResultSet keys = psSub.getGeneratedKeys()) {
                    if (keys.next()) {
                        submissionId = keys.getLong(1);
                    } else {
                        con.rollback();
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("message", "Failed to create feedback submission."));
                    }
                }
            }

            // Insert answers
            try (PreparedStatement psQ = con.prepareStatement(
                    "SELECT id, question_type FROM feedback_question WHERE form_id = ? ORDER BY question_order"
            )) {
                psQ.setLong(1, formId);
                try (ResultSet rsQ = psQ.executeQuery()) {
                    try (PreparedStatement psAns = con.prepareStatement(
                            "INSERT INTO feedback_answer (submission_id, question_id, answer_text, answer_number) VALUES (?, ?, ?, ?)"
                    )) {
                        while (rsQ.next()) {
                            long qid = rsQ.getLong("id");
                            String type = rsQ.getString("question_type");
                            String raw = answers.get("q_" + qid);

                            psAns.setLong(1, submissionId);
                            psAns.setLong(2, qid);

                            if ("RATING".equalsIgnoreCase(type)) {
                                Integer val = null;
                                try {
                                    if (raw != null && !raw.trim().isEmpty()) {
                                        val = Integer.parseInt(raw.trim());
                                    }
                                } catch (Exception ignored) {}
                                psAns.setString(3, null);
                                if (val == null) psAns.setObject(4, null);
                                else psAns.setInt(4, val);
                            } else {
                                psAns.setString(3, raw == null ? null : raw.trim());
                                psAns.setObject(4, null);
                            }
                            psAns.addBatch();
                        }
                        psAns.executeBatch();
                    }
                }
            }

            con.commit();
            return ResponseEntity.ok(Map.of("message", "Feedback submitted successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }
}
