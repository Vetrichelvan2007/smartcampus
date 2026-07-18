import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import './Feedback.css';

export default function Feedback() {
    const { user } = useAuth();
    
    const [forms, setForms] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    
    const [activeForm, setActiveForm] = useState(null);
    const [formDetails, setFormDetails] = useState(null);
    const [loadingForm, setLoadingForm] = useState(false);
    
    const [answers, setAnswers] = useState({});
    const [submitting, setSubmitting] = useState(false);
    const [successMsg, setSuccessMsg] = useState('');

    useEffect(() => {
        const fetchFeedbackForms = async () => {
            try {
                const response = await fetch('/api/student/feedback');
                if (response.ok) {
                    const data = await response.json();
                    setForms(data || []);
                } else {
                    const data = await response.json();
                    setError(data.message || 'Failed to fetch feedback surveys.');
                }
            } catch (err) {
                console.error(err);
                setError('Network error. Failed to load feedback.');
            } finally {
                setLoading(false);
            }
        };

        fetchFeedbackForms();
    }, []);

    const handleSelectForm = async (formId) => {
        setLoadingForm(true);
        setActiveForm(formId);
        setFormDetails(null);
        setError('');
        setAnswers({});
        setSuccessMsg('');

        try {
            const response = await fetch(`/api/student/feedback/${formId}`);
            if (response.ok) {
                const data = await response.json();
                setFormDetails(data);
                
                // Initialize empty answers
                const initAnswers = {};
                data.questions?.forEach(q => {
                    initAnswers[`q_${q.id}`] = '';
                });
                setAnswers(initAnswers);
            } else {
                const data = await response.json();
                setError(data.message || 'Failed to load survey questions.');
            }
        } catch (err) {
            console.error(err);
            setError('Failed to fetch survey questions.');
        } finally {
            setLoadingForm(false);
        }
    };

    const handleAnswerChange = (questionId, value) => {
        setAnswers(prev => ({
            ...prev,
            [`q_${questionId}`]: value
        }));
    };

    const handleFeedbackSubmit = async (e) => {
        e.preventDefault();
        
        // Validate required questions
        let missingRequired = false;
        formDetails.questions?.forEach(q => {
            if (q.required && !answers[`q_${q.id}`]) {
                missingRequired = true;
            }
        });

        if (missingRequired) {
            setError('Please answer all required questions marked with *');
            return;
        }

        setSubmitting(true);
        setError('');
        try {
            const response = await fetch(`/api/student/feedback/${activeForm}/submit`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(answers)
            });

            if (response.ok) {
                setSuccessMsg('Thank you! Your feedback has been recorded anonymously.');
                
                // Refresh main forms list
                const updatedForms = forms.map(f => {
                    if (f.formId === activeForm) {
                        return { ...f, submitted: true };
                    }
                    return f;
                });
                setForms(updatedForms);
                
                setTimeout(() => {
                    setActiveForm(null);
                    setFormDetails(null);
                    setSuccessMsg('');
                }, 2000);
            } else {
                const data = await response.json();
                setError(data.message || 'Failed to submit feedback.');
            }
        } catch (err) {
            console.error(err);
            setError('Submission failed. Check your network.');
        } finally {
            setSubmitting(false);
        }
    };

    // 3D Perspective mouse tilt tracker
    const handleCardMouseMove = (e) => {
        const card = e.currentTarget;
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        card.style.setProperty('--mouse-x', `${x}px`);
        card.style.setProperty('--mouse-y', `${y}px`);

        const centerX = rect.width / 2;
        const centerY = rect.height / 2;
        const rotateX = ((y - centerY) / centerY) * -5;
        const rotateY = ((x - centerX) / centerX) * 5;
        
        card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateY(-4px)`;
    };

    const handleCardMouseLeave = (e) => {
        const card = e.currentTarget;
        card.style.transform = '';
    };

    return (
        <DashboardLayout role="student">
            <div className="feedback-page-content">
                
                {/* Cyberpunk background decorations */}
                <div className="feedback-decorations" aria-hidden="true">
                    <div className="decor-orb decor-orb--left"></div>
                    <div className="decor-orb decor-orb--right"></div>
                    <div className="decor-particles--left">
                        <span className="p-dot p-dot-1"></span>
                        <span className="p-dot p-dot-2"></span>
                        <span className="p-dot p-dot-3"></span>
                        <span className="p-dot p-dot-4"></span>
                        <span className="p-dot p-dot-5"></span>
                        <span className="p-dot p-dot-6"></span>
                        <span className="p-dot p-dot-7"></span>
                        <span className="p-dot p-dot-8"></span>
                    </div>
                    <div className="decor-lines--right">
                        <svg className="holo-wave-svg" viewBox="0 0 1000 400" xmlns="http://www.w3.org/2000/svg">
                            <path className="wave-path path-1" d="M 0 200 C 150 100, 350 300, 500 200 C 650 100, 850 300, 1000 200" fill="none" stroke="url(#holo-grad-1)" strokeWidth="2" />
                            <path className="wave-path path-2" d="M 0 220 C 200 120, 300 280, 500 220 C 700 160, 800 320, 1000 220" fill="none" stroke="url(#holo-grad-2)" strokeWidth="1.5" />
                            <path className="wave-path path-3" d="M 0 180 C 100 260, 400 120, 500 180 C 600 240, 900 100, 1000 180" fill="none" stroke="url(#holo-grad-3)" strokeWidth="1" />
                            <defs>
                                <linearGradient id="holo-grad-1" x1="0%" y1="0%" x2="100%" y2="0%">
                                    <stop offset="0%" stopColor="rgba(0, 229, 255, 0)" />
                                    <stop offset="50%" stopColor="rgba(0, 229, 255, 0.25)" />
                                    <stop offset="100%" stopColor="rgba(0, 229, 255, 0)" />
                                </linearGradient>
                                <linearGradient id="holo-grad-2" x1="0%" y1="0%" x2="100%" y2="0%">
                                    <stop offset="0%" stopColor="rgba(124, 77, 255, 0)" />
                                    <stop offset="50%" stopColor="rgba(124, 77, 255, 0.2)" />
                                    <stop offset="100%" stopColor="rgba(124, 77, 255, 0)" />
                                </linearGradient>
                                <linearGradient id="holo-grad-3" x1="0%" y1="0%" x2="100%" y2="0%">
                                    <stop offset="0%" stopColor="rgba(255, 79, 216, 0)" />
                                    <stop offset="50%" stopColor="rgba(255, 79, 216, 0.15)" />
                                    <stop offset="100%" stopColor="rgba(255, 79, 216, 0)" />
                                </linearGradient>
                            </defs>
                        </svg>
                    </div>
                </div>

                {error && <div className="error-banner">{error}</div>}
                {successMsg && <div className="success-banner">{successMsg}</div>}

                {loading ? (
                    <div className="loading-state">
                        <div className="spinner-glow"></div>
                        <h2>Loading feedback surveys...</h2>
                    </div>
                ) : (
                    <div className="feedback-layout">
                        {/* Left Column: List of Surveys */}
                        <div className="glass survey-list-panel">
                            <div className="panel-header">
                                <span className="dot"></span> Active Surveys
                            </div>
                            
                            {forms.length > 0 ? (
                                <div className="surveys-grid">
                                    {forms.map((f, i) => (
                                        <div
                                            key={f.formId}
                                            className={`survey-item-card ${activeForm === f.formId ? 'active' : ''}`}
                                            onClick={() => !f.submitted && handleSelectForm(f.formId)}
                                            onMouseMove={handleCardMouseMove}
                                            onMouseLeave={handleCardMouseLeave}
                                            style={{ animationDelay: `${i * 0.05}s` }}
                                        >
                                            <div className="item-meta">
                                                <span className="course-code">{f.courseCode}</span>
                                                <span className={`badge ${f.submitted ? 'badge--closed' : 'badge--live'}`}>
                                                    {f.submitted ? 'Completed' : 'Pending'}
                                                </span>
                                            </div>
                                            <h3>{f.title}</h3>
                                            <p className="desc">{f.description}</p>
                                            <div className="meta-footer">
                                                <span>Prof. {f.teacherName}</span>
                                                <span>{new Date(f.createdAt).toLocaleDateString()}</span>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="empty">
                                    <p>No active feedback surveys found at this time.</p>
                                </div>
                            )}
                        </div>

                        {/* Right Column: Active Question Form Sheet */}
                        <div className={`glass survey-question-panel ${activeForm ? 'has-active' : ''}`}>
                            {activeForm ? (
                                loadingForm ? (
                                    <div className="loading-state">
                                        <div className="spinner-glow"></div>
                                        <h3>Loading questions...</h3>
                                    </div>
                                ) : (
                                    formDetails && (
                                        <form onSubmit={handleFeedbackSubmit} className="feedback-form">
                                            <div className="form-header">
                                                <h2>{formDetails.title}</h2>
                                                <p className="course-line">{formDetails.courseCode} • {formDetails.courseName} • Prof. {formDetails.teacherName}</p>
                                                <p className="desc-line">{formDetails.description}</p>
                                            </div>

                                            <div className="questions-list">
                                                {formDetails.questions?.map((q, idx) => (
                                                    <div key={q.id} className="question-item">
                                                        <label className="question-label">
                                                            {idx + 1}. {q.text} {q.required && <span className="req-star">*</span>}
                                                        </label>

                                                        {q.type === 'RATING' ? (
                                                            /* Rating score scale */
                                                            <div className="rating-options-row">
                                                                {Array.from({ length: q.ratingMax || 5 }, (_, scoreIdx) => {
                                                                    const score = scoreIdx + 1;
                                                                    const isSelected = answers[`q_${q.id}`] === String(score);
                                                                    return (
                                                                        <button
                                                                            type="button"
                                                                            key={score}
                                                                            className={`rating-score-btn ${isSelected ? 'active' : ''}`}
                                                                            onClick={() => handleAnswerChange(q.id, String(score))}
                                                                        >
                                                                            {score}
                                                                        </button>
                                                                    );
                                                                })}
                                                                <span className="rating-guide">
                                                                    (1 = Poor, {q.ratingMax || 5} = Excellent)
                                                                </span>
                                                            </div>
                                                        ) : q.type === 'TEXT' ? (
                                                            /* Free text box */
                                                            <textarea
                                                                className="text-answer-input"
                                                                placeholder="Type your feedback description details here..."
                                                                value={answers[`q_${q.id}`] || ''}
                                                                onChange={(e) => handleAnswerChange(q.id, e.target.value)}
                                                                required={q.required}
                                                            />
                                                        ) : (
                                                            /* Multiple choice options */
                                                            <div className="options-select-row">
                                                                {q.options?.map((opt, optIdx) => {
                                                                    const isSelected = answers[`q_${q.id}`] === opt;
                                                                    return (
                                                                        <label key={optIdx} className={`opt-pill ${isSelected ? 'active' : ''}`}>
                                                                            <input
                                                                                type="radio"
                                                                                name={`q_${q.id}`}
                                                                                value={opt}
                                                                                checked={isSelected}
                                                                                onChange={() => handleAnswerChange(q.id, opt)}
                                                                            />
                                                                            <span>{opt}</span>
                                                                        </label>
                                                                    );
                                                                })}
                                                            </div>
                                                        )}
                                                    </div>
                                                ))}
                                            </div>

                                            <div className="form-footer-action">
                                                <button
                                                    type="submit"
                                                    className="liquid-btn btn-submit-feedback"
                                                    disabled={submitting}
                                                >
                                                    {submitting ? 'Submitting Responses...' : 'Submit Feedback'}
                                                </button>
                                            </div>
                                        </form>
                                    )
                                )
                            ) : (
                                <div className="question-panel-empty">
                                    <div className="empty-inner">
                                        <span>📝</span>
                                        <h3>Select a Survey</h3>
                                        <p>Click on any pending survey in the left panel to open the evaluation questionnaire sheet.</p>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </DashboardLayout>
    );
}
