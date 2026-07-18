import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import './StudentProfile.css';

export default function StudentProfile() {
    const { user } = useAuth();
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchProfileData = async () => {
            try {
                const response = await fetch('/api/student/profile');
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

    // Magnetic micro-interaction handler for fields
    const handleMouseMove = (e, target) => {
        const rect = target.getBoundingClientRect();
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
        <DashboardLayout role="student">
            <div className="student-profile-content">
                {/* Cyberpunk background decorations */}
                <div className="profile-decorations" aria-hidden="true">
                    <div className="decor-orb decor-orb--left"></div>
                    <div className="decor-orb decor-orb--right"></div>
                    <div className="decor-particles">
                        <span className="p-dot p-dot-1"></span>
                        <span className="p-dot p-dot-2"></span>
                        <span className="p-dot p-dot-3"></span>
                        <span className="p-dot p-dot-4"></span>
                        <span className="p-dot p-dot-5"></span>
                        <span className="p-dot p-dot-6"></span>
                    </div>
                </div>

                {error && <div className="error-banner">{error}</div>}

                {loading ? (
                    <div className="loading-state">
                        <div className="spinner-glow"></div>
                        <h2>Loading profile...</h2>
                    </div>
                ) : (
                    profile && (
                        <div className="profile-grid-container">
                            {/* Row 1: Hero Profile Card */}
                            <div className="glass-card profile-hero-panel">
                                <div className="hero-left-section">
                                    <div className="avatar-wrap">
                                        <div className="avatar-glow"></div>
                                        <div className="holo-ring-outer"></div>
                                        <div className="holo-ring-inner"></div>
                                        <div className="avatar-placeholder">
                                            {profile.student?.name ? profile.student.name.charAt(0).toUpperCase() : 'U'}
                                        </div>
                                    </div>
                                    <div className="name-block">
                                        <h2>{profile.student?.name}</h2>
                                        <div className="badge-row">
                                            <div className="roll-badge">
                                                Roll No: {profile.student?.rollNumber}
                                            </div>
                                            <div className="verified-badge-chip">
                                                <span className="check-icon">✓</span> Verified Student
                                            </div>
                                        </div>
                                        <div className="profile-meta">
                                            <span className="meta-chip">{profile.student?.department}</span>
                                            <span className="meta-chip">Year {profile.student?.year}</span>
                                            <span className="meta-chip">Sem {profile.student?.sem}</span>
                                        </div>
                                    </div>
                                </div>

                                <div className="hero-right-section">
                                    <div className="analytics-mini-panel">
                                        <div className="analytics-stat">
                                            <span className="stat-label">Courses Enrolled</span>
                                            <div className="stat-value">6</div>
                                        </div>
                                        <div className="analytics-stat">
                                            <span className="stat-label">Attendance</span>
                                            <div className="stat-value">92%</div>
                                        </div>
                                        <div className="analytics-stat">
                                            <span className="stat-label">Credits Earned</span>
                                            <div className="stat-value">18</div>
                                        </div>
                                        <div className="analytics-stat">
                                            <span className="stat-label">Status</span>
                                            <div className="stat-value verified">VERIFIED</div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* Row 2: Student Personal Details */}
                            <div className="glass-card personal-details-card animate-delay-1">
                                <div className="card-header">
                                    <div className="section-title">Student Details</div>
                                </div>
                                <div className="fields-grid grid-3-cols">
                                    {[
                                        { label: 'Name', value: profile.student?.name, icon: '👤' },
                                        { label: 'Roll Number', value: profile.student?.rollNumber, icon: '🆔' },
                                        { label: 'Email', value: profile.student?.email, icon: '📧' },
                                        { label: 'Phone', value: profile.student?.phone, icon: '📞' },
                                        { label: 'Department', value: profile.student?.department, icon: '🏛️' },
                                        { label: 'Year', value: profile.student?.year, icon: '📅' },
                                        { label: 'Semester', value: profile.student?.sem, icon: '⏱️' },
                                        { label: 'Date of Birth', value: profile.student?.dob, icon: '🎂' },
                                        { label: 'Gender', value: profile.student?.gender, icon: '⚧️' },
                                        { label: 'Nationality', value: profile.student?.nationality, icon: '🌐' },
                                        { label: 'Mother Tongue', value: profile.student?.motherTongue, icon: '🗣️' },
                                        { label: 'Address', value: profile.student?.address, icon: '🏠' },
                                    ].map((f, i) => (
                                        <div
                                            key={i}
                                            className="field-item"
                                            onMouseMove={(e) => handleMouseMove(e, e.currentTarget)}
                                            onMouseLeave={(e) => handleMouseLeave(e.currentTarget)}
                                        >
                                            <div className="field-icon-wrap">
                                                <span className="field-icon">{f.icon}</span>
                                            </div>
                                            <div className="field-info">
                                                <span className="field-label">{f.label}</span>
                                                <div className="field-value">{f.value || 'N/A'}</div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Row 3 Column 1: Father Details */}
                            <div className="glass-card father-details-card animate-delay-2">
                                <div className="card-header">
                                    <div className="section-title">Father Details</div>
                                </div>
                                <div className="fields-grid grid-2-cols">
                                    {[
                                        { label: 'Name', value: profile.father?.name, icon: '👤' },
                                        { label: 'Phone', value: profile.father?.phone, icon: '📞' },
                                        { label: 'Email', value: profile.father?.email, icon: '📧' },
                                        { label: 'Occupation', value: profile.father?.occupation, icon: '💼' },
                                        { label: 'Annual Income', value: profile.father?.annualIncome, icon: '💰' },
                                        { label: 'Address', value: profile.father?.address, icon: '🏠' },
                                    ].map((f, i) => (
                                        <div
                                            key={i}
                                            className="field-item"
                                            onMouseMove={(e) => handleMouseMove(e, e.currentTarget)}
                                            onMouseLeave={(e) => handleMouseLeave(e.currentTarget)}
                                        >
                                            <div className="field-icon-wrap">
                                                <span className="field-icon">{f.icon}</span>
                                            </div>
                                            <div className="field-info">
                                                <span className="field-label">{f.label}</span>
                                                <div className="field-value">{f.value || 'N/A'}</div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Row 3 Column 2: Mother Details */}
                            <div className="glass-card mother-details-card animate-delay-2">
                                <div className="card-header">
                                    <div className="section-title">Mother Details</div>
                                </div>
                                <div className="fields-grid grid-2-cols">
                                    {[
                                        { label: 'Name', value: profile.mother?.name, icon: '👤' },
                                        { label: 'Phone', value: profile.mother?.phone, icon: '📞' },
                                        { label: 'Email', value: profile.mother?.email, icon: '📧' },
                                        { label: 'Occupation', value: profile.mother?.occupation, icon: '💼' },
                                        { label: 'Annual Income', value: profile.mother?.annualIncome, icon: '💰' },
                                        { label: 'Address', value: profile.mother?.address, icon: '🏠' },
                                    ].map((f, i) => (
                                        <div
                                            key={i}
                                            className="field-item"
                                            onMouseMove={(e) => handleMouseMove(e, e.currentTarget)}
                                            onMouseLeave={(e) => handleMouseLeave(e.currentTarget)}
                                        >
                                            <div className="field-icon-wrap">
                                                <span className="field-icon">{f.icon}</span>
                                            </div>
                                            <div className="field-info">
                                                <span className="field-label">{f.label}</span>
                                                <div className="field-value">{f.value || 'N/A'}</div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Row 4: Identity Details */}
                            <div className="glass-card identity-details-card animate-delay-3">
                                <div className="card-header">
                                    <div className="section-title">Identity Details</div>
                                </div>
                                <div className="fields-grid grid-3-cols">
                                    {[
                                        { label: 'Aadhar Number', value: profile.identity?.aadharNumber, icon: '🪪' },
                                        { label: 'PAN Number', value: profile.identity?.panNumber, icon: '💳' },
                                        { label: 'Passport Number', value: profile.identity?.passportNumber, icon: '🛂' },
                                    ].map((f, i) => (
                                        <div
                                            key={i}
                                            className="field-item"
                                            onMouseMove={(e) => handleMouseMove(e, e.currentTarget)}
                                            onMouseLeave={(e) => handleMouseLeave(e.currentTarget)}
                                        >
                                            <div className="field-icon-wrap">
                                                <span className="field-icon">{f.icon}</span>
                                            </div>
                                            <div className="field-info">
                                                <span className="field-label">{f.label}</span>
                                                <div className="field-value">{f.value || 'N/A'}</div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    )
                )}
            </div>
        </DashboardLayout>
    );
}
