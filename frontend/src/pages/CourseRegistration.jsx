import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import './CourseRegistration.css';

export default function CourseRegistration() {
    const { user } = useAuth();
    const navigate = useNavigate();

    const [status, setStatus] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    
    // Tracks student selections: { courseId: teacherId }
    const [selections, setSelections] = useState({});
    const [submitting, setSubmitting] = useState(false);
    const [successMsg, setSuccessMsg] = useState('');

    useEffect(() => {
        const fetchRegistrationStatus = async () => {
            try {
                const response = await fetch('/api/student/registration/status');
                if (response.ok) {
                    const data = await response.json();
                    setStatus(data);
                    
                    // Initialize selections if registering
                    if (!data.alreadyRegistered && data.courses) {
                        const initialSelections = {};
                        data.courses.forEach(c => {
                            if (c.teachers && c.teachers.length > 0) {
                                // Default to first teacher
                                initialSelections[c.courseId] = c.teachers[0].teacherId;
                            }
                        });
                        setSelections(initialSelections);
                    }
                } else {
                    const data = await response.json();
                    setError(data.message || 'Failed to fetch course registration status.');
                }
            } catch (err) {
                console.error(err);
                setError('Network error. Failed to load registration.');
            } finally {
                setLoading(false);
            }
        };

        fetchRegistrationStatus();
    }, []);

    const handleTeacherChange = (courseId, teacherId) => {
        setSelections(prev => ({
            ...prev,
            [courseId]: teacherId
        }));
    };

    const handleRegisterSubmit = async (e) => {
        e.preventDefault();
        
        // Convert selections object to list of items
        const payload = Object.keys(selections).map(courseId => ({
            courseId: parseInt(courseId),
            teacherId: parseInt(selections[courseId])
        }));

        if (payload.length === 0) {
            setError('No courses selected for registration.');
            return;
        }

        setSubmitting(true);
        setError('');
        try {
            const response = await fetch('/api/student/registration/submit', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                setSuccessMsg('Course enrollment submitted successfully!');
                setTimeout(() => {
                    // Force refresh status
                    window.location.reload();
                }, 1500);
            } else {
                const data = await response.json();
                setError(data.message || 'Registration failed.');
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
            <div className="course-reg-page-content">
                {error && <div className="error-banner">{error}</div>}
                {successMsg && <div className="success-banner">{successMsg}</div>}

                {loading ? (
                    <div className="loading-state">
                        <h2>Loading Course Registration details...</h2>
                    </div>
                ) : (
                    status && (
                        <div className="registration-wrapper">
                            {/* Page Header */}
                            <div className="glass reg-header">
                                <div className="reg-icon-badge">✍️</div>
                                <h1>Semester Course Registration</h1>
                                <p className="reg-sub">
                                    Enroll in required core, lab, or elective courses for your current semester: <span>Semester {status.currentSemester}</span>
                                </p>
                            </div>

                            {status.alreadyRegistered ? (
                                /* Enrolled Courses List (Static Details Card) */
                                <div className="glass enrolled-sheet">
                                    <div className="lock-indicator">
                                        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                                        </svg>
                                        <span>ENROLLMENT COMPLETE</span>
                                    </div>
                                    
                                    <h3>Your Enrolled Course Roster</h3>
                                    <div className="table-wrap">
                                        <table className="table">
                                            <thead>
                                                <tr>
                                                    <th>Course Code</th>
                                                    <th>Course Name</th>
                                                    <th>Course Type</th>
                                                    <th>Assigned Faculty</th>
                                                    <th>Status</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {status.courses?.map(c => (
                                                    <tr key={c.courseId}>
                                                        <td><strong>{c.courseCode}</strong></td>
                                                        <td>{c.courseName}</td>
                                                        <td><span className="row-type">{c.courseType}</span></td>
                                                        <td>
                                                            {c.teachers && c.teachers.length > 0
                                                                ? c.teachers[0].teacherName
                                                                : 'Awaiting Faculty Allocation'}
                                                        </td>
                                                        <td>
                                                            <span className="badge badge--active">Enrolled</span>
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            ) : (
                                /* Active Course Selection Checklist Form */
                                <form onSubmit={handleRegisterSubmit} className="glass registration-form">
                                    <h3>Choose Enrolled Faculty</h3>
                                    <p className="form-tip">Review each syllabus and select your preferred section professor for this academic cycle.</p>
                                    
                                    <div className="courses-select-list">
                                        {status.courses?.map(c => (
                                            <div key={c.courseId} className="reg-course-row">
                                                <div className="course-info">
                                                    <span className="code">{c.courseCode}</span>
                                                    <h4>{c.courseName}</h4>
                                                    <span className="row-type">{c.courseType}</span>
                                                </div>
                                                <div className="teacher-select-wrap">
                                                    <label>Select Professor:</label>
                                                    <select
                                                        className="faculty-select"
                                                        value={selections[c.courseId] || ''}
                                                        onChange={(e) => handleTeacherChange(c.courseId, e.target.value)}
                                                        required
                                                    >
                                                        {c.teachers?.map(t => (
                                                            <option key={t.teacherId} value={t.teacherId}>
                                                                {t.teacherName}
                                                            </option>
                                                        ))}
                                                        {(!c.teachers || c.teachers.length === 0) && (
                                                            <option value="">No Faculty Allocated</option>
                                                        )}
                                                    </select>
                                                </div>
                                            </div>
                                        ))}
                                    </div>

                                    <div className="submit-action-wrap">
                                        <button
                                            type="submit"
                                            className="btn btn--accent btn-enroll"
                                            disabled={submitting}
                                        >
                                            {submitting ? 'Registering Courses...' : 'Lock and Enroll in Courses'}
                                        </button>
                                    </div>
                                </form>
                            )}
                        </div>
                    )
                )}
            </div>
        </DashboardLayout>
    );
}
