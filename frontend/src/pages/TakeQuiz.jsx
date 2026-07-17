import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import './TakeQuiz.css';

export default function TakeQuiz() {
    const { quizId } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();
    
    const [quiz, setQuiz] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    
    const [answers, setAnswers] = useState({});
    const [submitting, setSubmitting] = useState(false);
    const [timeLeft, setTimeLeft] = useState(null);
    const [scoreResult, setScoreResult] = useState(null);

    const timerRef = useRef(null);

    useEffect(() => {
        const fetchQuizDetails = async () => {
            try {
                const response = await fetch(`/api/student/quiz/${quizId}`);
                if (response.ok) {
                    const data = await response.json();
                    setQuiz(data);
                    
                    if (!data.alreadySubmitted && data.inWindow && data.durationMinutes) {
                        setTimeLeft(data.durationMinutes * 60);
                    }
                    if (data.alreadySubmitted) {
                        setScoreResult({ score: data.score, scorePublished: data.scorePublished });
                    }
                } else {
                    const data = await response.json();
                    setError(data.message || 'Failed to fetch quiz details.');
                }
            } catch (err) {
                console.error(err);
                setError('Network error. Failed to load quiz.');
            } finally {
                setLoading(false);
            }
        };

        fetchQuizDetails();
    }, [quizId]);

    // Timer Countdown effect
    useEffect(() => {
        if (timeLeft === null) return;
        if (timeLeft <= 0) {
            clearInterval(timerRef.current);
            handleSubmitQuiz(true); // auto-submit when timer expires
            return;
        }

        timerRef.current = setInterval(() => {
            setTimeLeft(prev => prev - 1);
        }, 1000);

        return () => clearInterval(timerRef.current);
    }, [timeLeft]);

    // Format remaining time to MM:SS
    const formatTime = (seconds) => {
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return `${m < 10 ? '0' + m : m}:${s < 10 ? '0' + s : s}`;
    };

    const handleAnswerChange = (questionId, value) => {
        setAnswers(prev => ({
            ...prev,
            [`q_${questionId}`]: value
        }));
    };

    const handleSubmitQuiz = async (isAuto = false) => {
        if (submitting) return;
        if (!isAuto && !window.confirm('Are you sure you want to submit your quiz answers?')) return;

        setSubmitting(true);
        try {
            const response = await fetch(`/api/student/quiz/${quizId}/submit`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(answers)
            });

            if (response.ok) {
                const data = await response.json();
                setScoreResult({ score: data.score, scorePublished: true });
                setQuiz(prev => ({ ...prev, alreadySubmitted: true }));
                alert(isAuto ? 'Time limit reached! Your answers were submitted automatically.' : 'Quiz submitted successfully!');
            } else {
                const data = await response.json();
                setError(data.message || 'Submission failed.');
            }
        } catch (err) {
            console.error(err);
            setError('Submission failed. Check your network.');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <DashboardLayout role="student">
            <div className="take-quiz-page-content">
                {error && <div className="error-banner">{error}</div>}

                {loading ? (
                    <div className="loading-state">
                        <h2>Loading exam paper...</h2>
                    </div>
                ) : (
                    quiz && (
                        <div className="exam-card-wrap">
                            {/* Quiz Header & Timer */}
                            <div className="glass exam-header">
                                <div className="header-top">
                                    <div>
                                        <span className="exam-course">{quiz.courseCode} • {quiz.courseName}</span>
                                        <h1 className="exam-title">{quiz.title}</h1>
                                        <p className="exam-teacher">Instructor: <span>{quiz.teacherName}</span></p>
                                    </div>
                                    {timeLeft !== null && !quiz.alreadySubmitted && (
                                        <div className={`timer-badge ${timeLeft < 60 ? 'timer-critical' : ''}`}>
                                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                                            </svg>
                                            <span>Time Left: {formatTime(timeLeft)}</span>
                                        </div>
                                    )}
                                </div>

                                <div className="exam-instructions">
                                    <strong>Instructions:</strong> {quiz.instructions || 'Answer all questions carefully. Do not refresh this page during the exam.'}
                                </div>
                            </div>

                            {/* Score Display (If Submitted) */}
                            {quiz.alreadySubmitted ? (
                                <div className="glass score-display-card">
                                    <div className="score-glow"></div>
                                    <div className="score-inner">
                                        <span className="medal">🏆</span>
                                        <h2>Exam Submitted!</h2>
                                        {scoreResult?.scorePublished ? (
                                            <div className="score-value">
                                                Your Score: <span>{scoreResult.score} / {quiz.totalMarks}</span>
                                            </div>
                                        ) : (
                                            <p className="grading-msg">Your submission was recorded. Your grade will be available as soon as your professor publishes the final results.</p>
                                        )}
                                        <div style={{ marginTop: '24px' }}>
                                            <Link to={`/student-classroom/${quiz.courseCode}`} className="btn btn--accent">
                                                Back to Course
                                            </Link>
                                        </div>
                                    </div>
                                </div>
                            ) : (
                                /* Active Question Sheets */
                                <div className="questions-sheet">
                                    {quiz.questions && quiz.questions.length > 0 ? (
                                        <>
                                            {quiz.questions.map((q, idx) => (
                                                <div key={q.id} className="glass question-card">
                                                    <div className="q-head">
                                                        <span className="q-number">Question {idx + 1}</span>
                                                        <span className="q-marks">{q.marks} {q.marks === 1 ? 'Mark' : 'Marks'}</span>
                                                    </div>
                                                    <p className="q-text">{q.text}</p>

                                                    {/* MCQ options selection */}
                                                    {q.type === 'MCQ' ? (
                                                        <div className="options-grid">
                                                            {q.options?.map((opt, optIdx) => {
                                                                const isSelected = answers[`q_${q.id}`] === String(optIdx);
                                                                return (
                                                                    <label key={optIdx} className={`option-label ${isSelected ? 'selected' : ''}`}>
                                                                        <input
                                                                            type="radio"
                                                                            name={`q_${q.id}`}
                                                                            value={optIdx}
                                                                            checked={isSelected}
                                                                            onChange={() => handleAnswerChange(q.id, String(optIdx))}
                                                                        />
                                                                        <span className="opt-letter">{String.fromCharCode(65 + optIdx)}</span>
                                                                        <span className="opt-text">{opt}</span>
                                                                    </label>
                                                                );
                                                            })}
                                                        </div>
                                                    ) : (
                                                        /* Subjective/Theory Answer sheet */
                                                        <div className="theory-wrap">
                                                            <textarea
                                                                className="theory-input"
                                                                placeholder="Type your explanation answer here..."
                                                                value={answers[`q_${q.id}`] || ''}
                                                                onChange={(e) => handleAnswerChange(q.id, e.target.value)}
                                                            />
                                                        </div>
                                                    )}
                                                </div>
                                            ))}

                                            <div className="sheet-actions">
                                                <button
                                                    onClick={() => handleSubmitQuiz(false)}
                                                    className="btn btn--accent btn-submit-exam"
                                                    disabled={submitting}
                                                >
                                                    {submitting ? 'Submitting Answers...' : 'Submit Exam Paper'}
                                                </button>
                                            </div>
                                        </>
                                    ) : (
                                        <div className="glass empty-questions-card">
                                            <p>No questions found in this quiz paper.</p>
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    )
                )}
            </div>
        </DashboardLayout>
    );
}
