package com.vetri.smartcampus.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        String message = URLEncoder.encode("File is too large. Maximum upload size is 100 MB.", StandardCharsets.UTF_8);
        return buildUploadRedirect(request, message);
    }

    @ExceptionHandler(MultipartException.class)
    public String handleMultipartException(MultipartException ex, HttpServletRequest request) {
        String message = URLEncoder.encode("Upload request could not be processed. Check file size and form inputs, then try again.", StandardCharsets.UTF_8);
        return buildUploadRedirect(request, message);
    }

    private String buildUploadRedirect(HttpServletRequest request, String message) {
        String courseId = request.getParameter("courseId");
        if ("/teacher-upload-material".equals(request.getRequestURI())) {
            if (courseId != null && !courseId.isBlank()) {
                return "redirect:/teacher-upload-material?courseId=" + courseId + "&uploadError=" + message;
            }
            return "redirect:/teacher-upload-material?uploadError=" + message;
        }
        if ("/teacher-upload-assignment".equals(request.getRequestURI())) {
            if (courseId != null && !courseId.isBlank()) {
                return "redirect:/teacher-upload-assignment?courseId=" + courseId + "&uploadError=" + message;
            }
            return "redirect:/teacher-upload-assignment?uploadError=" + message;
        }
        return "redirect:/teacher-dashboard?uploadError=" + message;
    }
}
