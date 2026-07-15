package com.vetri.smartcampus.controllers.student;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Controller
public class StudentProfileController extends StudentControllerSupport {

    @GetMapping("/student-profile")
    public String studentProfile(HttpSession session, Model model) {
        Long studentId = getStudentId(session);
        if (studentId == null) {
            return "redirect:/login";
        }

        try {
            Connection con = openConnection();

            PreparedStatement studentPs = con.prepareStatement(
                    "SELECT s.id, s.roll_number, s.name, s.dob, s.gender, s.blood_group, s.mother_tongue, s.nationality, s.address, s.dept_id, d.dept_name, s.current_year, s.current_semester, s.email, s.phone " +
                            "FROM student s JOIN department d ON s.dept_id = d.id WHERE s.id = ?"
            );
            studentPs.setLong(1, studentId);
            ResultSet studentRs = studentPs.executeQuery();
            if (studentRs.next()) {
                model.addAttribute("studentName", studentRs.getString("name"));
                model.addAttribute("studentRollNumber", studentRs.getString("roll_number"));
                model.addAttribute("studentEmail", studentRs.getString("email"));
                model.addAttribute("studentDepartmet", studentRs.getString("dept_name"));
                model.addAttribute("studentYear", studentRs.getString("current_year"));
                model.addAttribute("studentSem", studentRs.getString("current_semester"));
                model.addAttribute("studentDob", studentRs.getString("dob"));
                model.addAttribute("studentGender", studentRs.getString("gender"));
                model.addAttribute("studentNationality", studentRs.getString("nationality"));
                model.addAttribute("studentBloodGroup", studentRs.getString("blood_group"));
                model.addAttribute("studentMotherTongue", studentRs.getString("mother_tongue"));
                model.addAttribute("studentAddress", studentRs.getString("address"));
                model.addAttribute("studentPhone", studentRs.getString("phone"));
            }
            studentRs.close();
            studentPs.close();

            PreparedStatement fatherPs = con.prepareStatement("SELECT * FROM father_details WHERE student_id = ?");
            fatherPs.setLong(1, studentId);
            ResultSet fatherRs = fatherPs.executeQuery();
            if (fatherRs.next()) {
                model.addAttribute("fatherName", fatherRs.getString("name"));
                model.addAttribute("fatherPhone", fatherRs.getString("phone"));
                model.addAttribute("fatherEmail", fatherRs.getString("email"));
                model.addAttribute("fatherOccupation", fatherRs.getString("occupation"));
                model.addAttribute("fatherAnnual_income", fatherRs.getString("annual_income"));
                model.addAttribute("fatherAddress", fatherRs.getString("address"));
            }
            fatherRs.close();
            fatherPs.close();

            PreparedStatement motherPs = con.prepareStatement("SELECT * FROM mother_details WHERE student_id = ?");
            motherPs.setLong(1, studentId);
            ResultSet motherRs = motherPs.executeQuery();
            if (motherRs.next()) {
                model.addAttribute("motherName", motherRs.getString("name"));
                model.addAttribute("motherPhone", motherRs.getString("phone"));
                model.addAttribute("motherEmail", motherRs.getString("email"));
                model.addAttribute("motherOccupation", motherRs.getString("occupation"));
                model.addAttribute("motherAnnual_income", motherRs.getString("annual_income"));
                model.addAttribute("motherAddress", motherRs.getString("address"));
            }
            motherRs.close();
            motherPs.close();

            PreparedStatement identityPs = con.prepareStatement("SELECT * FROM identity_details WHERE student_id = ?");
            identityPs.setLong(1, studentId);
            ResultSet identityRs = identityPs.executeQuery();
            if (identityRs.next()) {
                model.addAttribute("aadharNumber", identityRs.getString("aadhar_number"));
                model.addAttribute("panNumber", identityRs.getString("pan_number"));
                model.addAttribute("passportNumber", identityRs.getString("passport_number"));
            }
            identityRs.close();
            identityPs.close();

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Student/StudentProfile";
    }
}
