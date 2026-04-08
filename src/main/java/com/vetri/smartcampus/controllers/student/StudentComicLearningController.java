package com.vetri.smartcampus.controllers.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetri.smartcampus.models.student.ComicLearningRequest;
import com.vetri.smartcampus.services.ComicLearningClient;
import com.vetri.smartcampus.services.CourseMaterialExtractionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

@Controller
public class StudentComicLearningController extends StudentControllerSupport {

    private final ComicLearningClient comicLearningClient;
    private final CourseMaterialExtractionService courseMaterialExtractionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudentComicLearningController(ComicLearningClient comicLearningClient,
                                          CourseMaterialExtractionService courseMaterialExtractionService) {
        this.comicLearningClient = comicLearningClient;
        this.courseMaterialExtractionService = courseMaterialExtractionService;
    }

    @PostMapping(value = "/course/{courseCode}/comic-learning/stream", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> streamComic(@PathVariable String courseCode,
                                                             @RequestBody ComicLearningRequest request,
                                                             HttpSession session) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(outputStream -> writeJson(outputStream, Map.of("message", "Login required")));
        }

        if (!hasCourseAccess(studentId, courseCode)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(outputStream -> writeJson(outputStream, Map.of("message", "Course not found for this student")));
        }

        Long materialId = request.getMaterialId();
        String materialContent = trimToNull(request.getMaterialContent());
        String userQuery = trimToNull(request.getUserQuery());
        String studentLevel = trimToNull(request.getStudentLevel());
        int panelCount = clampPanelCount(request.getPanelCount());

        if ((materialContent == null && materialId == null) || userQuery == null || studentLevel == null) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(outputStream -> writeJson(outputStream, Map.of(
                            "message", "Choose a study material or provide notes, and include userQuery and studentLevel"
                    )));
        }

        String resolvedMaterialContent = materialContent;
        if (materialId != null) {
            try {
                String extractedMaterial = courseMaterialExtractionService.extractForStudent(studentId, courseCode, materialId);
                if (extractedMaterial == null || extractedMaterial.isBlank()) {
                    return ResponseEntity.badRequest()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(outputStream -> writeJson(outputStream, Map.of(
                                    "message", "The selected study material could not be converted into text. Paste notes manually for this file."
                            )));
                }
                resolvedMaterialContent = materialContent == null
                        ? extractedMaterial
                        : extractedMaterial + "\n\nAdditional student notes:\n" + materialContent;
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(outputStream -> writeJson(outputStream, Map.of(
                                "message", "Unable to read the selected study material. " + summarize(e.getMessage())
                        )));
            }
        }

        ComicLearningRequest upstreamRequest = new ComicLearningRequest();
        upstreamRequest.setMaterialContent(resolvedMaterialContent);
        upstreamRequest.setUserQuery(userQuery);
        upstreamRequest.setStudentLevel(studentLevel);
        upstreamRequest.setPanelCount(panelCount);

        StreamingResponseBody body = outputStream -> {
            try {
                comicLearningClient.streamComic(upstreamRequest, outputStream);
            } catch (Exception e) {
                writeSseError(outputStream, "Comic generator unavailable. " + summarize(e.getMessage()));
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    @GetMapping("/comic-learning/generated-image")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> serveGeneratedImage(@RequestParam("path") String imagePath,
                                                                     HttpSession session) {
        if (getStudentId(session) == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Path file = validateGeneratedImagePath(imagePath);
            MediaType mediaType = MediaType.parseMediaType(detectMediaType(file));

            StreamingResponseBody body = outputStream -> {
                try (var inputStream = Files.newInputStream(file)) {
                    StreamUtils.copy(inputStream, outputStream);
                }
            };

            return ResponseEntity.status(HttpStatusCode.valueOf(200))
                    .contentType(mediaType)
                    .cacheControl(CacheControl.noStore())
                    .body(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private boolean hasCourseAccess(long studentId, String courseCode) {
        try {
            Connection con = openConnection();
            PreparedStatement ps = con.prepareStatement(
                    "SELECT 1 " +
                            "FROM course c " +
                            "JOIN student_course_teacher sct ON sct.course_id = c.id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE c.course_code = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester " +
                            "LIMIT 1"
            );
            ps.setLong(1, studentId);
            ps.setString(2, courseCode);
            ResultSet rs = ps.executeQuery();
            boolean found = rs.next();
            rs.close();
            ps.close();
            con.close();
            return found;
        } catch (Exception e) {
            return false;
        }
    }

    private void writeJson(java.io.OutputStream outputStream, Map<String, String> body) throws IOException {
        outputStream.write(objectMapper.writeValueAsBytes(body));
        outputStream.flush();
    }

    private void writeSseError(java.io.OutputStream outputStream, String message) throws IOException {
        String payload = objectMapper.writeValueAsString(Map.of("message", message));
        outputStream.write(("event: error\n" + "data: " + payload + "\n\n").getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    private static int clampPanelCount(Integer panelCount) {
        if (panelCount == null) {
            return 4;
        }
        return Math.max(1, Math.min(panelCount, 8));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String summarize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Check that the local Python API is running on http://localhost:8000.";
        }
        String normalized = raw.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 220) {
            return normalized.substring(0, 220) + "...";
        }
        return normalized;
    }

    private static Path validateGeneratedImagePath(String rawPath) throws IOException {
        if (rawPath == null || rawPath.isBlank()) {
            throw new IOException("Image path missing");
        }

        Path file = Paths.get(rawPath).toAbsolutePath().normalize();
        String filename = file.getFileName() == null ? "" : file.getFileName().toString().toLowerCase();
        if (!filename.matches("panel_[a-z0-9]+\\.(png|jpg|jpeg|webp)$")) {
            throw new IOException("Unsupported image file");
        }

        Path parent = file.getParent();
        String parentName = parent == null || parent.getFileName() == null ? "" : parent.getFileName().toString().toLowerCase();
        if (!"output".equals(parentName) || !Files.exists(file) || !Files.isReadable(file)) {
            throw new IOException("Image not accessible");
        }

        return file;
    }

    private static String detectMediaType(Path file) throws IOException {
        String detected = Files.probeContentType(file);
        if (detected != null && !detected.isBlank()) {
            return detected;
        }

        String filename = file.getFileName() == null ? "" : file.getFileName().toString().toLowerCase();
        if (filename.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return MediaType.IMAGE_JPEG_VALUE;
        if (filename.endsWith(".webp")) return "image/webp";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
