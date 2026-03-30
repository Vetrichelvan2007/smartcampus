-- Quiz feature tables (Teacher creates quiz per course; eligible students attempt; teacher tracks status).
-- MySQL / MariaDB.

CREATE TABLE IF NOT EXISTS quiz (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    instructions TEXT NULL,
    total_marks INT NOT NULL,
    duration_minutes INT NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    is_published TINYINT(1) NOT NULL DEFAULT 1,
    is_score_published TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_quiz_course_teacher (course_id, teacher_id),
    CONSTRAINT fk_quiz_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quiz_question (
    id BIGINT NOT NULL AUTO_INCREMENT,
    quiz_id BIGINT NOT NULL,
    question_order INT NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(20) NOT NULL, -- MCQ | DESCRIPTIVE
    marks INT NOT NULL,
    correct_option_index INT NULL, -- 1..4 for MCQ
    PRIMARY KEY (id),
    KEY idx_qq_quiz (quiz_id),
    CONSTRAINT fk_qq_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quiz_option (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    option_index INT NOT NULL,
    option_text VARCHAR(500) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_qo_question_index (question_id, option_index),
    KEY idx_qo_question (question_id),
    CONSTRAINT fk_qo_question FOREIGN KEY (question_id) REFERENCES quiz_question(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quiz_submission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    quiz_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    score INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_qs_quiz_student (quiz_id, student_id),
    KEY idx_qs_quiz (quiz_id),
    KEY idx_qs_student (student_id),
    CONSTRAINT fk_qs_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE,
    CONSTRAINT fk_qs_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quiz_answer (
    id BIGINT NOT NULL AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_index INT NULL, -- for MCQ
    answer_text TEXT NULL,          -- for descriptive
    marks_awarded INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_qa_submission_question (submission_id, question_id),
    KEY idx_qa_submission (submission_id),
    KEY idx_qa_question (question_id),
    CONSTRAINT fk_qa_submission FOREIGN KEY (submission_id) REFERENCES quiz_submission(id) ON DELETE CASCADE,
    CONSTRAINT fk_qa_question FOREIGN KEY (question_id) REFERENCES quiz_question(id) ON DELETE CASCADE
);
