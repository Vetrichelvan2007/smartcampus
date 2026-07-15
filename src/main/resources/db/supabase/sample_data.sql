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
INSERT INTO batch (dept_id, batch_year, batch_name) VALUES
((SELECT id FROM department WHERE dept_name = 'Computer Science & Engineering'), '2023-06-01', '2023-2027 CSE'),
((SELECT id FROM department WHERE dept_name = 'Information Technology'), '2023-06-01', '2023-2027 IT');

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
('STU001', 'John Doe', '2005-05-15', 'MALE', '123 Main St', (SELECT id FROM department WHERE dept_name = 'Computer Science & Engineering'), (SELECT id FROM batch WHERE batch_name = '2023-2027 CSE'), 2023, 2, 4, 'student1@smartcampus.local', '9876543210', 'O+', 'English', 'American'),
('STU002', 'Jane Smith', '2005-08-20', 'FEMALE', '456 Oak Ave', (SELECT id FROM department WHERE dept_name = 'Computer Science & Engineering'), (SELECT id FROM batch WHERE batch_name = '2023-2027 CSE'), 2023, 2, 4, 'student2@smartcampus.local', '9876543211', 'A+', 'English', 'American')
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
INSERT INTO teacher_qualification_details (teacher_id, ug_degree, pg_degree, phd_status, specialization, university_name, year_of_passing) VALUES
((SELECT id FROM teacher WHERE teacher_clg_id = 'TCH001'), 'B.Tech CSE', 'M.Tech CSE', 'COMPLETED', 'Theoretical Computer Science', 'Princeton University', 2005),
((SELECT id FROM teacher WHERE teacher_clg_id = 'TCH002'), 'B.Sc Math', 'M.Sc CS', 'COMPLETED', 'Programming Languages & Compilers', 'Yale University', 2007)
ON CONFLICT (id) DO NOTHING;

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
INSERT INTO course_material (course_id, teacher_id, title, module, material_type, description, original_filename, stored_filename, stored_path, mime_type, file_size, download_allowed) VALUES
((SELECT id FROM course WHERE course_code = 'CS-401'), (SELECT id FROM teacher WHERE teacher_clg_id = 'TCH001'), 'Introduction to Web Programming', 'Module 1', 'PDF', 'Basics of HTML, CSS, and JS', 'lecture1.pdf', 'stored_lecture1.pdf', '/materials/stored_lecture1.pdf', 'application/pdf', 102456, 1);

-- 19. course_assignment
INSERT INTO course_assignment (course_id, teacher_id, title, assignment_mode, question_text, instructions, original_filename, stored_filename, stored_path, mime_type, file_size, due_at, max_marks, download_allowed, is_published) VALUES
((SELECT id FROM course WHERE course_code = 'CS-401'), (SELECT id FROM teacher WHERE teacher_clg_id = 'TCH001'), 'HTML/CSS Portfolio', 'FILE', 'Build a responsive personal portfolio using semantic HTML and custom CSS.', 'Upload zip containing files.', 'instructions.pdf', 'stored_instructions.pdf', '/assignments/stored_instructions.pdf', 'application/pdf', 54321, CURRENT_TIMESTAMP + INTERVAL '14 days', 50, 1, 1);

-- 20. course_assignment_submission
INSERT INTO course_assignment_submission (assignment_id, student_id, original_filename, stored_filename, stored_path, mime_type, file_size) VALUES
((SELECT id FROM course_assignment WHERE title = 'HTML/CSS Portfolio'), (SELECT id FROM student WHERE roll_number = 'STU001'), 'john_doe_portfolio.zip', 'stored_john_doe_portfolio.zip', '/submissions/stored_john_doe_portfolio.zip', 'application/zip', 987654)
ON CONFLICT (assignment_id, student_id) DO NOTHING;

-- 21. quiz
INSERT INTO quiz (course_id, teacher_id, title, instructions, total_marks, duration_minutes, start_at, end_at, is_published, is_score_published) VALUES
((SELECT id FROM course WHERE course_code = 'CS-401'), (SELECT id FROM teacher WHERE teacher_clg_id = 'TCH001'), 'HTML Basics Quiz', 'Answer all questions. 10 minutes limit.', 10, 10, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '1 day', 1, 1);

