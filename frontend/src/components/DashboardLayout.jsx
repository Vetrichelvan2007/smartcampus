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
    ];

    // Teacher Navigation Links
    const teacherLinks = [
        { path: '/teacher-dashboard', label: 'Dashboard', icon: '📊' },
        { path: '/teacher-profile', label: 'Profile', icon: '👤' },
    ];

    const navLinks = role === 'student' ? studentLinks : teacherLinks;

    return (
        <div className="dashboard-layout-body">
            {/* Ambient Background Orbs */}
            <div className="animated-bg">
                <div className="orb orb-1"></div>
                <div className="orb orb-2"></div>
                <div className="orb orb-3"></div>
            </div>

            {/* Top Bar */}
            <header className="topbar">
                <div className="topbar-brand" onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>
                    <div className="icon">🎓</div>
                    <strong>SMARTCAMPUS</strong>
                </div>

                <div className="search-wrapper">
                    <input
                        type="text"
                        placeholder="Search..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>

                <div className="topbar-right">
                    <div className="profile" onClick={() => setMenuOpen(!menuOpen)}>
                        <div className="avatar">
                            {user && user.name ? user.name.charAt(0).toUpperCase() : 'U'}
                        </div>

                        {menuOpen && (
                            <div className="profile-menu">
                                <div className="profile-head">
                                    <strong>{user ? user.name : 'User'}</strong>
                                    <small>{user ? user.email : ''}</small>
                                </div>
                                <Link to={role === 'student' ? '/student-profile' : '/teacher-profile'}>Profile</Link>
                                <button className="logout-menu-btn" onClick={handleLogout}>Logout</button>
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
