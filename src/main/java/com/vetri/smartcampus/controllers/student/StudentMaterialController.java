package com.vetri.smartcampus.controllers.student;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Controller
public class StudentMaterialController extends StudentControllerSupport {

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
            try {
                response.sendError(500);
            } catch (Exception ignored) {
            }
        }
    }
}
