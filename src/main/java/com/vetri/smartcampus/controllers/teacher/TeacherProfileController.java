package com.vetri.smartcampus.controllers.teacher;

import com.vetri.smartcampus.models.teacher.TeacherDTO;
import com.vetri.smartcampus.models.teacher.TeacherQualificationDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Controller
public class TeacherProfileController extends TeacherControllerSupport {

    @GetMapping("/old-profile")
    public String teacherProfile(HttpSession session, Model model) {
        try {
            Long teacherId = getTeacherId(session);
            if (teacherId == null) {
                return "redirect:/login";
            }

            Connection con = openConnection();

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
                rs.close();
                ps.close();
                con.close();
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
            teacher.setExperienceYears(parseLeadingInt(rs.getString("experience_years"), 0));
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

            PreparedStatement psQual = con.prepareStatement(
                    "SELECT teacher_id, ug_degree, pg_degree, teacher_qualification_details.phd_status, " +
                            "specialization, university_name, year_of_passing " +
                            "FROM teacher_qualification_details WHERE teacher_id = ?"
            );

            psQual.setLong(1, teacherId);
            ResultSet rsQual = psQual.executeQuery();
            List<TeacherQualificationDTO> qualificationDetails = new ArrayList<>();

            while (rsQual.next()) {
                TeacherQualificationDTO qualification = new TeacherQualificationDTO();
                qualification.setTeacherId(rsQual.getLong("teacher_id"));
                qualification.setUgDegree(rsQual.getString("ug_degree"));
                qualification.setPgDegree(rsQual.getString("pg_degree"));
                qualification.setPhdStatus(rsQual.getString("phd_status"));
                qualification.setSpecialization(rsQual.getString("specialization"));
                qualification.setUniversityName(rsQual.getString("university_name"));
                qualification.setYearOfPassing(rsQual.getInt("year_of_passing"));
                qualificationDetails.add(qualification);
            }

            model.addAttribute("qualificationDetails", qualificationDetails);

            rsQual.close();
            psQual.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }

        return "teacher/profile";
    }
}
