package com.vetri.smartcampus.controllers;

import com.vetri.smartcampus.models.DataBaseConnection;
import com.vetri.smartcampus.models.TeacherDTO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

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

                    "SELECT t.*, " +
                            "p.gender, p.date_of_birth, p.blood_group, p.address, " +
                            "e.department_id, e.designation, e.employment_type, e.joining_date, e.experience_years, e.office_location, e.staff_type, " +
                            "q.phd_status, q.specialization, q.university_name, q.year_of_passing, " +
                            "r.papers_published, r.conferences_attended, r.workshops_attended, r.patents, r.funded_projects, " +
                            "l.casual_leave_balance, l.medical_leave_balance, l.earned_leave_balance " +

                            "FROM teacher t " +
                            "LEFT JOIN teacher_personal_details p ON t.id = p.teacher_id " +
                            "LEFT JOIN teacher_employment_details e ON t.id = e.teacher_id " +
                            "LEFT JOIN teacher_qualification_details q ON t.id = q.teacher_id " +
                            "LEFT JOIN teacher_research_publications r ON t.id = r.teacher_id " +
                            "LEFT JOIN teacher_leave_balance l ON t.id = l.teacher_id " +
                            "WHERE t.id = ?"

            );

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

            rs.close();
            ps.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }

        return "teacher/profile";
    }

}
