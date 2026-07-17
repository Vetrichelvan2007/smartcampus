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

    return (
        <DashboardLayout role="student">
            <div className="feedback-page-content">
                {error && <div className="error-banner">{error}</div>}
                {successMsg && <div className="success-banner">{successMsg}</div>}

                {loading ? (
                    <div className="loading-state">
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
                                    {forms.map(f => (
                                        <div
                                            key={f.formId}
                                            className={`survey-item-card ${activeForm === f.formId ? 'active' : ''}`}
                                            onClick={() => !f.submitted && handleSelectForm(f.formId)}
                                        >
                                            <div className="item-meta">
                                                <span className="course-code">{f.courseCode}</span>
                                                <span className={`badge ${f.submitted ? 'badge--live' : 'badge--closed'}`}>
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
                        <div className="glass survey-question-panel">
                            {activeForm ? (
                                loadingForm ? (
                                    <div className="loading-state">
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
                                                    className="btn btn--accent btn-submit-feedback"
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
