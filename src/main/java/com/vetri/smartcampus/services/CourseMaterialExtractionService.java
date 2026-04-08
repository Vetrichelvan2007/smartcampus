package com.vetri.smartcampus.services;

import com.vetri.smartcampus.models.common.DataBaseConnection;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Service
public class CourseMaterialExtractionService {

    private static final int MAX_EXTRACTED_CHARS = 24_000;

    private final Tika tika = new Tika();

    public String extractForStudent(long studentId, String courseCode, long materialId) throws Exception {
        MaterialFile materialFile = loadMaterialFile(studentId, courseCode, materialId);
        if (materialFile == null) {
            return null;
        }

        Path file = resolveMaterialPath(materialFile.storedPath());
        String extracted = extractText(file, materialFile.mimeType());
        if (extracted == null || extracted.isBlank()) {
            return null;
        }
        return clamp(extracted);
    }

    private MaterialFile loadMaterialFile(long studentId, String courseCode, long materialId) throws Exception {
        Connection con = DataBaseConnection.getConnection();
        PreparedStatement ps = con.prepareStatement(
                "SELECT cm.stored_path, cm.mime_type " +
                        "FROM course_material cm " +
                        "JOIN course c ON c.id = cm.course_id " +
                        "JOIN student_course_teacher sct ON sct.course_id = cm.course_id AND sct.teacher_id = cm.teacher_id AND sct.student_id = ? " +
                        "JOIN student st ON st.id = sct.student_id " +
                        "WHERE c.course_code = ? AND cm.id = ? " +
                        "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                        "AND st.current_semester = sct.semester"
        );
        ps.setLong(1, studentId);
        ps.setString(2, courseCode);
        ps.setLong(3, materialId);
        ResultSet rs = ps.executeQuery();

        MaterialFile materialFile = null;
        if (rs.next()) {
            materialFile = new MaterialFile(rs.getString("stored_path"), rs.getString("mime_type"));
        }

        rs.close();
        ps.close();
        con.close();
        return materialFile;
    }

    private Path resolveMaterialPath(String storedPath) throws IOException {
        if (storedPath == null || storedPath.contains("..")) {
            throw new IOException("Material path is invalid.");
        }

        Path base = Paths.get("material").toAbsolutePath().normalize();
        Path file = Paths.get(storedPath).toAbsolutePath().normalize();
        if (!file.startsWith(base) || !Files.exists(file)) {
            throw new IOException("Material file was not found.");
        }
        return file;
    }

    private String extractText(Path file, String mimeType) throws IOException {
        String extracted;
        try {
            extracted = tika.parseToString(file);
        } catch (Exception e) {
            extracted = "";
        }

        if (!extracted.isBlank()) {
            return extracted;
        }

        if (mimeType != null && mimeType.startsWith("text")) {
            return Files.readString(file, StandardCharsets.UTF_8);
        }

        return extracted;
    }

    private String clamp(String raw) {
        String normalized = raw.replace("\u0000", "")
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        if (normalized.length() <= MAX_EXTRACTED_CHARS) {
            return normalized;
        }
        return normalized.substring(0, MAX_EXTRACTED_CHARS) + "\n\n[Content truncated for streaming.]";
    }

    private record MaterialFile(String storedPath, String mimeType) {
    }
}
