package com.vetri.smartcampus.controllers.teacher;

import com.vetri.smartcampus.models.common.CourseMaterialDTO;
import com.vetri.smartcampus.models.teacher.AssignedCourses;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class TeacherMaterialController extends TeacherControllerSupport {

    @GetMapping("/teacher-upload-material")
    public String teacherUploadMaterial(HttpSession session,
                                        @RequestParam(value = "courseId", required = false) Long courseId,
                                        Model model) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return "redirect:/login";
        }
        model.addAttribute("teacherId", teacherId);

        try {
            Connection con = openConnection();
            List<AssignedCourses> courses = loadAssignedCourses(con, teacherId);
            model.addAttribute("courses", courses);

            if (courseId != null) {
                if (!isTeacherCourseAllocated(con, teacherId, courseId)) {
                    con.close();
                    return "redirect:/teacher-upload-material";
                }

                model.addAttribute("selectedCourse", findAssignedCourse(courses, courseId));

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
        Long teacherId = getTeacherId(session);
        if (teacherId == null) {
            return "redirect:/login";
        }

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
            Connection con = openConnection();

            if (!isTeacherCourseAllocated(con, teacherId, courseId)) {
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
            try {
                response.sendError(500);
            } catch (Exception ignored) {
            }
        }
    }
}
