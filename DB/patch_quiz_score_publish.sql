-- Patch: add score publish toggle support for quizzes.
-- Adds quiz.is_score_published (0/1) if missing.

SET @schema_name := DATABASE();

SET @has_col :=
  (SELECT COUNT(*)
   FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'quiz' AND COLUMN_NAME = 'is_score_published');

SET @sql := IF(@has_col = 0,
  'ALTER TABLE quiz ADD COLUMN is_score_published TINYINT(1) NOT NULL DEFAULT 0',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

