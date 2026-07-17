import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import './StudentDashboard.css';

export default function StudentDashboard() {
    const { user } = useAuth();
    const [time, setTime] = useState(new Date());

    /* ── Live clock tick ── */
    useEffect(() => {
        const id = setInterval(() => setTime(new Date()), 1000);
        return () => clearInterval(id);
    }, []);

    const formatTime = (d) =>
        d.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true });

    const formatDate = (d) =>
        d.toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'short', year: 'numeric' });

    /* ── Card/Button Cursor Position Tracker for Pixel Hover Effect ── */
    const handleCardMouseMove = (e) => {
        const card = e.currentTarget;
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        card.style.setProperty('--mouse-x', `${x}px`);
        card.style.setProperty('--mouse-y', `${y}px`);
    };

    /* ── Magnetic hover on action buttons ── */
    const handleBtnMouseMove = (e) => {
        const btn = e.currentTarget;
        const rect = btn.getBoundingClientRect();
        const x = e.clientX - (rect.left + rect.width / 2);
        const y = e.clientY - (rect.top + rect.height / 2);
        const dist = Math.sqrt(x * x + y * y);
        if (dist < 120) {
            btn.style.transform = `translate(${x * 0.1}px, ${y * 0.1}px)`;
        } else {
            btn.style.transform = '';
        }
    };

    const handleBtnMouseLeave = (e) => {
        e.currentTarget.style.transform = '';
    };

    /* ── Ripple on click ── */
    const handleButtonClick = (e) => {
        const button = e.currentTarget;
        const old = button.querySelector('.ripple');
        if (old) old.remove();

        const circle = document.createElement('span');
        const diameter = Math.max(button.clientWidth, button.clientHeight);
        const radius = diameter / 2;
        const rect = button.getBoundingClientRect();

        circle.style.width  = circle.style.height = `${diameter}px`;
        circle.style.left   = `${e.clientX - rect.left - radius}px`;
        circle.style.top    = `${e.clientY - rect.top  - radius}px`;
        circle.classList.add('ripple');
        button.appendChild(circle);
    };

    /* ── Attendance ring SVG constants ── */
    const ATTENDANCE = 92;          // percent
    const RING_R     = 36;
    const CIRCUMFERENCE = 2 * Math.PI * RING_R;
    const DASH_OFFSET   = CIRCUMFERENCE * (1 - ATTENDANCE / 100);

    return (
        <DashboardLayout role="student">
            <div className="student-dashboard-content">

                {/* ── Ambient background orbs ── */}
                <div className="ambient-orbs" aria-hidden="true">
                    <div className="orb orb-purple" />
                    <div className="orb orb-cyan"   />
                    <div className="orb orb-emerald"/>
                </div>

                {/* ── Welcome Greeting Banner ── */}
                <div className="welcome-banner card" onMouseMove={handleCardMouseMove}>
                    <div className="pixel-grid-hover" aria-hidden="true"></div>
                    
                    <div className="welcome-banner-text">

                        {/* Top pill row */}
                        <div className="banner-pills-row">
                            <div className="status-badge">
                                <span className="pulse-dot" />
                                <span>TODAY'S STATUS: ACTIVE &amp; SECURED</span>
                            </div>
                            <div className="banner-pill classes-pill">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="pill-icon">
                                    <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
                                    <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
                                </svg>
                                3 Classes Today
                            </div>
                        </div>

                        {/* Glow halo behind heading */}
                        <div className="holo-glow-halo" aria-hidden="true" />

                        <h2>Welcome back, {user ? user.name : 'Student'}! 🎓</h2>
                        <p>Access and manage your courses, view your classroom performance, and update details directly from your campus portal.</p>

                        {/* Bottom widget row */}
                        <div className="banner-widgets-row">
                            {/* Live clock */}
                            <div className="clock-widget">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="clock-icon">
                                    <circle cx="12" cy="12" r="10"/>
                                    <polyline points="12 6 12 12 16 14"/>
                                </svg>
                                <div className="clock-details">
                                    <span className="clock-time">{formatTime(time)}</span>
                                    <span className="clock-date">{formatDate(time)}</span>
                                </div>
                            </div>

                            {/* Attendance ring */}
                            <div className="attendance-ring-widget">
                                <svg className="attendance-ring-svg" viewBox="0 0 88 88">
                                    {/* Track */}
                                    <circle cx="44" cy="44" r={RING_R} fill="none"
                                        stroke="rgba(0,229,255,0.12)" strokeWidth="7"/>
                                    {/* Progress */}
                                    <circle cx="44" cy="44" r={RING_R} fill="none"
                                        stroke="url(#ringGradient)" strokeWidth="7"
                                        strokeLinecap="round"
                                        strokeDasharray={CIRCUMFERENCE}
                                        strokeDashoffset={DASH_OFFSET}
                                        transform="rotate(-90 44 44)"
                                        className="ring-progress"/>
                                    <defs>
                                        <linearGradient id="ringGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                                            <stop offset="0%"   stopColor="#00E5FF"/>
                                            <stop offset="100%" stopColor="#00FFB3"/>
                                        </linearGradient>
                                    </defs>
                                    <text x="44" y="49" textAnchor="middle"
                                        fill="#00E5FF" fontSize="15" fontWeight="700"
                                        fontFamily="'Orbitron', sans-serif">
                                        {ATTENDANCE}%
                                    </text>
                                </svg>
                                <span className="attendance-label">Attendance</span>
                            </div>
                        </div>

                    </div>
                </div>

                {/* ── Dashboard Grid ── */}
                <div className="dashboard-grid">

                    {/* ── Profile Information Widget ── */}
                    <div className="student-info-card card" onMouseMove={handleCardMouseMove}>
                        <div className="pixel-grid-hover" aria-hidden="true"></div>
                        <h3>Student Profile Overview</h3>
                        {user && (
                            <div className="info-list">
                                <div className="info-item">
                                    <span className="label">
                                        <svg className="info-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                                            <circle cx="12" cy="7" r="4"/>
                                        </svg>
                                        Name
                                    </span>
                                    <span className="val">{user.name}</span>
                                </div>
                                <div className="info-item">
                                    <span className="label">
                                        <svg className="info-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                                            <line x1="16" y1="2" x2="16" y2="6"/>
                                            <line x1="8"  y1="2" x2="8"  y2="6"/>
                                            <line x1="3"  y1="10" x2="21" y2="10"/>
                                        </svg>
                                        Roll Number
                                    </span>
                                    <span className="val">{user.rollNumber || 'N/A'}</span>
                                </div>
                                <div className="info-item">
                                    <span className="label">
                                        <svg className="info-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                                            <polyline points="22,6 12,13 2,6"/>
                                        </svg>
                                        Email
                                    </span>
                                    <span className="val">{user.email}</span>
                                </div>
                                <div className="info-item">
                                    <span className="label">
                                        <svg className="info-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                                        </svg>
                                        Role
                                    </span>
                                    <span className="val uppercase role-badge">{user.role}</span>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* ── Quick Access Portal Actions ── */}
                    <div className="quick-actions-card card" onMouseMove={handleCardMouseMove}>
                        <div className="pixel-grid-hover" aria-hidden="true"></div>
                        <h3>Quick Navigation</h3>
                        <p className="sub">Access key portal pages directly:</p>
                        <div className="action-buttons">
                            <a
                                href="/student-classroom"
                                className="action-btn"
                                onMouseMove={(e) => { handleBtnMouseMove(e); handleCardMouseMove(e); }}
                                onMouseLeave={handleBtnMouseLeave}
                                onClick={handleButtonClick}
                            >
                                <div className="pixel-grid-hover" aria-hidden="true"></div>
                                <span className="icon">🏫</span>
                                <div className="btn-text">
                                    <strong>Go to Classroom</strong>
                                    <small>View enrolled courses and classes</small>
                                </div>
                                <span className="arrow">→</span>
                            </a>
                            <a
                                href="/student-profile"
                                className="action-btn"
                                onMouseMove={(e) => { handleBtnMouseMove(e); handleCardMouseMove(e); }}
                                onMouseLeave={handleBtnMouseLeave}
                                onClick={handleButtonClick}
                            >
                                <div className="pixel-grid-hover" aria-hidden="true"></div>
                                <span className="icon">👤</span>
                                <div className="btn-text">
                                    <strong>View Full Profile</strong>
                                    <small>Personal, parental &amp; identity data</small>
                                </div>
                                <span className="arrow">→</span>
                            </a>
                        </div>
                    </div>

                </div>
            </div>
        </DashboardLayout>
    );
}
