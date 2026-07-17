import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Login.css';
import enhancedCampus from '../assets/full-screen enhanced REC campus.png';

export default function Login() {
    const { login, user } = useAuth();
    const navigate = useNavigate();

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [theme, setTheme] = useState(localStorage.getItem('smartcampus-theme') || 'dark');
    const [error, setError] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const [emailFocused, setEmailFocused] = useState(false);
    const [passwordFocused, setPasswordFocused] = useState(false);

    const cardRef = useRef(null);
    const btnRef = useRef(null);

    const handleBtnMouseMove = (e) => {
        if (!btnRef.current) return;
        const btn = btnRef.current;
        const rect = btn.getBoundingClientRect();
        const x = e.clientX - (rect.left + rect.width / 2);
        const y = e.clientY - (rect.top + rect.height / 2);
        
        const dist = Math.sqrt(x * x + y * y);
        if (dist < 100) {
            const strength = 0.2; // subtle magnetic pull
            btn.style.transform = `translate(${x * strength}px, ${y * strength}px)`;
        } else {
            btn.style.transform = '';
        }
    };

    const handleBtnMouseLeave = () => {
        if (!btnRef.current) return;
        btnRef.current.style.transform = '';
    };

    const handleButtonClick = (e) => {
        const button = e.currentTarget;
        const circle = document.createElement("span");
        const diameter = Math.max(button.clientWidth, button.clientHeight);
        const radius = diameter / 2;

        const rect = button.getBoundingClientRect();
        circle.style.width = circle.style.height = `${diameter}px`;
        circle.style.left = `${e.clientX - rect.left - radius}px`;
        circle.style.top = `${e.clientY - rect.top - radius}px`;
        circle.classList.add("ripple");

        const ripple = button.getElementsByClassName("ripple")[0];
        if (ripple) {
            ripple.remove();
        }

        button.appendChild(circle);
    };

    // Redirect to dashboards if already logged in
    useEffect(() => {
        if (user && user.authenticated) {
            if (user.role.toLowerCase() === 'student') navigate('/student-dashboard');
            else if (user.role.toLowerCase() === 'teacher') navigate('/teacher-dashboard');
            else if (user.role.toLowerCase() === 'admin') navigate('/admin-dashboard');
        }
    }, [user, navigate]);

    // Apply theme
    useEffect(() => {
        const html = document.documentElement;
        if (theme === 'dark') {
            html.setAttribute('data-theme', 'dark');
            localStorage.setItem('smartcampus-theme', 'dark');
        } else {
            html.removeAttribute('data-theme');
            localStorage.setItem('smartcampus-theme', 'light');
        }
    }, [theme]);

    const handleThemeToggle = () => {
        setTheme(prev => (prev === 'dark' ? 'light' : 'dark'));
    };

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        setSubmitting(true);

        try {
            const res = await login(username, password);
            if (res.success) {
                if (res.role.toLowerCase() === 'student') navigate('/student-dashboard');
                else if (res.role.toLowerCase() === 'teacher') navigate('/teacher-dashboard');
                else if (res.role.toLowerCase() === 'admin') navigate('/admin-dashboard');
            } else {
                setError(res.message);
            }
        } catch (err) {
            setError('Something went wrong. Please check your connection and try again.');
        } finally {
            setSubmitting(false);
        }
    };

    const handleMouseMove = (e) => {
        if (!cardRef.current) return;
        const card = cardRef.current;
        const rect = card.getBoundingClientRect();
        const x = (e.clientX - rect.left) / rect.width;
        const y = (e.clientY - rect.top) / rect.height;

        const rotateX = (y - 0.5) * 2;
        const rotateY = (x - 0.5) * -2;

        card.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`;
    };

    const handleMouseLeave = () => {
        if (!cardRef.current) return;
        cardRef.current.style.transform = '';
    };

    return (
        <div 
            className="login-page-body"
            style={{ '--bg-image': `url("${enhancedCampus}")` }}
        >
            <div className="animated-bg">
                <div className="orb orb-1"></div>
                <div className="orb orb-2"></div>
                <div className="orb orb-3"></div>
            </div>

            {/* Weather Widget */}
            <div className="weather-widget">
                <div className="weather-info">
                    <svg className="weather-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/>
                        <path d="M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z"/>
                    </svg>
                    <div className="weather-details">
                        <span className="temp">28°C</span>
                        <span className="condition">Partly Cloudy</span>
                    </div>
                </div>
                <div className="location">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ width: '12px', height: '12px', marginRight: '4px', verticalAlign: 'middle' }}>
                        <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                        <circle cx="12" cy="10" r="3" />
                    </svg>
                    Chennai, India
                </div>
            </div>

            <button
                className="theme-toggle-btn"
                onClick={handleThemeToggle}
                title="Toggle dark/light mode"
                aria-label="Toggle theme"
            >
                <span>{theme === 'dark' ? '☀️' : '🌙'}</span>
            </button>

            <div className="container">
                <div
                    className="card"
                    ref={cardRef}
                    onMouseMove={handleMouseMove}
                    onMouseLeave={handleMouseLeave}
                >
                    <div className="left-section">
                        <div className="left-content">
                            <div className="logo-circle">🎓</div>
                            <h1>SMART CAMPUS</h1>
                            <p className="tagline">One Campus. Infinite Possibilities.</p>
                            <div className="glowing-divider"></div>

                            <div className="features">
                                <div className="feature">
                                    <div className="feature-dot"></div>
                                    <span>Intelligent Learning</span>
                                </div>
                                <div className="feature">
                                    <div className="feature-dot"></div>
                                    <span>Real-time Analytics</span>
                                </div>
                                <div className="feature">
                                    <div className="feature-dot"></div>
                                    <span>Secure Access</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="right-section">
                        <h2>Welcome Back!</h2>
                        <p className="welcome-subtext">Login to continue your smart journey</p>

                        {error && <div className="login-error-message">{error}</div>}

                        <form className="form" onSubmit={handleLogin}>
                            <div
                                className={`form-group ${emailFocused ? 'focused' : ''} ${
                                    username ? 'filled' : ''
                                }`}
                            >
                                <svg className="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <rect x="2" y="4" width="20" height="16" rx="2"></rect>
                                    <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"></path>
                                </svg>
                                <input
                                    type="email"
                                    value={username}
                                    onChange={(e) => setUsername(e.target.value)}
                                    onFocus={() => setEmailFocused(true)}
                                    onBlur={() => setEmailFocused(false)}
                                    required
                                    placeholder=" "
                                />
                                <label className="form-label">Email or Roll Number</label>
                                <div className="input-border"></div>
                            </div>

                            <div
                                className={`form-group ${passwordFocused ? 'focused' : ''} ${
                                    password ? 'filled' : ''
                                }`}
                            >
                                <svg className="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                    <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                                </svg>
                                <input
                                    type={showPassword ? 'text' : 'password'}
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    onFocus={() => setPasswordFocused(true)}
                                    onBlur={() => setPasswordFocused(false)}
                                    required
                                    placeholder=" "
                                />
                                <button
                                    type="button"
                                    className="password-toggle"
                                    onClick={() => setShowPassword(prev => !prev)}
                                >
                                    {showPassword ? '🙈' : '👁'}
                                </button>
                                <label className="form-label">Password</label>
                                <div className="input-border"></div>
                            </div>

                            <div className="options-row">
                                <label className="remember-me">
                                    <input type="checkbox" className="cyan-checkbox" />
                                    <span>Remember me</span>
                                </label>
                                <a href="#" className="forgot-password">Forgot Password?</a>
                            </div>

                            <button 
                                type="submit" 
                                className="submit-btn" 
                                disabled={submitting}
                                ref={btnRef}
                                onMouseMove={handleBtnMouseMove}
                                onMouseLeave={handleBtnMouseLeave}
                                onClick={handleButtonClick}
                            >
                                <span className="btn-text">{submitting ? 'Logging in...' : 'LOGIN'}</span>
                                <span className="btn-arrow-icon">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                        <path d="M9 18l6-6-6-6" />
                                    </svg>
                                </span>
                            </button>
                        </form>

                        <div className="social-divider">
                            <span className="line"></span>
                            <span className="divider-text">OR CONTINUE WITH</span>
                            <span className="line"></span>
                        </div>

                        <div className="social-buttons">
                            <button type="button" className="social-btn google-btn" title="Google">
                                <svg viewBox="0 0 24 24" fill="currentColor">
                                    <path d="M12.24 10.285V14.4h6.887c-.648 2.41-2.519 4.114-5.136 4.114-3.41 0-6.19-2.78-6.19-6.19s2.78-6.19 6.19-6.19c1.7 0 3.02.59 4.02 1.48l2.97-2.97C19.04 2.19 15.9 1 12.24 1c-6.07 0-11 4.93-11 11s4.93 11 11 11c5.77 0 10.42-4.13 11-9.715H12.24z"/>
                                </svg>
                            </button>
                            <button type="button" className="social-btn microsoft-btn" title="Microsoft">
                                <svg viewBox="0 0 23 23" fill="currentColor">
                                    <path d="M0 0h11v11H0z" fill="#f25022"/>
                                    <path d="M12 0h11v11H12z" fill="#7fba00"/>
                                    <path d="M0 12h11v11H0z" fill="#00a4ef"/>
                                    <path d="M12 12h11v11H12z" fill="#ffb900"/>
                                </svg>
                            </button>
                            <button type="button" className="social-btn portal-btn" title="Student Portal">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
                                </svg>
                            </button>
                        </div>

                        <div className="new-here">
                            <span>New here? </span>
                            <a href="#" className="create-account-link">Create an account</a>
                        </div>

                        <footer className="copyright">© 2026 College Management System</footer>
                    </div>
                </div>

                {/* Right Side Updates Panel */}
                <div className="updates-panel">
                    <div className="panel-header">
                        <svg className="panel-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0" />
                        </svg>
                        <h3>CAMPUS UPDATES</h3>
                    </div>
                    <div className="update-items">
                        <div className="update-item">
                            <div className="update-icon-wrapper">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                                    <line x1="16" y1="2" x2="16" y2="6"></line>
                                    <line x1="8" y1="2" x2="8" y2="6"></line>
                                    <line x1="3" y1="10" x2="21" y2="10"></line>
                                </svg>
                            </div>
                            <div className="update-info">
                                <span className="update-title">Tech Fest ’24</span>
                                <span className="update-date">Starts in 5 days</span>
                            </div>
                        </div>
                        <div className="update-item">
                            <div className="update-icon-wrapper">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"></path>
                                    <polyline points="12 6 12 12 16 14"></polyline>
                                </svg>
                            </div>
                            <div className="update-info">
                                <span className="update-title">Library Extended Hours</span>
                                <span className="update-date">Till 9:00 PM this week</span>
                            </div>
                        </div>
                        <div className="update-item">
                            <div className="update-icon-wrapper">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path>
                                </svg>
                            </div>
                            <div className="update-info">
                                <span className="update-title">Placement Drive</span>
                                <span className="update-date">TCS | 20th May 2024</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Bottom Floating Stats Panel */}
            <div className="stats-panel">
                <div className="stat-column">
                    <div className="stat-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 7a4 4 0 1 0 0-8 4 4 0 0 0 0 8z" />
                        </svg>
                    </div>
                    <div className="stat-text">
                        <span className="stat-value">2350+</span>
                        <span className="stat-label">Students</span>
                    </div>
                </div>
                <div className="stat-column">
                    <div className="stat-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 7a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
                        </svg>
                    </div>
                    <div className="stat-text">
                        <span className="stat-value">120+</span>
                        <span className="stat-label">Faculty</span>
                    </div>
                </div>
                <div className="stat-column">
                    <div className="stat-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M4 19.5A2.5 2.5 0 0 0 6.5 22H20M4 19.5V3.5A2.5 2.5 0 0 1 6.5 1V20" />
                        </svg>
                    </div>
                    <div className="stat-text">
                        <span className="stat-value">25+</span>
                        <span className="stat-label">Departments</span>
                    </div>
                </div>
                <div className="stat-column">
                    <div className="stat-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <polygon points="12 2 2 22 22 22"></polygon>
                        </svg>
                    </div>
                    <div className="stat-text">
                        <span className="stat-value">18+</span>
                        <span className="stat-label">Clubs</span>
                    </div>
                </div>
            </div>

            {/* Footer */}
            <div className="footer-panel">
                <span className="footer-item">© 2024 Smart Campus. All rights reserved.</span>
                <span className="footer-item center">
                    <svg className="shield-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ width: '14px', height: '14px', marginRight: '6px', verticalAlign: 'middle' }}>
                        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                    </svg>
                    Secured with 256-bit encryption
                </span>
                <span className="footer-item right">
                    Designed for a Smarter Tomorrow
                    <svg className="heart-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ width: '14px', height: '14px', marginLeft: '6px', verticalAlign: 'middle' }}>
                        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                    </svg>
                </span>
            </div>
        </div>
    );
}
