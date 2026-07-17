package com.vetri.smartcampus.controllers;

import com.vetri.smartcampus.models.common.DataBaseConnection;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Controller
public class AuthController {

    private String resolveDashboardRedirect(HttpSession session) {
        Object studentId = session.getAttribute("studentId");
        if (studentId != null) {
            return "redirect:/student-dashboard";
        }

        Object teacherId = session.getAttribute("teacherId");
        if (teacherId != null) {
            return "redirect:/teacher-dashboard";
        }

        Object role = session.getAttribute("role");
        if (role != null) {
            if ("student".equalsIgnoreCase(role.toString())) {
                return "redirect:/student-dashboard";
            }
            if ("teacher".equalsIgnoreCase(role.toString())) {
                return "redirect:/teacher-dashboard";
            }
            if ("admin".equalsIgnoreCase(role.toString())) {
                return "redirect:/admin-dashboard";
            }
        }

        return null;
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

    @GetMapping({"/old-login", "/old-login.html"})
    public String login(HttpSession session) {
        String dashboardRedirect = resolveDashboardRedirect(session);
        return dashboardRedirect != null ? dashboardRedirect : "Auth/Login";
    }

    @GetMapping("/old-welcome")
    public String welcome() {
        return "Auth/Welcome";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username, @RequestParam String password, HttpSession session) {

        try {

            Connection con = DataBaseConnection.getConnection();
            PreparedStatement userPs = DataBaseConnection.getPreparedStatement(con,"SELECT * FROM users WHERE email=? AND password=?");

            userPs.setString(1, username);
            userPs.setString(2, password);

            ResultSet userRs = userPs.executeQuery();

            if (userRs.next()) {
                clearRoleSpecificSession(session);

                int userId = userRs.getInt("id");
                String role = userRs.getString("role");

                session.setAttribute("userId", userId);
                session.setAttribute("email", userRs.getString("email"));
                session.setAttribute("role", role);


                if ("student".equalsIgnoreCase(role)) {

                    PreparedStatement studentPs = DataBaseConnection.getPreparedStatement(con,"SELECT * FROM student WHERE email=?");
                    studentPs.setString(1, username);

                    ResultSet studentRs = studentPs.executeQuery();

                    if (studentRs.next()) {
                        session.setAttribute("studentId", studentRs.getLong("id"));
                        session.setAttribute("studentRollNumber", studentRs.getString("roll_number"));
                        session.setAttribute("studentName", studentRs.getString("name"));
                        session.setAttribute("department_id", studentRs.getString("dept_id"));
                    }
                    studentPs.close();
                    studentRs.close();

                    return "redirect:/student-dashboard";
                }
                if ("teacher".equalsIgnoreCase(role)) {

                    PreparedStatement teacherPs =
                            DataBaseConnection.getPreparedStatement(con,
                                    "SELECT t.id, t.name, t.email, t.teacher_clg_id, t.account_status, " +
                                            "e.department_id, e.designation, e.staff_type " +
                                            "FROM teacher t " +
                                            "LEFT JOIN teacher_employment_details e ON t.id = e.teacher_id " +
                                            "WHERE t.email=?");

                    teacherPs.setString(1, username);

                    ResultSet teacherRs = teacherPs.executeQuery();

                    if (teacherRs.next()) {

                        session.setAttribute("teacherId", teacherRs.getLong("id"));
                        session.setAttribute("teacherName", teacherRs.getString("name"));
                        session.setAttribute("teacherEmail", teacherRs.getString("email"));
                        session.setAttribute("teacherClgId", teacherRs.getString("teacher_clg_id"));
                        session.setAttribute("department_id", teacherRs.getLong("department_id"));
                        session.setAttribute("designation", teacherRs.getString("designation"));
                        session.setAttribute("staffType", teacherRs.getString("staff_type"));
                        session.setAttribute("accountStatus", teacherRs.getString("account_status"));

                    }

                    teacherRs.close();
                    teacherPs.close();

                    return "redirect:/teacher-dashboard";
                }
                if ("admin".equalsIgnoreCase(role)) {
                    return "redirect:/admin-dashboard";
                }

                session.invalidate();
            }
            userPs.close();
            userRs.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return "redirect:/";
    }

}
