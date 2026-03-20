package com.vetri.smartcampus.controllers;

import com.vetri.smartcampus.models.AssignedCourses;
import com.vetri.smartcampus.models.DataBaseConnection;
import com.vetri.smartcampus.models.TeacherDTO;
import com.vetri.smartcampus.models.TeacherQualificationDTO;
import com.vetri.smartcampus.models.ViewStudentDTO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
@Controller
public class TeacherController {

    @GetMapping("/teacher-dashboard")
    public String teacherDashboard(HttpSession session, Model model){

        try{

            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }

            Long teacherId = Long.parseLong(tid.toString());

            Connection con = DataBaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT DISTINCT\n" +
                            "c.id as course_id,\n"+
                            "c.course_name,\n" +
                            "c.course_code,\n" +
                            "d.dept_name,\n" +
                            "cfd.sem,\n" +
                            "c.credit\n" +
                            "FROM course_teacher_allocation cta\n" +
                            "JOIN course c ON c.id = cta.course_id\n" +
                            "JOIN course_for_depts cfd ON cfd.course_id = c.id\n" +
                            "JOIN department d ON d.id = cfd.dept_id\n" +
                            "WHERE cta.teacher_id = ?"
            );

            ps.setLong(1, teacherId);

            ResultSet rs = ps.executeQuery();

            List<AssignedCourses> courses = new ArrayList<>();

            while(rs.next()){

                AssignedCourses ac = new AssignedCourses();
                ac.setCourseid(rs.getLong("course_id"));
                ac.setCoursename(rs.getString("course_name"));
                ac.setCoursecode(rs.getString("course_code"));
                ac.setCoursedept(rs.getString("dept_name"));
                ac.setSem(rs.getInt("sem"));
                ac.setCredits(rs.getInt("credit"));

                courses.add(ac);
            }

            model.addAttribute("courses", courses);

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }

        return "Teacher/Dashboard";
    }

    @GetMapping("/teacher-mysubjects")
    public String teachermysubjects(){
        return "Teacher/MySubjects";
    }

    @GetMapping("/teacher-create-quiz")
    public String teacherCreateQuiz() {
        return "Teacher/CreateQuiz";
    }

    @GetMapping("/teacher-view-student/{teacherId}/{courseId}")
    public String teacherViewStudentByCourse(@PathVariable("teacherId") long teacherId, @PathVariable("courseId") long courseId, Model model) {
        try {
            Connection con = DataBaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT s.name, s.roll_number, d.dept_name, sct.semester, sct.status " +
                            "FROM student_course_teacher sct " +
                            "JOIN student s ON sct.student_id = s.id " +
                            "JOIN department d ON s.dept_id = d.id " +
                            "WHERE sct.teacher_id = ? AND sct.course_id = ?"
            );

            ps.setLong(1, teacherId);
            ps.setLong(2, courseId);

            ResultSet rs = ps.executeQuery();
            List<ViewStudentDTO> students = new ArrayList<>();

            while (rs.next()) {
                ViewStudentDTO dto = new ViewStudentDTO();
                dto.setStudentName(rs.getString("name"));
                dto.setStudentRegisterNumber(rs.getString("roll_number"));
                dto.setDepartment(rs.getString("dept_name"));
                dto.setSemester(rs.getInt("semester"));
                dto.setStatus(rs.getString("status"));
                students.add(dto);
            }

            model.addAttribute("teacherId", teacherId);
            model.addAttribute("courseId", courseId);
            model.addAttribute("students", students);

            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Teacher/ViewStudent";
    }

    @GetMapping("/teacher-upload-material")
    public String teacherUploadMaterial() {
        return "Teacher/UploadMaterial";
    }

    @PostMapping("/teacher-upload-material")
    public String teacherUploadMaterialSubmit() {
        return "Teacher/UploadMaterial";
    }

    @GetMapping("/teacher-assign-feedback")
    public String teacherAssignFeedback() {
        return "Teacher/AssignFeedback";
    }

    @PostMapping("/teacher-assign-feedback")
    public String teacherAssignFeedbackSubmit() {
        return "Teacher/AssignFeedback";
    }


    @GetMapping("/view-subject/{courseid}")
    public String viewsubject(@PathVariable("courseid") int courseid, Model model, HttpSession session){

        try{
            Object tid = session.getAttribute("teacherId");
            if (tid == null) {
                return "redirect:/login";
            }

            Long teacherId = Long.parseLong(tid.toString());
            model.addAttribute("teacherId", teacherId);

            Connection con = DataBaseConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT c.id, c.course_name, c.course_code, c.credit, cfd.sem, d.dept_name " +
                            "FROM course c " +
                            "JOIN course_for_depts cfd ON c.id = cfd.course_id " +
                            "JOIN department d ON cfd.dept_id = d.id " +
                            "WHERE c.id = ?"
            );

            ps.setInt(1, courseid);

            ResultSet rs = ps.executeQuery();

            AssignedCourses course = new AssignedCourses();

            if(rs.next()){
                course.setCourseid(rs.getLong("id"));
                course.setCoursename(rs.getString("course_name"));
                course.setCoursecode(rs.getString("course_code"));
                course.setCredits(rs.getInt("credit"));
                course.setSem(rs.getInt("sem"));
                course.setCoursedept(rs.getString("dept_name"));
            }

            model.addAttribute("course", course);

        } catch (Exception e) {
            e.printStackTrace();
        }

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

            PreparedStatement psQual = con.prepareStatement(
                    "SELECT teacher_id, ug_degree, pg_degree, teacher_qualification_details.phd_status, " +
                            "specialization, university_name, year_of_passing " +
                            "FROM teacher_qualification_details WHERE teacher_id = 1"
            );

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
