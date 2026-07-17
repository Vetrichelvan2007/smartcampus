-- Sample data for SmartCampus LMS (PostgreSQL format)

-- 1. users
INSERT INTO users (email, password, role) VALUES
('student1@smartcampus.local', 'student1', 'STUDENT'),
('student2@smartcampus.local', 'student2', 'STUDENT'),
('teacher1@smartcampus.local', 'teacher1', 'TEACHER'),
('teacher2@smartcampus.local', 'teacher2', 'TEACHER')
ON CONFLICT (email) DO NOTHING;

-- 2. department
INSERT INTO department (dept_name) VALUES
('Computer Science & Engineering'),
('Information Technology')
ON CONFLICT (dept_name) DO NOTHING;

-- 3. batch
INSERT INTO batch (dept_id, batch_year, batch_name)
SELECT d.id, v.batch_year::date, v.batch_name
FROM (VALUES
    ('Computer Science & Engineering', '2023-06-01', '2023-2027 CSE'),
    ('Information Technology', '2023-06-01', '2023-2027 IT')
) AS v(dept_name, batch_year, batch_name)
JOIN department d ON d.dept_name = v.dept_name
WHERE NOT EXISTS (SELECT 1 FROM batch b WHERE b.batch_name = v.batch_name);

-- 4. course
INSERT INTO course (course_name, course_code, credit, course_type) VALUES
('Web Programming', 'CS-401', 4, 'THEORY'),
('Database Management Systems Lab', 'CS-402', 2, 'LAB')
ON CONFLICT (course_code) DO NOTHING;

-- 5. course_for_depts
INSERT INTO course_for_depts (course_id, dept_id, sem) VALUES
((SELECT id FROM course WHERE course_code = 'CS-401'), (SELECT id FROM department WHERE dept_name = 'Computer Science & Engineering'), 4),
((SELECT id FROM course WHERE course_code = 'CS-402'), (SELECT id FROM department WHERE dept_name = 'Computer Science & Engineering'), 4)
ON CONFLICT DO NOTHING;

-- 6. student
INSERT INTO student (roll_number, name, dob, gender, address, dept_id, batch_id, batch_year, current_year, current_semester, email, phone, blood_group, mother_tongue, nationality) VALUES
('STU001', 'John Doe', '2005-05-15', 'MALE', '123 Main St', (SELECT id FROM department WHERE dept_name = 'Computer Science & Engineering'), (SELECT id FROM batch WHERE batch_name = '2023-2027 CSE' ORDER BY id LIMIT 1), 2023, 2, 4, 'student1@smartcampus.local', '9876543210', 'O+', 'English', 'American'),
('STU002', 'Jane Smith', '2005-08-20', 'FEMALE', '456 Oak Ave', (SELECT id FROM department WHERE dept_name = 'Computer Science & Engineering'), (SELECT id FROM batch WHERE batch_name = '2023-2027 CSE' ORDER BY id LIMIT 1), 2023, 2, 4, 'student2@smartcampus.local', '9876543211', 'A+', 'English', 'American')
ON CONFLICT (roll_number) DO NOTHING;

-- 7. father_details
INSERT INTO father_details (student_id, name, phone, email, occupation, annual_income, address) VALUES
((SELECT id FROM student WHERE roll_number = 'STU001'), 'Robert Doe', '1234567890', 'robert@doe.local', 'Engineer', '80000', '123 Main St'),
((SELECT id FROM student WHERE roll_number = 'STU002'), 'James Smith', '1234567891', 'james@smith.local', 'Manager', '95000', '456 Oak Ave')
ON CONFLICT (student_id) DO NOTHING;

-- 8. mother_details
INSERT INTO mother_details (student_id, name, phone, email, occupation, annual_income, address) VALUES
((SELECT id FROM student WHERE roll_number = 'STU001'), 'Mary Doe', '1234567892', 'mary@doe.local', 'Teacher', '60000', '123 Main St'),
((SELECT id FROM student WHERE roll_number = 'STU002'), 'Patricia Smith', '1234567893', 'patricia@smith.local', 'Doctor', '120000', '456 Oak Ave')
ON CONFLICT (student_id) DO NOTHING;

-- 9. identity_details
INSERT INTO identity_details (student_id, aadhar_number, pan_number, passport_number) VALUES
((SELECT id FROM student WHERE roll_number = 'STU001'), '1234-5678-9012', 'ABCDE1234F', 'Z1234567'),
((SELECT id FROM student WHERE roll_number = 'STU002'), '9876-5432-1098', 'XYZWR5678K', 'Y9876543')
ON CONFLICT (student_id) DO NOTHING;

-- 10. teacher
INSERT INTO teacher (teacher_clg_id, name, email, phone, username, account_status, last_login) VALUES
('TCH001', 'Dr. Alan Turing', 'teacher1@smartcampus.local', '9876543220', 'turing', 'ACTIVE', CURRENT_TIMESTAMP),
('TCH002', 'Dr. Grace Hopper', 'teacher2@smartcampus.local', '9876543221', 'hopper', 'ACTIVE', CURRENT_TIMESTAMP)
ON CONFLICT (teacher_clg_id) DO NOTHING;

-- 11. teacher_personal_details
INSERT INTO teacher_personal_details (teacher_id, gender, date_of_birth, blood_group, address) VALUES
((SELECT id FROM teacher WHERE teacher_clg_id = 'TCH001'), 'MALE', '1980-06-23', 'B+', '789 Math Rd'),
((SELECT id FROM teacher WHERE teacher_clg_id = 'TCH002'), 'FEMALE', '1982-12-09', 'AB+', '321 Compiler Ln')
ON CONFLICT (teacher_id) DO NOTHING;

