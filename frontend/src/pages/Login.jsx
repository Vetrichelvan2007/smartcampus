import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Login.css';

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
        <div className="login-page-body">
            <div className="animated-bg">
                <div className="orb orb-1"></div>
                <div className="orb orb-2"></div>
                <div className="orb orb-3"></div>
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
                            <p>Smart learning. Smart management.</p>

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
                        <h2>Welcome Back</h2>
                        <p>Access your dashboard</p>

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
                                <label className="form-label">Email</label>
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

                            <button type="submit" className="submit-btn" disabled={submitting}>
                                {submitting ? 'Logging in...' : 'Login'}
                            </button>
                        </form>

                        <footer className="copyright">© 2026 College Management System</footer>
                    </div>
                </div>
            </div>
        </div>
    );
}
