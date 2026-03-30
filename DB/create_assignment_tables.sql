CREATE TABLE IF NOT EXISTS course_assignment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  assignment_mode VARCHAR(20) NOT NULL,
  question_text LONGTEXT NULL,
  instructions TEXT NULL,
  original_filename VARCHAR(255) NULL,
  stored_filename VARCHAR(255) NULL,
  stored_path VARCHAR(512) NULL,
  mime_type VARCHAR(120) NULL,
  file_size BIGINT NULL,
  due_at DATETIME NULL,
  max_marks INT NULL,
  download_allowed TINYINT NOT NULL DEFAULT 1,
  is_published TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ca_course (course_id),
  KEY idx_ca_teacher (teacher_id),
  KEY idx_ca_course_teacher (course_id, teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS course_assignment_submission (
  id BIGINT NOT NULL AUTO_INCREMENT,
  assignment_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  stored_filename VARCHAR(255) NOT NULL,
  stored_path VARCHAR(512) NOT NULL,
  mime_type VARCHAR(120) NULL,
  file_size BIGINT NULL,
  submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_assignment_student (assignment_id, student_id),
  KEY idx_cas_assignment (assignment_id),
  KEY idx_cas_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