-- 12. teacher_employment_details
INSERT INTO teacher_employment_details (teacher_id, department_id, designation, employment_type, joining_date, experience_years, office_location, staff_type) VALUES
((SELECT id FROM teacher WHERE teacher_clg_id = 'TCH001'), (SELECT id FROM department WHERE dept_name = 'Computer Science & Engineering'), 'Professor', 'FULLTIME', '2015-06-01', '10', 'CSE Block, Room 101', 'TEACHING'),
((SELECT id FROM teacher WHERE teacher_clg_id = 'TCH002'), (SELECT id FROM department WHERE dept_name = 'Computer Science & Engineering'), 'Associate Professor', 'FULLTIME', '2018-09-15', '8', 'CSE Block, Room 102', 'TEACHING')
ON CONFLICT (teacher_id) DO NOTHING;

-- 13. teacher_qualification_details
INSERT INTO teacher_qualification_details (teacher_id, ug_degree, pg_degree, phd_status, specialization, university_name, year_of_passing)
SELECT t.id, v.ug_degree, v.pg_degree, 'COMPLETED', v.specialization, v.university_name, v.year_of_passing
FROM (VALUES
    ('TCH001', 'B.Tech CSE', 'M.Tech CSE', 'Theoretical Computer Science', 'Princeton University', 2005),
    ('TCH002', 'B.Sc Math', 'M.Sc CS', 'Programming Languages & Compilers', 'Yale University', 2007)
) AS v(teacher_clg_id, ug_degree, pg_degree, specialization, university_name, year_of_passing)
JOIN teacher t ON t.teacher_clg_id = v.teacher_clg_id
WHERE NOT EXISTS (
    SELECT 1 FROM teacher_qualification_details q
    WHERE q.teacher_id = t.id AND q.specialization = v.specialization
);

-- 14. teacher_research_publications
INSERT INTO teacher_research_publications (teacher_id, papers_published, conferences_attended, workshops_attended, patents, funded_projects) VALUES
((SELECT id FROM teacher WHERE teacher_clg_id = 'TCH001'), 15, 10, 5, 2, 1),
((SELECT id FROM teacher WHERE teacher_clg_id = 'TCH002'), 12, 8, 6, 1, 2)
ON CONFLICT (teacher_id) DO NOTHING;

-- 15. teacher_leave_balance
INSERT INTO teacher_leave_balance (teacher_id, casual_leave_balance, medical_leave_balance, earned_leave_balance) VALUES
((SELECT id FROM teacher WHERE teacher_clg_id = 'TCH001'), 12, 10, 30),
((SELECT id FROM teacher WHERE teacher_clg_id = 'TCH002'), 12, 10, 30)
ON CONFLICT (teacher_id) DO NOTHING;

-- 16. course_teacher_allocation
INSERT INTO course_teacher_allocation (course_id, teacher_id) VALUES
((SELECT id FROM course WHERE course_code = 'CS-401'), (SELECT id FROM teacher WHERE teacher_clg_id = 'TCH001')),
((SELECT id FROM course WHERE course_code = 'CS-402'), (SELECT id FROM teacher WHERE teacher_clg_id = 'TCH002'))
ON CONFLICT (course_id, teacher_id) DO NOTHING;

-- 17. student_course_teacher
INSERT INTO student_course_teacher (student_id, course_id, teacher_id, semester, status) VALUES
((SELECT id FROM student WHERE roll_number = 'STU001'), (SELECT id FROM course WHERE course_code = 'CS-401'), (SELECT id FROM teacher WHERE teacher_clg_id = 'TCH001'), 4, 'ACTIVE'),
((SELECT id FROM student WHERE roll_number = 'STU001'), (SELECT id FROM course WHERE course_code = 'CS-402'), (SELECT id FROM teacher WHERE teacher_clg_id = 'TCH002'), 4, 'ACTIVE'),
((SELECT id FROM student WHERE roll_number = 'STU002'), (SELECT id FROM course WHERE course_code = 'CS-401'), (SELECT id FROM teacher WHERE teacher_clg_id = 'TCH001'), 4, 'ACTIVE')
ON CONFLICT DO NOTHING;

-- 18. course_material
INSERT INTO course_material (course_id, teacher_id, title, module, material_type, description, original_filename, stored_filename, stored_path, mime_type, file_size, download_allowed)
SELECT c.id, t.id, 'Introduction to Web Programming', 'Module 1', 'PDF', 'Basics of HTML, CSS, and JS', 'lecture1.pdf', 'stored_lecture1.pdf', '/materials/stored_lecture1.pdf', 'application/pdf', 102456, 1
FROM course c
JOIN teacher t ON t.teacher_clg_id = 'TCH001'
WHERE c.course_code = 'CS-401'
  AND NOT EXISTS (
      SELECT 1 FROM course_material m
      WHERE m.course_id = c.id AND m.teacher_id = t.id AND m.title = 'Introduction to Web Programming'
  );

-- 19. course_assignment
INSERT INTO course_assignment (course_id, teacher_id, title, assignment_mode, question_text, instructions, original_filename, stored_filename, stored_path, mime_type, file_size, due_at, max_marks, download_allowed, is_published)
SELECT c.id, t.id, 'HTML/CSS Portfolio', 'FILE', 'Build a responsive personal portfolio using semantic HTML and custom CSS.', 'Upload zip containing files.', 'instructions.pdf', 'stored_instructions.pdf', '/assignments/stored_instructions.pdf', 'application/pdf', 54321, CURRENT_TIMESTAMP + INTERVAL '14 days', 50, 1, 1
FROM course c
JOIN teacher t ON t.teacher_clg_id = 'TCH001'
WHERE c.course_code = 'CS-401'
  AND NOT EXISTS (
      SELECT 1 FROM course_assignment a
      WHERE a.course_id = c.id AND a.teacher_id = t.id AND a.title = 'HTML/CSS Portfolio'
  );

-- 20. course_assignment_submission
INSERT INTO course_assignment_submission (assignment_id, student_id, original_filename, stored_filename, stored_path, mime_type, file_size) VALUES
((SELECT id FROM course_assignment WHERE title = 'HTML/CSS Portfolio' ORDER BY id LIMIT 1), (SELECT id FROM student WHERE roll_number = 'STU001'), 'john_doe_portfolio.zip', 'stored_john_doe_portfolio.zip', '/submissions/stored_john_doe_portfolio.zip', 'application/zip', 987654)
ON CONFLICT (assignment_id, student_id) DO NOTHING;

