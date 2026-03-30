-- Patch: bring `student_course_teacher` in line with application code (StudentController/TeacherController)
-- Adds:
--   - semester (the semester in which the student registered the course)
--   - status   (ACTIVE/INACTIVE etc.)
--
-- Safe to run multiple times (uses INFORMATION_SCHEMA guards).

-- 1) Add missing columns
SET @schema_name := DATABASE();

SET @has_semester :=
  (SELECT COUNT(*)
   FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'student_course_teacher' AND COLUMN_NAME = 'semester');

SET @has_status :=
  (SELECT COUNT(*)
   FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'student_course_teacher' AND COLUMN_NAME = 'status');

SET @sql := IF(@has_semester = 0,
  'ALTER TABLE student_course_teacher ADD COLUMN semester INT NOT NULL DEFAULT 1',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@has_status = 0,
  'ALTER TABLE student_course_teacher ADD COLUMN status VARCHAR(20) NULL',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Backfill semester from curriculum mapping (course_for_depts) using student's department.
-- If a course is mapped to multiple semesters for the same department, this will pick the MIN(sem).
UPDATE student_course_teacher sct
JOIN student s ON s.id = sct.student_id
JOIN (
  SELECT course_id, dept_id, MIN(sem) AS sem
  FROM course_for_depts
  GROUP BY course_id, dept_id
) cfd ON cfd.course_id = sct.course_id AND cfd.dept_id = s.dept_id
SET sct.semester = cfd.sem
WHERE (sct.semester IS NULL OR sct.semester = 0 OR sct.semester <> cfd.sem);

-- 3) Default status for existing rows (if missing)
UPDATE student_course_teacher
SET status = 'ACTIVE'
WHERE status IS NULL OR status = '';
