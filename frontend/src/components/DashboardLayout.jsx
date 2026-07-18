import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './DashboardLayout.css';

export default function DashboardLayout({ children, role }) {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();
    const [menuOpen, setMenuOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');

    const handleLogout = async () => {
        const success = await logout();
        if (success) {
            navigate('/login');
        }
    };

    // Auto-close menu on location change
    useEffect(() => {
        setMenuOpen(false);
    }, [location.pathname]);

    // Student Navigation Links
    const studentLinks = [
        { path: '/student-dashboard', label: 'Dashboard', icon: '📊' },
        { path: '/student-classroom', label: 'Classroom', icon: '🏫' },
        { path: '/student-calendar', label: 'Calendar', icon: '📅' },
        { path: '/student-courses', label: 'Registration', icon: '✍️' },
        { path: '/student-feedback', label: 'Feedback', icon: '📝' },
        { path: '/student-profile', label: 'Profile', icon: '👤' },
        { path: '/logout', label: 'Logout', icon: '👤' },
    ];

    // Teacher Navigation Links
    const teacherLinks = [
        { path: '/teacher-dashboard', label: 'Dashboard', icon: '📊' },
        { path: '/teacher-profile', label: 'Profile', icon: '👤' },
    ];

    const navLinks = role === 'student' ? studentLinks : teacherLinks;

    /* ── Root Page Parallax + Spotlight position updater ── */
    const handleLayoutMouseMove = (e) => {
        const x = (window.innerWidth / 2 - e.clientX) / 45;
        const y = (window.innerHeight / 2 - e.clientY) / 45;
        
        // update CSS variables for parallax and spotlight
        const root = e.currentTarget;
        root.style.setProperty('--parallax-x', `${x}px`);
        root.style.setProperty('--parallax-y', `${y}px`);
        
        const rect = root.getBoundingClientRect();
        const px = e.clientX - rect.left;
        const py = e.clientY - rect.top;
        root.style.setProperty('--spotlight-x', `${px}px`);
        root.style.setProperty('--spotlight-y', `${py}px`);
    };

    /* ── Magnetic link hover ── */
    const handleLinkMouseMove = (e) => {
        const link = e.currentTarget;
        const rect = link.getBoundingClientRect();
        const x = e.clientX - (rect.left + rect.width / 2);
        const y = e.clientY - (rect.top + rect.height / 2);
        
        // Translate link element slightly
        link.style.transform = `translate(${x * 0.12}px, ${y * 0.12}px)`;
    };

    const handleLinkMouseLeave = (e) => {
        e.currentTarget.style.transform = '';
    };

    /* ── Sidebar Link Ripple ── */
    const handleLinkClick = (e) => {
        const link = e.currentTarget;
        const old = link.querySelector('.ripple');
        if (old) old.remove();

        const circle = document.createElement('span');
        const diameter = Math.max(link.clientWidth, link.clientHeight);
        const radius = diameter / 2;
        const rect = link.getBoundingClientRect();

        circle.style.width  = circle.style.height = `${diameter}px`;
        circle.style.left   = `${e.clientX - rect.left - radius}px`;
        circle.style.top    = `${e.clientY - rect.top  - radius}px`;
        circle.classList.add('ripple');
        link.appendChild(circle);
    };

    return (
        <div className="dashboard-layout-body" onMouseMove={handleLayoutMouseMove}>
            
            {/* Spotlight spotlight element inside layout */}
            <div className="layout-spotlight" aria-hidden="true"></div>

            {/* Ambient Background Layers */}
            <div className="animated-bg">
                {/* Hexagonal grid overlay */}
                <div className="hex-grid"></div>

                {/* Base Orbs (drifting with parallax transform logic) */}
                <div className="orb orb-1"></div>
                <div className="orb orb-2"></div>
                <div className="orb orb-3"></div>
                
                {/* Holographic background circles */}
                <div className="holo-bg-circle circle-1"></div>
                <div className="holo-bg-circle circle-2"></div>

                {/* Glowing network lines */}
                <div className="network-lines">
                    <div className="line line-1"></div>
                    <div className="line line-2"></div>
                    <div className="line line-3"></div>
                </div>

                {/* Floating neon particles (dust/sparks) */}
                <div className="particles-container">
                    {/* Generates multiple particles using CSS box-shadow in stylesheet */}
                    <div className="particle-layer layer-1"></div>
                    <div className="particle-layer layer-2"></div>
                    <div className="particle-layer layer-3"></div>
                </div>

                {/* Animated wave particles across lower section */}
                <div className="wave-particles">
                    <div className="wave wave-1"></div>
                    <div className="wave wave-2"></div>
                </div>
            </div>

            {/* Top Bar */}
            <header className="topbar">
                {/* Scan line effect overlay on topbar */}
                <div className="topbar-scanner" aria-hidden="true"></div>

                <div className="topbar-brand" onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>
                    <div className="icon">🎓</div>
                    <strong>SMARTCAMPUS</strong>
                </div>

                <div className="search-wrapper">
                    <svg className="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <circle cx="11" cy="11" r="8"></circle>
                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                    <input
                        type="text"
                        placeholder="Search..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>

                <div className="topbar-right">
                    <div className="profile" onClick={() => setMenuOpen(!menuOpen)}>
                        <div className="avatar-ring-container">
                            <div className="avatar-gradient-ring"></div>
                            <div className="avatar">
                                {user && user.name ? user.name.charAt(0).toUpperCase() : 'U'}
                            </div>
                        </div>

                        {menuOpen && (
                            <div className="profile-menu" onClick={(e) => e.stopPropagation()}>
                                <div className="profile-head">
                                    <strong>{user ? user.name : 'User'}</strong>
                                    <small>{user ? user.email : ''}</small>
                                </div>
                                <Link 
                                    to={role === 'student' ? '/student-profile' : '/teacher-profile'}
                                    onClick={() => setMenuOpen(false)}
                                >
                                    Profile
                                </Link>
                                <button 
                                    className="logout-menu-btn" 
                                    onClick={(e) => {
                                        setMenuOpen(false);
                                        handleLogout();
                                    }}
                                >
                                    Logout
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            </header>

            {/* Layout Body */}
            <div className="layout">
                {/* Sidebar Navigation */}
                <aside className="sidebar">
                    <div className="sidebar-header">
                        <h3>Navigation</h3>
                    </div>

                    <nav className="sidebar-nav">
                        {navLinks.map((link) => {
                            const isActive = location.pathname === link.path;
                            return (
                                <Link
                                    key={link.path}
                                    to={link.path}
                                    className={`sidebar-link ${isActive ? 'active' : ''}`}
                                    onMouseMove={handleLinkMouseMove}
                                    onMouseLeave={handleLinkMouseLeave}
                                    onClick={(e) => {
                                        if (link.path === '/logout') {
                                            e.preventDefault();
                                            handleLogout();
                                        } else {
                                            handleLinkClick(e);
                                        }
                                    }}
                                >
                                    <span className="link-icon">{link.icon}</span>
                                    <span>{link.label}</span>
                                </Link>
                            );
                        })}
                    </nav>
                </aside>

                {/* Main Content Area */}
                <main className="content-container">
                    {children}
                </main>
            </div>
        </div>
    );
}
