import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import './TeacherDashboard.css';

export default function TeacherDashboard() {
    const { user } = useAuth();
    const [courses, setCourses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [currentDate, setCurrentDate] = useState('');

    useEffect(() => {
        const now = new Date();
        setCurrentDate(now.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }));

        const fetchTeacherDashboard = async () => {
            try {
                const response = await fetch('/api/teacher/dashboard');
                if (response.ok) {
                    const data = await response.json();
                    setCourses(data.courses || []);
                } else {
                    const data = await response.json();
                    setError(data.message || 'Failed to fetch dashboard data.');
                }
            } catch (err) {
                console.error(err);
                setError('Network error. Failed to load teacher dashboard.');
            } finally {
                setLoading(false);
            }
        };

        fetchTeacherDashboard();
    }, []);

    return (
        <DashboardLayout role="teacher">
            <div className="teacher-dashboard-content">
                {/* Cyberpunk background decorations */}
                <div className="dashboard-decorations" aria-hidden="true">
                    <div className="decor-orb orb-purple"></div>
                    <div className="decor-orb orb-cyan"></div>
                    <div className="decor-particles">
                        <span className="p-dot p-1"></span>
                        <span className="p-dot p-2"></span>
                        <span className="p-dot p-3"></span>
                        <span className="p-dot p-4"></span>
                        <span className="p-dot p-5"></span>
                        <span className="p-dot p-6"></span>
                    </div>
                </div>

                {error && <div className="error-banner">{error}</div>}

                {loading ? (
                    <div className="loading-state">
                        <div className="spinner-glow"></div>
                        <h2>Loading Dashboard...</h2>
                    </div>
                ) : (
                    <>
                        {/* Welcome Banner */}
                        <div className="glass-card welcome-card">
                            <div className="welcome-glow-bg"></div>
                            <div className="welcome-left">
                                <h1 className="welcome-title">Welcome, <span>{user ? user.name : 'Professor'}</span></h1>
                                <p className="welcome-sub">The Smart Campus is live. You have 2 classes today and 12 pending tasks requiring attention.</p>
                                <div className="welcome-date">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <rect x="3" y="4" width="18" height="18" rx="2"/>
                                        <line x1="16" y1="2" x2="16" y2="6"/>
                                        <line x1="8" y1="2" x2="8" y2="6"/>
                                        <line x1="3" y1="10" x2="21" y2="10"/>
                                    </svg>
                                    <span>{currentDate}</span>
                                </div>
                            </div>
                            <div className="welcome-right">
                                <div className="stat-node">
                                    <div className="stat-ring-wrap">
                                        <div className="stat-ring-glow"></div>
                                        <svg className="stat-ring-svg" viewBox="0 0 36 36">
                                            <path className="ring-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                                            <path className="ring-fill fill-indigo" strokeDasharray="92, 100" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                                        </svg>
                                        <div className="stat-val indigo">92%</div>
                                    </div>
                                    <div className="stat-lbl">Attendance</div>
                                </div>
                                <div className="stat-node">
                                    <div className="stat-ring-wrap">
                                        <div className="stat-ring-glow"></div>
                                        <svg className="stat-ring-svg" viewBox="0 0 36 36">
                                            <path className="ring-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                                            <path className="ring-fill fill-cyan" strokeDasharray="96, 100" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                                        </svg>
                                        <div className="stat-val cyan">4.8</div>
                                    </div>
                                    <div className="stat-lbl">Rating</div>
                                </div>
                            </div>
                        </div>

                        {/* Summary Metrics */}
                        <div className="summary-grid">
                            <div className="glass-card summary-card animate-delay-1">
                                <div className="summary-icon primary">
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
                                    </svg>
                                </div>
                                <div className="summary-val">{courses.length < 10 ? `0${courses.length}` : courses.length}</div>
                                <div className="summary-label">Subjects</div>
                                <div className="card-ambient-glow"></div>
                            </div>

                            <div className="glass-card summary-card animate-delay-2">
                                <div className="summary-icon accent">
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
                                    </svg>
                                </div>
                                <div className="summary-val">248</div>
                                <div className="summary-label">Students</div>
                                <div className="card-ambient-glow"></div>
                            </div>

                            <div className="glass-card summary-card animate-delay-3">
                                <div className="summary-icon primary">
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
                                    </svg>
                                </div>
                                <div className="summary-val">04</div>
                                <div className="summary-label">Today's Sessions</div>
                                <div className="card-ambient-glow"></div>
                            </div>

                            <div className="glass-card summary-card animate-delay-4">
                                <div className="summary-icon accent">
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
                                    </svg>
                                </div>
                                <div className="summary-val">12</div>
                                <div className="summary-label">Pending Reviews</div>
                                <div className="card-ambient-glow"></div>
                            </div>
                        </div>

                        {/* Schedule and Portfolio */}
                        <div className="bottom-grid">
                            <div className="glass-card portfolio-card">
                                <div className="portfolio-header">
                                    <h2>Academic Portfolio</h2>
                                </div>
                                <div className="table-wrap">
                                    <table>
                                        <thead>
                                            <tr>
                                                <th>Subject</th>
                                                <th>Code</th>
                                                <th>Department</th>
                                                <th>Semester</th>
                                                <th>Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {courses.map((c) => (
                                                <tr key={c.courseId} className="portfolio-row">
                                                    <td><strong>{c.courseName}</strong></td>
                                                    <td><span className="code-capsule">{c.courseCode}</span></td>
                                                    <td>
                                                        <span className="dept-badge">{c.department}</span>
                                                    </td>
                                                    <td><span className="sem-capsule">Semester {c.semester}</span></td>
                                                    <td>
                                                        <Link to={`/teacher-classroom/${c.courseId}`} className="btn-manage-glass">
                                                            Manage
                                                        </Link>
                                                    </td>
                                                </tr>
                                            ))}
                                            {courses.length === 0 && (
                                                <tr>
                                                    <td colSpan="5" style={{ textAlign: 'center', color: 'var(--text-muted)' }}>
                                                        No allocated subjects found.
                                                     </td>
                                                </tr>
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            <div className="glass-card schedule-card">
                                <h2 className="schedule-header">
                                    <span className="holographic-clock">🕒</span> Live Schedule
                                </h2>
                                <div className="schedule-timeline">
                                    <div className="timeline-connector-line"></div>
                                    
                                    <div className="upcoming-item active-session">
                                        <div className="time-capsule">
                                            <span>09:30</span>
                                            <small>AM</small>
                                        </div>
                                        <div className="upcoming-details">
                                            <div className="title">Theory of Computation</div>
                                            <div className="loc">Room 402 • Hall B</div>
                                        </div>
                                        <div className="live-pulse-container">
                                            <span className="pulse-ring ring-1"></span>
                                            <span className="pulse-ring ring-2"></span>
                                            <span className="pulse-dot"></span>
                                        </div>
                                    </div>

                                    <div className="upcoming-item offline">
                                        <div className="time-capsule grey">
                                            <span>11:45</span>
                                            <small>AM</small>
                                        </div>
                                        <div className="upcoming-details">
                                            <div className="title">Neural Networks</div>
                                            <div className="loc">Lab 02 • South Wing</div>
                                        </div>
                                        <div className="offline-dot"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </>
                )}
            </div>
        </DashboardLayout>
    );
}
