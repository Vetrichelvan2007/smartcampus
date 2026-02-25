package com.vetri.smartcampus.controllers;

import com.vetri.smartcampus.models.DataBaseConnection;
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

    @GetMapping("/login")
    public String login(HttpSession session) {

        Object sid = session.getAttribute("studentId");
        if (sid != null) {
            return "redirect:/student-dashboard";
        }
        return "Auth/Login";
    }

    @GetMapping("/")
    public String welcome() {
        return "Auth/Welcome";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username, @RequestParam String password, @RequestParam String role, HttpSession session) {

        try {

            Connection con = DataBaseConnection.getConnection();
            PreparedStatement userPs = DataBaseConnection.getPreparedStatement(con,"SELECT * FROM users WHERE email=? AND password=? AND role=?");

            userPs.setString(1, username);
            userPs.setString(2, password);
            userPs.setString(3, role);

            ResultSet userRs = userPs.executeQuery();

            if (userRs.next()) {

                int userId = userRs.getInt("id");

                session.setAttribute("userId", userId);
                session.setAttribute("email", userRs.getString("email"));
                session.setAttribute("role", userRs.getString("role"));


                if ("student".equals(role)) {

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
                if ("teacher".equals(role)) {

                    PreparedStatement teacherPs =
                            DataBaseConnection.getPreparedStatement(con,
                                    "SELECT * FROM teacher WHERE email=?");

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
