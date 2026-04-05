package com.vetri.smartcampus.controllers.teacher;

import com.vetri.smartcampus.models.common.DataBaseConnection;
import com.vetri.smartcampus.models.teacher.AssignedCourses;
import jakarta.servlet.http.HttpSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class TeacherControllerSupport {

    protected static final DateTimeFormatter UI_DTF = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a", Locale.ENGLISH);
    protected static final int ASSIGNMENT_TEXT_WORD_LIMIT = 500;

    protected static String fmt(LocalDateTime dt) {
        return dt == null ? "-" : dt.format(UI_DTF);
    }

    protected static int parseLeadingInt(String raw, int defaultValue) {
        if (raw == null) return defaultValue;
        Matcher m = Pattern.compile("(\\d+)").matcher(raw);
        if (!m.find()) return defaultValue;
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
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

    protected static int countWords(String raw) {
        if (raw == null) return 0;
        String text = raw.trim();
        if (text.isEmpty()) return 0;
        return text.split("\\s+").length;
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
                        "AND table_name = 'course_assignment'"
        );
        ResultSet rs = ps.executeQuery();
        int cnt = 0;
        if (rs.next()) cnt = rs.getInt("cnt");
        rs.close();
        ps.close();
        return cnt == 1;
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

    protected static Long getTeacherId(HttpSession session) {
        Object tid = session.getAttribute("teacherId");
        if (tid == null) {
            return null;
        }
        return Long.parseLong(tid.toString());
    }

    protected static List<AssignedCourses> loadAssignedCourses(Connection con, long teacherId) throws Exception {
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
                        "WHERE cta.teacher_id = ? " +
                        "ORDER BY c.course_name"
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
        rs.close();
        ps.close();
        return courses;
    }

    protected static boolean isTeacherCourseAllocated(Connection con, long teacherId, long courseId) throws Exception {
        PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM course_teacher_allocation WHERE teacher_id = ? AND course_id = ? LIMIT 1"
        );
        ps.setLong(1, teacherId);
        ps.setLong(2, courseId);
        ResultSet rs = ps.executeQuery();
        boolean allowed = rs.next();
        rs.close();
        ps.close();
        return allowed;
    }

    protected static AssignedCourses findAssignedCourse(List<AssignedCourses> courses, long courseId) {
        for (AssignedCourses course : courses) {
            if (course.getCourseid() == courseId) {
                return course;
            }
        }
        return null;
    }

    protected static Connection openConnection() throws Exception {
        return DataBaseConnection.getConnection();
    }
}