-- 21. quiz
INSERT INTO quiz (course_id, teacher_id, title, instructions, total_marks, duration_minutes, start_at, end_at, is_published, is_score_published)
SELECT c.id, t.id, 'HTML Basics Quiz', 'Answer all questions. 10 minutes limit.', 10, 10, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '1 day', 1, 1
FROM course c
JOIN teacher t ON t.teacher_clg_id = 'TCH001'
WHERE c.course_code = 'CS-401'
  AND NOT EXISTS (
      SELECT 1 FROM quiz q
      WHERE q.course_id = c.id AND q.teacher_id = t.id AND q.title = 'HTML Basics Quiz'
  );

-- 22. quiz_question
INSERT INTO quiz_question (quiz_id, question_order, question_text, question_type, marks, correct_option_index)
SELECT q.id, v.question_order, v.question_text, v.question_type, v.marks, v.correct_option_index
FROM (VALUES
    (1, 'What does HTML stand for?', 'MCQ', 5, 1),
    (2, 'Explain the difference between block and inline elements.', 'DESCRIPTIVE', 5, NULL)
) AS v(question_order, question_text, question_type, marks, correct_option_index)
JOIN quiz q ON q.title = 'HTML Basics Quiz'
WHERE NOT EXISTS (
    SELECT 1 FROM quiz_question qq
    WHERE qq.quiz_id = q.id AND qq.question_order = v.question_order
);

-- 23. quiz_option
INSERT INTO quiz_option (question_id, option_index, option_text) VALUES
((SELECT id FROM quiz_question WHERE question_text = 'What does HTML stand for?' ORDER BY id LIMIT 1), 1, 'HyperText Markup Language'),
((SELECT id FROM quiz_question WHERE question_text = 'What does HTML stand for?' ORDER BY id LIMIT 1), 2, 'Hyperlinks and Text Markup Language'),
((SELECT id FROM quiz_question WHERE question_text = 'What does HTML stand for?' ORDER BY id LIMIT 1), 3, 'Home Tool Markup Language'),
((SELECT id FROM quiz_question WHERE question_text = 'What does HTML stand for?' ORDER BY id LIMIT 1), 4, 'HyperText Machine Language')
ON CONFLICT (question_id, option_index) DO NOTHING;

-- 24. quiz_submission
INSERT INTO quiz_submission (quiz_id, student_id, score) VALUES
((SELECT id FROM quiz WHERE title = 'HTML Basics Quiz' ORDER BY id LIMIT 1), (SELECT id FROM student WHERE roll_number = 'STU001'), 5)
ON CONFLICT (quiz_id, student_id) DO NOTHING;

-- 25. quiz_answer
INSERT INTO quiz_answer (submission_id, question_id, selected_option_index, answer_text, marks_awarded) VALUES
((SELECT id FROM quiz_submission WHERE quiz_id = (SELECT id FROM quiz WHERE title = 'HTML Basics Quiz' ORDER BY id LIMIT 1) AND student_id = (SELECT id FROM student WHERE roll_number = 'STU001') ORDER BY id LIMIT 1), (SELECT id FROM quiz_question WHERE question_text = 'What does HTML stand for?' ORDER BY id LIMIT 1), 1, NULL, 5),
((SELECT id FROM quiz_submission WHERE quiz_id = (SELECT id FROM quiz WHERE title = 'HTML Basics Quiz' ORDER BY id LIMIT 1) AND student_id = (SELECT id FROM student WHERE roll_number = 'STU001') ORDER BY id LIMIT 1), (SELECT id FROM quiz_question WHERE question_text = 'Explain the difference between block and inline elements.' ORDER BY id LIMIT 1), NULL, 'Block elements start on a new line and take full width. Inline elements stay in line.', NULL)
ON CONFLICT (submission_id, question_id) DO NOTHING;

-- 26. feedback_form
INSERT INTO feedback_form (course_id, teacher_id, title, description, is_active)
SELECT c.id, t.id, 'Mid-Semester Course Feedback', 'Provide honest feedback about course pace and quality.', 1
FROM course c
JOIN teacher t ON t.teacher_clg_id = 'TCH001'
WHERE c.course_code = 'CS-401'
  AND NOT EXISTS (
      SELECT 1 FROM feedback_form f
      WHERE f.course_id = c.id AND f.teacher_id = t.id AND f.title = 'Mid-Semester Course Feedback'
  );

-- 27. feedback_question
INSERT INTO feedback_question (form_id, question_order, question_text, question_type, options_text, rating_max, required)
SELECT f.id, v.question_order, v.question_text, v.question_type, NULL, v.rating_max, v.required
FROM (VALUES
    (1, 'Rate the teaching pace (1 to 5)', 'RATING', 5, 1),
    (2, 'Any suggestions for improvement?', 'TEXT', NULL, 0)
) AS v(question_order, question_text, question_type, rating_max, required)
JOIN feedback_form f ON f.title = 'Mid-Semester Course Feedback'
WHERE NOT EXISTS (
    SELECT 1 FROM feedback_question fq
    WHERE fq.form_id = f.id AND fq.question_order = v.question_order
);

-- 28. feedback_submission
INSERT INTO feedback_submission (form_id, student_id) VALUES
((SELECT id FROM feedback_form WHERE title = 'Mid-Semester Course Feedback' ORDER BY id LIMIT 1), (SELECT id FROM student WHERE roll_number = 'STU001'))
ON CONFLICT (form_id, student_id) DO NOTHING;

