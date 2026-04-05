package com.vetri.smartcampus.controllers.student;

import com.vetri.smartcampus.models.common.CourseAssignmentDTO;
import com.vetri.smartcampus.models.common.CourseMaterialDTO;
import com.vetri.smartcampus.models.common.DataBaseConnection;
import com.vetri.smartcampus.models.student.StudentQuizListDTO;
import jakarta.servlet.http.HttpSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

abstract class StudentControllerSupport {

    protected static final DateTimeFormatter UI_DTF = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a", Locale.ENGLISH);

    protected static String fmt(LocalDateTime dt) {
        return dt == null ? "-" : dt.format(UI_DTF);
    }

    protected static String quizStatus(LocalDateTime now, LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && now.isBefore(startAt)) return "UPCOMING";
        if (endAt != null && now.isAfter(endAt)) return "CLOSED";
        return "ACTIVE";
    }

    protected static List<String> splitNonEmptyLines(String raw) {
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

    protected static boolean feedbackTablesExist(Connection con) throws Exception {
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

    protected static boolean materialTablesExist(Connection con) throws Exception {
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

    protected static boolean assignmentTablesExist(Connection con) throws Exception {
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

    protected static boolean quizTablesExist(Connection con) throws Exception {
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

    protected static List<CourseAssignmentDTO> loadAssignmentsForCourse(Connection con, long studentId, long courseId) throws Exception {
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

    protected static List<CourseMaterialDTO> loadMaterialsForCourse(Connection con, long studentId, long courseId) throws Exception {
        List<CourseMaterialDTO> resources = new ArrayList<>();
        if (!materialTablesExist(con)) return resources;

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
        return resources;
    }

    protected static List<StudentQuizListDTO> loadQuizzesForCourse(Connection con, long studentId, long courseId) throws Exception {
        List<StudentQuizListDTO> quizzes = new ArrayList<>();
        if (!quizTablesExist(con)) return quizzes;

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
            int score = rs.getInt("score");
            dto.setScore(rs.wasNull() ? null : score);
            quizzes.add(dto);
        }
        rs.close();
        ps.close();
        return quizzes;
    }

    protected static Long getStudentId(HttpSession session) {
        Object sid = session.getAttribute("studentId");
        if (sid == null) {
            return null;
        }
        return Long.parseLong(sid.toString());
    }

    protected static Connection openConnection() throws Exception {
        return DataBaseConnection.getConnection();
    }
}
