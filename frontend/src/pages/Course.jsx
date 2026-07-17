import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import DashboardLayout from '../components/DashboardLayout';
import './Course.css';

export default function Course() {
    const { courseCode } = useParams();
    const { user } = useAuth();
    const [course, setCourse] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [activeTab, setActiveTab] = useState('resources');

    // Comic learning states
    const [selectedMaterialId, setSelectedMaterialId] = useState('');
    const [manualNotes, setManualNotes] = useState('');
    const [userQuery, setUserQuery] = useState('');
    const [studentLevel, setStudentLevel] = useState('BEGINNER');
    const [panelCount, setPanelCount] = useState(4);
    const [comicGenerating, setComicGenerating] = useState(false);
    const [comicPanels, setComicPanels] = useState([]);
    const [comicError, setComicError] = useState('');
    const sseRef = useRef(null);

    // Assignment upload simulation states
    const [uploadingAssignmentId, setUploadingAssignmentId] = useState(null);
    const [uploadFileName, setUploadFileName] = useState('');
    const [uploadSuccess, setUploadSuccess] = useState('');

    useEffect(() => {
        const fetchCourseDetails = async () => {
            try {
                const response = await fetch(`/api/student/course/${courseCode}`);
                if (response.ok) {
                    const data = await response.json();
                    setCourse(data);
                } else {
                    const data = await response.json();
                    setError(data.message || 'Failed to fetch course details.');
                }
            } catch (err) {
                console.error(err);
                setError('Network error. Failed to load course.');
            } finally {
                setLoading(false);
            }
        };

        fetchCourseDetails();
    }, [courseCode]);

    // Handle assignment upload simulation
    const handleAssignmentUpload = (e, assignmentId) => {
        const file = e.target.files[0];
        if (file) {
            setUploadingAssignmentId(assignmentId);
            setUploadFileName(file.name);
            setUploadSuccess('');
            
            // Simulate API upload delay
            setTimeout(() => {
                setCourse(prev => {
                    const updatedAssignments = prev.assignments.map(a => {
                        if (a.id === assignmentId) {
                            return {
                                ...a,
                                submitted: true,
                                submissionOriginalFileName: file.name,
                                submittedAt: new Date().toISOString()
                            };
                        }
                        return a;
                    });
                    return { ...prev, assignments: updatedAssignments };
                });
                setUploadSuccess(`Successfully submitted "${file.name}"!`);
                setUploadingAssignmentId(null);
            }, 1500);
        }
    };

    // Handle Comic generation streaming
    const handleGenerateComic = async (e) => {
        e.preventDefault();
        if (!userQuery) {
            setComicError('Please specify what you want to learn.');
            return;
        }

        setComicGenerating(true);
        setComicPanels([]);
        setComicError('');

        try {
            const response = await fetch(`/course/${courseCode}/comic-learning/stream`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    materialId: selectedMaterialId ? parseInt(selectedMaterialId) : null,
                    materialContent: manualNotes || null,
                    userQuery,
                    studentLevel,
                    panelCount
                })
            });

            if (!response.ok) {
                const errData = await response.json();
                setComicError(errData.message || 'Generation failed.');
                setComicGenerating(false);
                return;
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let rawBuffer = '';

            while (true) {
                const { value, done } = await reader.read();
                if (done) break;

                rawBuffer += decoder.decode(value, { stream: true });
                const lines = rawBuffer.split('\n');
                rawBuffer = lines.pop(); // keep partial line

                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        try {
                            const dataStr = line.slice(5).trim();
                            const data = JSON.parse(dataStr);
                            
                            if (data.panels) {
                                setComicPanels(data.panels);
                            } else if (data.message) {
                                setComicError(data.message);
                            }
                        } catch (err) {
                            // Ignore parsing errors of incomplete JSON frames
                        }
                    } else if (line.startsWith('event: error')) {
                        setComicError('Error during generation stream.');
                    }
                }
            }
        } catch (err) {
            console.error(err);
            setComicError('Network error. Failed to stream comic learning.');
        } finally {
            setComicGenerating(false);
        }
    };

    return (
        <DashboardLayout role="student">
            <div className="course-page-content">
                {error && <div className="error-banner">{error}</div>}

                {loading ? (
                    <div className="loading-state">
                        <h2>Loading course details...</h2>
                    </div>
                ) : (
                    course && (
                        <>
                            {/* Course Header */}
                            <div className="glass course-header">
                                <div className="course-label">{course.courseCode}</div>
                                <h1 className="course-title">{course.courseName}</h1>
                                <div className="course-meta">
                                    Enrolled under Department: <span>{user?.department || 'Computer Science'}</span>
                                </div>

                                {/* Tabs Navigation */}
                                <div className="tabs">
                                    {[
                                        { id: 'resources', label: 'Resources' },
                                        { id: 'assignments', label: 'Assignments' },
                                        { id: 'quizzes', label: 'Quizzes & Exams' },
                                        { id: 'comic', label: 'AI Comic Learning 🪄' }
                                    ].map(t => (
                                        <button
                                            key={t.id}
                                            className={`tab ${activeTab === t.id ? 'active' : ''}`}
                                            onClick={() => setActiveTab(t.id)}
                                        >
                                            <span>{t.label}</span>
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* Resources Panel */}
                            {activeTab === 'resources' && (
                                <div className="glass panel">
                                    <div className="section-title">
                                        <span className="dot"></span> Study Materials
                                    </div>
                                    {course.materials?.length > 0 ? (
                                        <div className="table-wrap">
                                            <table className="table">
                                                <thead>
                                                    <tr>
                                                        <th>Title</th>
                                                        <th>Module</th>
                                                        <th>Type</th>
                                                        <th>Size</th>
                                                        <th>Date Uploaded</th>
                                                        <th>Action</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {course.materials.map(m => (
                                                        <tr key={m.id}>
                                                            <td><strong>{m.title}</strong></td>
                                                            <td>{m.module || 'General'}</td>
                                                            <td><span className="row-type">{m.type}</span></td>
                                                            <td>{m.fileSize ? `${(m.fileSize / 1024).toFixed(1)} KB` : 'N/A'}</td>
                                                            <td>{m.uploadedAt ? new Date(m.uploadedAt).toLocaleDateString() : '-'}</td>
                                                            <td>
                                                                <a href={`/material/${m.id}/download`} className="btn btn-download" target="_blank" rel="noopener noreferrer">
                                                                    Download
                                                                </a>
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    ) : (
                                        <div className="empty">
                                            <p>No study materials uploaded for this course yet.</p>
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* Assignments Panel */}
                            {activeTab === 'assignments' && (
                                <div className="glass panel">
                                    <div className="section-title">
                                        <span className="dot"></span> Assignments
                                    </div>
                                    {uploadSuccess && <div className="success-banner">{uploadSuccess}</div>}
                                    {course.assignments?.length > 0 ? (
                                        <div className="table-wrap">
                                            <table className="table">
                                                <thead>
                                                    <tr>
                                                        <th>Title</th>
                                                        <th>Due Date</th>
                                                        <th>Max Marks</th>
                                                        <th>Status</th>
                                                        <th>Submission File</th>
                                                        <th>Action</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {course.assignments.map(a => (
                                                        <tr key={a.id}>
                                                            <td>
                                                                <strong>{a.title}</strong>
                                                                {a.assignmentMode === 'FILE' && (
                                                                    <div style={{ marginTop: '4px' }}>
                                                                        <a href={`/assignment/${a.id}/download`} target="_blank" rel="noopener noreferrer" style={{ fontSize: '12px', color: '#ec4899', textDecoration: 'none', fontWeight: '500' }}>
                                                                            📄 Download Question File ({a.originalFileName || 'File'})
                                                                        </a>
                                                                    </div>
                                                                )}
                                                            </td>
                                                            <td>{a.dueAt ? new Date(a.dueAt).toLocaleString() : 'No due date'}</td>
                                                            <td>{a.maxMarks || 'N/A'}</td>
                                                            <td>
                                                                <span className={`badge ${a.submitted ? 'badge--live' : 'badge--closed'}`}>
                                                                    {a.submitted ? 'Submitted' : 'Pending'}
                                                                </span>
                                                            </td>
                                                            <td>{a.submissionOriginalFileName || '-'}</td>
                                                            <td>
                                                                {a.submitted ? (
                                                                    <span className="mini-note">Completed</span>
                                                                ) : a.submissionClosed ? (
                                                                    <span className="badge badge--closed">Closed</span>
                                                                ) : (
                                                                    <div className="assignment-upload">
                                                                        <input
                                                                            type="file"
                                                                            onChange={(e) => handleAssignmentUpload(e, a.id)}
                                                                            disabled={uploadingAssignmentId === a.id}
                                                                        />
                                                                        {uploadingAssignmentId === a.id && <span className="mini-note">Uploading...</span>}
                                                                    </div>
                                                                )}
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    ) : (
                                        <div className="empty">
                                            <p>No assignments issued for this course yet.</p>
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* Quizzes Panel */}
                            {activeTab === 'quizzes' && (
                                <div className="glass panel">
                                    <div className="section-title">
                                        <span className="dot"></span> Active Quizzes & Online Exams
                                    </div>
                                    {course.quizzes?.length > 0 ? (
                                        <div className="table-wrap">
                                            <table className="table">
                                                <thead>
                                                    <tr>
                                                        <th>Quiz Title</th>
                                                        <th>Start Time</th>
                                                        <th>End Time</th>
                                                        <th>Status</th>
                                                        <th>Score</th>
                                                        <th>Action</th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {course.quizzes.map(q => (
                                                        <tr key={q.quizId}>
                                                            <td><strong>{q.title}</strong></td>
                                                            <td>{q.startAt ? new Date(q.startAt).toLocaleString() : '-'}</td>
                                                            <td>{q.endAt ? new Date(q.endAt).toLocaleString() : '-'}</td>
                                                            <td>
                                                                <span className={`badge ${
                                                                    q.status === 'ACTIVE' ? 'badge--live' :
                                                                    q.status === 'UPCOMING' ? 'badge--upcoming' : 'badge--closed'
                                                                }`}>
                                                                    {q.status}
                                                                </span>
                                                            </td>
                                                            <td>
                                                                {q.submitted ? (
                                                                    q.scorePublished ? `Score: ${q.score}` : 'Submitted (Awaiting scores)'
                                                                ) : '-'}
                                                            </td>
                                                            <td>
                                                                {q.submitted ? (
                                                                    <span className="mini-note">Completed</span>
                                                                ) : q.status === 'ACTIVE' ? (
                                                                    <Link to={`/student-quiz/${q.quizId}`} className="btn btn--accent">
                                                                        Take Quiz
                                                                    </Link>
                                                                ) : (
                                                                    <span className="mini-note">Unavailable</span>
                                                                )}
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    ) : (
                                        <div className="empty">
                                            <p>No quizzes assigned for this course yet.</p>
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* Comic Learning Panel */}
                            {activeTab === 'comic' && (
                                <div className="glass panel">
                                    <div className="comic-shell">
                                        <div className="comic-card comic-form-card">
                                            <div className="comic-kicker">AI Learning</div>
                                            <h2 className="comic-form-title">Explain via Comic</h2>
                                            <p className="comic-form-copy">Generate visual comic panels explaining complex concepts directly from your lecture notes or handouts.</p>
                                            
                                            <form onSubmit={handleGenerateComic} className="comic-form">
                                                <div className="comic-field">
                                                    <label className="comic-label">Select Resource</label>
                                                    <select
                                                        className="comic-select"
                                                        value={selectedMaterialId}
                                                        onChange={(e) => setSelectedMaterialId(e.target.value)}
                                                    >
                                                        <option value="">-- Optional: Choose Study File --</option>
                                                        {course.materials?.map(m => (
                                                            <option key={m.id} value={m.id}>{m.title}</option>
                                                        ))}
                                                    </select>
                                                </div>

                                                <div className="comic-field">
                                                    <label className="comic-label">Additional Custom Notes</label>
                                                    <textarea
                                                        className="comic-textarea"
                                                        placeholder="Paste your specific definitions or additional lecture notes here..."
                                                        value={manualNotes}
                                                        onChange={(e) => setManualNotes(e.target.value)}
                                                    />
                                                </div>

                                                <div className="comic-field">
                                                    <label className="comic-label">What to explain?</label>
                                                    <input
                                                        type="text"
                                                        className="comic-input"
                                                        placeholder="e.g. explain time complexity of Bubble Sort"
                                                        value={userQuery}
                                                        onChange={(e) => setUserQuery(e.target.value)}
                                                        required
                                                    />
                                                </div>

                                                <div className="comic-grid-2">
                                                    <div className="comic-field">
                                                        <label className="comic-label">Difficulty</label>
                                                        <select
                                                            className="comic-select"
                                                            value={studentLevel}
                                                            onChange={(e) => setStudentLevel(e.target.value)}
                                                        >
                                                            <option value="BEGINNER">Beginner</option>
                                                            <option value="INTERMEDIATE">Intermediate</option>
                                                            <option value="ADVANCED">Advanced</option>
                                                        </select>
                                                    </div>
                                                    <div className="comic-field">
                                                        <label className="comic-label">Panels Count</label>
                                                        <select
                                                            className="comic-select"
                                                            value={panelCount}
                                                            onChange={(e) => setPanelCount(parseInt(e.target.value))}
                                                        >
                                                            <option value={2}>2 Panels</option>
                                                            <option value={4}>4 Panels</option>
                                                            <option value={6}>6 Panels</option>
                                                        </select>
                                                    </div>
                                                </div>

                                                <button
                                                    type="submit"
                                                    className="btn btn--comic"
                                                    disabled={comicGenerating}
                                                >
                                                    {comicGenerating ? 'Generating comic...' : 'Generate AI Comic ⚡'}
                                                </button>
                                            </form>
                                        </div>

                                        <div className="comic-display-area">
                                            {comicError && <div className="error-banner">{comicError}</div>}
                                            {comicGenerating && comicPanels.length === 0 && (
                                                <div className="loading-state">
                                                    <div className="avatar-glow pulse-animation"></div>
                                                    <h3>Contacting AI Generator... Please wait...</h3>
                                                </div>
                                            )}
                                            {comicPanels.length > 0 ? (
                                                <div className="comic-panels-grid">
                                                    {comicPanels.map((p, idx) => (
                                                        <div key={idx} className="panel-node">
                                                            <div className="panel-image-wrap">
                                                                <img
                                                                    src={`/comic-learning/generated-image?path=${encodeURIComponent(p.image_path)}`}
                                                                    alt={`Panel ${idx + 1}`}
                                                                    className="panel-img"
                                                                />
                                                                <span className="panel-num">#{idx + 1}</span>
                                                            </div>
                                                            <div className="panel-narrative">{p.narrative}</div>
                                                        </div>
                                                    ))}
                                                </div>
                                            ) : (
                                                !comicGenerating && (
                                                    <div className="comic-empty-display">
                                                        <div className="display-glow"></div>
                                                        <div className="display-inner">
                                                            <span>🎨</span>
                                                            <h3>Visual AI Comic Output</h3>
                                                            <p>Your generated comic strips, panels, and dialogues will stream here in real time once generated.</p>
                                                        </div>
                                                    </div>
                                                )
                                            )}
                                        </div>
                                    </div>
                                </div>
                            )}
                        </>
                    )
                )}
            </div>
        </DashboardLayout>
    );
}