-- 29. feedback_answer
INSERT INTO feedback_answer (submission_id, question_id, answer_text, answer_number) VALUES
((SELECT id FROM feedback_submission WHERE form_id = (SELECT id FROM feedback_form WHERE title = 'Mid-Semester Course Feedback' ORDER BY id LIMIT 1) AND student_id = (SELECT id FROM student WHERE roll_number = 'STU001') ORDER BY id LIMIT 1), (SELECT id FROM feedback_question WHERE question_text = 'Rate the teaching pace (1 to 5)' ORDER BY id LIMIT 1), NULL, 5),
((SELECT id FROM feedback_submission WHERE form_id = (SELECT id FROM feedback_form WHERE title = 'Mid-Semester Course Feedback' ORDER BY id LIMIT 1) AND student_id = (SELECT id FROM student WHERE roll_number = 'STU001') ORDER BY id LIMIT 1), (SELECT id FROM feedback_question WHERE question_text = 'Any suggestions for improvement?' ORDER BY id LIMIT 1), 'The classes are excellent. More lab examples would be helpful.', NULL)
ON CONFLICT (submission_id, question_id) DO NOTHING;

-- 30. extra departments
INSERT INTO department (dept_name) VALUES
('Electronics and Communication Engineering'),
('Artificial Intelligence and Data Science')
ON CONFLICT (dept_name) DO NOTHING;

-- 31. extra batches
INSERT INTO batch (dept_id, batch_year, batch_name)
SELECT d.id, v.batch_year::date, v.batch_name
FROM (VALUES
    ('Computer Science & Engineering', '2024-06-01', '2024-2028 CSE'),
    ('Information Technology', '2024-06-01', '2024-2028 IT'),
    ('Electronics and Communication Engineering', '2023-06-01', '2023-2027 ECE'),
    ('Artificial Intelligence and Data Science', '2023-06-01', '2023-2027 AIDS')
) AS v(dept_name, batch_year, batch_name)
JOIN department d ON d.dept_name = v.dept_name
WHERE NOT EXISTS (SELECT 1 FROM batch b WHERE b.batch_name = v.batch_name);

-- 32. extra users
INSERT INTO users (email, password, role)
SELECT format('student%s@smartcampus.local', n), format('student%s', n), 'STUDENT'
FROM generate_series(3, 62) AS s(n)
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (email, password, role) VALUES
('teacher3@smartcampus.local', 'teacher3', 'TEACHER'),
('teacher4@smartcampus.local', 'teacher4', 'TEACHER'),
('teacher5@smartcampus.local', 'teacher5', 'TEACHER'),
('teacher6@smartcampus.local', 'teacher6', 'TEACHER')
ON CONFLICT (email) DO NOTHING;

-- 33. extra teachers
INSERT INTO teacher (teacher_clg_id, name, email, phone, username, account_status, last_login) VALUES
('TCH003', 'Dr. Katherine Johnson', 'teacher3@smartcampus.local', '9876543222', 'kjohnson', 'ACTIVE', CURRENT_TIMESTAMP),
('TCH004', 'Dr. Radia Perlman', 'teacher4@smartcampus.local', '9876543223', 'rperlman', 'ACTIVE', CURRENT_TIMESTAMP),
('TCH005', 'Dr. Barbara Liskov', 'teacher5@smartcampus.local', '9876543224', 'bliskov', 'ACTIVE', CURRENT_TIMESTAMP),
('TCH006', 'Dr. Tim Berners-Lee', 'teacher6@smartcampus.local', '9876543225', 'tbernerslee', 'ACTIVE', CURRENT_TIMESTAMP)
ON CONFLICT (teacher_clg_id) DO NOTHING;

INSERT INTO teacher_personal_details (teacher_id, gender, date_of_birth, blood_group, address)
SELECT t.id, v.gender, v.date_of_birth::date, v.blood_group, v.address
FROM (VALUES
    ('TCH003', 'FEMALE', '1978-08-26', 'O+', 'AIDS Block, Room 201'),
    ('TCH004', 'FEMALE', '1975-12-18', 'A-', 'IT Block, Room 204'),
    ('TCH005', 'FEMALE', '1972-11-07', 'B-', 'CSE Block, Room 205'),
    ('TCH006', 'MALE', '1970-06-08', 'AB-', 'CSE Block, Room 206')
) AS v(teacher_clg_id, gender, date_of_birth, blood_group, address)
JOIN teacher t ON t.teacher_clg_id = v.teacher_clg_id
ON CONFLICT (teacher_id) DO NOTHING;

INSERT INTO teacher_employment_details (teacher_id, department_id, designation, employment_type, joining_date, experience_years, office_location, staff_type)
SELECT t.id, d.id, v.designation, 'FULLTIME', v.joining_date::date, v.experience_years, v.office_location, 'TEACHING'
FROM (VALUES
    ('TCH003', 'Artificial Intelligence and Data Science', 'Professor', '2014-07-01', '12', 'AIDS Block, Room 201'),
    ('TCH004', 'Information Technology', 'Associate Professor', '2016-07-01', '10', 'IT Block, Room 204'),
    ('TCH005', 'Computer Science & Engineering', 'Professor', '2013-06-15', '14', 'CSE Block, Room 205'),
    ('TCH006', 'Computer Science & Engineering', 'Assistant Professor', '2020-08-01', '6', 'CSE Block, Room 206')
) AS v(teacher_clg_id, dept_name, designation, joining_date, experience_years, office_location)
JOIN teacher t ON t.teacher_clg_id = v.teacher_clg_id
JOIN department d ON d.dept_name = v.dept_name
ON CONFLICT (teacher_id) DO NOTHING;

INSERT INTO teacher_qualification_details (teacher_id, ug_degree, pg_degree, phd_status, specialization, university_name, year_of_passing)
SELECT t.id, v.ug_degree, v.pg_degree, 'COMPLETED', v.specialization, v.university_name, v.year_of_passing
FROM (VALUES
    ('TCH003', 'B.Tech CSE', 'M.Tech AI', 'Machine Learning', 'Stanford University', 2008),
    ('TCH004', 'B.Tech IT', 'M.Tech Networks', 'Computer Networks', 'MIT', 2006),
    ('TCH005', 'B.Tech CSE', 'M.Tech Software Engineering', 'Distributed Systems', 'Harvard University', 2004),
    ('TCH006', 'B.Tech CSE', 'M.Tech Web Science', 'Semantic Web', 'University of Oxford', 2005)
) AS v(teacher_clg_id, ug_degree, pg_degree, specialization, university_name, year_of_passing)
JOIN teacher t ON t.teacher_clg_id = v.teacher_clg_id
WHERE NOT EXISTS (
    SELECT 1 FROM teacher_qualification_details q
    WHERE q.teacher_id = t.id AND q.specialization = v.specialization
);

