package com.vetri.smartcampus.controllers;

import com.vetri.smartcampus.models.DataBaseConnection;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Controller
public class TeacherController {

    @GetMapping("/teacher-dashboard")
    public String teacerdashboard(){
        return "Teacher/Dashboard";
    }

    @GetMapping("/teacher-mysubjects")
    public String teachermysubjects(){
        return "Teacher/MySubjects";
    }

    @GetMapping("/view-subject")
    public String viewsubject(){
        return "Teacher/ViewSubject";
    }

    @GetMapping("/profile")
    public String teacherProfile(HttpSession session, Model model) {

        try {
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }

            Long teacherId = Long.parseLong(tid.toString());
            Connection con = DataBaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT * FROM teacher WHERE id = ?");
            ps.setLong(1, teacherId);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return "redirect:/login";
            }

            TeacherDTO teacher = new TeacherDTO();

            teacher.setId(rs.getLong("id"));
            teacher.setTeacherClgId(rs.getString("teacher_clg_id"));
            teacher.setName(rs.getString("name"));
            teacher.setEmail(rs.getString("email"));
            teacher.setPhone(rs.getString("phone"));
            teacher.setGender(rs.getString("gender"));
            teacher.setDateOfBirth(rs.getDate("date_of_birth"));
            teacher.setBloodGroup(rs.getString("blood_group"));
            teacher.setAddress(rs.getString("address"));
            teacher.setDesignation(rs.getString("designation"));
            teacher.setEmploymentType(rs.getString("employment_type"));
            teacher.setJoiningDate(rs.getDate("joining_date"));
            teacher.setExperienceYears(rs.getInt("experience_years"));
            teacher.setOfficeLocation(rs.getString("office_location"));
            teacher.setStaffType(rs.getString("staff_type"));

            teacher.setPapersPublished(rs.getInt("papers_published"));
            teacher.setConferencesAttended(rs.getInt("conferences_attended"));
            teacher.setWorkshopsAttended(rs.getInt("workshops_attended"));
            teacher.setPatents(rs.getInt("patents"));
            teacher.setFundedProjects(rs.getInt("funded_projects"));

            teacher.setCasualLeaveBalance(rs.getInt("casual_leave_balance"));
            teacher.setMedicalLeaveBalance(rs.getInt("medical_leave_balance"));
            teacher.setEarnedLeaveBalance(rs.getInt("earned_leave_balance"));

            teacher.setUsername(rs.getString("username"));
            teacher.setAccountStatus(rs.getString("account_status"));
            teacher.setLastLogin(rs.getTimestamp("last_login"));

            teacher.setPhdStatus(rs.getString("phd_status"));
            teacher.setSpecialization(rs.getString("specialization"));
            teacher.setUniversityName(rs.getString("university_name"));
            teacher.setYearOfPassing(rs.getInt("year_of_passing"));

            model.addAttribute("teacher", teacher);
            System.out.println("Blood Group: " + rs.getString("blood_group"));

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }

        return "teacher/profile";   // ✅ Correct view path
    }

}
