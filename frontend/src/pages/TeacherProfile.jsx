import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import './TeacherProfile.css';

export default function TeacherProfile() {
    const { user } = useAuth();
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchProfileData = async () => {
            try {
                const response = await fetch('/api/teacher/profile');
                if (response.ok) {
                    const data = await response.json();
                    setProfile(data);
                } else {
                    const data = await response.json();
                    setError(data.message || 'Failed to load profile details.');
                }
            } catch (err) {
                console.error(err);
                setError('Network error. Failed to load profile.');
            } finally {
                setLoading(false);
            }
        };

        fetchProfileData();
    }, []);

    // Magnetic micro-interaction handler for fields + Pixel Hover tracking
    const handleMouseMove = (e, target) => {
        const rect = target.getBoundingClientRect();
        
        // Pixel coordinates
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        target.style.setProperty('--mouse-x', `${x}px`);
        target.style.setProperty('--mouse-y', `${y}px`);

        // Magnetic displacement
        const cx = rect.left + rect.width / 2;
        const cy = rect.top + rect.height / 2;
        const dx = (e.clientX - cx) / (rect.width / 2);
        const dy = (e.clientY - cy) / (rect.height / 2);
        target.style.transform = `translateY(-2px) translate(${dx * 3}px, ${dy * 1.5}px)`;
    };

    const handleMouseLeave = (target) => {
        target.style.transform = '';
    };

    return (
        <DashboardLayout role="teacher">
            <div className="teacher-profile-content">
                
                {/* Ambient background orbs */}
                <div className="ambient-orbs" aria-hidden="true">
                    <div className="orb orb-purple" />
                    <div className="orb orb-cyan"   />
                    <div className="orb orb-emerald"/>
                </div>

                {error && (
                    <div className="error-banner card">
                        <span className="error-icon">⚠️</span>
                        <div>
                            <strong>Access Error</strong>
                            <p>{error}</p>
                        </div>
                    </div>
                )}

                {loading ? (
                    <div className="loading-state">
                        <div className="spinner-glow"></div>
                        <h2>Decrypting Faculty Profile...</h2>
                    </div>
                ) : (
                    profile && (
                        <div className="card main-profile-card">
                            <div className="pixel-grid-hover" aria-hidden="true"></div>

                            {/* Profile Top Summary */}
                            <div className="profile-top">
                                <div className="avatar-wrap">
                                    <div className="avatar-glow"></div>
                                    <div className="avatar-placeholder">
                                        {profile.teacher?.name ? profile.teacher.name.substring(0, 2).toUpperCase() : 'PR'}
                                    </div>
                                </div>
                                <div className="name-block">
                                    <h2>{profile.teacher?.name}</h2>
                                    <div className="roll-badge">
                                        <span className="pulse-dot"></span>
                                        College ID: {profile.teacher?.teacherClgId}
                                    </div>
                                    <div className="profile-meta">
                                        <span className="meta-chip">{profile.teacher?.designation}</span>
                                        <span className="meta-chip">{profile.teacher?.staffType}</span>
                                        <span className="meta-chip">{profile.teacher?.employmentType}</span>
                                    </div>
                                </div>
                            </div>

                            {/* Teacher Personal Details */}
                            <div className="section-block">
                                <div className="section-title">
                                    <span className="title-icon">👤</span>
                                    Teacher Details
                                </div>
                                <div className="grid">
                                    {[
                                        { label: 'Name', value: profile.teacher?.name },
                                        { label: 'College ID', value: profile.teacher?.teacherClgId, glow: true },
                                        { label: 'Email', value: profile.teacher?.email, glow: true },
                                        { label: 'Phone', value: profile.teacher?.phone },
                                        { label: 'Gender', value: profile.teacher?.gender },
                                        { label: 'Date of Birth', value: profile.teacher?.dateOfBirth },
                                        { label: 'Blood Group', value: profile.teacher?.bloodGroup },
                                        { label: 'Address', value: profile.teacher?.address },
                                        { label: 'Designation', value: profile.teacher?.designation },
                                        { label: 'Office Location', value: profile.teacher?.officeLocation },
                                        { label: 'Account Status', value: profile.teacher?.accountStatus, badge: true },
                                    ].map((f, i) => (
                                        <div
                                            key={i}
                                            className="field"
                                            onMouseMove={(e) => handleMouseMove(e, e.currentTarget)}
                                            onMouseLeave={(e) => handleMouseLeave(e.currentTarget)}
                                        >
                                            <div className="pixel-grid-hover" aria-hidden="true"></div>
                                            <span>{f.label}</span>
                                            {f.badge ? (
                                                <div className="status-badge-inline">
                                                    <span className="pulse-dot-inline"></span>
                                                    {f.value || 'N/A'}
                                                </div>
                                            ) : f.glow ? (
                                                <div className="value glow-value">{f.value || 'N/A'}</div>
                                            ) : (
                                                <div className="value">{f.value || 'N/A'}</div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="divider"></div>

                            {/* Research & Publications */}
                            <div className="section-block">
                                <div className="section-title">
                                    <span className="title-icon">📚</span>
                                    Research & Publications
                                </div>
                                <div className="grid">
                                    {[
                                        { label: 'Papers Published', value: profile.teacher?.papersPublished, icon: '📄' },
                                        { label: 'Conferences Attended', value: profile.teacher?.conferencesAttended, icon: '🤝' },
                                        { label: 'Workshops Attended', value: profile.teacher?.workshopsAttended, icon: '🛠️' },
                                        { label: 'Patents Filed', value: profile.teacher?.patents, icon: '💡' },
                                        { label: 'Funded Projects', value: profile.teacher?.fundedProjects, icon: '💰' },
                                    ].map((f, i) => (
                                        <div
                                            key={i}
                                            className="field metric-field"
                                            onMouseMove={(e) => handleMouseMove(e, e.currentTarget)}
                                            onMouseLeave={(e) => handleMouseLeave(e.currentTarget)}
                                        >
                                            <div className="pixel-grid-hover" aria-hidden="true"></div>
                                            <span className="metric-header">
                                                <span className="metric-icon">{f.icon}</span>
                                                {f.label}
                                            </span>
                                            <div className="value metric-value">{f.value !== undefined ? f.value : '0'}</div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="divider"></div>

                            {/* Leaves Balance */}
                            <div className="section-block">
                                <div className="section-title">
                                    <span className="title-icon">📅</span>
                                    Leave Balance Details
                                </div>
                                <div className="grid">
                                    {[
                                        { label: 'Casual Leaves', value: profile.teacher?.casualLeaveBalance },
                                        { label: 'Medical Leaves', value: profile.teacher?.medicalLeaveBalance },
                                        { label: 'Earned Leaves', value: profile.teacher?.earnedLeaveBalance },
                                    ].map((f, i) => (
                                        <div
                                            key={i}
                                            className="field"
                                            onMouseMove={(e) => handleMouseMove(e, e.currentTarget)}
                                            onMouseLeave={(e) => handleMouseLeave(e.currentTarget)}
                                        >
                                            <div className="pixel-grid-hover" aria-hidden="true"></div>
                                            <span>{f.label}</span>
                                            <div className="value">{f.value !== undefined ? f.value : '0'}</div>
                                            {/* Progress bar container */}
                                            <div className="leave-progress-bar">
                                                <div className="progress-fill" style={{ width: `${Math.min(100, ((f.value || 0) / 15) * 100)}%` }}></div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="divider"></div>

                            {/* Qualifications List */}
                            <div className="section-block">
                                <div className="section-title">
                                    <span className="title-icon">🎓</span>
                                    Qualifications & Degrees
                                </div>
                                {profile.qualifications && profile.qualifications.length > 0 ? (
                                    <div className="qualifications-list">
                                        {profile.qualifications.map((q, idx) => (
                                            <div 
                                                key={idx} 
                                                className="qualification-card"
                                                onMouseMove={(e) => handleMouseMove(e, e.currentTarget)}
                                                onMouseLeave={(e) => handleMouseLeave(e.currentTarget)}
                                            >
                                                <div className="pixel-grid-hover" aria-hidden="true"></div>
                                                <div className="qual-card-header">
                                                    <span className="grad-icon">🎓</span>
                                                    <h4>Degree Record #{idx + 1}</h4>
                                                </div>
                                                <div className="qual-grid">
                                                    <div className="field"><span>UG Degree</span><div className="value">{q.ugDegree || 'N/A'}</div></div>
                                                    <div className="field"><span>PG Degree</span><div className="value">{q.pgDegree || 'N/A'}</div></div>
                                                    <div className="field"><span>Ph.D Status</span><div className="value">{q.phdStatus || 'N/A'}</div></div>
                                                    <div className="field"><span>Specialization</span><div className="value">{q.specialization || 'N/A'}</div></div>
                                                    <div className="field"><span>University</span><div className="value">{q.universityName || 'N/A'}</div></div>
                                                    <div className="field"><span>Passing Year</span><div className="value">{q.yearOfPassing || 'N/A'}</div></div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    <p style={{ color: 'var(--text-muted)', fontSize: '13px' }}>No qualification details registered.</p>
                                )}
                            </div>
                        </div>
                    )
                )}
            </div>
        </DashboardLayout>
    );
}