INSERT INTO teacher_research_publications (teacher_id, papers_published, conferences_attended, workshops_attended, patents, funded_projects)
SELECT t.id, v.papers_published, v.conferences_attended, v.workshops_attended, v.patents, v.funded_projects
FROM (VALUES
    ('TCH003', 22, 14, 9, 3, 4),
    ('TCH004', 18, 12, 7, 2, 3),
    ('TCH005', 28, 16, 11, 4, 5),
    ('TCH006', 16, 9, 8, 1, 2)
) AS v(teacher_clg_id, papers_published, conferences_attended, workshops_attended, patents, funded_projects)
JOIN teacher t ON t.teacher_clg_id = v.teacher_clg_id
ON CONFLICT (teacher_id) DO NOTHING;

INSERT INTO teacher_leave_balance (teacher_id, casual_leave_balance, medical_leave_balance, earned_leave_balance)
SELECT t.id, 12, 10, 30
FROM teacher t
WHERE t.teacher_clg_id IN ('TCH003', 'TCH004', 'TCH005', 'TCH006')
ON CONFLICT (teacher_id) DO NOTHING;

-- 34. extra courses and department mappings
INSERT INTO course (course_name, course_code, credit, course_type) VALUES
('Data Structures', 'CS-301', 4, 'THEORY'),
('Operating Systems', 'CS-501', 4, 'THEORY'),
('Computer Networks', 'IT-401', 4, 'THEORY'),
('Machine Learning', 'AI-501', 4, 'THEORY'),
('Digital Signal Processing', 'EC-401', 4, 'THEORY'),
('Full Stack Development Lab', 'CS-403', 2, 'LAB')
ON CONFLICT (course_code) DO NOTHING;

INSERT INTO course_for_depts (course_id, dept_id, sem)
SELECT c.id, d.id, v.sem
FROM (VALUES
    ('CS-301', 'Computer Science & Engineering', 3),
    ('CS-501', 'Computer Science & Engineering', 5),
    ('IT-401', 'Information Technology', 4),
    ('AI-501', 'Artificial Intelligence and Data Science', 5),
    ('EC-401', 'Electronics and Communication Engineering', 4),
    ('CS-403', 'Computer Science & Engineering', 4)
) AS v(course_code, dept_name, sem)
JOIN course c ON c.course_code = v.course_code
JOIN department d ON d.dept_name = v.dept_name
ON CONFLICT (course_id, dept_id, sem) DO NOTHING;

INSERT INTO course_teacher_allocation (course_id, teacher_id)
SELECT c.id, t.id
FROM (VALUES
    ('CS-301', 'TCH005'),
    ('CS-501', 'TCH006'),
    ('IT-401', 'TCH004'),
    ('AI-501', 'TCH003'),
    ('EC-401', 'TCH004'),
    ('CS-403', 'TCH006')
) AS v(course_code, teacher_clg_id)
JOIN course c ON c.course_code = v.course_code
JOIN teacher t ON t.teacher_clg_id = v.teacher_clg_id
ON CONFLICT (course_id, teacher_id) DO NOTHING;

-- 35. 60 extra students
WITH student_seed AS (
    SELECT
        n,
        'STU' || lpad(n::text, 3, '0') AS roll_number,
        'Sample Student ' || n AS student_name,
        CASE n % 4
            WHEN 0 THEN 'Computer Science & Engineering'
            WHEN 1 THEN 'Information Technology'
            WHEN 2 THEN 'Electronics and Communication Engineering'
            ELSE 'Artificial Intelligence and Data Science'
        END AS dept_name,
        CASE n % 4
            WHEN 0 THEN '2024-2028 CSE'
            WHEN 1 THEN '2024-2028 IT'
            WHEN 2 THEN '2023-2027 ECE'
            ELSE '2023-2027 AIDS'
        END AS batch_name,
        CASE WHEN n % 4 IN (0, 1) THEN 2024 ELSE 2023 END AS batch_year_value,
        CASE WHEN n % 4 IN (0, 1) THEN 1 ELSE 2 END AS current_year_value,
        CASE WHEN n % 4 IN (0, 1) THEN 2 ELSE 4 END AS current_semester_value
    FROM generate_series(3, 62) AS s(n)
)
INSERT INTO student (roll_number, name, dob, gender, address, dept_id, batch_id, batch_year, current_year, current_semester, email, phone, blood_group, mother_tongue, nationality)
SELECT
    ss.roll_number,
    ss.student_name,
    ('2005-01-01'::date + ((ss.n % 365) * INTERVAL '1 day'))::date,
    CASE WHEN ss.n % 2 = 0 THEN 'MALE' ELSE 'FEMALE' END,
    'Hostel Block ' || ((ss.n % 6) + 1) || ', SmartCampus',
    d.id,
    b.id,
    ss.batch_year_value,
    ss.current_year_value,
    ss.current_semester_value,
    format('student%s@smartcampus.local', ss.n),
    '90000' || lpad(ss.n::text, 5, '0'),
    (ARRAY['O+', 'A+', 'B+', 'AB+', 'O-', 'A-'])[(ss.n % 6) + 1],
    (ARRAY['Tamil', 'English', 'Hindi', 'Telugu', 'Malayalam'])[(ss.n % 5) + 1],
    'Indian'
FROM student_seed ss
JOIN department d ON d.dept_name = ss.dept_name
JOIN LATERAL (
    SELECT id FROM batch WHERE batch_name = ss.batch_name ORDER BY id LIMIT 1
) b ON true
ON CONFLICT (roll_number) DO NOTHING;

