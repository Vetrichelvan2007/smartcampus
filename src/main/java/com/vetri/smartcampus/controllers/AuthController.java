package com.vetri.smartcampus.controllers;

import com.vetri.smartcampus.models.DataBaseConnection;
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
    public String login() {
        return "Login";
    }

    @GetMapping("/")
    public String welcome() {
        return "Welcome";
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
            }
            userPs.close();
            userRs.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/login";
    }

}
