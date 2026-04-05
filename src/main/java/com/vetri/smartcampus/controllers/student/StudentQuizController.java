package com.vetri.smartcampus.controllers.student;

import com.vetri.smartcampus.models.common.QuizQuestionDTO;
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
public class StudentQuizController extends StudentControllerSupport {

    @GetMapping("/quiz/{quizId}")
    public String takeQuiz(@PathVariable("quizId") long quizId, HttpSession session, Model model) {
        Long studentId = getStudentId(session);
        if (studentId == null) return "redirect:/login";

        try {
            Connection con = openConnection();
            if (!quizTablesExist(con)) {
                con.close();
                return "redirect:/student-dashboard";
            }

            PreparedStatement psQuiz = con.prepareStatement(
                    "SELECT q.id, q.title, q.instructions, q.duration_minutes, q.total_marks, q.start_at, q.end_at, q.is_score_published, " +
                            "q.course_id, c.course_name, c.course_code, t.name AS teacher_name, " +
                            "qs.id AS submission_id, qs.score " +
                            "FROM quiz q " +
                            "JOIN course c ON c.id = q.course_id " +
                            "JOIN teacher t ON t.id = q.teacher_id " +
                            "JOIN student_course_teacher sct ON sct.course_id = q.course_id AND sct.teacher_id = q.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "LEFT JOIN quiz_submission qs ON qs.quiz_id = q.id AND qs.student_id = ? " +
                            "WHERE q.id = ? AND q.is_published = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            );
            psQuiz.setLong(1, studentId);
            psQuiz.setLong(2, studentId);
            psQuiz.setLong(3, quizId);
            ResultSet rsQuiz = psQuiz.executeQuery();
            if (!rsQuiz.next()) {
                rsQuiz.close();
                psQuiz.close();
                con.close();
                return "redirect:/student-dashboard";
            }

            model.addAttribute("quizId", quizId);
            model.addAttribute("quizTitle", rsQuiz.getString("title"));
            model.addAttribute("instructions", rsQuiz.getString("instructions"));
            model.addAttribute("durationMinutes", rsQuiz.getInt("duration_minutes"));
            model.addAttribute("totalMarks", rsQuiz.getInt("total_marks"));
            model.addAttribute("scorePublished", rsQuiz.getInt("is_score_published") == 1);
            java.time.LocalDateTime startAt = rsQuiz.getTimestamp("start_at").toLocalDateTime();
            java.time.LocalDateTime endAt = rsQuiz.getTimestamp("end_at").toLocalDateTime();
            model.addAttribute("startAt", startAt);
            model.addAttribute("endAt", endAt);
            model.addAttribute("startAtText", fmt(startAt));
            model.addAttribute("endAtText", fmt(endAt));
            model.addAttribute("courseName", rsQuiz.getString("course_name"));
            model.addAttribute("courseCode", rsQuiz.getString("course_code"));
            model.addAttribute("teacherName", rsQuiz.getString("teacher_name"));
            model.addAttribute("courseId", rsQuiz.getLong("course_id"));

            rsQuiz.getLong("submission_id");
            boolean alreadySubmitted = !rsQuiz.wasNull();
            model.addAttribute("alreadySubmitted", alreadySubmitted);
            int score = rsQuiz.getInt("score");
            model.addAttribute("score", rsQuiz.wasNull() ? null : score);

            rsQuiz.close();
            psQuiz.close();

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            boolean inWindow = (now.isAfter(startAt) || now.isEqual(startAt)) && (now.isBefore(endAt) || now.isEqual(endAt));
            model.addAttribute("inWindow", inWindow);

            List<QuizQuestionDTO> questions = new ArrayList<>();
            if (!alreadySubmitted && inWindow) {
                PreparedStatement psQ = con.prepareStatement(
                        "SELECT id, question_order, question_text, question_type, marks " +
                                "FROM quiz_question WHERE quiz_id = ? ORDER BY question_order"
                );
                psQ.setLong(1, quizId);
                ResultSet rsQ = psQ.executeQuery();
                while (rsQ.next()) {
                    QuizQuestionDTO q = new QuizQuestionDTO();
                    q.setId(rsQ.getLong("id"));
                    q.setOrder(rsQ.getInt("question_order"));
                    q.setText(rsQ.getString("question_text"));
                    q.setType(rsQ.getString("question_type"));
                    q.setMarks(rsQ.getInt("marks"));

                    if ("MCQ".equalsIgnoreCase(q.getType())) {
                        PreparedStatement psOpt = con.prepareStatement(
                                "SELECT option_index, option_text FROM quiz_option WHERE question_id = ? ORDER BY option_index"
                        );
                        psOpt.setLong(1, q.getId());
                        ResultSet rsOpt = psOpt.executeQuery();
                        List<String> opts = new ArrayList<>();
                        while (rsOpt.next()) {
                            opts.add(rsOpt.getString("option_text"));
                        }
                        rsOpt.close();
                        psOpt.close();
                        q.setOptions(opts);
                    }

                    questions.add(q);
                }
                rsQ.close();
                psQ.close();
            }

            model.addAttribute("questions", questions);
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/student-dashboard";
        }

        return "Student/TakeQuiz";
    }

    @PostMapping("/quiz/{quizId}/submit")
    public String submitQuiz(@PathVariable("quizId") long quizId, HttpSession session, @RequestParam Map<String, String> params) {
        Long studentId = getStudentId(session);
        if (studentId == null) return "redirect:/login";

        try {
            Connection con = openConnection();
            con.setAutoCommit(false);

            if (!quizTablesExist(con)) {
                con.rollback();
                con.close();
                return "redirect:/student-dashboard";
            }

            PreparedStatement psCheck = con.prepareStatement(
                    "SELECT q.start_at, q.end_at " +
                            "FROM quiz q " +
                            "JOIN student_course_teacher sct ON sct.course_id = q.course_id AND sct.teacher_id = q.teacher_id AND sct.student_id = ? " +
                            "JOIN student st ON st.id = sct.student_id " +
                            "WHERE q.id = ? AND q.is_published = 1 " +
                            "AND (sct.status IS NULL OR UPPER(sct.status) = 'ACTIVE') " +
                            "AND st.current_semester = sct.semester"
            );
            psCheck.setLong(1, studentId);
            psCheck.setLong(2, quizId);
            ResultSet rsCheck = psCheck.executeQuery();
            if (!rsCheck.next()) {
                rsCheck.close();
                psCheck.close();
                con.rollback();
                con.close();
                return "redirect:/student-dashboard";
            }
            java.time.LocalDateTime startAt = rsCheck.getTimestamp("start_at").toLocalDateTime();
            java.time.LocalDateTime endAt = rsCheck.getTimestamp("end_at").toLocalDateTime();
            rsCheck.close();
            psCheck.close();

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            boolean inWindow = (now.isAfter(startAt) || now.isEqual(startAt)) && (now.isBefore(endAt) || now.isEqual(endAt));
            if (!inWindow) {
                con.rollback();
                con.close();
                return "redirect:/quiz/" + quizId;
            }

            PreparedStatement psAlready = con.prepareStatement(
                    "SELECT 1 FROM quiz_submission WHERE quiz_id = ? AND student_id = ? LIMIT 1"
            );
            psAlready.setLong(1, quizId);
            psAlready.setLong(2, studentId);
            ResultSet rsAlready = psAlready.executeQuery();
            boolean already = rsAlready.next();
            rsAlready.close();
            psAlready.close();
            if (already) {
                con.rollback();
                con.close();
                return "redirect:/quiz/" + quizId;
            }

            PreparedStatement psSub = con.prepareStatement(
                    "INSERT INTO quiz_submission (quiz_id, student_id, score) VALUES (?, ?, 0)",
                    java.sql.Statement.RETURN_GENERATED_KEYS
            );
            psSub.setLong(1, quizId);
            psSub.setLong(2, studentId);
            psSub.executeUpdate();
            ResultSet keys = psSub.getGeneratedKeys();
            if (!keys.next()) {
                keys.close();
                psSub.close();
                con.rollback();
                con.close();
                return "redirect:/quiz/" + quizId;
            }
            long submissionId = keys.getLong(1);
            keys.close();
            psSub.close();

            PreparedStatement psQ = con.prepareStatement(
                    "SELECT id, question_type, marks, correct_option_index " +
                            "FROM quiz_question WHERE quiz_id = ? ORDER BY question_order"
            );
            psQ.setLong(1, quizId);
            ResultSet rsQ = psQ.executeQuery();

            PreparedStatement psAns = con.prepareStatement(
                    "INSERT INTO quiz_answer (submission_id, question_id, selected_option_index, answer_text, marks_awarded) VALUES (?, ?, ?, ?, ?)"
            );

            int totalScore = 0;
            while (rsQ.next()) {
                long qid = rsQ.getLong("id");
                String type = rsQ.getString("question_type");
                int marks = rsQ.getInt("marks");
                int corr = rsQ.getInt("correct_option_index");
                Integer correct = rsQ.wasNull() ? null : corr;
                String raw = params.get("q_" + qid);

                psAns.setLong(1, submissionId);
                psAns.setLong(2, qid);

                if (type != null && type.equalsIgnoreCase("MCQ")) {
                    Integer selected = null;
                    try {
                        if (raw != null && !raw.trim().isEmpty()) selected = Integer.parseInt(raw.trim());
                    } catch (Exception ignored) {
                    }
                    int awarded = (selected != null && correct != null && selected.intValue() == correct.intValue()) ? marks : 0;
                    totalScore += awarded;

                    if (selected == null) psAns.setObject(3, null);
                    else psAns.setInt(3, selected);
                    psAns.setString(4, null);
                    psAns.setInt(5, awarded);
                } else {
                    psAns.setObject(3, null);
                    psAns.setString(4, raw == null ? null : raw.trim());
                    psAns.setObject(5, null);
                }
                psAns.addBatch();
            }

            rsQ.close();
            psQ.close();
            psAns.executeBatch();
            psAns.close();

            PreparedStatement psUpd = con.prepareStatement("UPDATE quiz_submission SET score = ? WHERE id = ?");
            psUpd.setInt(1, totalScore);
            psUpd.setLong(2, submissionId);
            psUpd.executeUpdate();
            psUpd.close();

            con.commit();
            con.close();
            return "redirect:/quiz/" + quizId;
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/quiz/" + quizId;
        }
    }
}