INSERT INTO father_details (student_id, name, phone, email, occupation, annual_income, address)
SELECT s.id, 'Father of ' || s.name, '91000' || lpad(right(s.roll_number, 3), 5, '0'), lower(replace('father.' || s.roll_number || '@family.local', ' ', '')), 'Private Employee', '650000', s.address
FROM student s
WHERE s.roll_number BETWEEN 'STU003' AND 'STU062'
ON CONFLICT (student_id) DO NOTHING;

INSERT INTO mother_details (student_id, name, phone, email, occupation, annual_income, address)
SELECT s.id, 'Mother of ' || s.name, '92000' || lpad(right(s.roll_number, 3), 5, '0'), lower(replace('mother.' || s.roll_number || '@family.local', ' ', '')), 'Homemaker', '350000', s.address
FROM student s
WHERE s.roll_number BETWEEN 'STU003' AND 'STU062'
ON CONFLICT (student_id) DO NOTHING;

INSERT INTO identity_details (student_id, aadhar_number, pan_number, passport_number)
SELECT s.id,
       '6000-7000-' || lpad(right(s.roll_number, 3), 4, '0'),
       'PAN' || right(s.roll_number, 3) || 'SC',
       'P' || lpad(right(s.roll_number, 3), 7, '0')
FROM student s
WHERE s.roll_number BETWEEN 'STU003' AND 'STU062'
ON CONFLICT (student_id) DO NOTHING;

-- 36. extra student-course-teacher enrollments
WITH enrollment_seed AS (
    SELECT s.id AS student_id, s.roll_number, d.dept_name, s.current_semester
    FROM student s
    JOIN department d ON d.id = s.dept_id
    WHERE s.roll_number BETWEEN 'STU003' AND 'STU062'
)
INSERT INTO student_course_teacher (student_id, course_id, teacher_id, semester, status)
SELECT es.student_id, c.id, t.id, es.current_semester, 'ACTIVE'
FROM enrollment_seed es
JOIN LATERAL (
    SELECT *
    FROM (VALUES
        ('Computer Science & Engineering', 'CS-401', 'TCH001'),
        ('Computer Science & Engineering', 'CS-403', 'TCH006'),
        ('Information Technology', 'IT-401', 'TCH004'),
        ('Electronics and Communication Engineering', 'EC-401', 'TCH004'),
        ('Artificial Intelligence and Data Science', 'AI-501', 'TCH003')
    ) AS v(dept_name, course_code, teacher_clg_id)
    WHERE v.dept_name = es.dept_name
) v ON true
JOIN course c ON c.course_code = v.course_code
JOIN teacher t ON t.teacher_clg_id = v.teacher_clg_id
ON CONFLICT DO NOTHING;

-- 37. extra course materials
INSERT INTO course_material (course_id, teacher_id, title, module, material_type, description, original_filename, stored_filename, stored_path, mime_type, file_size, download_allowed)
SELECT c.id, t.id, v.title, v.module, 'PDF', v.description, v.original_filename, v.stored_filename, v.stored_path, 'application/pdf', v.file_size, 1
FROM (VALUES
    ('CS-301', 'TCH005', 'Stacks and Queues Notes', 'Module 2', 'Linear data structures with worked examples.', 'stacks_queues.pdf', 'stored_stacks_queues.pdf', '/materials/stored_stacks_queues.pdf', 184320),
    ('CS-501', 'TCH006', 'Process Scheduling Guide', 'Module 1', 'CPU scheduling algorithms and sample problems.', 'process_scheduling.pdf', 'stored_process_scheduling.pdf', '/materials/stored_process_scheduling.pdf', 212992),
    ('IT-401', 'TCH004', 'Network Layers Overview', 'Module 1', 'OSI and TCP/IP model comparison.', 'network_layers.pdf', 'stored_network_layers.pdf', '/materials/stored_network_layers.pdf', 164800),
    ('AI-501', 'TCH003', 'Regression Fundamentals', 'Module 2', 'Linear regression, loss functions, and model evaluation.', 'regression_fundamentals.pdf', 'stored_regression_fundamentals.pdf', '/materials/stored_regression_fundamentals.pdf', 245760),
    ('EC-401', 'TCH004', 'DSP Signals Primer', 'Module 1', 'Discrete signals and systems introduction.', 'dsp_signals.pdf', 'stored_dsp_signals.pdf', '/materials/stored_dsp_signals.pdf', 198144)
) AS v(course_code, teacher_clg_id, title, module, description, original_filename, stored_filename, stored_path, file_size)
JOIN course c ON c.course_code = v.course_code
JOIN teacher t ON t.teacher_clg_id = v.teacher_clg_id
WHERE NOT EXISTS (
    SELECT 1 FROM course_material m
    WHERE m.course_id = c.id AND m.teacher_id = t.id AND m.title = v.title
);

-- 38. extra assignments
INSERT INTO course_assignment (course_id, teacher_id, title, assignment_mode, question_text, instructions, due_at, max_marks, download_allowed, is_published)
SELECT c.id, t.id, v.title, 'TEXT', v.question_text, v.instructions, CURRENT_TIMESTAMP + (v.days_until_due || ' days')::interval, v.max_marks, 1, 1
FROM (VALUES
    ('CS-301', 'TCH005', 'Linked List Implementation', 'Implement singly and doubly linked list operations.', 'Submit algorithm notes and complexity analysis.', 10, 25),
    ('CS-501', 'TCH006', 'Scheduling Case Study', 'Compare FCFS, SJF, Priority, and Round Robin scheduling.', 'Include a tabular comparison with waiting time.', 12, 30),
    ('IT-401', 'TCH004', 'Subnetting Practice', 'Solve the given subnet allocation scenario.', 'Show subnet mask, network address, and host ranges.', 9, 20),
    ('AI-501', 'TCH003', 'Regression Mini Project', 'Train and evaluate a regression model on a small dataset.', 'Summarize features, metric, and observations.', 15, 40),
    ('EC-401', 'TCH004', 'Signal Sampling Notes', 'Explain aliasing and Nyquist sampling with examples.', 'Attach diagrams where required.', 11, 25)
) AS v(course_code, teacher_clg_id, title, question_text, instructions, days_until_due, max_marks)
JOIN course c ON c.course_code = v.course_code
JOIN teacher t ON t.teacher_clg_id = v.teacher_clg_id
WHERE NOT EXISTS (
    SELECT 1 FROM course_assignment a
    WHERE a.course_id = c.id AND a.teacher_id = t.id AND a.title = v.title
);

