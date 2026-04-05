package com.vetri.smartcampus.controllers.student;

import com.vetri.smartcampus.models.common.FeedbackQuestionDTO;
import com.vetri.smartcampus.models.student.StudentFeedbackFormListDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class StudentFeedbackController extends StudentControllerSupport {

    @GetMapping("/feedback")
    public String feedback(HttpSession session, Model model) {
        try {
            Long studentId = getStudentId(session);
            if (studentId == null) {
                return "redirect:/login";
            }

            Connection con = openConnection();
            if (!feedbackTablesExist(con)) {
                con.close();
                model.addAttribute("feedbackDbMissing", true);
                model.addAttribute("forms", List.of());
                return "Student/Feedback";
            }

            PreparedStatement ps = con.prepareStatement(
                    "SELECT ff.id AS form_id, ff.title, ff.description, ff.created_at, " +
                            "c.course_name, c.course_code, t.name AS teacher_name, " +
                            "CASE WHEN fs.id IS NULL THEN 0 ELSE 1 END AS submitted " +
                            "FROM feedback_form ff " +
                            "JOIN student_course_teacher sct ON sct.course_id = ff.course_id AND sct.teacher_id = ff.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "JOIN course c ON c.id = ff.course_id " +
                            "JOIN teacher t ON t.id = ff.teacher_id " +
                            "LEFT JOIN feedback_submission fs ON fs.form_id = ff.id AND fs.student_id = ? " +
                            "WHERE ff.is_active = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester " +
                            "ORDER BY ff.created_at DESC"
            );
            ps.setLong(1, studentId);
            ps.setLong(2, studentId);
            ResultSet rs = ps.executeQuery();

            List<StudentFeedbackFormListDTO> forms = new ArrayList<>();
            while (rs.next()) {
                StudentFeedbackFormListDTO dto = new StudentFeedbackFormListDTO();
                dto.setFormId(rs.getLong("form_id"));
                dto.setTitle(rs.getString("title"));
                dto.setDescription(rs.getString("description"));
                dto.setCourseName(rs.getString("course_name"));
                dto.setCourseCode(rs.getString("course_code"));
                dto.setTeacherName(rs.getString("teacher_name"));
                dto.setSubmitted(rs.getInt("submitted") == 1);
                dto.setCreatedAt(rs.getTimestamp("created_at"));
                forms.add(dto);
            }
            rs.close();
            ps.close();
            con.close();

            model.addAttribute("forms", forms);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Student/Feedback";
    }

    @GetMapping("/feedback/{formId}")
    public String viewFeedbackForm(@PathVariable("formId") long formId, HttpSession session, Model model) {
        try {
            Long studentId = getStudentId(session);
            if (studentId == null) {
                return "redirect:/login";
            }

            Connection con = openConnection();
            if (!feedbackTablesExist(con)) {
                con.close();
                return "redirect:/feedback";
            }

            PreparedStatement psForm = con.prepareStatement(
                    "SELECT ff.id, ff.title, ff.description, ff.course_id, ff.teacher_id, c.course_name, c.course_code, t.name AS teacher_name " +
                            "FROM feedback_form ff " +
                            "JOIN course c ON c.id = ff.course_id " +
                            "JOIN teacher t ON t.id = ff.teacher_id " +
                            "JOIN student_course_teacher sct ON sct.course_id = ff.course_id AND sct.teacher_id = ff.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE ff.id = ? AND ff.is_active = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            );
            psForm.setLong(1, studentId);
            psForm.setLong(2, formId);
            ResultSet rsForm = psForm.executeQuery();
            if (!rsForm.next()) {
                rsForm.close();
                psForm.close();
                con.close();
                return "redirect:/feedback";
            }

            model.addAttribute("formId", formId);
            model.addAttribute("formTitle", rsForm.getString("title"));
            model.addAttribute("formDescription", rsForm.getString("description"));
            model.addAttribute("courseName", rsForm.getString("course_name"));
            model.addAttribute("courseCode", rsForm.getString("course_code"));
            model.addAttribute("teacherName", rsForm.getString("teacher_name"));
            long courseId = rsForm.getLong("course_id");
            long teacherId = rsForm.getLong("teacher_id");
            rsForm.close();
            psForm.close();

            PreparedStatement psSubmitted = con.prepareStatement(
                    "SELECT 1 FROM feedback_submission WHERE form_id = ? AND student_id = ? LIMIT 1"
            );
            psSubmitted.setLong(1, formId);
            psSubmitted.setLong(2, studentId);
            ResultSet rsSubmitted = psSubmitted.executeQuery();
            model.addAttribute("alreadySubmitted", rsSubmitted.next());
            rsSubmitted.close();
            psSubmitted.close();

            PreparedStatement psQ = con.prepareStatement(
                    "SELECT id, question_order, question_text, question_type, options_text, rating_max, required " +
                            "FROM feedback_question WHERE form_id = ? ORDER BY question_order"
            );
            psQ.setLong(1, formId);
            ResultSet rsQ = psQ.executeQuery();
            List<FeedbackQuestionDTO> questions = new ArrayList<>();
            while (rsQ.next()) {
                FeedbackQuestionDTO q = new FeedbackQuestionDTO();
                q.setId(rsQ.getLong("id"));
                q.setOrder(rsQ.getInt("question_order"));
                q.setText(rsQ.getString("question_text"));
                q.setType(rsQ.getString("question_type"));
                q.setOptionsText(rsQ.getString("options_text"));
                int ratingMax = rsQ.getInt("rating_max");
                q.setRatingMax(rsQ.wasNull() ? null : ratingMax);
                q.setRequired(rsQ.getInt("required") == 1);
                if (q.getOptionsText() != null) {
                    q.setOptions(splitNonEmptyLines(q.getOptionsText()));
                }
                questions.add(q);
            }
            rsQ.close();
            psQ.close();

            model.addAttribute("questions", questions);
            model.addAttribute("courseId", courseId);
            model.addAttribute("teacherId", teacherId);
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/feedback";
        }
        return "Student/FeedbackForm";
    }

    @PostMapping("/feedback/{formId}/submit")
    public String submitFeedbackForm(@PathVariable("formId") long formId,
                                     HttpSession session,
                                     @RequestParam Map<String, String> params) {
        try {
            Long studentId = getStudentId(session);
            if (studentId == null) {
                return "redirect:/login";
            }

            Connection con = openConnection();
            con.setAutoCommit(false);

            if (!feedbackTablesExist(con)) {
                con.rollback();
                con.close();
                return "redirect:/feedback";
            }

            PreparedStatement psCheck = con.prepareStatement(
                    "SELECT ff.course_id, ff.teacher_id " +
                            "FROM feedback_form ff " +
                            "JOIN student_course_teacher sct ON sct.course_id = ff.course_id AND sct.teacher_id = ff.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE ff.id = ? AND ff.is_active = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            );
            psCheck.setLong(1, studentId);
            psCheck.setLong(2, formId);
            ResultSet rsCheck = psCheck.executeQuery();
            if (!rsCheck.next()) {
                rsCheck.close();
                psCheck.close();
                con.rollback();
                con.close();
                return "redirect:/feedback";
            }
            rsCheck.close();
            psCheck.close();

            PreparedStatement psAlready = con.prepareStatement(
                    "SELECT 1 FROM feedback_submission WHERE form_id = ? AND student_id = ? LIMIT 1"
            );
            psAlready.setLong(1, formId);
            psAlready.setLong(2, studentId);
            ResultSet rsAlready = psAlready.executeQuery();
            boolean already = rsAlready.next();
            rsAlready.close();
            psAlready.close();
            if (already) {
                con.rollback();
                con.close();
                return "redirect:/feedback/" + formId;
            }

            PreparedStatement psSub = con.prepareStatement(
                    "INSERT INTO feedback_submission (form_id, student_id) VALUES (?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
            );
            psSub.setLong(1, formId);
            psSub.setLong(2, studentId);
            psSub.executeUpdate();

            ResultSet keys = psSub.getGeneratedKeys();
            if (!keys.next()) {
                keys.close();
                psSub.close();
                con.rollback();
                con.close();
                return "redirect:/feedback/" + formId;
            }
            long submissionId = keys.getLong(1);
            keys.close();
            psSub.close();

            PreparedStatement psQ = con.prepareStatement(
                    "SELECT id, question_type FROM feedback_question WHERE form_id = ? ORDER BY question_order"
            );
            psQ.setLong(1, formId);
            ResultSet rsQ = psQ.executeQuery();

            PreparedStatement psAns = con.prepareStatement(
                    "INSERT INTO feedback_answer (submission_id, question_id, answer_text, answer_number) VALUES (?, ?, ?, ?)"
            );

            while (rsQ.next()) {
                long qid = rsQ.getLong("id");
                String type = rsQ.getString("question_type");
                String raw = params.get("q_" + qid);

                psAns.setLong(1, submissionId);
                psAns.setLong(2, qid);

                if (type != null && type.equalsIgnoreCase("RATING")) {
                    Integer val = null;
                    try {
                        if (raw != null && !raw.trim().isEmpty()) val = Integer.parseInt(raw.trim());
                    } catch (Exception ignored) {
                    }
                    psAns.setString(3, null);
                    if (val == null) psAns.setObject(4, null);
                    else psAns.setInt(4, val);
                } else {
                    psAns.setString(3, raw == null ? null : raw.trim());
                    psAns.setObject(4, null);
                }

                psAns.addBatch();
            }

            rsQ.close();
            psQ.close();
            psAns.executeBatch();
            psAns.close();

            con.commit();
            con.close();
            return "redirect:/feedback";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/feedback/" + formId;
        }
    }
}
