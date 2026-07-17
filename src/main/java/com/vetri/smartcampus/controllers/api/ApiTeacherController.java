package com.vetri.smartcampus.controllers.api;

import com.vetri.smartcampus.models.common.DataBaseConnection;
import com.vetri.smartcampus.models.teacher.AssignedCourses;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class ApiTeacherController {

    private Long getTeacherId(HttpSession session) {
        Object tid = session.getAttribute("teacherId");
        if (tid == null) {
            return null;
        }
        return Long.parseLong(tid.toString());
    }

    private void ensureSchema(Connection con) {
        try (Statement stmt = con.createStatement()) {
            stmt.execute("ALTER TABLE course_assignment_submission ADD COLUMN IF NOT EXISTS marks_awarded integer");
            stmt.execute("ALTER TABLE course_assignment_submission ADD COLUMN IF NOT EXISTS feedback text");
        } catch (Exception e) {
            System.err.println("Migration warning: " + e.getMessage());
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getTeacherDashboard(HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            PreparedStatement ps = con.prepareStatement(
                    "SELECT DISTINCT c.id as course_id, c.course_name, c.course_code, d.dept_name, cfd.sem, c.credit " +
                            "FROM course_teacher_allocation cta " +
                            "JOIN course c ON c.id = cta.course_id " +
                            "JOIN course_for_depts cfd ON cfd.course_id = c.id " +
                            "JOIN department d ON d.id = cfd.dept_id " +
                            "WHERE cta.teacher_id = ? " +
                            "ORDER BY c.course_name"
            );
            ps.setLong(1, teacherId);

            List<Map<String, Object>> courses = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> course = new HashMap<>();
                    course.put("courseId", rs.getLong("course_id"));
                    course.put("courseName", rs.getString("course_name"));
                    course.put("courseCode", rs.getString("course_code"));
                    course.put("department", rs.getString("dept_name"));
                    course.put("semester", rs.getInt("sem"));
                    course.put("credits", rs.getInt("credit"));
                    courses.add(course);
                }
            }

            // Retrieve teacher profile name
            String teacherName = "Instructor";
            try (PreparedStatement psT = con.prepareStatement("SELECT name FROM teacher WHERE id = ?")) {
                psT.setLong(1, teacherId);
                try (ResultSet rsT = psT.executeQuery()) {
                    if (rsT.next()) teacherName = rsT.getString("name");
                }
            }

            return ResponseEntity.ok(Map.of("teacherName", teacherName, "courses", courses));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getTeacherCourseDetails(@PathVariable long courseId, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }
            ensureSchema(con);

            Map<String, Object> courseDetails = new HashMap<>();

            // Fetch course meta
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id, course_name, course_code, credit FROM course WHERE id = ?"
            )) {
                ps.setLong(1, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("message", "Course not found."));
                    }
                    courseDetails.put("courseId", courseId);
                    courseDetails.put("courseName", rs.getString("course_name"));
                    courseDetails.put("courseCode", rs.getString("course_code"));
                    courseDetails.put("credits", rs.getInt("credit"));
                }
            }

            // Fetch materials list
            List<Map<String, Object>> materials = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT id, title, material_type, module, file_size, created_at FROM course_material " +
                            "WHERE course_id = ? AND teacher_id = ? ORDER BY created_at DESC"
            )) {
                ps.setLong(1, courseId);
                ps.setLong(2, teacherId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        materials.add(Map.of(
                                "id", rs.getLong("id"),
                                "title", rs.getString("title"),
                                "type", rs.getString("material_type"),
                                "module", rs.getString("module") == null ? "-" : rs.getString("module"),
                                "fileSize", rs.getInt("file_size"),
                                "uploadedAt", rs.getTimestamp("created_at") == null ? new java.sql.Timestamp(System.currentTimeMillis()) : rs.getTimestamp("created_at")
                        ));
                    }
                }
            }
            courseDetails.put("materials", materials);

            // Fetch assignments list
            List<Map<String, Object>> assignments = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT ca.id, ca.title, ca.due_at, ca.max_marks, " +
                            "(SELECT COUNT(*) FROM course_assignment_submission sas WHERE sas.assignment_id = ca.id) AS submission_count " +
                            "FROM course_assignment ca " +
                            "WHERE ca.course_id = ? AND ca.teacher_id = ? ORDER BY ca.created_at DESC"
            )) {
                ps.setLong(1, courseId);
                ps.setLong(2, teacherId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        assignments.add(Map.of(
                                "id", rs.getLong("id"),
                                "title", rs.getString("title"),
                                "dueAt", rs.getTimestamp("due_at"),
                                "maxMarks", rs.getInt("max_marks"),
                                "submissionCount", rs.getInt("submission_count")
                        ));
                    }
                }
            }
            courseDetails.put("assignments", assignments);

            // Fetch quizzes list
            List<Map<String, Object>> quizzes = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT q.id, q.title, q.total_marks, q.duration_minutes, q.start_at, q.end_at, q.is_published, q.is_score_published, " +
                            "(SELECT COUNT(*) FROM quiz_submission qs WHERE qs.quiz_id = q.id) AS submission_count " +
                            "FROM quiz q " +
                            "WHERE q.course_id = ? AND q.teacher_id = ? ORDER BY q.created_at DESC"
            )) {
                ps.setLong(1, courseId);
                ps.setLong(2, teacherId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        quizzes.add(Map.of(
                                "id", rs.getLong("id"),
                                "title", rs.getString("title"),
                                "totalMarks", rs.getInt("total_marks"),
                                "durationMinutes", rs.getInt("duration_minutes"),
                                "startAt", rs.getTimestamp("start_at"),
                                "endAt", rs.getTimestamp("end_at"),
                                "isPublished", rs.getInt("is_published") == 1,
                                "isScorePublished", rs.getInt("is_score_published") == 1,
                                "submissionCount", rs.getInt("submission_count")
                        ));
                    }
                }
            }
            courseDetails.put("quizzes", quizzes);

            return ResponseEntity.ok(courseDetails);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/course/{courseId}/materials/upload")
    public ResponseEntity<?> uploadMaterial(@PathVariable long courseId, @RequestBody Map<String, String> body, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        String title = body.get("title");
        String type = body.get("type");
        String module = body.get("module");

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Title is required."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO course_material (course_id, teacher_id, title, material_type, module, file_size, stored_path, original_filename, stored_filename, download_allowed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)"
            )) {
                ps.setLong(1, courseId);
                ps.setLong(2, teacherId);
                ps.setString(3, title.trim());
                ps.setString(4, type == null ? "PDF" : type);
                ps.setString(5, module);
                ps.setInt(6, 120489); // Simulated file size (120 KB)
                String simName = "simulated_" + System.currentTimeMillis() + "." + (type == null ? "pdf" : type.toLowerCase());
                ps.setString(7, simName);
                ps.setString(8, title.trim() + "." + (type == null ? "pdf" : type.toLowerCase()));
                ps.setString(9, simName);
                ps.executeUpdate();
            }

            return ResponseEntity.ok(Map.of("message", "Study resource uploaded successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to upload resource: " + e.getMessage()));
        }
    }

    @PostMapping("/course/{courseId}/assignments/create")
    public ResponseEntity<?> createAssignment(@PathVariable long courseId, @RequestBody Map<String, String> body, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        String title = body.get("title");
        String description = body.get("description");
        int maxMarks = Integer.parseInt(body.getOrDefault("maxMarks", "100"));
        String dueAtStr = body.get("dueAt"); // Expect ISO Date like 'YYYY-MM-DDTHH:MM'

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Title is required."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            java.sql.Timestamp dueAt = dueAtStr != null ? java.sql.Timestamp.valueOf(dueAtStr.replace("T", " ") + ":00") : null;

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO course_assignment (course_id, teacher_id, title, assignment_mode, instructions, max_marks, due_at) VALUES (?, ?, ?, 'TEXT', ?, ?, ?)"
            )) {
                ps.setLong(1, courseId);
                ps.setLong(2, teacherId);
                ps.setString(3, title.trim());
                ps.setString(4, description);
                ps.setInt(5, maxMarks);
                ps.setTimestamp(6, dueAt);
                ps.executeUpdate();
            }

            return ResponseEntity.ok(Map.of("message", "Assignment created successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create assignment: " + e.getMessage()));
        }
    }

    @PostMapping("/course/{courseId}/quizzes/create")
    public ResponseEntity<?> createQuiz(@PathVariable long courseId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        String title = (String) body.get("title");
        String instructions = (String) body.get("instructions");
        int duration = Integer.parseInt(body.getOrDefault("durationMinutes", "30").toString());
        String startAtStr = (String) body.get("startAt");
        String endAtStr = (String) body.get("endAt");
        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");

        if (title == null || title.trim().isEmpty() || questions == null || questions.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Title and questions list are required."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            con.setAutoCommit(false);

            java.sql.Timestamp startAt = startAtStr != null ? java.sql.Timestamp.valueOf(startAtStr.replace("T", " ") + ":00") : null;
            java.sql.Timestamp endAt = endAtStr != null ? java.sql.Timestamp.valueOf(endAtStr.replace("T", " ") + ":00") : null;

            int totalMarks = 0;
            for (Map<String, Object> q : questions) {
                totalMarks += Integer.parseInt(q.getOrDefault("marks", "1").toString());
            }

            long quizId = 0;
            try (PreparedStatement psQuiz = con.prepareStatement(
                    "INSERT INTO quiz (course_id, teacher_id, title, instructions, total_marks, duration_minutes, start_at, end_at, is_published) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                psQuiz.setLong(1, courseId);
                psQuiz.setLong(2, teacherId);
                psQuiz.setString(3, title.trim());
                psQuiz.setString(4, instructions);
                psQuiz.setInt(5, totalMarks);
                psQuiz.setInt(6, duration);
                psQuiz.setTimestamp(7, startAt);
                psQuiz.setTimestamp(8, endAt);
                psQuiz.executeUpdate();

                try (ResultSet keys = psQuiz.getGeneratedKeys()) {
                    if (keys.next()) {
                        quizId = keys.getLong(1);
                    }
                }
            }

            if (quizId == 0) {
                con.rollback();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to create quiz header."));
            }

            try (PreparedStatement psQ = con.prepareStatement(
                    "INSERT INTO quiz_question (quiz_id, question_order, question_text, question_type, marks, correct_option_index) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                try (PreparedStatement psOpt = con.prepareStatement(
                        "INSERT INTO quiz_option (question_id, option_index, option_text) VALUES (?, ?, ?)"
                )) {
                    int order = 1;
                    for (Map<String, Object> q : questions) {
                        String qtext = (String) q.get("text");
                        String qtype = (String) q.get("type");
                        int marks = Integer.parseInt(q.getOrDefault("marks", "1").toString());
                        Integer correctIndex = q.get("correctOptionIndex") != null ? Integer.parseInt(q.get("correctOptionIndex").toString()) : null;

                        psQ.setLong(1, quizId);
                        psQ.setInt(2, order++);
                        psQ.setString(3, qtext);
                        psQ.setString(4, qtype.toUpperCase());
                        psQ.setInt(5, marks);
                        if (correctIndex == null) psQ.setObject(6, null);
                        else psQ.setInt(6, correctIndex);
                        psQ.executeUpdate();

                        long questionId = 0;
                        try (ResultSet keys = psQ.getGeneratedKeys()) {
                            if (keys.next()) {
                                questionId = keys.getLong(1);
                            }
                        }

                        if ("MCQ".equalsIgnoreCase(qtype) && questionId > 0) {
                            List<String> options = (List<String>) q.get("options");
                            if (options != null) {
                                int optIdx = 1;
                                for (String o : options) {
                                    psOpt.setLong(1, questionId);
                                    psOpt.setInt(2, optIdx++);
                                    psOpt.setString(3, o.trim());
                                    psOpt.addBatch();
                                }
                                psOpt.executeBatch();
                            }
                        }
                    }
                }
            }

            con.commit();
            return ResponseEntity.ok(Map.of("message", "Quiz paper generated successfully!", "quizId", quizId));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create quiz paper: " + e.getMessage()));
        }
    }

    @GetMapping("/assignment/{assignmentId}/submissions")
    public ResponseEntity<?> getAssignmentSubmissions(@PathVariable long assignmentId, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }
            ensureSchema(con);

            List<Map<String, Object>> submissions = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT sas.id, sas.submitted_at, sas.original_filename, sas.marks_awarded, sas.feedback, " +
                            "s.name AS student_name, s.roll_number " +
                            "FROM course_assignment_submission sas " +
                            "JOIN student s ON s.id = sas.student_id " +
                            "WHERE sas.assignment_id = ? ORDER BY sas.submitted_at DESC"
            )) {
                ps.setLong(1, assignmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> sub = new HashMap<>();
                        sub.put("submissionId", rs.getLong("id"));
                        sub.put("submittedAt", rs.getTimestamp("submitted_at"));
                        sub.put("fileName", rs.getString("original_filename"));
                        sub.put("marksAwarded", rs.getObject("marks_awarded"));
                        sub.put("feedback", rs.getString("feedback"));
                        sub.put("studentName", rs.getString("student_name"));
                        sub.put("rollNumber", rs.getString("roll_number"));
                        submissions.add(sub);
                    }
                }
            }

            return ResponseEntity.ok(submissions);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/assignment/grade")
    public ResponseEntity<?> gradeAssignment(@RequestBody Map<String, Object> body, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        long submissionId = Long.parseLong(body.get("submissionId").toString());
        int marks = Integer.parseInt(body.get("marks").toString());
        String feedback = (String) body.get("feedback");

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }
            ensureSchema(con);

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE course_assignment_submission SET marks_awarded = ?, feedback = ? WHERE id = ?"
            )) {
                ps.setInt(1, marks);
                ps.setString(2, feedback);
                ps.setLong(3, submissionId);
                ps.executeUpdate();
            }

            return ResponseEntity.ok(Map.of("message", "Assignment graded successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Grading failed: " + e.getMessage()));
        }
    }

    @GetMapping("/quiz/{quizId}/submissions")
    public ResponseEntity<?> getQuizSubmissions(@PathVariable long quizId, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            // Check if quiz exists and belongs to teacher, get courseId
            long courseId = -1;
            String quizTitle = "";
            int totalMarks = 0;
            try (PreparedStatement psQuiz = con.prepareStatement(
                    "SELECT course_id, title, total_marks FROM quiz WHERE id = ? AND teacher_id = ?"
            )) {
                psQuiz.setLong(1, quizId);
                psQuiz.setLong(2, teacherId);
                try (ResultSet rsQuiz = psQuiz.executeQuery()) {
                    if (rsQuiz.next()) {
                        courseId = rsQuiz.getLong("course_id");
                        quizTitle = rsQuiz.getString("title");
                        totalMarks = rsQuiz.getInt("total_marks");
                    }
                }
            }

            if (courseId == -1) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Quiz not found or not allocated to you."));
            }

            List<Map<String, Object>> submissions = new ArrayList<>();
            try (PreparedStatement psStatus = con.prepareStatement(
                    "SELECT DISTINCT s.id AS student_id, s.name, s.roll_number, qs.submitted_at, qs.score " +
                            "FROM student_course_teacher sct " +
                            "JOIN student s ON s.id = sct.student_id " +
                            "LEFT JOIN quiz_submission qs ON qs.quiz_id = ? AND qs.student_id = s.id " +
                            "WHERE sct.course_id = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND s.current_semester = sct.semester " +
                            "ORDER BY s.roll_number"
            )) {
                psStatus.setLong(1, quizId);
                psStatus.setLong(2, courseId);
                try (ResultSet rs = psStatus.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> sub = new HashMap<>();
                        sub.put("studentId", rs.getLong("student_id"));
                        sub.put("studentName", rs.getString("name"));
                        sub.put("rollNumber", rs.getString("roll_number"));
                        sub.put("submittedAt", rs.getTimestamp("submitted_at"));
                        sub.put("score", rs.getObject("score") == null ? null : rs.getInt("score"));
                        submissions.add(sub);
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                    "quizId", quizId,
                    "title", quizTitle,
                    "totalMarks", totalMarks,
                    "submissions", submissions
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/quiz/{quizId}/publish-score")
    public ResponseEntity<?> publishQuizScore(@PathVariable long quizId, @RequestBody Map<String, Boolean> body, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        boolean publish = body.getOrDefault("published", false);

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE quiz SET is_score_published = ? WHERE id = ? AND teacher_id = ?"
            )) {
                ps.setInt(1, publish ? 1 : 0);
                ps.setLong(2, quizId);
                ps.setLong(3, teacherId);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Quiz not found or not allocated to you."));
                }
            }

            return ResponseEntity.ok(Map.of("message", "Quiz scores " + (publish ? "published" : "unpublished") + " successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update publish status: " + e.getMessage()));
        }
    }

    private java.sql.Timestamp parseTimestamp(String dtStr) {
        if (dtStr == null || dtStr.trim().isEmpty()) return null;
        try {
            String clean = dtStr.replace("T", " ").replace("Z", "");
            if (clean.length() > 19) clean = clean.substring(0, 19);
            else if (clean.length() == 16) clean = clean + ":00";
            return java.sql.Timestamp.valueOf(clean);
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/quiz/{quizId}/student/{studentId}/answers")
    public ResponseEntity<?> getStudentQuizAnswers(@PathVariable long quizId, @PathVariable long studentId, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            // Verify quiz owner and get submission details
            long submissionId = -1;
            String studentName = "";
            String rollNumber = "";
            String quizTitle = "";
            int score = 0;
            int totalMarks = 0;

            try (PreparedStatement psSub = con.prepareStatement(
                    "SELECT qs.id AS submission_id, qs.submitted_at, qs.score, " +
                            "s.name AS student_name, s.roll_number, " +
                            "q.title AS quiz_title, q.total_marks " +
                            "FROM quiz_submission qs " +
                            "JOIN quiz q ON q.id = qs.quiz_id " +
                            "JOIN student s ON s.id = qs.student_id " +
                            "WHERE qs.quiz_id = ? AND qs.student_id = ? AND q.teacher_id = ?"
            )) {
                psSub.setLong(1, quizId);
                psSub.setLong(2, studentId);
                psSub.setLong(3, teacherId);
                try (ResultSet rsSub = psSub.executeQuery()) {
                    if (rsSub.next()) {
                        submissionId = rsSub.getLong("submission_id");
                        studentName = rsSub.getString("student_name");
                        rollNumber = rsSub.getString("roll_number");
                        quizTitle = rsSub.getString("quiz_title");
                        score = rsSub.getInt("score");
                        totalMarks = rsSub.getInt("total_marks");
                    }
                }
            }

            if (submissionId == -1) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Submission not found or quiz does not belong to you."));
            }

            List<Map<String, Object>> answers = new ArrayList<>();
            try (PreparedStatement psAns = con.prepareStatement(
                    "SELECT qq.id AS question_id, qq.question_order, qq.question_text, qq.question_type, qq.marks, qq.correct_option_index, " +
                            "qa.selected_option_index, qa.answer_text, qa.marks_awarded " +
                            "FROM quiz_question qq " +
                            "LEFT JOIN quiz_answer qa ON qa.question_id = qq.id AND qa.submission_id = ? " +
                            "WHERE qq.quiz_id = ? " +
                            "ORDER BY qq.question_order"
            )) {
                psAns.setLong(1, submissionId);
                psAns.setLong(2, quizId);
                try (ResultSet rsAns = psAns.executeQuery()) {
                    while (rsAns.next()) {
                        Map<String, Object> row = new HashMap<>();
                        long qid = rsAns.getLong("question_id");
                        row.put("questionId", qid);
                        row.put("order", rsAns.getInt("question_order"));
                        row.put("text", rsAns.getString("question_text"));
                        row.put("type", rsAns.getString("question_type"));
                        row.put("marks", rsAns.getInt("marks"));

                        int ci = rsAns.getInt("correct_option_index");
                        row.put("correctOptionIndex", rsAns.wasNull() ? null : ci);

                        int sel = rsAns.getInt("selected_option_index");
                        row.put("selectedOptionIndex", rsAns.wasNull() ? null : sel);

                        row.put("answerText", rsAns.getString("answer_text"));

                        int ma = rsAns.getInt("marks_awarded");
                        row.put("marksAwarded", rsAns.wasNull() ? null : ma);

                        if ("MCQ".equalsIgnoreCase(rsAns.getString("question_type"))) {
                            List<String> optionsList = new ArrayList<>();
                            try (PreparedStatement psOpt = con.prepareStatement(
                                    "SELECT option_text FROM quiz_option WHERE question_id = ? ORDER BY option_index"
                            )) {
                                psOpt.setLong(1, qid);
                                try (ResultSet rsOpt = psOpt.executeQuery()) {
                                    while (rsOpt.next()) {
                                        optionsList.add(rsOpt.getString("option_text"));
                                    }
                                }
                            }
                            row.put("options", optionsList);
                        }
                        answers.add(row);
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                    "quizTitle", quizTitle,
                    "studentName", studentName,
                    "rollNumber", rollNumber,
                    "score", score,
                    "totalMarks", totalMarks,
                    "answers", answers
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/quiz/{quizId}/update")
    public ResponseEntity<?> updateQuizDetails(@PathVariable long quizId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        String title = (String) body.get("title");
        String instructions = (String) body.get("instructions");
        int duration = Integer.parseInt(body.getOrDefault("durationMinutes", "30").toString());
        String startAtStr = (String) body.get("startAt");
        String endAtStr = (String) body.get("endAt");

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Title is required."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            java.sql.Timestamp startAt = parseTimestamp(startAtStr);
            java.sql.Timestamp endAt = parseTimestamp(endAtStr);

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE quiz SET title = ?, instructions = ?, duration_minutes = ?, start_at = ?, end_at = ? WHERE id = ? AND teacher_id = ?"
            )) {
                ps.setString(1, title.trim());
                ps.setString(2, instructions);
                ps.setInt(3, duration);
                ps.setTimestamp(4, startAt);
                ps.setTimestamp(5, endAt);
                ps.setLong(6, quizId);
                ps.setLong(7, teacherId);

                int rows = ps.executeUpdate();
                if (rows == 0) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Quiz not found or not allocated to you."));
                }
            }

            return ResponseEntity.ok(Map.of("message", "Quiz details updated successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update quiz: " + e.getMessage()));
        }
    }

    @GetMapping("/course/{courseId}/students")
    public ResponseEntity<?> getCourseStudents(@PathVariable long courseId, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            List<Map<String, Object>> students = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT DISTINCT s.name, s.roll_number, s.email, d.dept_name, sct.semester, sct.status " +
                            "FROM student_course_teacher sct " +
                            "JOIN student s ON sct.student_id = s.id " +
                            "JOIN department d ON s.dept_id = d.id " +
                            "WHERE sct.course_id = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND s.current_semester = sct.semester " +
                            "ORDER BY s.roll_number"
            )) {
                ps.setLong(1, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> stu = new HashMap<>();
                        stu.put("name", rs.getString("name"));
                        stu.put("rollNumber", rs.getString("roll_number"));
                        stu.put("email", rs.getString("email"));
                        stu.put("department", rs.getString("dept_name"));
                        stu.put("semester", rs.getInt("semester"));
                        stu.put("status", rs.getString("status") == null ? "ACTIVE" : rs.getString("status"));
                        students.add(stu);
                    }
                }
            }

            return ResponseEntity.ok(students);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/course/{courseId}/feedbacks")
    public ResponseEntity<?> getCourseFeedbacks(@PathVariable long courseId, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            int eligibleCount = 0;
            try (PreparedStatement psEligible = con.prepareStatement(
                    "SELECT COUNT(DISTINCT sct.student_id) AS cnt " +
                            "FROM student_course_teacher sct " +
                            "JOIN student s ON s.id = sct.student_id " +
                            "WHERE sct.course_id = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND s.current_semester = sct.semester"
            )) {
                psEligible.setLong(1, courseId);
                try (ResultSet rsEligible = psEligible.executeQuery()) {
                    if (rsEligible.next()) eligibleCount = rsEligible.getInt("cnt");
                }
            }

            List<Map<String, Object>> forms = new ArrayList<>();
            try (PreparedStatement psForms = con.prepareStatement(
                    "SELECT ff.id, ff.course_id, ff.teacher_id, ff.title, ff.description, ff.is_active, ff.created_at, " +
                            "(SELECT COUNT(*) FROM feedback_question fq WHERE fq.form_id = ff.id) AS question_count, " +
                            "(SELECT COUNT(*) FROM feedback_submission fs WHERE fs.form_id = ff.id) AS submitted_count " +
                            "FROM feedback_form ff " +
                            "WHERE ff.teacher_id = ? AND ff.course_id = ? " +
                            "ORDER BY ff.created_at DESC"
            )) {
                psForms.setLong(1, teacherId);
                psForms.setLong(2, courseId);
                try (ResultSet rsForms = psForms.executeQuery()) {
                    while (rsForms.next()) {
                        Map<String, Object> f = new HashMap<>();
                        f.put("id", rsForms.getLong("id"));
                        f.put("courseId", rsForms.getLong("course_id"));
                        f.put("teacherId", rsForms.getLong("teacher_id"));
                        f.put("title", rsForms.getString("title"));
                        f.put("description", rsForms.getString("description"));
                        f.put("isActive", rsForms.getInt("is_active") == 1);
                        f.put("createdAt", rsForms.getTimestamp("created_at"));
                        f.put("questionCount", rsForms.getInt("question_count"));
                        f.put("submittedCount", rsForms.getInt("submitted_count"));
                        f.put("eligibleCount", eligibleCount);
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

    @PostMapping("/course/{courseId}/feedbacks/create")
    public ResponseEntity<?> createFeedbackForm(@PathVariable long courseId, @RequestBody Map<String, Object> body, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        String title = (String) body.get("title");
        String description = (String) body.get("description");
        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Title is required."));
        }
        if (questions == null || questions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "At least one question is required."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            con.setAutoCommit(false);

            long formId;
            try (PreparedStatement psForm = con.prepareStatement(
                    "INSERT INTO feedback_form (course_id, teacher_id, title, description, is_active) VALUES (?, ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                psForm.setLong(1, courseId);
                psForm.setLong(2, teacherId);
                psForm.setString(3, title.trim());
                psForm.setString(4, description);
                psForm.executeUpdate();

                try (ResultSet keys = psForm.getGeneratedKeys()) {
                    if (!keys.next()) {
                        con.rollback();
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("message", "Failed to generate feedback form ID."));
                    }
                    formId = keys.getLong(1);
                }
            }

            try (PreparedStatement psQ = con.prepareStatement(
                    "INSERT INTO feedback_question (form_id, question_order, question_text, question_type, options_text, rating_max, required) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 1)"
            )) {
                int index = 1;
                for (Map<String, Object> q : questions) {
                    String qText = (String) q.get("text");
                    String qType = (String) q.get("type");
                    if (qText == null || qText.trim().isEmpty()) continue;
                    if (qType == null) qType = "TEXT";

                    psQ.setLong(1, formId);
                    psQ.setInt(2, index++);
                    psQ.setString(3, qText.trim());
                    psQ.setString(4, qType.toUpperCase());

                    if ("RATING".equalsIgnoreCase(qType)) {
                        Object rMaxObj = q.get("ratingMax");
                        int rMax = 5;
                        if (rMaxObj instanceof Number) {
                            rMax = ((Number) rMaxObj).intValue();
                        }
                        psQ.setObject(5, null);
                        psQ.setInt(6, rMax);
                    } else {
                        psQ.setObject(5, null);
                        psQ.setObject(6, null);
                    }
                    psQ.addBatch();
                }
                psQ.executeBatch();
            }

            con.commit();
            return ResponseEntity.ok(Map.of("message", "Feedback Survey generated successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create feedback: " + e.getMessage()));
        }
    }

    @GetMapping("/feedback/{formId}/submissions")
    public ResponseEntity<?> getFeedbackSubmissions(@PathVariable long formId, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            long courseId = -1;
            String formTitle = "";
            String formDesc = "";
            try (PreparedStatement psForm = con.prepareStatement(
                    "SELECT course_id, title, description FROM feedback_form WHERE id = ? AND teacher_id = ?"
            )) {
                psForm.setLong(1, formId);
                psForm.setLong(2, teacherId);
                try (ResultSet rs = psForm.executeQuery()) {
                    if (rs.next()) {
                        courseId = rs.getLong("course_id");
                        formTitle = rs.getString("title");
                        formDesc = rs.getString("description");
                    }
                }
            }

            if (courseId == -1) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Feedback form not found or not allocated to you."));
            }

            List<Map<String, Object>> submissions = new ArrayList<>();
            try (PreparedStatement psStatus = con.prepareStatement(
                    "SELECT DISTINCT s.id AS student_id, s.name, s.roll_number, d.dept_name, fs.submitted_at " +
                            "FROM student_course_teacher sct " +
                            "JOIN student s ON s.id = sct.student_id " +
                            "JOIN department d ON d.id = s.dept_id " +
                            "LEFT JOIN feedback_submission fs ON fs.form_id = ? AND fs.student_id = s.id " +
                            "WHERE sct.course_id = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND s.current_semester = sct.semester " +
                            "ORDER BY s.roll_number"
            )) {
                psStatus.setLong(1, formId);
                psStatus.setLong(2, courseId);
                try (ResultSet rsStatus = psStatus.executeQuery()) {
                    while (rsStatus.next()) {
                        Map<String, Object> sub = new HashMap<>();
                        sub.put("studentId", rsStatus.getLong("student_id"));
                        sub.put("studentName", rsStatus.getString("name"));
                        sub.put("rollNumber", rsStatus.getString("roll_number"));
                        sub.put("department", rsStatus.getString("dept_name"));
                        sub.put("submittedAt", rsStatus.getTimestamp("submitted_at"));
                        submissions.add(sub);
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                    "formId", formId,
                    "title", formTitle,
                    "description", formDesc,
                    "submissions", submissions
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/feedback/{formId}/student/{studentId}/answers")
    public ResponseEntity<?> getStudentFeedbackAnswers(@PathVariable long formId, @PathVariable long studentId, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            String studentName = "";
            String rollNumber = "";
            try (PreparedStatement psStu = con.prepareStatement(
                    "SELECT name, roll_number FROM student WHERE id = ?"
            )) {
                psStu.setLong(1, studentId);
                try (ResultSet rs = psStu.executeQuery()) {
                    if (rs.next()) {
                        studentName = rs.getString("name");
                        rollNumber = rs.getString("roll_number");
                    }
                }
            }

            long submissionId = -1;
            try (PreparedStatement psSub = con.prepareStatement(
                    "SELECT id FROM feedback_submission WHERE form_id = ? AND student_id = ?"
            )) {
                psSub.setLong(1, formId);
                psSub.setLong(2, studentId);
                try (ResultSet rs = psSub.executeQuery()) {
                    if (rs.next()) {
                        submissionId = rs.getLong("id");
                    }
                }
            }

            if (submissionId == -1) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No submission found for this student."));
            }

            List<Map<String, Object>> answers = new ArrayList<>();
            try (PreparedStatement psAns = con.prepareStatement(
                    "SELECT fq.question_order, fq.question_text, fq.question_type, fq.rating_max, " +
                            "fa.answer_text, fa.answer_number " +
                            "FROM feedback_question fq " +
                            "LEFT JOIN feedback_answer fa ON fa.question_id = fq.id AND fa.submission_id = ? " +
                            "WHERE fq.form_id = ? " +
                            "ORDER BY fq.question_order"
            )) {
                psAns.setLong(1, submissionId);
                psAns.setLong(2, formId);
                try (ResultSet rsAns = psAns.executeQuery()) {
                    while (rsAns.next()) {
                        Map<String, Object> ans = new HashMap<>();
                        ans.put("order", rsAns.getInt("question_order"));
                        ans.put("text", rsAns.getString("question_text"));
                        ans.put("type", rsAns.getString("question_type"));
                        ans.put("answerText", rsAns.getString("answer_text"));
                        int val = rsAns.getInt("answer_number");
                        ans.put("answerNumber", rsAns.wasNull() ? null : val);
                        int rMax = rsAns.getInt("rating_max");
                        ans.put("ratingMax", rsAns.wasNull() ? null : rMax);
                        answers.add(ans);
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                    "studentName", studentName,
                    "rollNumber", rollNumber,
                    "answers", answers
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/feedback/{formId}/toggle")
    public ResponseEntity<?> toggleFeedbackActive(@PathVariable long formId, @RequestBody Map<String, Boolean> body, HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized. Please log in as a teacher."));
        }

        Boolean active = body.get("active");
        if (active == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Active flag is required."));
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Database connection failed."));
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE feedback_form SET is_active = ? WHERE id = ? AND teacher_id = ?"
            )) {
                ps.setInt(1, active ? 1 : 0);
                ps.setLong(2, formId);
                ps.setLong(3, teacherId);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("message", "Feedback form not found or unauthorized."));
                }
            }

            return ResponseEntity.ok(Map.of("message", "Feedback survey status updated successfully!"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred: " + e.getMessage()));
        }
    }
}
