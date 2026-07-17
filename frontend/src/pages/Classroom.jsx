import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import './Classroom.css';

// Import background images for matching course cards
import webtechBg from '../assets/webtech.png';
import databaseBg from '../assets/database.png';
import fullstackBg from '../assets/fullstack.png';

export default function Classroom() {
    const { user } = useAuth();
    const [courses, setCourses] = useState([]);
    const [currentSem, setCurrentSem] = useState(1);
    const [selectedSem, setSelectedSem] = useState(1);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        const fetchClassroomData = async () => {
            try {
                const response = await fetch('/api/student/classroom');
                if (response.ok) {
                    const data = await response.json();
                    setCourses(data.courseDatas || []);
                    setCurrentSem(data.currentSem || 1);
                    setSelectedSem(data.currentSem || 1);
                } else {
                    const data = await response.json();
                    setError(data.message || 'Failed to fetch classroom data.');
                }
            } catch (err) {
                console.error(err);
                setError('Network error. Failed to load classroom details.');
            } finally {
                setLoading(false);
            }
        };

        fetchClassroomData();
    }, []);

    // Filter courses by semester AND search query
    const filteredCourses = courses.filter((course) => {
        const matchesSem = course.courseSem === selectedSem;
        const matchesQuery =
            course.courseName.toLowerCase().includes(searchQuery.toLowerCase()) ||
            course.courseCode.toLowerCase().includes(searchQuery.toLowerCase());
        return matchesSem && matchesQuery;
    });

    /* ── Coordinate Tracker for Pixel Hover grid ── */
    const handleCardMouseMove = (e) => {
        const card = e.currentTarget;
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        card.style.setProperty('--mouse-x', `${x}px`);
        card.style.setProperty('--mouse-y', `${y}px`);
    };

    /* ── Magnetic Hover on Semester Buttons ── */
    const handleBtnMouseMove = (e) => {
        const btn = e.currentTarget;
        const rect = btn.getBoundingClientRect();
        const x = e.clientX - (rect.left + rect.width / 2);
        const y = e.clientY - (rect.top + rect.height / 2);
        const dist = Math.sqrt(x * x + y * y);
        if (dist < 100) {
            btn.style.transform = `translate(${x * 0.12}px, ${y * 0.12}px)`;
        } else {
            btn.style.transform = '';
        }
    };

    const handleBtnMouseLeave = (e) => {
        e.currentTarget.style.transform = '';
    };

    /* ── Click Ripple Trigger ── */
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

    return (
        <DashboardLayout role="student">
            <div className="classroom-page-content">
                
                {/* Ambient Background Glow Layer */}
                <div className="ambient-orbs" aria-hidden="true">
                    <div className="orb orb-purple" />
                    <div className="orb orb-cyan"   />
                    <div className="orb orb-emerald"/>
                </div>

                {error && (
                    <div className="error-banner card">
                        <span className="error-icon">⚠️</span>
                        <div>
                            <strong>Connection Failure</strong>
                            <p>{error}</p>
                        </div>
                    </div>
                )}

                {loading ? (
                    <div className="loading-state">
                        <div className="spinner-glow"></div>
                        <h2>Decrypting Classroom Data...</h2>
                    </div>
                ) : (
                    <div className="layout">
                        {/* Semester Selector Sidebar */}
                        <aside className="sidebar card" onMouseMove={handleCardMouseMove}>
                            <div className="pixel-grid-hover" aria-hidden="true"></div>
                            <div className="sidebar-header">
                                <h3>Semester</h3>
                            </div>
                            <div className="sem-list">
                                {Array.from({ length: currentSem }, (_, index) => {
                                    const sem = index + 1;
                                    const isActive = selectedSem === sem;
                                    return (
                                        <button
                                            key={sem}
                                            onClick={(e) => { handleButtonClick(e); setSelectedSem(sem); }}
                                            onMouseMove={handleBtnMouseMove}
                                            onMouseLeave={handleBtnMouseLeave}
                                            className={`sem-btn ${isActive ? 'active' : ''}`}
                                        >
                                            <span className="sem-num">{sem}</span>
                                            <span>Semester {sem}</span>
                                            <span className="sem-arrow">→</span>
                                        </button>
                                    );
                                })}
                            </div>
                        </aside>

                        {/* Courses Grid */}
                        <section className="content">
                            <div className="content-header">
                                <span className="content-title">Courses</span>
                                <div className="classroom-header-right">
                                    <div className="search-wrap">
                                        <svg className="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <circle cx="11" cy="11" r="8"></circle>
                                            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                                        </svg>
                                        <input
                                            type="text"
                                            placeholder="Search courses..."
                                            value={searchQuery}
                                            onChange={(e) => setSearchQuery(e.target.value)}
                                            className="classroom-search-input"
                                        />
                                    </div>
                                    <span className="course-count">
                                        <span className="pulse-dot"></span>
                                        {filteredCourses.length} courses
                                    </span>
                                </div>
                            </div>

                            <div className="course-grid">
                                {filteredCourses.map((course, i) => {
                                    const typeClass = course.courseType ? course.courseType.toLowerCase() : '';
                                    
                                    // Determine theme background image based on course title
                                    let bgImage = '';
                                    const name = course.courseName.toLowerCase();
                                    if (name.includes('web programming')) {
                                        bgImage = webtechBg;
                                    } else if (name.includes('database management systems')) {
                                        bgImage = databaseBg;
                                    } else if (name.includes('full stack development')) {
                                        bgImage = fullstackBg;
                                    }

                                    const style = {
                                        animationDelay: `${i * 0.05}s`,
                                        textDecoration: 'none'
                                    };

                                    if (bgImage) {
                                        style.backgroundImage = `
                                            linear-gradient(135deg, rgba(4, 10, 25, 0.72) 0%, rgba(16, 25, 53, 0.72) 100%),
                                            linear-gradient(135deg, rgba(0, 229, 255, 0.12) 0%, rgba(124, 77, 255, 0.12) 100%),
                                            url(${bgImage})
                                        `;
                                        style.backgroundSize = 'cover';
                                        style.backgroundPosition = 'center';
                                    }

                                    return (
                                        <Link
                                            key={course.courseCode}
                                            to={`/student-classroom/${course.courseCode}`}
                                            className={`course-card show ${bgImage ? 'has-bg' : ''}`}
                                            style={style}
                                            onMouseMove={handleCardMouseMove}
                                        >
                                            <div className="pixel-grid-hover" aria-hidden="true"></div>
                                            <div className="card-overlay" aria-hidden="true"></div>
                                            <div className="card-meta">
                                                <span className={`badge ${typeClass}`}>
                                                    {course.courseType}
                                                </span>
                                            </div>
                                            <h3>{course.courseName}</h3>
                                            <div className="code">{course.courseCode}</div>
                                        </Link>
                                    );
                                })}

                                {filteredCourses.length === 0 && (
                                    <div className="empty-state card">
                                        <div className="empty-illustration">📭</div>
                                        <p>No courses found for Semester {selectedSem}</p>
                                    </div>
                                )}
                            </div>
                        </section>
                    </div>
                )}
            </div>
        </DashboardLayout>
    );
}
