-- Course materials uploaded by teachers.
-- Files are stored on disk in the app working directory under ./material
-- and referenced by stored_path (relative).

CREATE TABLE IF NOT EXISTS course_material (
  id BIGINT NOT NULL AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  module VARCHAR(80) NULL,
  material_type VARCHAR(50) NOT NULL,
  description TEXT NULL,
  original_filename VARCHAR(255) NOT NULL,
  stored_filename VARCHAR(255) NOT NULL,
  stored_path VARCHAR(512) NOT NULL,
  mime_type VARCHAR(120) NULL,
  file_size BIGINT NULL,
  publish_at DATETIME NULL,
  expiry_at DATETIME NULL,
  download_allowed TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_cm_course (course_id),
  KEY idx_cm_teacher (teacher_id),
  KEY idx_cm_course_teacher (course_id, teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
