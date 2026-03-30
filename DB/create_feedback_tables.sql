-- Feedback feature tables (Teacher creates per-course feedback forms; Students submit; Teacher tracks status).
-- MySQL / MariaDB.

CREATE TABLE IF NOT EXISTS feedback_form (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ff_course_teacher (course_id, teacher_id),
    CONSTRAINT fk_ff_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE,
    CONSTRAINT fk_ff_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS feedback_question (
    id BIGINT NOT NULL AUTO_INCREMENT,
    form_id BIGINT NOT NULL,
    question_order INT NOT NULL,
    question_text VARCHAR(500) NOT NULL,
    question_type VARCHAR(20) NOT NULL, -- RATING | MCQ | TEXT
    options_text TEXT NULL,             -- for MCQ: one option per line
    rating_max INT NULL,                -- for RATING: usually 5
    required TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    KEY idx_fq_form (form_id),
    CONSTRAINT fk_fq_form FOREIGN KEY (form_id) REFERENCES feedback_form(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS feedback_submission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    form_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_fs_form_student (form_id, student_id),
    KEY idx_fs_form (form_id),
    KEY idx_fs_student (student_id),
    CONSTRAINT fk_fs_form FOREIGN KEY (form_id) REFERENCES feedback_form(id) ON DELETE CASCADE,
    CONSTRAINT fk_fs_student FOREIGN KEY (student_id) REFERENCES student(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS feedback_answer (
    id BIGINT NOT NULL AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_text TEXT NULL,
    answer_number INT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_fa_submission_question (submission_id, question_id),
    KEY idx_fa_submission (submission_id),
    KEY idx_fa_question (question_id),
    CONSTRAINT fk_fa_submission FOREIGN KEY (submission_id) REFERENCES feedback_submission(id) ON DELETE CASCADE,
    CONSTRAINT fk_fa_question FOREIGN KEY (question_id) REFERENCES feedback_question(id) ON DELETE CASCADE
);