-- 22. quiz_question
INSERT INTO quiz_question (quiz_id, question_order, question_text, question_type, marks, correct_option_index) VALUES
((SELECT id FROM quiz WHERE title = 'HTML Basics Quiz'), 1, 'What does HTML stand for?', 'MCQ', 5, 1),
((SELECT id FROM quiz WHERE title = 'HTML Basics Quiz'), 2, 'Explain the difference between block and inline elements.', 'DESCRIPTIVE', 5, NULL);

-- 23. quiz_option
INSERT INTO quiz_option (question_id, option_index, option_text) VALUES
((SELECT id FROM quiz_question WHERE question_text = 'What does HTML stand for?'), 1, 'HyperText Markup Language'),
((SELECT id FROM quiz_question WHERE question_text = 'What does HTML stand for?'), 2, 'Hyperlinks and Text Markup Language'),
((SELECT id FROM quiz_question WHERE question_text = 'What does HTML stand for?'), 3, 'Home Tool Markup Language'),
((SELECT id FROM quiz_question WHERE question_text = 'What does HTML stand for?'), 4, 'HyperText Machine Language')
ON CONFLICT (question_id, option_index) DO NOTHING;

-- 24. quiz_submission
INSERT INTO quiz_submission (quiz_id, student_id, score) VALUES
((SELECT id FROM quiz WHERE title = 'HTML Basics Quiz'), (SELECT id FROM student WHERE roll_number = 'STU001'), 5)
ON CONFLICT (quiz_id, student_id) DO NOTHING;

-- 25. quiz_answer
INSERT INTO quiz_answer (submission_id, question_id, selected_option_index, answer_text, marks_awarded) VALUES
((SELECT id FROM quiz_submission WHERE quiz_id = (SELECT id FROM quiz WHERE title = 'HTML Basics Quiz') AND student_id = (SELECT id FROM student WHERE roll_number = 'STU001')), (SELECT id FROM quiz_question WHERE question_text = 'What does HTML stand for?'), 1, NULL, 5),
((SELECT id FROM quiz_submission WHERE quiz_id = (SELECT id FROM quiz WHERE title = 'HTML Basics Quiz') AND student_id = (SELECT id FROM student WHERE roll_number = 'STU001')), (SELECT id FROM quiz_question WHERE question_text = 'Explain the difference between block and inline elements.'), NULL, 'Block elements start on a new line and take full width. Inline elements stay in line.', NULL)
ON CONFLICT (submission_id, question_id) DO NOTHING;

-- 26. feedback_form
INSERT INTO feedback_form (course_id, teacher_id, title, description, is_active) VALUES
((SELECT id FROM course WHERE course_code = 'CS-401'), (SELECT id FROM teacher WHERE teacher_clg_id = 'TCH001'), 'Mid-Semester Course Feedback', 'Provide honest feedback about course pace and quality.', 1);

-- 27. feedback_question
INSERT INTO feedback_question (form_id, question_order, question_text, question_type, options_text, rating_max, required) VALUES
((SELECT id FROM feedback_form WHERE title = 'Mid-Semester Course Feedback'), 1, 'Rate the teaching pace (1 to 5)', 'RATING', NULL, 5, 1),
((SELECT id FROM feedback_form WHERE title = 'Mid-Semester Course Feedback'), 2, 'Any suggestions for improvement?', 'TEXT', NULL, NULL, 0);

-- 28. feedback_submission
INSERT INTO feedback_submission (form_id, student_id) VALUES
((SELECT id FROM feedback_form WHERE title = 'Mid-Semester Course Feedback'), (SELECT id FROM student WHERE roll_number = 'STU001'))
ON CONFLICT (form_id, student_id) DO NOTHING;

-- 29. feedback_answer
INSERT INTO feedback_answer (submission_id, question_id, answer_text, answer_number) VALUES
((SELECT id FROM feedback_submission WHERE form_id = (SELECT id FROM feedback_form WHERE title = 'Mid-Semester Course Feedback') AND student_id = (SELECT id FROM student WHERE roll_number = 'STU001')), (SELECT id FROM feedback_question WHERE question_text = 'Rate the teaching pace (1 to 5)'), NULL, 5),
((SELECT id FROM feedback_submission WHERE form_id = (SELECT id FROM feedback_form WHERE title = 'Mid-Semester Course Feedback') AND student_id = (SELECT id FROM student WHERE roll_number = 'STU001')), (SELECT id FROM feedback_question WHERE question_text = 'Any suggestions for improvement?'), 'The classes are excellent. More lab examples would be helpful.', NULL)
ON CONFLICT (submission_id, question_id) DO NOTHING;
