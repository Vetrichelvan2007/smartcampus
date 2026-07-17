package com.vetri.smartcampus.controllers.teacher;

import com.vetri.smartcampus.models.common.QuizQuestionDTO;
import com.vetri.smartcampus.models.teacher.AssignedCourses;
import com.vetri.smartcampus.models.teacher.QuizDTO;
import com.vetri.smartcampus.models.teacher.QuizStudentStatusDTO;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class TeacherQuizController extends TeacherControllerSupport {

    @GetMapping("/old-teacher-create-quiz")
    public String teacherCreateQuiz() {
        return "redirect:/old-teacher-assign-quiz";
    }

    @GetMapping("/old-teacher-assign-quiz")
    public String teacherAssignQuiz(HttpSession session,
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
                    return "redirect:/old-teacher-assign-quiz";
                }

                model.addAttribute("selectedCourse", findAssignedCourse(courses, courseId));

                boolean quizReady = quizTablesExist(con);
                model.addAttribute("quizDbMissing", !quizReady);
                if (!quizReady) {
                    con.close();
                    return "Teacher/AssignQuiz";
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

                PreparedStatement psQuizzes = con.prepareStatement(
                        "SELECT q.id, q.course_id, q.teacher_id, q.title, q.total_marks, q.duration_minutes, q.start_at, q.end_at, q.is_published, q.is_score_published, q.created_at, " +
                                "(SELECT COUNT(*) FROM quiz_question qq WHERE qq.quiz_id = q.id) AS question_count, " +
                                "(SELECT COUNT(*) FROM quiz_submission qs WHERE qs.quiz_id = q.id) AS submitted_count " +
                                "FROM quiz q " +
                                "WHERE q.teacher_id = ? AND q.course_id = ? " +
                                "ORDER BY q.created_at DESC"
                );
                psQuizzes.setLong(1, teacherId);
                psQuizzes.setLong(2, courseId);
                ResultSet rsQuizzes = psQuizzes.executeQuery();
                List<QuizDTO> quizzes = new ArrayList<>();
                while (rsQuizzes.next()) {
                    QuizDTO q = new QuizDTO();
                    q.setId(rsQuizzes.getLong("id"));
                    q.setCourseId(rsQuizzes.getLong("course_id"));
                    q.setTeacherId(rsQuizzes.getLong("teacher_id"));
                    q.setTitle(rsQuizzes.getString("title"));
                    q.setTotalMarks(rsQuizzes.getInt("total_marks"));
                    q.setDurationMinutes(rsQuizzes.getInt("duration_minutes"));
                    q.setStartAt(rsQuizzes.getTimestamp("start_at").toLocalDateTime());
                    q.setEndAt(rsQuizzes.getTimestamp("end_at").toLocalDateTime());
                    q.setStartAtText(fmt(q.getStartAt()));
                    q.setEndAtText(fmt(q.getEndAt()));
                    q.setPublished(rsQuizzes.getInt("is_published") == 1);
                    q.setScorePublished(rsQuizzes.getInt("is_score_published") == 1);
                    q.setCreatedAt(rsQuizzes.getTimestamp("created_at"));
                    q.setQuestionCount(rsQuizzes.getInt("question_count"));
                    q.setSubmittedCount(rsQuizzes.getInt("submitted_count"));
                    q.setEligibleCount(eligibleCount);
                    quizzes.add(q);
                }
                rsQuizzes.close();
                psQuizzes.close();
                model.addAttribute("quizzes", quizzes);
            }

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/login";
        }
        return "Teacher/AssignQuiz";
    }

    @PostMapping("/old-teacher-assign-quiz/create")
    public String teacherCreateQuizForCourse(HttpSession session,
                                             @RequestParam("courseId") long courseId,
                                             @RequestParam("title") String title,
                                             @RequestParam(value = "instructions", required = false) String instructions,
                                             @RequestParam("durationMinutes") int durationMinutes,
                                             @RequestParam("startDate") String startDate,
                                             @RequestParam("startTime") String startTime,
                                             @RequestParam("endDate") String endDate,
                                             @RequestParam("endTime") String endTime,
                                             @RequestParam("questionText") String[] questionText,
                                             @RequestParam("questionType") String[] questionType,
                                             @RequestParam("marks") String[] marks,
                                             @RequestParam(value = "opt1", required = false) String[] opt1,
                                             @RequestParam(value = "opt2", required = false) String[] opt2,
                                             @RequestParam(value = "opt3", required = false) String[] opt3,
                                             @RequestParam(value = "opt4", required = false) String[] opt4,
                                             @RequestParam(value = "correctIndex", required = false) String[] correctIndex) {
        try {
            Long teacherId = getTeacherId(session);
            if (teacherId == null) {
                return "redirect:/login";
            }

            if (title == null || title.trim().isEmpty()) {
                return "redirect:/old-teacher-assign-quiz?courseId=" + courseId;
            }
            if (questionText == null || questionText.length == 0) {
                return "redirect:/old-teacher-assign-quiz?courseId=" + courseId;
            }

            LocalDateTime startAt;
            LocalDateTime endAt;
            try {
                startAt = LocalDateTime.parse(startDate + "T" + startTime);
                endAt = LocalDateTime.parse(endDate + "T" + endTime);
            } catch (DateTimeParseException ex) {
                return "redirect:/old-teacher-assign-quiz?courseId=" + courseId;
            }
            if (!startAt.isBefore(endAt)) {
                return "redirect:/old-teacher-assign-quiz?courseId=" + courseId;
            }

            Connection con = openConnection();
            con.setAutoCommit(false);

            if (!quizTablesExist(con)) {
                con.rollback();
                con.close();
                return "redirect:/old-teacher-assign-quiz?courseId=" + courseId;
            }

            if (!isTeacherCourseAllocated(con, teacherId, courseId)) {
                con.rollback();
                con.close();
                return "redirect:/old-teacher-assign-quiz";
            }

            int totalMarks = 0;
            if (marks != null) {
                for (String m : marks) totalMarks += parseLeadingInt(m, 0);
            }
            if (totalMarks <= 0) totalMarks = 1;

            PreparedStatement psQuiz = con.prepareStatement(
                    "INSERT INTO quiz (course_id, teacher_id, title, instructions, total_marks, duration_minutes, start_at, end_at, is_published) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
                    Statement.RETURN_GENERATED_KEYS
            );
            psQuiz.setLong(1, courseId);
            psQuiz.setLong(2, teacherId);
            psQuiz.setString(3, title.trim());
            psQuiz.setString(4, instructions);
            psQuiz.setInt(5, totalMarks);
            psQuiz.setInt(6, durationMinutes <= 0 ? 1 : durationMinutes);
            psQuiz.setTimestamp(7, java.sql.Timestamp.valueOf(startAt));
            psQuiz.setTimestamp(8, java.sql.Timestamp.valueOf(endAt));
            psQuiz.executeUpdate();

            ResultSet keys = psQuiz.getGeneratedKeys();
            if (!keys.next()) {
                keys.close();
                psQuiz.close();
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-quiz?courseId=" + courseId;
            }
            long quizId = keys.getLong(1);
            keys.close();
            psQuiz.close();

            PreparedStatement psQ = con.prepareStatement(
                    "INSERT INTO quiz_question (quiz_id, question_order, question_text, question_type, marks, correct_option_index) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            PreparedStatement psOpt = con.prepareStatement(
                    "INSERT INTO quiz_option (question_id, option_index, option_text) VALUES (?, ?, ?)"
            );

            int added = 0;
            for (int i = 0; i < questionText.length; i++) {
                String qt = questionText[i] == null ? "" : questionText[i].trim();
                if (qt.isEmpty()) continue;

                String type = (questionType != null && questionType.length > i && questionType[i] != null) ? questionType[i].trim() : "MCQ";
                int m = (marks != null && marks.length > i) ? parseLeadingInt(marks[i], 1) : 1;
                if (m <= 0) m = 1;

                Integer corr = null;
                if ("MCQ".equalsIgnoreCase(type)) {
                    corr = (correctIndex != null && correctIndex.length > i) ? parseLeadingInt(correctIndex[i], 0) : 0;
                    if (corr == null || corr < 1 || corr > 4) corr = 1;
                } else {
                    type = "DESCRIPTIVE";
                }

                psQ.setLong(1, quizId);
                psQ.setInt(2, i + 1);
                psQ.setString(3, qt);
                psQ.setString(4, type.toUpperCase(Locale.ROOT));
                psQ.setInt(5, m);
                if (corr == null) psQ.setObject(6, null);
                else psQ.setInt(6, corr);
                psQ.executeUpdate();

                ResultSet qKeys = psQ.getGeneratedKeys();
                if (!qKeys.next()) {
                    qKeys.close();
                    continue;
                }
                long qid = qKeys.getLong(1);
                qKeys.close();

                if ("MCQ".equalsIgnoreCase(type)) {
                    String o1 = (opt1 != null && opt1.length > i) ? opt1[i] : "";
                    String o2 = (opt2 != null && opt2.length > i) ? opt2[i] : "";
                    String o3 = (opt3 != null && opt3.length > i) ? opt3[i] : "";
                    String o4 = (opt4 != null && opt4.length > i) ? opt4[i] : "";

                    psOpt.setLong(1, qid);
                    psOpt.setInt(2, 1);
                    psOpt.setString(3, o1 == null ? "" : o1.trim());
                    psOpt.addBatch();
                    psOpt.setLong(1, qid);
                    psOpt.setInt(2, 2);
                    psOpt.setString(3, o2 == null ? "" : o2.trim());
                    psOpt.addBatch();
                    psOpt.setLong(1, qid);
                    psOpt.setInt(2, 3);
                    psOpt.setString(3, o3 == null ? "" : o3.trim());
                    psOpt.addBatch();
                    psOpt.setLong(1, qid);
                    psOpt.setInt(2, 4);
                    psOpt.setString(3, o4 == null ? "" : o4.trim());
                    psOpt.addBatch();
                }

                added++;
            }

            if (added == 0) {
                psOpt.close();
                psQ.close();
                con.rollback();
                con.close();
                return "redirect:/teacher-assign-quiz?courseId=" + courseId;
            }

            psOpt.executeBatch();
            psOpt.close();
            psQ.close();

            con.commit();
            con.close();

            return "redirect:/old-teacher-assign-quiz?courseId=" + courseId;
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/old-teacher-assign-quiz?courseId=" + courseId;
        }
    }

    @GetMapping("/old-teacher-quiz/{quizId}")
    public String teacherViewQuizStatus(@PathVariable("quizId") long quizId, HttpSession session, Model model) {
        try {
            Long teacherId = getTeacherId(session);
            if (teacherId == null) {
                return "redirect:/login";
            }

            Connection con = openConnection();
            if (!quizTablesExist(con)) {
                con.close();
                return "redirect:/old-teacher-assign-quiz";
            }

            PreparedStatement psQuiz = con.prepareStatement(
                    "SELECT q.id, q.course_id, q.teacher_id, q.title, q.instructions, q.total_marks, q.duration_minutes, q.start_at, q.end_at, q.is_published, q.is_score_published, q.created_at, " +
                            "c.course_name, c.course_code " +
                            "FROM quiz q " +
                            "JOIN course c ON c.id = q.course_id " +
                            "WHERE q.id = ? AND q.teacher_id = ?"
            );
            psQuiz.setLong(1, quizId);
            psQuiz.setLong(2, teacherId);
            ResultSet rsQuiz = psQuiz.executeQuery();
            if (!rsQuiz.next()) {
                rsQuiz.close();
                psQuiz.close();
                con.close();
                return "redirect:/old-teacher-assign-quiz";
            }

            QuizDTO quiz = new QuizDTO();
            quiz.setId(rsQuiz.getLong("id"));
            quiz.setCourseId(rsQuiz.getLong("course_id"));
            quiz.setTeacherId(rsQuiz.getLong("teacher_id"));
            quiz.setTitle(rsQuiz.getString("title"));
            quiz.setInstructions(rsQuiz.getString("instructions"));
            quiz.setTotalMarks(rsQuiz.getInt("total_marks"));
            quiz.setDurationMinutes(rsQuiz.getInt("duration_minutes"));
            quiz.setStartAt(rsQuiz.getTimestamp("start_at").toLocalDateTime());
            quiz.setEndAt(rsQuiz.getTimestamp("end_at").toLocalDateTime());
            quiz.setStartAtText(fmt(quiz.getStartAt()));
            quiz.setEndAtText(fmt(quiz.getEndAt()));
            quiz.setPublished(rsQuiz.getInt("is_published") == 1);
            quiz.setScorePublished(rsQuiz.getInt("is_score_published") == 1);
            quiz.setCreatedAt(rsQuiz.getTimestamp("created_at"));
            model.addAttribute("courseName", rsQuiz.getString("course_name"));
            model.addAttribute("courseCode", rsQuiz.getString("course_code"));
            rsQuiz.close();
            psQuiz.close();

            PreparedStatement psQ = con.prepareStatement(
                    "SELECT qq.id, qq.question_order, qq.question_text, qq.question_type, qq.marks, qq.correct_option_index " +
                            "FROM quiz_question qq WHERE qq.quiz_id = ? ORDER BY qq.question_order"
            );
            psQ.setLong(1, quizId);
            ResultSet rsQ = psQ.executeQuery();
            List<QuizQuestionDTO> questions = new ArrayList<>();
            while (rsQ.next()) {
                QuizQuestionDTO q = new QuizQuestionDTO();
                q.setId(rsQ.getLong("id"));
                q.setOrder(rsQ.getInt("question_order"));
                q.setText(rsQ.getString("question_text"));
                q.setType(rsQ.getString("question_type"));
                q.setMarks(rsQ.getInt("marks"));
                int ci = rsQ.getInt("correct_option_index");
                q.setCorrectOptionIndex(rsQ.wasNull() ? null : ci);
                questions.add(q);
            }
            rsQ.close();
            psQ.close();
            quiz.setQuestionCount(questions.size());

            PreparedStatement psStatus = con.prepareStatement(
                    "SELECT s.id AS student_id, s.name, s.roll_number, d.dept_name, sct.semester, qs.submitted_at, qs.score " +
                            "FROM student_course_teacher sct " +
                            "JOIN student s ON s.id = sct.student_id " +
                            "JOIN department d ON d.id = s.dept_id " +
                            "LEFT JOIN quiz_submission qs ON qs.quiz_id = ? AND qs.student_id = s.id " +
                            "WHERE sct.teacher_id = ? AND sct.course_id = ? " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND s.current_semester = sct.semester " +
                            "ORDER BY s.roll_number"
            );
            psStatus.setLong(1, quizId);
            psStatus.setLong(2, teacherId);
            psStatus.setLong(3, quiz.getCourseId());
            ResultSet rsStatus = psStatus.executeQuery();
            List<QuizStudentStatusDTO> statuses = new ArrayList<>();
            int submittedCount = 0;
            while (rsStatus.next()) {
                QuizStudentStatusDTO st = new QuizStudentStatusDTO();
                st.setStudentId(rsStatus.getLong("student_id"));
                st.setName(rsStatus.getString("name"));
                st.setRollNumber(rsStatus.getString("roll_number"));
                st.setDepartment(rsStatus.getString("dept_name"));
                st.setSemester(rsStatus.getInt("semester"));
                Timestamp subAt = rsStatus.getTimestamp("submitted_at");
                st.setSubmittedAt(subAt);
                st.setSubmitted(subAt != null);
                int sc = rsStatus.getInt("score");
                st.setScore(rsStatus.wasNull() ? null : sc);
                if (subAt != null) submittedCount++;
                statuses.add(st);
            }
            rsStatus.close();
            psStatus.close();

            quiz.setEligibleCount(statuses.size());
            quiz.setSubmittedCount(submittedCount);

            model.addAttribute("quiz", quiz);
            model.addAttribute("questions", questions);
            model.addAttribute("studentStatuses", statuses);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/old-teacher-assign-quiz";
        }
        return "Teacher/ViewQuizStatus";
    }

    @PostMapping("/old-teacher-quiz/{quizId}/publish-score")
    public String teacherPublishQuizScore(@PathVariable("quizId") long quizId,
                                          @RequestParam("published") boolean published,
                                          HttpSession session) {
        Long teacherId = getTeacherId(session);
        if (teacherId == null) return "redirect:/login";

        try {
            Connection con = openConnection();
            if (!quizTablesExist(con)) {
                con.close();
                return "redirect:/old-teacher-assign-quiz";
            }

            PreparedStatement ps = con.prepareStatement(
                    "UPDATE quiz SET is_score_published = ? WHERE id = ? AND teacher_id = ?"
            );
            ps.setInt(1, published ? 1 : 0);
            ps.setLong(2, quizId);
            ps.setLong(3, teacherId);
            int updated = ps.executeUpdate();
            ps.close();
            con.close();

            if (updated == 0) {
                return "redirect:/old-teacher-assign-quiz";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/old-teacher-quiz/" + quizId;
    }

    @GetMapping("/old-teacher-quiz/{quizId}/student/{studentId}")
    public String teacherViewQuizSubmission(@PathVariable("quizId") long quizId,
                                            @PathVariable("studentId") long studentId,
                                            HttpSession session,
                                            Model model) {
        try {
            Long teacherId = getTeacherId(session);
            if (teacherId == null) return "redirect:/login";

            Connection con = openConnection();
            if (!quizTablesExist(con)) {
                con.close();
                return "redirect:/old-teacher-assign-quiz";
            }

            PreparedStatement psSub = con.prepareStatement(
                    "SELECT qs.id AS submission_id, qs.submitted_at, qs.score, " +
                            "s.name AS student_name, s.roll_number, " +
                            "q.title AS quiz_title, c.course_name, c.course_code " +
                            "FROM quiz_submission qs " +
                            "JOIN quiz q ON q.id = qs.quiz_id " +
                            "JOIN course c ON c.id = q.course_id " +
                            "JOIN student s ON s.id = qs.student_id " +
                            "WHERE qs.quiz_id = ? AND qs.student_id = ? AND q.teacher_id = ?"
            );
            psSub.setLong(1, quizId);
            psSub.setLong(2, studentId);
            psSub.setLong(3, teacherId);
            ResultSet rsSub = psSub.executeQuery();
            if (!rsSub.next()) {
                rsSub.close();
                psSub.close();
                con.close();
                return "redirect:/old-teacher-quiz/" + quizId;
            }
            long submissionId = rsSub.getLong("submission_id");
            model.addAttribute("submittedAt", rsSub.getTimestamp("submitted_at"));
            int sc = rsSub.getInt("score");
            model.addAttribute("score", rsSub.wasNull() ? null : sc);
            model.addAttribute("studentName", rsSub.getString("student_name"));
            model.addAttribute("rollNumber", rsSub.getString("roll_number"));
            model.addAttribute("quizTitle", rsSub.getString("quiz_title"));
            model.addAttribute("courseName", rsSub.getString("course_name"));
            model.addAttribute("courseCode", rsSub.getString("course_code"));
            rsSub.close();
            psSub.close();

            PreparedStatement psAns = con.prepareStatement(
                    "SELECT qq.question_order, qq.question_text, qq.question_type, qq.marks, qq.correct_option_index, " +
                            "qa.selected_option_index, qa.answer_text, qa.marks_awarded " +
                            "FROM quiz_question qq " +
                            "LEFT JOIN quiz_answer qa ON qa.question_id = qq.id AND qa.submission_id = ? " +
                            "WHERE qq.quiz_id = ? " +
                            "ORDER BY qq.question_order"
            );
            psAns.setLong(1, submissionId);
            psAns.setLong(2, quizId);
            ResultSet rsAns = psAns.executeQuery();
            List<Map<String, Object>> answers = new ArrayList<>();
            while (rsAns.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("order", rsAns.getInt("question_order"));
                row.put("text", rsAns.getString("question_text"));
                row.put("type", rsAns.getString("question_type"));
                row.put("marks", rsAns.getInt("marks"));
                int ci = rsAns.getInt("correct_option_index");
                row.put("correct", rsAns.wasNull() ? null : ci);
                int sel = rsAns.getInt("selected_option_index");
                row.put("selected", rsAns.wasNull() ? null : sel);
                row.put("answerText", rsAns.getString("answer_text"));
                int ma = rsAns.getInt("marks_awarded");
                row.put("awarded", rsAns.wasNull() ? null : ma);
                answers.add(row);
            }
            rsAns.close();
            psAns.close();

            model.addAttribute("answers", answers);
            model.addAttribute("quizId", quizId);

            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/old-teacher-quiz/" + quizId;
        }
        return "Teacher/ViewQuizSubmission";
    }
}
