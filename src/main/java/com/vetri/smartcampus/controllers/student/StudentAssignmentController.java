package com.vetri.smartcampus.controllers.student;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

@Controller
public class StudentAssignmentController extends StudentControllerSupport {

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
        Long studentId = getStudentId(session);
        if (studentId == null) return "redirect:/login";

        try {
            Connection con = openConnection();
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
                    try {
                        Files.deleteIfExists(Paths.get(oldPath));
                    } catch (Exception ignored) {
                    }
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
        Long studentId = getStudentId(session);
        if (studentId == null) {
            try {
                response.sendRedirect("/login");
            } catch (Exception ignored) {
            }
            return;
        }

        try {
            Connection con = openConnection();
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
            try {
                response.sendError(500);
            } catch (Exception ignored) {
            }
        }
    }
}
