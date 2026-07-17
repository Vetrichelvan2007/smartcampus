package com.vetri.smartcampus.controllers.teacher;

import com.vetri.smartcampus.models.common.CourseAssignmentDTO;
import com.vetri.smartcampus.models.teacher.AssignedCourses;
import com.vetri.smartcampus.models.teacher.AssignmentStudentStatusDTO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class TeacherAssignmentController extends TeacherControllerSupport {

    @GetMapping("/old-teacher-upload-assignment")
    public String teacherUploadAssignment(HttpSession session,
                                          @RequestParam(value = "courseId", required = false) Long courseId,
                                          Model model) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) return "redirect:/login";
        model.addAttribute("teacherId", teacherId);

        try {
            Connection con = openConnection();
            List<AssignedCourses> courses = loadAssignedCourses(con, teacherId);
            model.addAttribute("courses", courses);

            if (courseId != null) {
                if (!isTeacherCourseAllocated(con, teacherId, courseId)) {
                    con.close();
                    return "redirect:/old-teacher-upload-assignment";
                }

                model.addAttribute("selectedCourse", findAssignedCourse(courses, courseId));

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

    @PostMapping("/old-teacher-upload-assignment")
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
        Long teacherId = getTeacherId(session);
        if (teacherId == null) return "redirect:/login";

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
            Connection con = openConnection();
            if (!isTeacherCourseAllocated(con, teacherId, courseId)) {
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
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
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
            try {
                response.sendError(500);
            } catch (Exception ignored) {
            }
        }
    }

    @GetMapping("/old-teacher-assignment/{assignmentId}")
    public String teacherAssignmentStatus(@PathVariable("assignmentId") long assignmentId,
                                          HttpSession session,
                                          Model model) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) return "redirect:/login";

        try {
            Connection con = openConnection();
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
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
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
            try {
                response.sendError(500);
            } catch (Exception ignored) {
            }
        }
    }
}
