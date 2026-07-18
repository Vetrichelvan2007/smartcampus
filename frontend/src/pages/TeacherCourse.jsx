import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import './TeacherCourse.css';

// ── Premium LiquidButton with ripple effect ──
function LiquidButton({ children, className = '', onClick, type = 'button', disabled = false, style = {} }) {
    const handleClick = (e) => {
        const btn = e.currentTarget;
        const old = btn.querySelector('.ripple');
        if (old) old.remove();
        const circle = document.createElement('span');
        const diameter = Math.max(btn.clientWidth, btn.clientHeight);
        const radius = diameter / 2;
        const rect = btn.getBoundingClientRect();
        circle.style.width = circle.style.height = `${diameter}px`;
        circle.style.left = `${e.clientX - rect.left - radius}px`;
        circle.style.top = `${e.clientY - rect.top - radius}px`;
        circle.classList.add('ripple');
        btn.appendChild(circle);
        if (onClick) onClick(e);
    };
    return (
        <button
            type={type}
            disabled={disabled}
            className={`liquid-btn ${className}`}
            onClick={handleClick}
            style={style}
        >
            {children}
        </button>
    );
}


export default function TeacherCourse() {
    const { courseId } = useParams();
    const { user } = useAuth();
    
    const [course, setCourse] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [successMsg, setSuccessMsg] = useState('');
    const [activeTab, setActiveTab] = useState('materials');

    // Forms triggers / modals
    const [showUploadModal, setShowUploadModal] = useState(false);
    const [showAssignModal, setShowAssignModal] = useState(false);
    const [showQuizModal, setShowQuizModal] = useState(false);

    // Active sub-selection (Submissions Grading)
    const [selectedAssignmentId, setSelectedAssignmentId] = useState(null);
    const [submissions, setSubmissions] = useState([]);
    const [loadingSubmissions, setLoadingSubmissions] = useState(false);

    // Active sub-selection (Quiz results/submissions)
    const [selectedQuizId, setSelectedQuizId] = useState(null);
    const [quizSubmissions, setQuizSubmissions] = useState([]);
    const [loadingQuizSubmissions, setLoadingQuizSubmissions] = useState(false);

    // Student Answers state
    const [showAnswersModal, setShowAnswersModal] = useState(false);
    const [selectedStudentAnswers, setSelectedStudentAnswers] = useState(null);

    // Edit Quiz state
    const [showEditQuizModal, setShowEditQuizModal] = useState(false);
    const [editQuizTitle, setEditQuizTitle] = useState('');
    const [editQuizInstructions, setEditQuizInstructions] = useState('');
    const [editQuizDuration, setEditQuizDuration] = useState(30);
    const [editQuizStart, setEditQuizStart] = useState('');
    const [editQuizEnd, setEditQuizEnd] = useState('');
    const [gradingSubId, setGradingSubId] = useState(null);
    const [gradingMarks, setGradingMarks] = useState('');
    const [gradingFeedback, setGradingFeedback] = useState('');

    // Students list state
    const [studentsList, setStudentsList] = useState([]);
    const [loadingStudents, setLoadingStudents] = useState(false);

    // Feedback Surveys state
    const [feedbacks, setFeedbacks] = useState([]);
    const [loadingFeedbacks, setLoadingFeedbacks] = useState(false);
    const [selectedFeedbackId, setSelectedFeedbackId] = useState(null);
    const [feedbackSubmissions, setFeedbackSubmissions] = useState(null);
    const [loadingFeedbackSubmissions, setLoadingFeedbackSubmissions] = useState(false);

    // Create Feedback Survey state
    const [showFeedbackModal, setShowFeedbackModal] = useState(false);
    const [feedbackTitle, setFeedbackTitle] = useState('');
    const [feedbackDesc, setFeedbackDesc] = useState('');
    const [feedbackQuestions, setFeedbackQuestions] = useState([{ text: '', type: 'RATING', ratingMax: 5 }]);

    // View Student Feedback answers
    const [showFeedbackAnswersModal, setShowFeedbackAnswersModal] = useState(false);
    const [selectedStudentFeedbackAnswers, setSelectedStudentFeedbackAnswers] = useState(null);

    // Upload Material state
    const [matTitle, setMatTitle] = useState('');
    const [matType, setMatType] = useState('PDF');
    const [matModule, setMatModule] = useState('Module 1');

    // Create Assignment state
    const [assTitle, setAssTitle] = useState('');
    const [assDesc, setAssDesc] = useState('');
    const [assMaxMarks, setAssMaxMarks] = useState(100);
    const [assDueAt, setAssDueAt] = useState('');

    // Create Quiz state
    const [quizTitle, setQuizTitle] = useState('');
    const [quizInstructions, setQuizInstructions] = useState('');
    const [quizDuration, setQuizDuration] = useState(30);
    const [quizStart, setQuizStart] = useState('');
    const [quizEnd, setQuizEnd] = useState('');
    const [quizQuestions, setQuizQuestions] = useState([
        { text: '', type: 'MCQ', marks: 1, options: ['', '', '', ''], correctOptionIndex: 1 }
    ]);

    const fetchCourseDetails = async () => {
        try {
            const response = await fetch(`/api/teacher/course/${courseId}`);
            if (response.ok) {
                const data = await response.json();
                setCourse(data);
            } else {
                const data = await response.json();
                setError(data.message || 'Failed to load course details.');
            }
        } catch (err) {
            console.error(err);
            setError('Failed to fetch details.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCourseDetails();
    }, [courseId]);

    const fetchCourseStudents = async () => {
        setLoadingStudents(true);
        try {
            const response = await fetch(`/api/teacher/course/${courseId}/students`);
            if (response.ok) {
                const data = await response.json();
                setStudentsList(data || []);
            } else {
                setError('Failed to load class roster.');
            }
        } catch (err) {
            console.error(err);
            setError('Failed to fetch class roster.');
        } finally {
            setLoadingStudents(false);
        }
    };

    useEffect(() => {
        if (activeTab === 'students') {
            fetchCourseStudents();
        }
    }, [activeTab, courseId]);

    const fetchCourseFeedbacks = async () => {
        setLoadingFeedbacks(true);
        try {
            const response = await fetch(`/api/teacher/course/${courseId}/feedbacks`);
            if (response.ok) {
                const data = await response.json();
                setFeedbacks(data || []);
            }
        } catch (err) {
            console.error(err);
        } finally {
            setLoadingFeedbacks(false);
        }
    };

    const handleViewFeedbackSubmissions = async (formId) => {
        setLoadingFeedbackSubmissions(true);
        setSelectedFeedbackId(formId);
        try {
            const response = await fetch(`/api/teacher/feedback/${formId}/submissions`);
            if (response.ok) {
                const data = await response.json();
                setFeedbackSubmissions(data || null);
            }
        } catch (err) {
            console.error(err);
        } finally {
            setLoadingFeedbackSubmissions(false);
        }
    };

    const handleToggleFeedbackActive = async (formId, active) => {
        try {
            const response = await fetch(`/api/teacher/feedback/${formId}/toggle`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ active })
            });
            if (response.ok) {
                setSuccessMsg(active ? 'Feedback Survey opened!' : 'Feedback Survey closed!');
                fetchCourseFeedbacks();
                handleViewFeedbackSubmissions(formId);
            }
        } catch (err) {
            console.error(err);
        }
    };

    const handleViewStudentFeedback = async (studentId) => {
        try {
            const response = await fetch(`/api/teacher/feedback/${selectedFeedbackId}/student/${studentId}/answers`);
            if (response.ok) {
                const data = await response.json();
                setSelectedStudentFeedbackAnswers(data);
                setShowFeedbackAnswersModal(true);
            } else {
                setError('Failed to load student feedback responses.');
            }
        } catch (err) {
            console.error(err);
            setError('Failed to fetch answers.');
        }
    };

    const addFeedbackQuestionNode = () => {
        setFeedbackQuestions(prev => [...prev, { text: '', type: 'RATING', ratingMax: 5 }]);
    };

    const updateFeedbackQuestionField = (idx, field, value) => {
        setFeedbackQuestions(prev => {
            const updated = [...prev];
            updated[idx][field] = value;
            return updated;
        });
    };

    const handleFeedbackSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch(`/api/teacher/course/${courseId}/feedbacks/create`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    title: feedbackTitle,
                    description: feedbackDesc,
                    questions: feedbackQuestions
                })
            });

            if (response.ok) {
                setSuccessMsg('Feedback survey generated successfully!');
                setShowFeedbackModal(false);
                setFeedbackTitle('');
                setFeedbackDesc('');
                setFeedbackQuestions([{ text: '', type: 'RATING', ratingMax: 5 }]);
                fetchCourseFeedbacks();
            } else {
                const data = await response.json();
                setError(data.message || 'Failed to create feedback survey.');
            }
        } catch (err) {
            console.error(err);
            setError('Connection error creating feedback survey.');
        }
    };

    useEffect(() => {
        if (activeTab === 'feedback') {
            fetchCourseFeedbacks();
        }
    }, [activeTab, courseId]);

    // Handle resource upload submit
    const handleUploadSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch(`/api/teacher/course/${courseId}/materials/upload`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title: matTitle, type: matType, module: matModule })
            });

            if (response.ok) {
                setSuccessMsg('Resource uploaded successfully!');
                setShowUploadModal(false);
                setMatTitle('');
                fetchCourseDetails();
            } else {
                const data = await response.json();
                setError(data.message || 'Failed to upload material.');
            }
        } catch (err) {
            console.error(err);
            setError('Upload connection error.');
        }
    };

    // Handle assignment create submit
    const handleAssignSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch(`/api/teacher/course/${courseId}/assignments/create`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title: assTitle, description: assDesc, maxMarks: assMaxMarks, dueAt: assDueAt })
            });

            if (response.ok) {
                setSuccessMsg('Assignment issued successfully!');
                setShowAssignModal(false);
                setAssTitle('');
                setAssDesc('');
                fetchCourseDetails();
            } else {
                const data = await response.json();
                setError(data.message || 'Failed to create assignment.');
            }
        } catch (err) {
            console.error(err);
            setError('Submission failed.');
        }
    };

    // Handle Quiz create submit
    const handleQuizSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch(`/api/teacher/course/${courseId}/quizzes/create`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    title: quizTitle,
                    instructions: quizInstructions,
                    durationMinutes: quizDuration,
                    startAt: quizStart,
                    endAt: quizEnd,
                    questions: quizQuestions
                })
            });

            if (response.ok) {
                setSuccessMsg('Quiz template generated successfully!');
                setShowQuizModal(false);
                setQuizTitle('');
                setQuizInstructions('');
                setQuizQuestions([{ text: '', type: 'MCQ', marks: 1, options: ['', '', '', ''], correctOptionIndex: 1 }]);
                fetchCourseDetails();
            } else {
                const data = await response.json();
                setError(data.message || 'Failed to create quiz.');
            }
        } catch (err) {
            console.error(err);
            setError('Quiz creation connection error.');
        }
    };

    // Load Submissions list
    const handleViewSubmissions = async (assignmentId) => {
        setLoadingSubmissions(true);
        setSelectedAssignmentId(assignmentId);
        try {
            const response = await fetch(`/api/teacher/assignment/${assignmentId}/submissions`);
            if (response.ok) {
                const data = await response.json();
                setSubmissions(data || []);
            } else {
                setError('Failed to load submissions roster.');
            }
        } catch (err) {
            console.error(err);
            setError('Submissions load failed.');
        } finally {
            setLoadingSubmissions(false);
        }
    };

    // Grade submission
    const handleGradeSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch(`/api/teacher/assignment/grade`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ submissionId: gradingSubId, marks: gradingMarks, feedback: gradingFeedback })
            });

            if (response.ok) {
                setSuccessMsg('Graded successfully!');
                setGradingSubId(null);
                handleViewSubmissions(selectedAssignmentId);
            } else {
                const data = await response.json();
                setError(data.message || 'Grading failed.');
            }
        } catch (err) {
            console.error(err);
            setError('Connection error grading submission.');
        }
    };

    // Load Quiz Submissions (Student marks & results)
    const handleViewQuizSubmissions = async (quizId) => {
        setLoadingQuizSubmissions(true);
        setSelectedQuizId(quizId);
        try {
            const response = await fetch(`/api/teacher/quiz/${quizId}/submissions`);
            if (response.ok) {
                const data = await response.json();
                setQuizSubmissions(data || null);
            } else {
                setError('Failed to load quiz submissions roster.');
            }
        } catch (err) {
            console.error(err);
            setError('Quiz submissions load failed.');
        } finally {
            setLoadingQuizSubmissions(false);
        }
    };

    // Publish/Unpublish quiz score
    const handlePublishQuizScore = async (quizId, publish) => {
        try {
            const response = await fetch(`/api/teacher/quiz/${quizId}/publish-score`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ published: publish })
            });

            if (response.ok) {
                setSuccessMsg(publish ? 'Quiz scores published!' : 'Quiz scores unpublished!');
                fetchCourseDetails();
                handleViewQuizSubmissions(quizId);
            } else {
                const data = await response.json();
                setError(data.message || 'Failed to update score publication status.');
            }
        } catch (err) {
            console.error(err);
            setError('Connection error toggling score publication.');
        }
    };

    // Fetch student answers
    const handleViewStudentAnswers = async (studentId) => {
        try {
            const response = await fetch(`/api/teacher/quiz/${selectedQuizId}/student/${studentId}/answers`);
            if (response.ok) {
                const data = await response.json();
                setSelectedStudentAnswers(data);
                setShowAnswersModal(true);
            } else {
                setError('Failed to load student exam paper answers.');
            }
        } catch (err) {
            console.error(err);
            setError('Answers load connection error.');
        }
    };

    // Open edit quiz details modal
    const handleOpenEditQuizModal = (q) => {
        setEditQuizTitle(q.title);
        setEditQuizInstructions(q.instructions || '');
        setEditQuizDuration(q.durationMinutes);
        const formatForInput = (dtStr) => {
            if (!dtStr) return '';
            const date = new Date(dtStr);
            const pad = (num) => String(num).padStart(2, '0');
            return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
        };
        setEditQuizStart(formatForInput(q.startAt));
        setEditQuizEnd(formatForInput(q.endAt));
        setShowEditQuizModal(true);
    };

    // Submit edited quiz details
    const handleEditQuizSubmit = async (e) => {
        e.preventDefault();
        try {
            const response = await fetch(`/api/teacher/quiz/${selectedQuizId}/update`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    title: editQuizTitle,
                    instructions: editQuizInstructions,
                    durationMinutes: editQuizDuration,
                    startAt: editQuizStart,
                    endAt: editQuizEnd
                })
            });

            if (response.ok) {
                setSuccessMsg('Quiz details updated successfully!');
                setShowEditQuizModal(false);
                fetchCourseDetails();
                handleViewQuizSubmissions(selectedQuizId);
            } else {
                const data = await response.json();
                setError(data.message || 'Failed to update quiz details.');
            }
        } catch (err) {
            console.error(err);
            setError('Connection error updating quiz details.');
        }
    };

    // Questions list builders
    const addQuestionNode = () => {
        setQuizQuestions(prev => [
            ...prev,
            { text: '', type: 'MCQ', marks: 1, options: ['', '', '', ''], correctOptionIndex: 1 }
        ]);
    };

    const updateQuestionField = (idx, field, value) => {
        setQuizQuestions(prev => {
            const updated = [...prev];
            updated[idx][field] = value;
            return updated;
        });
    };

    const updateOptionField = (qIdx, optIdx, value) => {
        setQuizQuestions(prev => {
            const updated = [...prev];
            const opts = [...updated[qIdx].options];
            opts[optIdx] = value;
            updated[qIdx].options = opts;
            return updated;
        });
    };

    return (
        <DashboardLayout role="teacher">
            <div className="teacher-course-page-content">
                {error && <div className="error-banner" onClick={() => setError('')}>{error}</div>}
                {successMsg && <div className="success-banner" onClick={() => setSuccessMsg('')}>{successMsg}</div>}

                {loading ? (
                    <div className="loading-state">
                        <h2>Loading Course Details...</h2>
                    </div>
                ) : (
                    course && (
                        <>
                            {/* Course Header */}
                            <div className="glass course-header">
                                <div className="course-label">{course.courseCode} • Credits: {course.credits}</div>
                                <h1 className="course-title">{course.courseName}</h1>
                                <div className="course-meta">
                                    Course Management Dashboard for allocated instructor: <span>{user?.name}</span>
                                </div>

                                {/* Tabs Navigation */}
                                <div className="tabs">
                                    <button className={`tab ${activeTab === 'materials' ? 'active' : ''}`} onClick={() => { setActiveTab('materials'); setSelectedAssignmentId(null); setSelectedQuizId(null); setSelectedFeedbackId(null); }}>Resources</button>
                                    <button className={`tab ${activeTab === 'assignments' ? 'active' : ''}`} onClick={() => { setActiveTab('assignments'); setSelectedQuizId(null); setSelectedFeedbackId(null); }}>Assignments</button>
                                    <button className={`tab ${activeTab === 'quizzes' ? 'active' : ''}`} onClick={() => { setActiveTab('quizzes'); setSelectedAssignmentId(null); setSelectedQuizId(null); setSelectedFeedbackId(null); }}>Quizzes & Exams</button>
                                    <button className={`tab ${activeTab === 'feedback' ? 'active' : ''}`} onClick={() => { setActiveTab('feedback'); setSelectedAssignmentId(null); setSelectedQuizId(null); setSelectedFeedbackId(null); }}>Feedback Surveys</button>
                                    <button className={`tab ${activeTab === 'students' ? 'active' : ''}`} onClick={() => { setActiveTab('students'); setSelectedAssignmentId(null); setSelectedQuizId(null); setSelectedFeedbackId(null); }}>Students</button>
                                </div>
                            </div>

                            {/* Materials panel */}
                            {activeTab === 'materials' && (
                                <div className="glass panel">
                                    <div className="panel-top-row">
                                        <div className="section-title"><span className="dot"></span> Lecture Materials</div>
                                        <LiquidButton className="liquid-btn--accent" onClick={() => setShowUploadModal(true)}>⬆ Upload Resource</LiquidButton>
                                    </div>

                                    {course.materials?.length > 0 ? (
                                        <div className="table-wrap">
                                            <table className="table">
                                                <thead>
                                                    <tr>
                                                        <th>Title</th>
                                                        <th>Module</th>
                                                        <th>Type</th>
                                                        <th>File Size</th>
                                                        <th>Upload Date</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {course.materials.map(m => (
                                                        <tr key={m.id}>
                                                            <td>
                                                                <strong>{m.title}</strong>
                                                                <div style={{ marginTop: '4px' }}>
                                                                    <a href={`/teacher/material/${m.id}/download`} target="_blank" rel="noopener noreferrer" style={{ fontSize: '12px', color: '#ec4899', textDecoration: 'none', fontWeight: '500' }}>
                                                                        📄 Download ({m.originalFileName || 'File'})
                                                                    </a>
                                                                </div>
                                                            </td>
                                                            <td>{m.module}</td>
                                                            <td><span className="row-type">{m.type}</span></td>
                                                            <td>{m.fileSize ? `${(m.fileSize / 1024).toFixed(1)} KB` : 'N/A'}</td>
                                                            <td>{m.uploadedAt ? new Date(m.uploadedAt).toLocaleDateString() : '-'}</td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    ) : (
                                        <div className="empty"><p>No lecture resources uploaded yet.</p></div>
                                    )}
                                </div>
                            )}

                            {/* Assignments panel */}
                            {activeTab === 'assignments' && (
                                <div className="assignments-dual-panel">
                                    {/* Left Sub-panel: Assignments list */}
                                    <div className="glass panel">
                                        <div className="panel-top-row">
                                            <div className="section-title"><span className="dot"></span> Issued Assignments</div>
                                            <LiquidButton className="liquid-btn--accent" onClick={() => setShowAssignModal(true)}>＋ Create Assignment</LiquidButton>
                                        </div>

                                        {course.assignments?.length > 0 ? (
                                            <div className="assignment-rows">
                                                {course.assignments.map(a => (
                                                    <div
                                                        key={a.id}
                                                        className={`assignment-item-node ${selectedAssignmentId === a.id ? 'active' : ''}`}
                                                        onClick={() => handleViewSubmissions(a.id)}
                                                    >
                                                        <h4>{a.title}</h4>
                                                        <div className="meta-row">
                                                            <span>Max Marks: {a.maxMarks}</span>
                                                            <span>Due: {new Date(a.dueAt).toLocaleDateString()}</span>
                                                        </div>
                                                        <div className="meta-footer-line">
                                                            <span className="sub-count">{a.submissionCount} Submissions</span>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        ) : (
                                            <div className="empty"><p>No assignments created yet.</p></div>
                                        )}
                                    </div>

                                    {/* Right Sub-panel: Student submissions list */}
                                    <div className="glass panel">
                                        <div className="section-title"><span className="dot"></span> Student Submissions</div>
                                        {selectedAssignmentId ? (
                                            loadingSubmissions ? (
                                                <div className="loading-state"><h3>Loading Submissions...</h3></div>
                                            ) : (
                                                <div className="submissions-panel">
                                                    {submissions.length > 0 ? (
                                                        <div className="table-wrap">
                                                            <table className="table">
                                                                <thead>
                                                                    <tr>
                                                                        <th>Student</th>
                                                                        <th>File</th>
                                                                        <th>Marks</th>
                                                                        <th>Actions</th>
                                                                    </tr>
                                                                </thead>
                                                                <tbody>
                                                                    {submissions.map(sub => (
                                                                        <tr key={sub.submissionId}>
                                                                            <td>
                                                                                <strong>{sub.studentName}</strong>
                                                                                <small style={{ display: 'block', color: 'var(--text-muted)' }}>{sub.rollNumber}</small>
                                                                            </td>
                                                                            <td>
                                                                                <a href={`/teacher/assignment-submission/${sub.submissionId}/download`} target="_blank" rel="noopener noreferrer" className="file-download-link">
                                                                                    {sub.fileName}
                                                                                </a>
                                                                            </td>
                                                                            <td>{sub.marksAwarded !== null ? `${sub.marksAwarded} Marks` : 'Pending'}</td>
                                                                            <td>
                                                                                <LiquidButton
                                                                                    onClick={() => {
                                                                                        setGradingSubId(sub.submissionId);
                                                                                        setGradingMarks(sub.marksAwarded || '');
                                                                                        setGradingFeedback(sub.feedback || '');
                                                                                    }}
                                                                                >
                                                                                    ✏ Grade
                                                                                </LiquidButton>
                                                                            </td>
                                                                        </tr>
                                                                    ))}
                                                                </tbody>
                                                            </table>
                                                        </div>
                                                    ) : (
                                                        <div className="empty"><p>No submissions recorded for this assignment yet.</p></div>
                                                    )}
                                                </div>
                                            )
                                        ) : (
                                            <div className="empty"><p>Select an assignment on the left to review student papers.</p></div>
                                        )}
                                    </div>
                                </div>
                            )}

                            {/* Quizzes panel */}
                            {activeTab === 'quizzes' && (
                                <div className="assignments-dual-panel">
                                    {/* Left Sub-panel: Quizzes list */}
                                    <div className="glass panel">
                                        <div className="panel-top-row">
                                            <div className="section-title"><span className="dot"></span> Online Exam Sheets</div>
                                            <LiquidButton className="liquid-btn--accent" onClick={() => setShowQuizModal(true)}>＋ Create Quiz</LiquidButton>
                                        </div>

                                        {course.quizzes?.length > 0 ? (
                                            <div className="assignment-rows">
                                                {course.quizzes.map(q => (
                                                    <div
                                                        key={q.id}
                                                        className={`assignment-item-node ${selectedQuizId === q.id ? 'active' : ''}`}
                                                        onClick={() => handleViewQuizSubmissions(q.id)}
                                                    >
                                                        <h4>{q.title}</h4>
                                                        <div className="meta-row">
                                                            <span>Duration: {q.durationMinutes} min</span>
                                                            <span>Total Marks: {q.totalMarks}</span>
                                                        </div>
                                                        <div className="meta-footer-line" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                            <span className="sub-count">{q.submissionCount} Submissions</span>
                                                            <span className={`badge ${q.isPublished ? 'badge--live' : 'badge--closed'}`} style={{ fontSize: '10px', padding: '2px 8px' }}>
                                                                {q.isPublished ? 'Published' : 'Draft'}
                                                            </span>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        ) : (
                                            <div className="empty"><p>No online quizzes created yet.</p></div>
                                        )}
                                    </div>

                                    {/* Right Sub-panel: Student quiz results list */}
                                    <div className="glass panel">
                                        <div className="section-title"><span className="dot"></span> Quiz Results & Student Marks</div>
                                        {selectedQuizId ? (
                                            loadingQuizSubmissions ? (
                                                <div className="loading-state"><h3>Loading Quiz Results...</h3></div>
                                            ) : (
                                                quizSubmissions && (
                                                    <div className="submissions-panel">
                                                        <div style={{ marginBottom: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid rgba(255, 255, 255, 0.08)', paddingBottom: '12px' }}>
                                                            <div>
                                                                <h3 style={{ fontSize: '16px', color: '#f1f5f9', margin: 0 }}>{quizSubmissions.title}</h3>
                                                                <small style={{ color: 'var(--text-muted)' }}>Max Marks: {quizSubmissions.totalMarks}</small>
                                                            </div>
                                                            <div style={{ display: 'flex', gap: '10px' }}>
                                                                <LiquidButton onClick={() => handleOpenEditQuizModal(course.quizzes.find(q => q.id === selectedQuizId))}>
                                                                    📝 Edit Details
                                                                </LiquidButton>
                                                                {course.quizzes?.find(q => q.id === selectedQuizId)?.isScorePublished ? (
                                                                    <LiquidButton onClick={() => handlePublishQuizScore(selectedQuizId, false)}>
                                                                        Unpublish Scores
                                                                    </LiquidButton>
                                                                ) : (
                                                                    <LiquidButton className="liquid-btn--accent" onClick={() => handlePublishQuizScore(selectedQuizId, true)}>
                                                                        🚀 Publish Scores
                                                                    </LiquidButton>
                                                                )}
                                                            </div>
                                                        </div>

                                                        {quizSubmissions.submissions?.length > 0 ? (
                                                            <div className="table-wrap">
                                                                <table className="table">
                                                                    <thead>
                                                                        <tr>
                                                                            <th>Student</th>
                                                                            <th>Submission Status</th>
                                                                            <th>Marks / Score</th>
                                                                            <th>Actions</th>
                                                                        </tr>
                                                                    </thead>
                                                                    <tbody>
                                                                        {quizSubmissions.submissions.map(sub => (
                                                                            <tr key={sub.studentId}>
                                                                                <td>
                                                                                    <strong>{sub.studentName}</strong>
                                                                                    <small style={{ display: 'block', color: 'var(--text-muted)' }}>{sub.rollNumber}</small>
                                                                                </td>
                                                                                <td>
                                                                                    {sub.submittedAt ? (
                                                                                        <span style={{ color: '#4ade80', fontSize: '13px' }}>
                                                                                            Submitted at {new Date(sub.submittedAt).toLocaleDateString()}
                                                                                        </span>
                                                                                    ) : (
                                                                                        <span style={{ color: '#94a3b8', fontSize: '13px' }}>
                                                                                            Not Attempted
                                                                                        </span>
                                                                                    )}
                                                                                </td>
                                                                                <td>
                                                                                    <strong>
                                                                                        {sub.score !== null ? `${sub.score} / ${quizSubmissions.totalMarks}` : '-'}
                                                                                    </strong>
                                                                                </td>
                                                                                <td>
                                                                                    {sub.submittedAt && (
                                                                                        <LiquidButton className="liquid-btn--accent" style={{ padding: '5px 12px', fontSize: '12px' }} onClick={() => handleViewStudentAnswers(sub.studentId)}>
                                                                                            👁 View Answers
                                                                                        </LiquidButton>
                                                                                    )}
                                                                                </td>
                                                                            </tr>
                                                                        ))}
                                                                    </tbody>
                                                                </table>
                                                            </div>
                                                        ) : (
                                                            <div className="empty"><p>No students enrolled/eligible in this classroom.</p></div>
                                                        )}
                                                    </div>
                                                )
                                            )
                                        ) : (
                                            <div className="empty"><p>Select a quiz on the left to review student marks and grades.</p></div>
                                        )}
                                    </div>
                                </div>
                            )}

                            {/* Students list panel */}
                            {activeTab === 'students' && (
                                <div className="glass panel">
                                    <div className="panel-top-row">
                                        <div className="section-title"><span className="dot"></span> Enrolled Students Class Roster</div>
                                        <div style={{ color: 'var(--text-muted)', fontSize: '13px' }}>Total Enrolled: {studentsList.length}</div>
                                    </div>

                                    {loadingStudents ? (
                                        <div className="loading-state"><h3>Loading student list...</h3></div>
                                    ) : studentsList.length > 0 ? (
                                        <div className="table-wrap">
                                            <table className="table">
                                                <thead>
                                                    <tr>
                                                        <th>Student Name</th>
                                                        <th>Roll Number</th>
                                                        <th>Email Address</th>
                                                        <th>Department</th>
                                                        <th>Current Semester</th>
                                                        <th>Status</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {studentsList.map((s, idx) => (
                                                        <tr key={idx}>
                                                            <td><strong>{s.name}</strong></td>
                                                            <td><span style={{ fontFamily: 'monospace', color: '#ec4899', fontWeight: '500' }}>{s.rollNumber}</span></td>
                                                            <td>{s.email}</td>
                                                            <td>{s.department}</td>
                                                            <td>Semester {s.semester}</td>
                                                            <td>
                                                                <span className="row-type" style={{ background: 'rgba(34, 197, 94, 0.1)', color: '#4ade80', borderColor: 'rgba(34, 197, 94, 0.2)' }}>
                                                                    {s.status}
                                                                </span>
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    ) : (
                                        <div className="empty"><p>No students enrolled in this classroom roster.</p></div>
                                    )}
                                </div>
                            )}

                            {/* Feedback Surveys Tab Panel */}
                            {activeTab === 'feedback' && (
                                <div className="assignments-dual-panel">
                                    {/* Left Sub-panel: Feedbacks list */}
                                    <div className="glass panel">
                                        <div className="panel-top-row">
                                            <div className="section-title"><span className="dot"></span> Feedback Forms</div>
                                            <LiquidButton className="liquid-btn--accent" onClick={() => setShowFeedbackModal(true)}>＋ Assign Feedback</LiquidButton>
                                        </div>

                                        {loadingFeedbacks ? (
                                            <div className="loading-state"><h3>Loading Surveys...</h3></div>
                                        ) : feedbacks.length > 0 ? (
                                            <div className="assignment-rows">
                                                {feedbacks.map(f => (
                                                    <div
                                                        key={f.id}
                                                        className={`assignment-item-node ${selectedFeedbackId === f.id ? 'active' : ''}`}
                                                        onClick={() => handleViewFeedbackSubmissions(f.id)}
                                                    >
                                                        <h4>{f.title}</h4>
                                                        <div className="meta-row">
                                                            <span>Questions: {f.questionCount}</span>
                                                            <span>Submissions: {f.submittedCount} / {f.eligibleCount}</span>
                                                        </div>
                                                        <div className="meta-footer-line" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px' }}>
                                                            <span className={`badge ${f.isActive ? 'badge--live' : 'badge--closed'}`} style={{ fontSize: '10px', padding: '2px 8px' }}>
                                                                {f.isActive ? 'Active' : 'Closed'}
                                                            </span>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        ) : (
                                            <div className="empty"><p>No feedback surveys assigned yet.</p></div>
                                        )}
                                    </div>

                                    {/* Right Sub-panel: Submission details */}
                                    <div className="glass panel">
                                        <div className="section-title"><span className="dot"></span> Submissions & Responses</div>
                                        {selectedFeedbackId ? (
                                            loadingFeedbackSubmissions ? (
                                                <div className="loading-state"><h3>Loading Submissions...</h3></div>
                                            ) : (
                                                feedbackSubmissions && (
                                                    <div className="submissions-panel">
                                                        <div style={{ marginBottom: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid rgba(255, 255, 255, 0.08)', paddingBottom: '12px' }}>
                                                            <div>
                                                                <h3 style={{ fontSize: '16px', color: '#f1f5f9', margin: 0 }}>{feedbackSubmissions.title}</h3>
                                                                <small style={{ color: 'var(--text-muted)' }}>{feedbackSubmissions.description || 'No description'}</small>
                                                            </div>
                                                            <div>
                                                                {feedbacks.find(f => f.id === selectedFeedbackId)?.isActive ? (
                                                                    <LiquidButton onClick={() => handleToggleFeedbackActive(selectedFeedbackId, false)}>
                                                                        🔒 Close Survey
                                                                    </LiquidButton>
                                                                ) : (
                                                                    <LiquidButton className="liquid-btn--accent" onClick={() => handleToggleFeedbackActive(selectedFeedbackId, true)}>
                                                                        🔓 Open Survey
                                                                    </LiquidButton>
                                                                )}
                                                            </div>
                                                        </div>

                                                        {feedbackSubmissions.submissions?.length > 0 ? (
                                                            <div className="table-wrap">
                                                                <table className="table">
                                                                    <thead>
                                                                        <tr>
                                                                            <th>Student</th>
                                                                            <th>Department</th>
                                                                            <th>Submitted At</th>
                                                                            <th>Action</th>
                                                                        </tr>
                                                                    </thead>
                                                                    <tbody>
                                                                        {feedbackSubmissions.submissions.map(sub => (
                                                                            <tr key={sub.studentId}>
                                                                                <td>
                                                                                    <strong>{sub.studentName}</strong>
                                                                                    <small style={{ display: 'block', color: 'var(--text-muted)' }}>{sub.rollNumber}</small>
                                                                                </td>
                                                                                <td>{sub.department}</td>
                                                                                <td>
                                                                                    {sub.submittedAt ? (
                                                                                        <span style={{ color: '#4ade80', fontSize: '13px' }}>
                                                                                            {new Date(sub.submittedAt).toLocaleDateString()}
                                                                                        </span>
                                                                                    ) : (
                                                                                        <span style={{ color: '#94a3b8', fontSize: '13px' }}>
                                                                                            Pending
                                                                                        </span>
                                                                                    )}
                                                                                </td>
                                                                                <td>
                                                                                    {sub.submittedAt && (
                                                                                        <LiquidButton className="liquid-btn--accent" style={{ padding: '5px 12px', fontSize: '12px' }} onClick={() => handleViewStudentFeedback(sub.studentId)}>
                                                                                            👁 View Details
                                                                                        </LiquidButton>
                                                                                    )}
                                                                                </td>
                                                                            </tr>
                                                                        ))}
                                                                    </tbody>
                                                                </table>
                                                            </div>
                                                        ) : (
                                                            <div className="empty"><p>No students enrolled/eligible in this classroom.</p></div>
                                                        )}
                                                    </div>
                                                )
                                            )
                                        ) : (
                                            <div className="empty"><p>Select a feedback survey on the left to see results and student responses.</p></div>
                                        )}
                                    </div>
                                </div>
                            )}

                            {/* ── MODALS ── */}

                            {/* Upload Material Modal */}
                            {showUploadModal && (
                                <div className="modal-backdrop">
                                    <div className="glass modal-card">
                                        <h3>Upload Resource</h3>
                                        <form onSubmit={handleUploadSubmit} className="modal-form">
                                            <div className="modal-field">
                                                <label>Title</label>
                                                <input type="text" value={matTitle} onChange={(e) => setMatTitle(e.target.value)} required />
                                            </div>
                                            <div className="modal-field">
                                                <label>Module / Unit</label>
                                                <input type="text" value={matModule} onChange={(e) => setMatModule(e.target.value)} />
                                            </div>
                                            <div className="modal-field">
                                                <label>Type</label>
                                                <select value={matType} onChange={(e) => setMatType(e.target.value)}>
                                                    <option value="PDF">PDF Document</option>
                                                    <option value="PPT">Powerpoint Presentation</option>
                                                    <option value="TXT">Text Notes</option>
                                                </select>
                                            </div>
                                            <div className="modal-actions">
                                                <LiquidButton type="button" onClick={() => setShowUploadModal(false)}>Cancel</LiquidButton>
                                                <LiquidButton type="submit" className="liquid-btn--accent">⬆ Upload</LiquidButton>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            )}

                            {/* Create Assignment Modal */}
                            {showAssignModal && (
                                <div className="modal-backdrop">
                                    <div className="glass modal-card">
                                        <h3>Create Assignment</h3>
                                        <form onSubmit={handleAssignSubmit} className="modal-form">
                                            <div className="modal-field">
                                                <label>Title</label>
                                                <input type="text" value={assTitle} onChange={(e) => setAssTitle(e.target.value)} required />
                                            </div>
                                            <div className="modal-field">
                                                <label>Instructions Description</label>
                                                <textarea value={assDesc} onChange={(e) => setAssDesc(e.target.value)} />
                                            </div>
                                            <div className="modal-field">
                                                <label>Max Marks</label>
                                                <input type="number" value={assMaxMarks} onChange={(e) => setAssMaxMarks(parseInt(e.target.value))} required />
                                            </div>
                                            <div className="modal-field">
                                                <label>Due Date & Time</label>
                                                <input type="datetime-local" value={assDueAt} onChange={(e) => setAssDueAt(e.target.value)} required />
                                            </div>
                                            <div className="modal-actions">
                                                <LiquidButton type="button" onClick={() => setShowAssignModal(false)}>Cancel</LiquidButton>
                                                <LiquidButton type="submit" className="liquid-btn--accent">✔ Issue Assignment</LiquidButton>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            )}

                            {/* Create Quiz Modal */}
                            {showQuizModal && (
                                <div className="modal-backdrop">
                                    <div className="glass modal-card quiz-modal-card">
                                        <h3>Create Quiz Template</h3>
                                        <form onSubmit={handleQuizSubmit} className="modal-form">
                                            <div className="modal-field">
                                                <label>Quiz Title</label>
                                                <input type="text" value={quizTitle} onChange={(e) => setQuizTitle(e.target.value)} required />
                                            </div>
                                            <div className="modal-field">
                                                <label>Instructions</label>
                                                <textarea value={quizInstructions} onChange={(e) => setQuizInstructions(e.target.value)} />
                                            </div>
                                            <div className="modal-grid-2">
                                                <div className="modal-field">
                                                    <label>Duration (Minutes)</label>
                                                    <input type="number" value={quizDuration} onChange={(e) => setQuizDuration(parseInt(e.target.value))} required />
                                                </div>
                                            </div>
                                            <div className="modal-grid-2">
                                                <div className="modal-field">
                                                    <label>Start Window</label>
                                                    <input type="datetime-local" value={quizStart} onChange={(e) => setQuizStart(e.target.value)} required />
                                                </div>
                                                <div className="modal-field">
                                                    <label>End Window</label>
                                                    <input type="datetime-local" value={quizEnd} onChange={(e) => setQuizEnd(e.target.value)} required />
                                                </div>
                                            </div>

                                            <div className="questions-section">
                                                <div className="section-header">
                                                    <h4>Questions List</h4>
                                                    <LiquidButton type="button" className="liquid-btn--accent" onClick={addQuestionNode} style={{ padding: '5px 14px', fontSize: '12px' }}>＋ Add Question</LiquidButton>
                                                </div>

                                                {quizQuestions.map((q, idx) => (
                                                    <div key={idx} className="quiz-question-builder-node">
                                                        <div className="node-head">
                                                            <span>Question #{idx + 1}</span>
                                                        </div>
                                                        <div className="modal-field">
                                                            <label>Question Text</label>
                                                            <input type="text" value={q.text} onChange={(e) => updateQuestionField(idx, 'text', e.target.value)} required />
                                                        </div>
                                                        <div className="modal-grid-2">
                                                            <div className="modal-field">
                                                                <label>Type</label>
                                                                <select value={q.type} onChange={(e) => updateQuestionField(idx, 'type', e.target.value)}>
                                                                    <option value="MCQ">Multiple Choice (MCQ)</option>
                                                                    <option value="DESCRIPTIVE">Descriptive / Theory</option>
                                                                </select>
                                                            </div>
                                                            <div className="modal-field">
                                                                <label>Marks</label>
                                                                <input type="number" value={q.marks} onChange={(e) => updateQuestionField(idx, 'marks', parseInt(e.target.value))} required />
                                                            </div>
                                                        </div>

                                                        {q.type === 'MCQ' && (
                                                            <div className="mcq-options-builder">
                                                                <label>MCQ Options (4 allocated):</label>
                                                                {q.options.map((opt, optIdx) => (
                                                                    <div key={optIdx} className="option-row">
                                                                        <span className="opt-lbl">{String.fromCharCode(65 + optIdx)}</span>
                                                                        <input type="text" value={opt} onChange={(e) => updateOptionField(idx, optIdx, e.target.value)} placeholder={`Option ${optIdx + 1}`} required />
                                                                    </div>
                                                                ))}
                                                                <div className="modal-field" style={{ marginTop: '10px' }}>
                                                                    <label>Correct Answer Index</label>
                                                                    <select value={q.correctOptionIndex} onChange={(e) => updateQuestionField(idx, 'correctOptionIndex', parseInt(e.target.value))}>
                                                                        <option value={1}>Option A</option>
                                                                        <option value={2}>Option B</option>
                                                                        <option value={3}>Option C</option>
                                                                        <option value={4}>Option D</option>
                                                                    </select>
                                                                </div>
                                                            </div>
                                                        )}
                                                    </div>
                                                ))}
                                            </div>

                                            <div className="modal-actions">
                                                <LiquidButton type="button" onClick={() => setShowQuizModal(false)}>Cancel</LiquidButton>
                                                <LiquidButton type="submit" className="liquid-btn--accent">🚀 Generate Quiz</LiquidButton>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            )}

                            {/* Grading Submission Modal */}
                            {gradingSubId && (
                                <div className="modal-backdrop">
                                    <div className="glass modal-card">
                                        <h3>Grade Student Paper</h3>
                                        <form onSubmit={handleGradeSubmit} className="modal-form">
                                            <div className="modal-field">
                                                <label>Award Marks</label>
                                                <input type="number" value={gradingMarks} onChange={(e) => setGradingMarks(e.target.value)} required />
                                            </div>
                                            <div className="modal-field">
                                                <label>Feedback Notes</label>
                                                <textarea value={gradingFeedback} onChange={(e) => setGradingFeedback(e.target.value)} />
                                            </div>
                                            <div className="modal-actions">
                                                <LiquidButton type="button" onClick={() => setGradingSubId(null)}>Cancel</LiquidButton>
                                                <LiquidButton type="submit" className="liquid-btn--accent">✔ Save Grade</LiquidButton>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            )}

                            {/* Student Quiz Answers Modal */}
                            {showAnswersModal && selectedStudentAnswers && (
                                <div className="modal-backdrop">
                                    <div className="glass modal-card quiz-modal-card">
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', borderBottom: '1px solid rgba(255, 255, 255, 0.08)', paddingBottom: '12px' }}>
                                            <div>
                                                <h3 style={{ margin: 0, fontFamily: 'Orbitron', fontSize: '18px' }}>Student Exam Paper</h3>
                                                <p style={{ margin: '4px 0 0', color: 'var(--text-muted)', fontSize: '13px' }}>
                                                    {selectedStudentAnswers.studentName} ({selectedStudentAnswers.rollNumber})
                                                </p>
                                            </div>
                                            <div style={{ textAlign: 'right' }}>
                                                <span style={{ fontSize: '18px', fontWeight: '700', color: '#ec4899' }}>
                                                    Score: {selectedStudentAnswers.score} / {selectedStudentAnswers.totalMarks}
                                                </span>
                                            </div>
                                        </div>

                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', maxHeight: '60vh', overflowY: 'auto', paddingRight: '6px' }}>
                                            {selectedStudentAnswers.answers?.map((ans, idx) => (
                                                <div key={idx} style={{ padding: '16px', background: 'rgba(2, 6, 23, 0.35)', border: '1px solid rgba(255, 255, 255, 0.05)', borderRadius: '12px' }}>
                                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                                                        <strong style={{ color: '#ec4899' }}>Question {ans.order} ({ans.marks} Marks)</strong>
                                                        <span className="row-type" style={{ fontSize: '11px', padding: '2px 8px' }}>{ans.type}</span>
                                                    </div>
                                                    <p style={{ margin: '0 0 14px', fontSize: '14px', color: '#cbd5e1' }}>{ans.text}</p>

                                                    {ans.type === 'MCQ' ? (
                                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                                            {ans.options?.map((opt, optIdx) => {
                                                                const num = optIdx + 1;
                                                                const isSelected = ans.selectedOptionIndex === num;
                                                                const isCorrect = ans.correctOptionIndex === num;

                                                                let border = '1px solid rgba(255, 255, 255, 0.08)';
                                                                let bg = 'rgba(2, 6, 23, 0.2)';
                                                                let color = '#cbd5e1';

                                                                if (isCorrect) {
                                                                    border = '1px solid #22c55e';
                                                                    bg = 'rgba(34, 197, 94, 0.1)';
                                                                    color = '#4ade80';
                                                                } else if (isSelected) {
                                                                    border = '1px solid #ef4444';
                                                                    bg = 'rgba(239, 68, 68, 0.1)';
                                                                    color = '#f87171';
                                                                }

                                                                return (
                                                                    <div key={optIdx} style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '10px 14px', borderRadius: '8px', border, background, color, fontSize: '13px' }}>
                                                                        <strong style={{ fontFamily: 'Orbitron' }}>{String.fromCharCode(65 + optIdx)}.</strong>
                                                                        <span>{opt}</span>
                                                                        {isSelected && <span style={{ marginLeft: 'auto', fontSize: '11px', fontWeight: '600', textTransform: 'uppercase' }}>Selected</span>}
                                                                        {isCorrect && !isSelected && <span style={{ marginLeft: 'auto', fontSize: '11px', fontWeight: '600', textTransform: 'uppercase' }}>Correct Answer</span>}
                                                                    </div>
                                                                );
                                                            })}
                                                        </div>
                                                    ) : (
                                                        <div style={{ background: 'rgba(2, 6, 23, 0.5)', padding: '12px 16px', borderRadius: '8px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
                                                            <label style={{ fontSize: '11px', textTransform: 'uppercase', color: '#94a3b8', display: 'block', marginBottom: '6px' }}>Student's Answer:</label>
                                                            <p style={{ margin: 0, fontSize: '13.5px', whiteSpace: 'pre-wrap', color: '#cbd5e1' }}>{ans.answerText || '(Empty Answer)'}</p>
                                                        </div>
                                                    )}
                                                </div>
                                            ))}
                                        </div>

                                        <div className="modal-actions" style={{ marginTop: '20px' }}>
                                            <LiquidButton type="button" className="liquid-btn--accent" onClick={() => setShowAnswersModal(false)}>Close Paper</LiquidButton>
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* Edit Quiz Details Modal */}
                            {showEditQuizModal && (
                                <div className="modal-backdrop">
                                    <div className="glass modal-card">
                                        <h3>Edit Quiz Details</h3>
                                        <form onSubmit={handleEditQuizSubmit} className="modal-form">
                                            <div className="modal-field">
                                                <label>Quiz Title</label>
                                                <input type="text" value={editQuizTitle} onChange={(e) => setEditQuizTitle(e.target.value)} required />
                                            </div>
                                            <div className="modal-field">
                                                <label>Instructions</label>
                                                <textarea value={editQuizInstructions} onChange={(e) => setEditQuizInstructions(e.target.value)} />
                                            </div>
                                            <div className="modal-grid-2">
                                                <div className="modal-field">
                                                    <label>Duration (Minutes)</label>
                                                    <input type="number" value={editQuizDuration} onChange={(e) => setEditQuizDuration(parseInt(e.target.value))} required />
                                                </div>
                                            </div>
                                            <div className="modal-grid-2">
                                                <div className="modal-field">
                                                    <label>Start Window</label>
                                                    <input type="datetime-local" value={editQuizStart} onChange={(e) => setEditQuizStart(e.target.value)} required />
                                                </div>
                                                <div className="modal-field">
                                                    <label>End Window</label>
                                                    <input type="datetime-local" value={editQuizEnd} onChange={(e) => setEditQuizEnd(e.target.value)} required />
                                                </div>
                                            </div>
                                            <div className="modal-actions">
                                                <LiquidButton type="button" onClick={() => setShowEditQuizModal(false)}>Cancel</LiquidButton>
                                                <LiquidButton type="submit" className="liquid-btn--accent">✔ Save Changes</LiquidButton>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            )}

                            {/* Create Feedback Survey Modal */}
                            {showFeedbackModal && (
                                <div className="modal-backdrop">
                                    <div className="glass modal-card quiz-modal-card">
                                        <h3>Assign Feedback Form</h3>
                                        <form onSubmit={handleFeedbackSubmit} className="modal-form">
                                            <div className="modal-field">
                                                <label>Survey Title</label>
                                                <input type="text" value={feedbackTitle} onChange={(e) => setFeedbackTitle(e.target.value)} required />
                                            </div>
                                            <div className="modal-field">
                                                <label>Description / Subtitle</label>
                                                <textarea value={feedbackDesc} onChange={(e) => setFeedbackDesc(e.target.value)} />
                                            </div>

                                            <div className="questions-section">
                                                <div className="section-header">
                                                    <h4>Questions List</h4>
                                                    <LiquidButton type="button" className="liquid-btn--accent" onClick={addFeedbackQuestionNode} style={{ padding: '5px 14px', fontSize: '12px' }}>＋ Add Question</LiquidButton>
                                                </div>

                                                {feedbackQuestions.map((q, idx) => (
                                                    <div key={idx} className="quiz-question-builder-node">
                                                        <div className="node-head">
                                                            <span>Question #{idx + 1}</span>
                                                        </div>
                                                        <div className="modal-field">
                                                            <label>Question Text</label>
                                                            <input type="text" value={q.text} onChange={(e) => updateFeedbackQuestionField(idx, 'text', e.target.value)} required />
                                                        </div>
                                                        <div className="modal-grid-2">
                                                            <div className="modal-field">
                                                                <label>Type</label>
                                                                <select value={q.type} onChange={(e) => updateFeedbackQuestionField(idx, 'type', e.target.value)}>
                                                                    <option value="RATING">Rating Slider (1 - Max)</option>
                                                                    <option value="TEXT">Descriptive Comments</option>
                                                                </select>
                                                            </div>
                                                            {q.type === 'RATING' && (
                                                                <div className="modal-field">
                                                                    <label>Max Rating Value</label>
                                                                    <input type="number" value={q.ratingMax} onChange={(e) => updateFeedbackQuestionField(idx, 'ratingMax', parseInt(e.target.value))} required />
                                                                </div>
                                                            )}
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>

                                            <div className="modal-actions">
                                                <LiquidButton type="button" onClick={() => setShowFeedbackModal(false)}>Cancel</LiquidButton>
                                                <LiquidButton type="submit" className="liquid-btn--accent">🚀 Generate Survey</LiquidButton>
                                            </div>
                                        </form>
                                    </div>
                                </div>
                            )}

                            {/* View Student Feedback Answers Modal */}
                            {showFeedbackAnswersModal && selectedStudentFeedbackAnswers && (
                                <div className="modal-backdrop">
                                    <div className="glass modal-card quiz-modal-card">
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', borderBottom: '1px solid rgba(255, 255, 255, 0.08)', paddingBottom: '12px' }}>
                                            <div>
                                                <h3 style={{ margin: 0, fontFamily: 'Orbitron', fontSize: '18px' }}>Course Feedback</h3>
                                                <p style={{ margin: '4px 0 0', color: 'var(--text-muted)', fontSize: '13px' }}>
                                                    Submitted by {selectedStudentFeedbackAnswers.studentName} ({selectedStudentFeedbackAnswers.rollNumber})
                                                </p>
                                            </div>
                                        </div>

                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', maxHeight: '60vh', overflowY: 'auto', paddingRight: '6px' }}>
                                            {selectedStudentFeedbackAnswers.answers?.map((ans, idx) => (
                                                <div key={idx} style={{ padding: '16px', background: 'rgba(2, 6, 23, 0.35)', border: '1px solid rgba(255, 255, 255, 0.05)', borderRadius: '12px' }}>
                                                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                                                        <strong style={{ color: '#ec4899' }}>Question {ans.order}</strong>
                                                        <span className="row-type" style={{ fontSize: '11px', padding: '2px 8px' }}>{ans.type}</span>
                                                    </div>
                                                    <p style={{ margin: '0 0 14px', fontSize: '14px', color: '#cbd5e1' }}>{ans.text}</p>

                                                    {ans.type === 'RATING' ? (
                                                        <div style={{ background: 'rgba(2, 6, 23, 0.5)', padding: '12px 16px', borderRadius: '8px', border: '1px solid rgba(255, 255, 255, 0.08)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                            <span style={{ fontSize: '13px', color: '#94a3b8' }}>Student Rating Score:</span>
                                                            <strong style={{ fontSize: '16px', color: '#4ade80' }}>{ans.answerNumber} / {ans.ratingMax}</strong>
                                                        </div>
                                                    ) : (
                                                        <div style={{ background: 'rgba(2, 6, 23, 0.5)', padding: '12px 16px', borderRadius: '8px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
                                                            <label style={{ fontSize: '11px', textTransform: 'uppercase', color: '#94a3b8', display: 'block', marginBottom: '6px' }}>Student Response Comments:</label>
                                                            <p style={{ margin: 0, fontSize: '13.5px', whiteSpace: 'pre-wrap', color: '#cbd5e1' }}>{ans.answerText || '(Empty Response)'}</p>
                                                        </div>
                                                    )}
                                                </div>
                                            ))}
                                        </div>

                                        <div className="modal-actions" style={{ marginTop: '20px' }}>
                                            <LiquidButton type="button" className="liquid-btn--accent" onClick={() => setShowFeedbackAnswersModal(false)}>Close Answers</LiquidButton>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </>
                    )
                )}
            </div>
        </DashboardLayout>
    );
}
