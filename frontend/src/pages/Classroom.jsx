import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import './Classroom.css';

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

    return (
        <DashboardLayout role="student">
            <div className="classroom-page-content">
                {error && <div className="error-banner">{error}</div>}

                {loading ? (
                    <div className="loading-state">
                        <h2>Loading Classroom...</h2>
                    </div>
                ) : (
                    <div className="layout">
                        {/* Semester Selector Sidebar */}
                        <aside className="sidebar">
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
                                            onClick={() => setSelectedSem(sem)}
                                            className={`sem-btn ${isActive ? 'active' : ''}`}
                                        >
                                            <span className="sem-num">{sem}</span>
                                            <span>Semester {sem}</span>
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
                                    <input
                                        type="text"
                                        placeholder="Search courses..."
                                        value={searchQuery}
                                        onChange={(e) => setSearchQuery(e.target.value)}
                                        className="classroom-search-input"
                                    />
                                    <span className="course-count">
                                        {filteredCourses.length} courses
                                    </span>
                                </div>
                            </div>

                            <div className="course-grid">
                                {filteredCourses.map((course, i) => {
                                    const typeClass = course.courseType ? course.courseType.toLowerCase() : '';
                                    return (
                                        <Link
                                            key={course.courseCode}
                                            to={`/student-classroom/${course.courseCode}`}
                                            className="course-card show"
                                            style={{ animationDelay: `${i * 0.05}s`, textDecoration: 'none' }}
                                        >
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
                                    <div className="empty-state">
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
