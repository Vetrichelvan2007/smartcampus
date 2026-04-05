package com.vetri.smartcampus.controllers.teacher;

import com.vetri.smartcampus.models.common.FeedbackQuestionDTO;
import com.vetri.smartcampus.models.teacher.AssignedCourses;
import com.vetri.smartcampus.models.teacher.FeedbackFormDTO;
import com.vetri.smartcampus.models.teacher.FeedbackStudentStatusDTO;
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
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class TeacherFeedbackController extends TeacherControllerSupport {

    @GetMapping("/teacher-assign-feedback")
    public String teacherAssignFeedback(HttpSession session,
                                        @RequestParam(value = "courseId", required = false) Long courseId,
                                        Model model) {
        try {
            Long teacherId = getTeacherId(session);
            if (teacherId == null) {
                return "redirect:/login";
            }

            model.addAttribute("teacherId", teacherId);

            Connection con = openConnection();
            List<AssignedCourses> courses = loadAssignedCourses(con, teacherId);
            model.addAttribute("courses", courses);

            if (courseId != null) {
                if (!isTeacherCourseAllocated(con, teacherId, courseId)) {
                    con.close();
                    return "redirect:/teacher-assign-feedback";
                }

                model.addAttribute("selectedCourse", findAssignedCourse(courses, courseId));

                boolean feedbackReady = feedbackTablesExist(con);
                model.addAttribute("feedbackDbMissing", !feedbackReady);
                if (!feedbackReady) {
                    con.close();
                    return "Teacher/AssignFeedback";
                }

                int eligibleCount = 0;
                PreparedStatement psEligible = con.prepareStatement(
                        "SELECT COUNT(DISTINCT sct.student_id) AS cnt " +
                                "FROM student_course_teacher sct " +
                                "JOIN student s ON s.id = sct.student_id " +
                                "WHERE sct.teacher_id = ? " +
                                "AND sct.course_id = ? " +
                                "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                                "AND s.current_semester = sct.semester"
                );
                psEligible.setLong(1, teacherId);
                psEligible.setLong(2, courseId);
                ResultSet rsEligible = psEligible.executeQuery();
                if (rsEligible.next()) eligibleCount = rsEligible.getInt("cnt");
                rsEligible.close();
                psEligible.close();
                model.addAttribute("eligibleCount", eligibleCount);

                PreparedStatement psForms = con.prepareStatement(
                        "SELECT ff.id, ff.course_id, ff.teacher_id, ff.title, ff.description, ff.is_active, ff.created_at, " +
                                "(SELECT COUNT(*) FROM feedback_question fq WHERE fq.form_id = ff.id) AS question_count, " +
                                "(SELECT COUNT(*) FROM feedback_submission fs WHERE fs.form_id = ff.id) AS submitted_count " +
                                "FROM feedback_form ff " +
                                "WHERE ff.teacher_id = ? AND ff.course_id = ? " +
                                "ORDER BY ff.created_at DESC"
                );
                psForms.setLong(1, teacherId);
                psForms.setLong(2, courseId);
                ResultSet rsForms = psForms.executeQuery();
                List<FeedbackFormDTO> forms = new ArrayList<>();
                while (rsForms.next()) {
                    FeedbackFormDTO f = new FeedbackFormDTO();
                    f.setId(rsForms.getLong("id"));
                    f.setCourseId(rsForms.getLong("course_id"));
                    f.setTeacherId(rsForms.getLong("teacher_id"));
                    f.setTitle(rsForms.getString("title"));
                    f.setDescription(rsForms.getString("description"));
                    f.setActive(rsForms.getInt("is_active") == 1);
                    f.setCreatedAt(rsForms.getTimestamp("created_at"));
                    f.setQuestionCount(rsForms.getInt("question_count"));
                    f.setSubmittedCount(rsForms.getInt("submitted_count"));
                    f.setEligibleCount(eligibleCount);
                    forms.add(f);
                }
                rsForms.close();
                psForms.close();
                model.addAttribute("forms", forms);
            }

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }
        return "Teacher/AssignFeedback";
    }

    @PostMapping("/teacher-assign-feedback/create")
    public String teacherCreateFeedbackForm(HttpSession session,
                                            @RequestParam("courseId") long courseId,
                                            @RequestParam("title") String title,
                                            @RequestParam(value = "description", required = false) String description,
                                            @RequestParam("questionText") String[] questionText,
                                            @RequestParam("questionType") String[] questionType,
                                            @RequestParam(value = "questionOptions", required = false) String[] questionOptions,
                                            @RequestParam(value = "ratingMax", required = false) String[] ratingMax) {
        try {
            Long teacherId = getTeacherId(session);
            if (teacherId == null) {
                return "redirect:/login";
            }

            if (title == null || title.trim().isEmpty()) {
                return "redirect:/teacher-assign-feedback?courseId=" + courseId;
            }
            if (questionText == null || questionText.length == 0) {
                return "redirect:/teacher-assign-feedback?courseId=" + courseId;
            }

            Connection con = openConnection();
            con.setAutoCommit(false);

            if (!feedbackTablesExist(con)) {
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-feedback?courseId=" + courseId;
            }

            if (!isTeacherCourseAllocated(con, teacherId, courseId)) {
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-feedback";
            }

            PreparedStatement psForm = con.prepareStatement(
                    "INSERT INTO feedback_form (course_id, teacher_id, title, description, is_active) VALUES (?, ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS
            );
            psForm.setLong(1, courseId);
            psForm.setLong(2, teacherId);
            psForm.setString(3, title.trim());
            psForm.setString(4, description);
            psForm.executeUpdate();

            long formId;
            ResultSet keys = psForm.getGeneratedKeys();
            if (!keys.next()) {
                keys.close();
                psForm.close();
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-feedback?courseId=" + courseId;
            }
            formId = keys.getLong(1);
            keys.close();
            psForm.close();

            PreparedStatement psQ = con.prepareStatement(
                    "INSERT INTO feedback_question (form_id, question_order, question_text, question_type, options_text, rating_max, required) " +
                            "VALUES (?, ?, ?, ?, ?, ?, 1)"
            );

            int added = 0;
            for (int i = 0; i < questionText.length; i++) {
                String qt = questionText[i] == null ? "" : questionText[i].trim();
                String type = (questionType != null && questionType.length > i && questionType[i] != null) ? questionType[i].trim() : "TEXT";

                if (qt.isEmpty()) {
                    continue;
                }

                String options = (questionOptions != null && questionOptions.length > i) ? questionOptions[i] : null;
                String rMaxRaw = (ratingMax != null && ratingMax.length > i) ? ratingMax[i] : null;
                Integer rMax = null;
                if ("RATING".equalsIgnoreCase(type)) {
                    int parsed = parseLeadingInt(rMaxRaw, 5);
                    rMax = parsed <= 0 ? 5 : parsed;
                    options = null;
                } else if ("MCQ".equalsIgnoreCase(type)) {
                    rMax = null;
                    options = (options == null) ? "" : options.trim();
                } else {
                    rMax = null;
                    options = null;
                    type = "TEXT";
                }

                psQ.setLong(1, formId);
                psQ.setInt(2, i + 1);
                psQ.setString(3, qt);
                psQ.setString(4, type.toUpperCase(Locale.ROOT));
                psQ.setString(5, options);
                if (rMax == null) {
                    psQ.setObject(6, null);
                } else {
                    psQ.setInt(6, rMax);
                }
                psQ.addBatch();
                added++;
            }

            if (added == 0) {
                psQ.close();
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-feedback?courseId=" + courseId;
            }

            psQ.executeBatch();
            psQ.close();

            con.commit();
            con.close();

            return "redirect:/teacher-assign-feedback?courseId=" + courseId;
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/teacher-assign-feedback?courseId=" + courseId;
        }
    }

    @GetMapping("/teacher-feedback/{formId}")
    public String teacherViewFeedbackStatus(@PathVariable("formId") long formId, HttpSession session, Model model) {
        try {
            Long teacherId = getTeacherId(session);
            if (teacherId == null) {
                return "redirect:/login";
            }

            Connection con = openConnection();
            if (!feedbackTablesExist(con)) {
                con.close();
                return "redirect:/teacher-assign-feedback";
            }

            PreparedStatement psForm = con.prepareStatement(
                    "SELECT ff.id, ff.course_id, ff.teacher_id, ff.title, ff.description, ff.is_active, ff.created_at, " +
                            "c.course_name, c.course_code " +
                            "FROM feedback_form ff " +
                            "JOIN course c ON c.id = ff.course_id " +
                            "WHERE ff.id = ? AND ff.teacher_id = ?"
            );
            psForm.setLong(1, formId);
            psForm.setLong(2, teacherId);
            ResultSet rsForm = psForm.executeQuery();
            if (!rsForm.next()) {
                rsForm.close();
                psForm.close();
                con.close();
                return "redirect:/teacher-assign-feedback";
            }

            FeedbackFormDTO form = new FeedbackFormDTO();
            form.setId(rsForm.getLong("id"));
            form.setCourseId(rsForm.getLong("course_id"));
            form.setTeacherId(rsForm.getLong("teacher_id"));
            form.setTitle(rsForm.getString("title"));
            form.setDescription(rsForm.getString("description"));
            form.setActive(rsForm.getInt("is_active") == 1);
            form.setCreatedAt(rsForm.getTimestamp("created_at"));
            form.setCourseName(rsForm.getString("course_name"));
            form.setCourseCode(rsForm.getString("course_code"));
            rsForm.close();
            psForm.close();

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
                int rm = rsQ.getInt("rating_max");
                q.setRatingMax(rsQ.wasNull() ? null : rm);
                q.setRequired(rsQ.getInt("required") == 1);
                if (q.getOptionsText() != null) {
                    q.setOptions(splitNonEmptyLines(q.getOptionsText()));
                }
                questions.add(q);
            }
            rsQ.close();
            psQ.close();

            PreparedStatement psStatus = con.prepareStatement(
                    "SELECT s.id AS student_id, s.name, s.roll_number, d.dept_name, sct.semester, fs.submitted_at " +
                            "FROM student_course_teacher sct " +
                            "JOIN student s ON s.id = sct.student_id " +
                            "JOIN department d ON d.id = s.dept_id " +
                            "LEFT JOIN feedback_submission fs ON fs.form_id = ? AND fs.student_id = s.id " +
                            "WHERE sct.teacher_id = ? AND sct.course_id = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND s.current_semester = sct.semester " +
                            "ORDER BY s.roll_number"
            );
            psStatus.setLong(1, formId);
            psStatus.setLong(2, teacherId);
            psStatus.setLong(3, form.getCourseId());
            ResultSet rsStatus = psStatus.executeQuery();
            List<FeedbackStudentStatusDTO> studentStatuses = new ArrayList<>();
            int submittedCount = 0;
            while (rsStatus.next()) {
                FeedbackStudentStatusDTO st = new FeedbackStudentStatusDTO();
                st.setStudentId(rsStatus.getLong("student_id"));
                st.setName(rsStatus.getString("name"));
                st.setRollNumber(rsStatus.getString("roll_number"));
                st.setDepartment(rsStatus.getString("dept_name"));
                st.setSemester(rsStatus.getInt("semester"));
                Timestamp submittedAt = rsStatus.getTimestamp("submitted_at");
                st.setSubmittedAt(submittedAt);
                st.setSubmitted(submittedAt != null);
                if (submittedAt != null) submittedCount++;
                studentStatuses.add(st);
            }
            rsStatus.close();
            psStatus.close();

            form.setQuestionCount(questions.size());
            form.setEligibleCount(studentStatuses.size());
            form.setSubmittedCount(submittedCount);

            model.addAttribute("form", form);
            model.addAttribute("questions", questions);
            model.addAttribute("studentStatuses", studentStatuses);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/teacher-assign-feedback";
        }
        return "Teacher/ViewFeedbackStatus";
    }

    @GetMapping("/teacher-feedback/{formId}/student/{studentId}")
    public String teacherViewFeedbackSubmissionForStudent(@PathVariable("formId") long formId,
                                                          @PathVariable("studentId") long studentId,
                                                          HttpSession session,
                                                          Model model) {
        try {
            Long teacherId = getTeacherId(session);
            if (teacherId == null) {
                return "redirect:/login";
            }

            Connection con = openConnection();
            if (!feedbackTablesExist(con)) {
                con.close();
                return "redirect:/teacher-assign-feedback";
            }

            PreparedStatement psSub = con.prepareStatement(
                    "SELECT fs.id AS submission_id, fs.submitted_at, " +
                            "s.name AS student_name, s.roll_number, " +
                            "ff.title AS form_title, c.course_name, c.course_code " +
                            "FROM feedback_submission fs " +
                            "JOIN feedback_form ff ON ff.id = fs.form_id " +
                            "JOIN course c ON c.id = ff.course_id " +
                            "JOIN student s ON s.id = fs.student_id " +
                            "WHERE fs.form_id = ? AND fs.student_id = ? AND ff.teacher_id = ?"
            );
            psSub.setLong(1, formId);
            psSub.setLong(2, studentId);
            psSub.setLong(3, teacherId);
            ResultSet rsSub = psSub.executeQuery();
            if (!rsSub.next()) {
                rsSub.close();
                psSub.close();
                con.close();
                return "redirect:/teacher-feedback/" + formId;
            }
            long submissionId = rsSub.getLong("submission_id");
            model.addAttribute("submittedAt", rsSub.getTimestamp("submitted_at"));
            model.addAttribute("studentName", rsSub.getString("student_name"));
            model.addAttribute("rollNumber", rsSub.getString("roll_number"));
            model.addAttribute("formTitle", rsSub.getString("form_title"));
            model.addAttribute("courseName", rsSub.getString("course_name"));
            model.addAttribute("courseCode", rsSub.getString("course_code"));
            rsSub.close();
            psSub.close();

            PreparedStatement psAns = con.prepareStatement(
                    "SELECT fq.question_order, fq.question_text, fq.question_type, fq.options_text, fq.rating_max, " +
                            "fa.answer_text, fa.answer_number " +
                            "FROM feedback_question fq " +
                            "LEFT JOIN feedback_answer fa ON fa.question_id = fq.id AND fa.submission_id = ? " +
                            "WHERE fq.form_id = ? " +
                            "ORDER BY fq.question_order"
            );
            psAns.setLong(1, submissionId);
            psAns.setLong(2, formId);
            ResultSet rsAns = psAns.executeQuery();
            List<Map<String, Object>> answers = new ArrayList<>();
            while (rsAns.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("order", rsAns.getInt("question_order"));
                row.put("text", rsAns.getString("question_text"));
                row.put("type", rsAns.getString("question_type"));
                row.put("answerText", rsAns.getString("answer_text"));
                int num = rsAns.getInt("answer_number");
                row.put("answerNumber", rsAns.wasNull() ? null : num);
                answers.add(row);
            }
            rsAns.close();
            psAns.close();

            model.addAttribute("answers", answers);
            model.addAttribute("formId", formId);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/teacher-feedback/" + formId;
        }
        return "Teacher/ViewFeedbackSubmission";
    }
}
