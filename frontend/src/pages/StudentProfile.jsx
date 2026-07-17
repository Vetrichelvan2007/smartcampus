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
                {error && <div className="error-banner">{error}</div>}

                {loading ? (
                    <div className="loading-state">
                        <h2>Loading profile...</h2>
                    </div>
                ) : (
                    profile && (
                        <div className="card main-profile-card">
                            {/* Profile Top Summary */}
                            <div className="profile-top">
                                <div className="avatar-wrap">
                                    <div className="avatar-glow"></div>
                                    <div className="avatar-placeholder">
                                        {profile.student?.name ? profile.student.name.charAt(0).toUpperCase() : 'U'}
                                    </div>
                                </div>
                                <div className="name-block">
                                    <h2>{profile.student?.name}</h2>
                                    <div className="roll-badge">
                                        Roll No: {profile.student?.rollNumber}
                                    </div>
                                    <div className="profile-meta">
                                        <span className="meta-chip">{profile.student?.department}</span>
                                        <span className="meta-chip">Year {profile.student?.year}</span>
                                        <span className="meta-chip">Sem {profile.student?.sem}</span>
                                    </div>
                                </div>
                            </div>

                            {/* Student Personal Details */}
                            <div className="section-block">
                                <div className="section-title">Student Details</div>
                                <div className="grid">
                                    {[
                                        { label: 'Name', value: profile.student?.name },
                                        { label: 'Roll Number', value: profile.student?.rollNumber },
                                        { label: 'Email', value: profile.student?.email },
                                        { label: 'Phone', value: profile.student?.phone },
                                        { label: 'Department', value: profile.student?.department },
                                        { label: 'Year', value: profile.student?.year },
                                        { label: 'Semester', value: profile.student?.sem },
                                        { label: 'Date of Birth', value: profile.student?.dob },
                                        { label: 'Gender', value: profile.student?.gender },
                                        { label: 'Nationality', value: profile.student?.nationality },
                                        { label: 'Mother Tongue', value: profile.student?.motherTongue },
                                        { label: 'Address', value: profile.student?.address },
                                    ].map((f, i) => (
                                        <div
                                            key={i}
                                            className="field"
                                            onMouseMove={(e) => handleMouseMove(e, e.currentTarget)}
                                            onMouseLeave={(e) => handleMouseLeave(e.currentTarget)}
                                        >
                                            <span>{f.label}</span>
                                            <div className="value">{f.value || 'N/A'}</div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="divider"></div>

                            {/* Father Details */}
                            <div className="section-block">
                                <div className="section-title">Father Details</div>
                                <div className="grid">
                                    {[
                                        { label: 'Name', value: profile.father?.name },
                                        { label: 'Phone', value: profile.father?.phone },
                                        { label: 'Email', value: profile.father?.email },
                                        { label: 'Occupation', value: profile.father?.occupation },
                                        { label: 'Annual Income', value: profile.father?.annualIncome },
                                        { label: 'Address', value: profile.father?.address },
                                    ].map((f, i) => (
                                        <div
                                            key={i}
                                            className="field"
                                            onMouseMove={(e) => handleMouseMove(e, e.currentTarget)}
                                            onMouseLeave={(e) => handleMouseLeave(e.currentTarget)}
                                        >
                                            <span>{f.label}</span>
                                            <div className="value">{f.value || 'N/A'}</div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="divider"></div>

                            {/* Mother Details */}
                            <div className="section-block">
                                <div className="section-title">Mother Details</div>
                                <div className="grid">
                                    {[
                                        { label: 'Name', value: profile.mother?.name },
                                        { label: 'Phone', value: profile.mother?.phone },
                                        { label: 'Email', value: profile.mother?.email },
                                        { label: 'Occupation', value: profile.mother?.occupation },
                                        { label: 'Annual Income', value: profile.mother?.annualIncome },
                                        { label: 'Address', value: profile.mother?.address },
                                    ].map((f, i) => (
                                        <div
                                            key={i}
                                            className="field"
                                            onMouseMove={(e) => handleMouseMove(e, e.currentTarget)}
                                            onMouseLeave={(e) => handleMouseLeave(e.currentTarget)}
                                        >
                                            <span>{f.label}</span>
                                            <div className="value">{f.value || 'N/A'}</div>
                                        </div>
                                    ))}
                                </div>
                            </div>

                            <div className="divider"></div>

                            {/* Identity Details */}
                            <div className="section-block">
                                <div className="section-title">Identity Details</div>
                                <div className="grid">
                                    {[
                                        { label: 'Aadhar Number', value: profile.identity?.aadharNumber },
                                        { label: 'PAN Number', value: profile.identity?.panNumber },
                                        { label: 'Passport Number', value: profile.identity?.passportNumber },
                                    ].map((f, i) => (
                                        <div
                                            key={i}
                                            className="field"
                                            onMouseMove={(e) => handleMouseMove(e, e.currentTarget)}
                                            onMouseLeave={(e) => handleMouseLeave(e.currentTarget)}
                                        >
                                            <span>{f.label}</span>
                                            <div className="value">{f.value || 'N/A'}</div>
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