INSERT INTO course_assignment_submission (assignment_id, student_id, original_filename, stored_filename, stored_path, mime_type, file_size)
SELECT a.id, s.id,
       lower(s.roll_number || '_' || replace(a.title, ' ', '_') || '.txt'),
       lower('stored_' || s.roll_number || '_' || replace(a.title, ' ', '_') || '.txt'),
       lower('/submissions/stored_' || s.roll_number || '_' || replace(a.title, ' ', '_') || '.txt'),
       'text/plain',
       8192 + (right(s.roll_number, 3)::integer * 10)
FROM course_assignment a
JOIN course c ON c.id = a.course_id
JOIN student_course_teacher sct ON sct.course_id = a.course_id AND sct.teacher_id = a.teacher_id
JOIN student s ON s.id = sct.student_id
WHERE a.title IN ('Linked List Implementation', 'Scheduling Case Study', 'Subnetting Practice', 'Regression Mini Project', 'Signal Sampling Notes')
  AND right(s.roll_number, 3)::integer BETWEEN 3 AND 22
ON CONFLICT (assignment_id, student_id) DO NOTHING;

-- 39. extra quizzes
INSERT INTO quiz (course_id, teacher_id, title, instructions, total_marks, duration_minutes, start_at, end_at, is_published, is_score_published)
SELECT c.id, t.id, v.title, 'Answer all questions before submitting.', 10, 15, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP + INTERVAL '5 days', 1, 1
FROM (VALUES
    ('CS-301', 'TCH005', 'Data Structures Quick Check'),
    ('CS-501', 'TCH006', 'Operating Systems Quiz'),
    ('IT-401', 'TCH004', 'Networking Basics Quiz'),
    ('AI-501', 'TCH003', 'Machine Learning Basics Quiz'),
    ('EC-401', 'TCH004', 'DSP Fundamentals Quiz')
) AS v(course_code, teacher_clg_id, title)
JOIN course c ON c.course_code = v.course_code
JOIN teacher t ON t.teacher_clg_id = v.teacher_clg_id
WHERE NOT EXISTS (
    SELECT 1 FROM quiz q
    WHERE q.course_id = c.id AND q.teacher_id = t.id AND q.title = v.title
);

INSERT INTO quiz_question (quiz_id, question_order, question_text, question_type, marks, correct_option_index)
SELECT q.id, v.question_order, v.question_text, 'MCQ', 5, v.correct_option_index
FROM (VALUES
    ('Data Structures Quick Check', 1, 'Which structure follows LIFO order?', 2),
    ('Data Structures Quick Check', 2, 'Which traversal visits left subtree before root?', 1),
    ('Operating Systems Quiz', 1, 'What is a process?', 1),
    ('Operating Systems Quiz', 2, 'Which scheduling algorithm uses time quantum?', 3),
    ('Networking Basics Quiz', 1, 'Which layer handles routing?', 3),
    ('Networking Basics Quiz', 2, 'Which protocol provides reliable transport?', 2),
    ('Machine Learning Basics Quiz', 1, 'Which metric is common for regression?', 4),
    ('Machine Learning Basics Quiz', 2, 'What does overfitting mean?', 1),
    ('DSP Fundamentals Quiz', 1, 'What is aliasing?', 2),
    ('DSP Fundamentals Quiz', 2, 'Which transform converts time domain to frequency domain?', 4)
) AS v(quiz_title, question_order, question_text, correct_option_index)
JOIN quiz q ON q.title = v.quiz_title
WHERE NOT EXISTS (
    SELECT 1 FROM quiz_question qq
    WHERE qq.quiz_id = q.id AND qq.question_order = v.question_order
);

INSERT INTO quiz_option (question_id, option_index, option_text)
SELECT qq.id, v.option_index, v.option_text
FROM (VALUES
    ('Which structure follows LIFO order?', 1, 'Queue'),
    ('Which structure follows LIFO order?', 2, 'Stack'),
    ('Which structure follows LIFO order?', 3, 'Tree'),
    ('Which structure follows LIFO order?', 4, 'Graph'),
    ('Which traversal visits left subtree before root?', 1, 'Inorder'),
    ('Which traversal visits left subtree before root?', 2, 'Preorder'),
    ('Which traversal visits left subtree before root?', 3, 'Level order'),
    ('Which traversal visits left subtree before root?', 4, 'Breadth first'),
    ('What is a process?', 1, 'Program in execution'),
    ('What is a process?', 2, 'A compiled file only'),
    ('What is a process?', 3, 'A memory address'),
    ('What is a process?', 4, 'A device driver only'),
    ('Which scheduling algorithm uses time quantum?', 1, 'FCFS'),
    ('Which scheduling algorithm uses time quantum?', 2, 'SJF'),
    ('Which scheduling algorithm uses time quantum?', 3, 'Round Robin'),
    ('Which scheduling algorithm uses time quantum?', 4, 'Priority'),
    ('Which layer handles routing?', 1, 'Application'),
    ('Which layer handles routing?', 2, 'Transport'),
    ('Which layer handles routing?', 3, 'Network'),
    ('Which layer handles routing?', 4, 'Physical'),
    ('Which protocol provides reliable transport?', 1, 'UDP'),
    ('Which protocol provides reliable transport?', 2, 'TCP'),
    ('Which protocol provides reliable transport?', 3, 'IP'),
    ('Which protocol provides reliable transport?', 4, 'ARP'),
    ('Which metric is common for regression?', 1, 'Accuracy'),
    ('Which metric is common for regression?', 2, 'Precision'),
    ('Which metric is common for regression?', 3, 'Recall'),
    ('Which metric is common for regression?', 4, 'RMSE'),
    ('What does overfitting mean?', 1, 'Model memorizes training patterns and generalizes poorly'),
    ('What does overfitting mean?', 2, 'Model has no parameters'),
    ('What does overfitting mean?', 3, 'Dataset is encrypted'),
    ('What does overfitting mean?', 4, 'Training data is empty'),
    ('What is aliasing?', 1, 'Increasing signal amplitude'),
    ('What is aliasing?', 2, 'High frequency components appearing as lower frequencies'),
    ('What is aliasing?', 3, 'Reducing noise with a filter'),
    ('What is aliasing?', 4, 'Combining two channels'),
    ('Which transform converts time domain to frequency domain?', 1, 'Laplace table'),
    ('Which transform converts time domain to frequency domain?', 2, 'Z buffer'),
    ('Which transform converts time domain to frequency domain?', 3, 'Truth table'),
    ('Which transform converts time domain to frequency domain?', 4, 'Fourier transform')
) AS v(question_text, option_index, option_text)
JOIN quiz_question qq ON qq.question_text = v.question_text
ON CONFLICT (question_id, option_index) DO NOTHING;

