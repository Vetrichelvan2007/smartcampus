import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Welcome from './pages/Welcome';
import Login from './pages/Login';
import StudentDashboard from './pages/StudentDashboard';
import Classroom from './pages/Classroom';
import Course from './pages/Course';
import TakeQuiz from './pages/TakeQuiz';
import Calendar from './pages/Calendar';
import CourseRegistration from './pages/CourseRegistration';
import Feedback from './pages/Feedback';
import StudentProfile from './pages/StudentProfile';
import TeacherDashboard from './pages/TeacherDashboard';
import TeacherCourse from './pages/TeacherCourse';
import TeacherProfile from './pages/TeacherProfile';
import AdminDashboard from './pages/AdminDashboard';
import './App.css';

// ProtectedRoute helper component
const ProtectedRoute = ({ children, allowedRoles }) => {
    const { user, loading } = useAuth();

    if (loading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', background: '#0a0e27', color: 'white' }}>
                <h2>Loading smart campus...</h2>
            </div>
        );
    }

    if (!user || !user.authenticated) {
        return <Navigate to="/login" replace />;
    }

    if (allowedRoles && !allowedRoles.includes(user.role.toLowerCase())) {
        // Redirect to their own role's dashboard if they are logged in but unauthorized for this route
        if (user.role.toLowerCase() === 'student') return <Navigate to="/student-dashboard" replace />;
        if (user.role.toLowerCase() === 'teacher') return <Navigate to="/teacher-dashboard" replace />;
        if (user.role.toLowerCase() === 'admin') return <Navigate to="/admin-dashboard" replace />;
        return <Navigate to="/login" replace />;
    }

    return children;
};

function AppRoutes() {
    return (
        <Router>
            <Routes>
                {/* Public routes */}
                <Route path="/" element={<Welcome />} />
                <Route path="/login" element={<Login />} />

                {/* Protected dashboards */}
                <Route
                    path="/student-dashboard"
                    element={
                        <ProtectedRoute allowedRoles={['student']}>
                            <StudentDashboard />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/student-classroom"
                    element={
                        <ProtectedRoute allowedRoles={['student']}>
                            <Classroom />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/student-classroom/:courseCode"
                    element={
                        <ProtectedRoute allowedRoles={['student']}>
                            <Course />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/student-quiz/:quizId"
                    element={
                        <ProtectedRoute allowedRoles={['student']}>
                            <TakeQuiz />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/student-calendar"
                    element={
                        <ProtectedRoute allowedRoles={['student']}>
                            <Calendar />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/student-courses"
                    element={
                        <ProtectedRoute allowedRoles={['student']}>
                            <CourseRegistration />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/student-feedback"
                    element={
                        <ProtectedRoute allowedRoles={['student']}>
                            <Feedback />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/student-profile"
                    element={
                        <ProtectedRoute allowedRoles={['student']}>
                            <StudentProfile />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/teacher-dashboard"
                    element={
                        <ProtectedRoute allowedRoles={['teacher']}>
                            <TeacherDashboard />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/teacher-classroom/:courseId"
                    element={
                        <ProtectedRoute allowedRoles={['teacher']}>
                            <TeacherCourse />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/teacher-profile"
                    element={
                        <ProtectedRoute allowedRoles={['teacher']}>
                            <TeacherProfile />
                        </ProtectedRoute>
                    }
                />
                <Route
                    path="/admin-dashboard"
                    element={
                        <ProtectedRoute allowedRoles={['admin']}>
                            <AdminDashboard />
                        </ProtectedRoute>
                    }
                />

                {/* Redirects */}
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </Router>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <AppRoutes />
        </AuthProvider>
    );
}
