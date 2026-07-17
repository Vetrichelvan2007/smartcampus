import React from 'react';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import './StudentDashboard.css';

export default function StudentDashboard() {
    const { user } = useAuth();

    return (
        <DashboardLayout role="student">
            <div className="student-dashboard-content">
                {/* Welcome Greeting Banner */}
                <div className="welcome-banner card">
                    <div className="welcome-banner-text">
                        <h2>Welcome back, {user ? user.name : 'Student'}! 🎓</h2>
                        <p>Access and manage your courses, view your classroom performance, and update details directly from your campus portal.</p>
                    </div>
                </div>

                {/* Dashboard Stats / Grid */}
                <div className="dashboard-grid">
                    {/* Profile Information Widget */}
                    <div className="student-info-card card">
                        <h3>Student Profile Overview</h3>
                        {user && (
                            <div className="info-list">
                                <div className="info-item">
                                    <span className="label">Name:</span>
                                    <span className="val">{user.name}</span>
                                </div>
                                <div className="info-item">
                                    <span className="label">Roll Number:</span>
                                    <span className="val">{user.rollNumber || 'N/A'}</span>
                                </div>
                                <div className="info-item">
                                    <span className="label">Email:</span>
                                    <span className="val">{user.email}</span>
                                </div>
                                <div className="info-item">
                                    <span className="label">Role:</span>
                                    <span className="val uppercase">{user.role}</span>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Quick Access Portal Actions */}
                    <div className="quick-actions-card card">
                        <h3>Quick Navigation</h3>
                        <p className="sub">Access key portal pages directly:</p>
                        <div className="action-buttons">
                            <a href="/student-classroom" className="action-btn">
                                <span className="icon">🏫</span>
                                <div className="btn-text">
                                    <strong>Go to Classroom</strong>
                                    <small>View enrolled courses and classes</small>
                                </div>
                            </a>
                            <a href="/student-profile" className="action-btn">
                                <span className="icon">👤</span>
                                <div className="btn-text">
                                    <strong>View Full Profile</strong>
                                    <small>Personal, parental & identity data</small>
                                </div>
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </DashboardLayout>
    );
}
