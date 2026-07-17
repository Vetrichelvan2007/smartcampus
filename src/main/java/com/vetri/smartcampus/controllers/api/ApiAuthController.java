package com.vetri.smartcampus.controllers.api;

import com.vetri.smartcampus.models.common.DataBaseConnection;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpSession session) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Username and password are required.");
            return ResponseEntity.badRequest().body(error);
        }

        try (Connection con = DataBaseConnection.getConnection()) {
            if (con == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Database connection failed.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }

            try (PreparedStatement userPs = con.prepareStatement("SELECT * FROM users WHERE email=? AND password=?")) {
                userPs.setString(1, username);
                userPs.setString(2, password);

                try (ResultSet userRs = userPs.executeQuery()) {
                    if (userRs.next()) {
                        clearRoleSpecificSession(session);

                        int userId = userRs.getInt("id");
                        String role = userRs.getString("role");

                        session.setAttribute("userId", userId);
                        session.setAttribute("email", userRs.getString("email"));
                        session.setAttribute("role", role);

                        Map<String, Object> responseData = new HashMap<>();
                        responseData.put("success", true);
                        responseData.put("userId", userId);
                        responseData.put("email", userRs.getString("email"));
                        responseData.put("role", role);

                        if ("student".equalsIgnoreCase(role)) {
                            try (PreparedStatement studentPs = con.prepareStatement("SELECT * FROM student WHERE email=?")) {
                                studentPs.setString(1, username);
                                try (ResultSet studentRs = studentPs.executeQuery()) {
                                    if (studentRs.next()) {
                                        long studentId = studentRs.getLong("id");
                                        String name = studentRs.getString("name");
                                        String rollNum = studentRs.getString("roll_number");
                                        String deptId = studentRs.getString("dept_id");

                                        session.setAttribute("studentId", studentId);
                                        session.setAttribute("studentRollNumber", rollNum);
                                        session.setAttribute("studentName", name);
                                        session.setAttribute("department_id", deptId);

                                        responseData.put("name", name);
                                        responseData.put("rollNumber", rollNum);
                                    }
                                }
                            }
                        } else if ("teacher".equalsIgnoreCase(role)) {
                            try (PreparedStatement teacherPs = con.prepareStatement(
                                    "SELECT t.id, t.name, t.email, t.teacher_clg_id, t.account_status, " +
                                            "e.department_id, e.designation, e.staff_type " +
                                            "FROM teacher t " +
                                            "LEFT JOIN teacher_employment_details e ON t.id = e.teacher_id " +
                                            "WHERE t.email=?")) {
                                teacherPs.setString(1, username);
                                try (ResultSet teacherRs = teacherPs.executeQuery()) {
                                    if (teacherRs.next()) {
                                        long teacherId = teacherRs.getLong("id");
                                        String name = teacherRs.getString("name");
                                        session.setAttribute("teacherId", teacherId);
                                        session.setAttribute("teacherName", name);
                                        session.setAttribute("teacherEmail", teacherRs.getString("email"));
                                        session.setAttribute("teacherClgId", teacherRs.getString("teacher_clg_id"));
                                        session.setAttribute("department_id", teacherRs.getLong("department_id"));
                                        session.setAttribute("designation", teacherRs.getString("designation"));
                                        session.setAttribute("staffType", teacherRs.getString("staff_type"));
                                        session.setAttribute("accountStatus", teacherRs.getString("account_status"));

                                        responseData.put("name", name);
                                    }
                                }
                            }
                        }

                        return ResponseEntity.ok(responseData);
                    }
                }
            }

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Invalid username or password.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "An error occurred during authentication: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(HttpSession session) {
        Object userId = session.getAttribute("userId");
        Object role = session.getAttribute("role");
        Object email = session.getAttribute("email");

        if (userId == null || role == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        Map<String, Object> user = new HashMap<>();
        user.put("authenticated", true);
        user.put("userId", userId);
        user.put("email", email);
        user.put("role", role);

        if ("student".equalsIgnoreCase(role.toString())) {
            user.put("name", session.getAttribute("studentName"));
            user.put("rollNumber", session.getAttribute("studentRollNumber"));
            user.put("studentId", session.getAttribute("studentId"));
            user.put("departmentId", session.getAttribute("department_id"));
        } else if ("teacher".equalsIgnoreCase(role.toString())) {
            user.put("name", session.getAttribute("teacherName"));
            user.put("teacherId", session.getAttribute("teacherId"));
            user.put("departmentId", session.getAttribute("department_id"));
        } else if ("admin".equalsIgnoreCase(role.toString())) {
            user.put("name", "Administrator");
        }

        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    private void clearRoleSpecificSession(HttpSession session) {
        session.removeAttribute("studentId");
        session.removeAttribute("studentRollNumber");
        session.removeAttribute("studentName");
        session.removeAttribute("teacherId");
        session.removeAttribute("teacherName");
        session.removeAttribute("teacherEmail");
        session.removeAttribute("teacherClgId");
        session.removeAttribute("designation");
        session.removeAttribute("staffType");
        session.removeAttribute("accountStatus");
        session.removeAttribute("department_id");
    }
}
