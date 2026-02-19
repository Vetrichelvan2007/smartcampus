package com.vetri.smartcampus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.*;

import com.vetri.smartcampus.models.CourseData;
import com.vetri.smartcampus.models.CourseRegistration;
import com.vetri.smartcampus.models.DataBaseConnection;

import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class studentController {

    @GetMapping("/login")
    public String login() {
        return "Login";
    }

    @GetMapping("/")
    public String welcome() {
        return "Login";
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

    @GetMapping("/student-dashboard")
    public String studentDashboard() {
        return "StudentDashboard";
    }

    @GetMapping("/student-profile")
    public String studentprofile(HttpSession session, Model model) {

        Object sidObj = session.getAttribute("studentId");

        if (sidObj == null) {
            return "redirect:/login";
        }

        long studentId = Long.parseLong(sidObj.toString());

        String url = "jdbc:mysql://localhost:3306/smartcampus";
        String dbUser = "root";
        String dbPass = "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(url, dbUser, dbPass);

            PreparedStatement student_ps = con.prepareStatement("SELECT s.id, s.roll_number, s.name, s.dob, s.gender, s.blood_group, s.mother_tongue, s.nationality, s.address, s.dept_id, d.dept_name, s.current_year, s.current_semester, s.email, s.phone FROM student s JOIN department d ON s.dept_id = d.id WHERE s.id = ?;");
            student_ps.setLong(1, studentId);
            ResultSet student_rs = student_ps.executeQuery();

            if(student_rs.next()){
                model.addAttribute("studentName",student_rs.getString("name"));
                model.addAttribute("studentRollNumber",student_rs.getString("roll_number"));
                model.addAttribute("studentEmail",student_rs.getString("email"));
                model.addAttribute("studentDepartmet",student_rs.getString("dept_name"));
                model.addAttribute("studentYear",student_rs.getString("current_year"));
                model.addAttribute("studentSem",student_rs.getString("current_semester"));
                model.addAttribute("studentDob",student_rs.getString("dob"));
                model.addAttribute("studentGender",student_rs.getString("gender"));
                model.addAttribute("studentNationality",student_rs.getString("nationality"));
                model.addAttribute("studentBloodGroup",student_rs.getString("blood_group"));
                model.addAttribute("studentMotherTongue",student_rs.getString("mother_tongue"));
                model.addAttribute("studentAddress",student_rs.getString("address"));
                model.addAttribute("studentPhone",student_rs.getString("phone"));
            }


            PreparedStatement father_ps = con.prepareStatement("SELECT * FROM father_details WHERE student_id = ?");
            father_ps.setLong(1, studentId);
            ResultSet father_rs = father_ps.executeQuery();

            if (father_rs.next()) {
                model.addAttribute("fatherName",father_rs.getString("name"));
                model.addAttribute("fatherPhone",father_rs.getString("phone"));
                model.addAttribute("fatherEmail",father_rs.getString("email"));
                model.addAttribute("fatherOccupation",father_rs.getString("occupation"));
                model.addAttribute("fatherAnnual_income",father_rs.getString("annual_income"));
                model.addAttribute("fatherAddress",father_rs.getString("address"));
            }

            PreparedStatement mother_ps = con.prepareStatement("SELECT * FROM mother_details WHERE student_id = ?");
            mother_ps.setLong(1, studentId);
            ResultSet mother_rs = mother_ps.executeQuery();

            if (mother_rs.next()) {
                model.addAttribute("motherName",mother_rs.getString("name"));
                model.addAttribute("motherPhone",mother_rs.getString("phone"));
                model.addAttribute("motherEmail",mother_rs.getString("email"));
                model.addAttribute("motherOccupation",mother_rs.getString("occupation"));
                model.addAttribute("motherAnnual_income",mother_rs.getString("annual_income"));
                model.addAttribute("motherAddress",mother_rs.getString("address"));
            }

            PreparedStatement identity_ps = con.prepareStatement("SELECT * from identity_details where student_id=?");
            identity_ps.setLong(1,studentId);
            ResultSet indentity_rs = identity_ps.executeQuery();

            if(indentity_rs.next()){
                model.addAttribute("aadharNumber",indentity_rs.getString("aadhar_number"));
                model.addAttribute("panNumber",indentity_rs.getString("pan_number"));
                model.addAttribute("passportNumber",indentity_rs.getString("passport_number"));
            }

            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "StudentProfile";
    }

    @GetMapping("/student-classroom")
    public String classroom(Model model, HttpSession session) {

        List<CourseData> courseDatas = new ArrayList<>();

        Object deptIdObj = session.getAttribute("department_id");
        Object sidObj = session.getAttribute("studentId");

        if (deptIdObj == null || sidObj == null) {
            return "redirect:/login";
        }

        int deptId = Integer.parseInt(deptIdObj.toString());
        long studentId = Long.parseLong(sidObj.toString());

        int current_sem = 0;

        try {
            Connection con1 = DataBaseConnection.getConnection();
            PreparedStatement ps1 = DataBaseConnection.getPreparedStatement(con1,
                    "SELECT current_semester FROM student WHERE id=?"
            );
            ps1.setLong(1, studentId);

            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                current_sem = rs1.getInt("current_semester");
            }

            rs1.close();
            ps1.close();
            con1.close();

            Connection con2 = DataBaseConnection.getConnection();
            PreparedStatement ps2 = DataBaseConnection.getPreparedStatement(con2,
                    "SELECT d.dept_name, cd.sem, c.course_name, c.course_type, c.course_code " +
                            "FROM course_for_depts cd " +
                            "JOIN course c ON c.id = cd.course_id " +
                            "JOIN department d ON d.id = cd.dept_id " +
                            "WHERE cd.dept_id = ? AND cd.sem <= ? ORDER BY cd.sem"
            );

            ps2.setInt(1, deptId);
            ps2.setInt(2, current_sem);

            ResultSet rs2 = ps2.executeQuery();

            while (rs2.next()) {
                int sem = rs2.getInt("sem");
                    courseDatas.add(new CourseData(
                            rs2.getString("course_name"),
                            rs2.getString("course_code"),
                            rs2.getString("course_type"),
                            sem
                    ));
            }

            model.addAttribute("courseDatas", courseDatas);
            model.addAttribute("current_sem",current_sem);
            rs2.close();
            ps2.close();
            con2.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Classroom";
    }

    @GetMapping("/course/{courseCode}")
    public String course(@PathVariable String courseCode, Model model) {
        model.addAttribute("courseId", courseCode);

        try{
            Connection con = DataBaseConnection.getConnection();

            PreparedStatement ps = DataBaseConnection.getPreparedStatement(con,"Select course_name from course where course_code=?");
            ps.setString(1,courseCode);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                model.addAttribute("courseName",rs.getString("course_name"));
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        model.addAttribute("resources", List.of());
        return "Course";
    }

    @GetMapping("/course/{id}/quizzes")
    public String quizzes(@PathVariable String id, Model model) {
        model.addAttribute("courseId", id);
        model.addAttribute("courseName", "DBMS");
        model.addAttribute("activeTab", "quizzes");
        model.addAttribute("resources", List.of());
        return "Course";
    }

    @GetMapping("/courseregistration")
    public String courseregistration(HttpSession session, Model model){

        try{
            Connection con = DataBaseConnection.getConnection();

            Object sid = session.getAttribute("studentId");
            if (sid == null) {
                return "redirect:/login";
            }

            Long studentId = Long.parseLong(sid.toString());

            // 🔹 Get student department + semester
            PreparedStatement ps1 = DataBaseConnection.getPreparedStatement(
                    con,
                    "SELECT dept_id, current_semester FROM student WHERE id=?");

            ps1.setLong(1, studentId);
            ResultSet rs1 = ps1.executeQuery();

            Long deptId = null;
            int currentSemester = 1;

            if(rs1.next()){
                deptId = rs1.getLong("dept_id");
                currentSemester = rs1.getInt("current_semester");
            }

            model.addAttribute("current_semester", currentSemester);

            // 🔥 Fetch courses based on dept + semester (WITH course_code)
            PreparedStatement ps2 = DataBaseConnection.getPreparedStatement(con,
                    "SELECT c.id, c.course_code, c.course_name, c.course_type, cfd.sem, " +
                            "t.id, t.name " +
                            "FROM course_for_depts cfd " +
                            "JOIN course c ON c.id = cfd.course_id " +
                            "JOIN course_teacher_allocation cta ON c.id = cta.course_id " +
                            "JOIN teacher t ON cta.teacher_id = t.id " +
                            "WHERE cfd.dept_id = ? AND cfd.sem = ? " +
                            "ORDER BY c.id");

            ps2.setLong(1, deptId);
            ps2.setInt(2, currentSemester);

            ResultSet rs2 = ps2.executeQuery();

            Map<Long, CourseRegistration> courseMap = new LinkedHashMap<>();

            while(rs2.next()){

                Long courseId = rs2.getLong(1);
                String courseCode = rs2.getString(2);   // 🔥 new
                String courseName = rs2.getString(3);
                String courseType = rs2.getString(4);
                int courseSem = rs2.getInt(5);
                Long teacherId = rs2.getLong(6);
                String teacherName = rs2.getString(7);

                CourseRegistration course = courseMap.get(courseId);

                if(course == null){
                    course = new CourseRegistration(courseId, courseName, courseType);
                    course.setCourseCode(courseCode);   // 🔥 set code
                    course.setCourseSem(courseSem);
                    course.setTeacherIds(new ArrayList<>());
                    course.setTeacherNames(new ArrayList<>());
                    courseMap.put(courseId, course);
                }

                course.getTeacherIds().add(teacherId);
                course.getTeacherNames().add(teacherName);
            }

            List<CourseRegistration> courses =
                    new ArrayList<>(courseMap.values());

            model.addAttribute("courses", courses);

        } catch(Exception e){
            e.printStackTrace();
        }

        return "CourseRegistration";
    }


    @GetMapping("/feedback")
    public String feedback(HttpSession session, Model model){
        return "Feedback";
    }

    @GetMapping("/online-exam")
    public String onlineexam(){
        return "OnlineExam";
    }

    @GetMapping("/calendar")
    public String calendar(){
        return "Calendar";
    }

    @GetMapping("/drive")
    public String drive(){
        return "Drive";
    }

}