INSERT INTO quiz_submission (quiz_id, student_id, score)
SELECT q.id, s.id, CASE WHEN right(s.roll_number, 3)::integer % 3 = 0 THEN 10 ELSE 5 END
FROM quiz q
JOIN student_course_teacher sct ON sct.course_id = q.course_id AND sct.teacher_id = q.teacher_id
JOIN student s ON s.id = sct.student_id
WHERE q.title IN ('Data Structures Quick Check', 'Operating Systems Quiz', 'Networking Basics Quiz', 'Machine Learning Basics Quiz', 'DSP Fundamentals Quiz')
  AND right(s.roll_number, 3)::integer BETWEEN 3 AND 32
ON CONFLICT (quiz_id, student_id) DO NOTHING;

INSERT INTO quiz_answer (submission_id, question_id, selected_option_index, answer_text, marks_awarded)
SELECT qs.id, qq.id, qq.correct_option_index, NULL, 5
FROM quiz_submission qs
JOIN quiz_question qq ON qq.quiz_id = qs.quiz_id
JOIN quiz q ON q.id = qs.quiz_id
WHERE q.title IN ('Data Structures Quick Check', 'Operating Systems Quiz', 'Networking Basics Quiz', 'Machine Learning Basics Quiz', 'DSP Fundamentals Quiz')
ON CONFLICT (submission_id, question_id) DO NOTHING;

-- 40. extra feedback
INSERT INTO feedback_form (course_id, teacher_id, title, description, is_active)
SELECT c.id, t.id, v.title, 'Share course feedback for continuous improvement.', 1
FROM (VALUES
    ('CS-301', 'TCH005', 'Data Structures Feedback'),
    ('CS-501', 'TCH006', 'Operating Systems Feedback'),
    ('IT-401', 'TCH004', 'Computer Networks Feedback'),
    ('AI-501', 'TCH003', 'Machine Learning Feedback'),
    ('EC-401', 'TCH004', 'DSP Feedback')
) AS v(course_code, teacher_clg_id, title)
JOIN course c ON c.course_code = v.course_code
JOIN teacher t ON t.teacher_clg_id = v.teacher_clg_id
WHERE NOT EXISTS (
    SELECT 1 FROM feedback_form f
    WHERE f.course_id = c.id AND f.teacher_id = t.id AND f.title = v.title
);

INSERT INTO feedback_question (form_id, question_order, question_text, question_type, options_text, rating_max, required)
SELECT f.id, v.question_order, v.question_text, v.question_type, v.options_text, v.rating_max, v.required
FROM feedback_form f
JOIN LATERAL (
    VALUES
        (1, 'Rate the clarity of teaching', 'RATING', NULL::text, 5, 1),
        (2, 'Rate the usefulness of course materials', 'RATING', NULL::text, 5, 1),
        (3, 'What should be improved?', 'TEXT', NULL::text, NULL::integer, 0)
) AS v(question_order, question_text, question_type, options_text, rating_max, required) ON true
WHERE f.title IN ('Data Structures Feedback', 'Operating Systems Feedback', 'Computer Networks Feedback', 'Machine Learning Feedback', 'DSP Feedback')
  AND NOT EXISTS (
      SELECT 1 FROM feedback_question fq
      WHERE fq.form_id = f.id AND fq.question_order = v.question_order
  );

INSERT INTO feedback_submission (form_id, student_id)
SELECT f.id, s.id
FROM feedback_form f
JOIN student_course_teacher sct ON sct.course_id = f.course_id AND sct.teacher_id = f.teacher_id
JOIN student s ON s.id = sct.student_id
WHERE f.title IN ('Data Structures Feedback', 'Operating Systems Feedback', 'Computer Networks Feedback', 'Machine Learning Feedback', 'DSP Feedback')
  AND right(s.roll_number, 3)::integer BETWEEN 3 AND 42
ON CONFLICT (form_id, student_id) DO NOTHING;

INSERT INTO feedback_answer (submission_id, question_id, answer_text, answer_number)
SELECT fs.id, fq.id,
       CASE WHEN fq.question_type = 'TEXT' THEN 'More practical examples would help.' ELSE NULL END,
       CASE WHEN fq.question_type = 'RATING' THEN (4 + (fs.student_id % 2))::integer ELSE NULL END
FROM feedback_submission fs
JOIN feedback_question fq ON fq.form_id = fs.form_id
JOIN feedback_form f ON f.id = fs.form_id
WHERE f.title IN ('Data Structures Feedback', 'Operating Systems Feedback', 'Computer Networks Feedback', 'Machine Learning Feedback', 'DSP Feedback')
ON CONFLICT (submission_id, question_id) DO NOTHING;
