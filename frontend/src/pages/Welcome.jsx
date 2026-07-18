import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Welcome.css';
import recCampus from '../assets/rec-campus.jpg';

export default function Welcome() {
    const navigate = useNavigate();
    const [progress, setProgress] = useState(0);
    const [exit, setExit] = useState(false);

    useEffect(() => {
        // Animate progress bar from 0 to 100% over 2.5 seconds
        const startTime = Date.now();
        const duration = 2500;

        const interval = setInterval(() => {
            const elapsed = Date.now() - startTime;
            const percentage = Math.min((elapsed / duration) * 100, 100);
            setProgress(Math.floor(percentage));

            if (percentage >= 100) {
                clearInterval(interval);
            }
        }, 30);

        // Exit animation triggers after 4.5 seconds
        const exitTimeout = setTimeout(() => {
            setExit(true);
        }, 4500);

        // Redirect to login after 5.3 seconds
        const redirectTimeout = setTimeout(() => {
            navigate('/login');
        }, 5300);

        return () => {
            clearInterval(interval);
            clearTimeout(exitTimeout);
            clearTimeout(redirectTimeout);
        };
    }, [navigate]);

    return (
        <div 
            className={`welcome-page-body ${exit ? 'exit-animation' : ''}`}
            style={{ '--bg-image': `url(${recCampus})` }}
        >
            {/* Ambient Orbs */}
            <div className="orb orb-1"></div>
            <div className="orb orb-2"></div>
            <div className="orb orb-3"></div>

            {/* Grid Background */}
            <div className="grid-bg"></div>

            {/* Scan Line */}
            <div className="scan-line"></div>

            {/* Floating Particles */}
            <div className="particles">
                {Array.from({ length: 30 }).map((_, i) => {
                    const left = Math.random() * 100;
                    const size = Math.random() * 3 + 2;
                    const duration = Math.random() * 6 + 6;
                    const delay = Math.random() * 8;
                    return (
                        <div
                            key={i}
                            className="particle"
                            style={{
                                left: `${left}%`,
                                width: `${size}px`,
                                height: `${size}px`,
                                animationDuration: `${duration}s`,
                                animationDelay: `${delay}s`,
                            }}
                        />
                    );
                })}
            </div>

            {/* Corner Decorations */}
            <div className="corner-deco tl"></div>
            <div className="corner-deco tr"></div>
            <div className="corner-deco bl"></div>
            <div className="corner-deco br"></div>

            {/* Main Content */}
            <div className="welcome-container">
                {/* Animated Logo Ring */}
                <div className="logo-ring-wrapper">
                    <div className="logo-ring"></div>
                    <div className="logo-center">
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M4.26 10.147a60.438 60.438 0 0 0-.491 6.347A48.62 48.62 0 0 1 12 20.904a48.62 48.62 0 0 1 8.232-4.41 60.46 60.46 0 0 0-.491-6.347m-15.482 0a50.636 50.636 0 0 0-2.658-.813A59.906 59.906 0 0 1 12 3.493a59.903 59.903 0 0 1 10.399 5.84c-.896.248-1.783.52-2.658.814m-15.482 0A50.717 50.717 0 0 1 12 13.489a50.702 50.702 0 0 1 7.74-3.342M6.75 15a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5Zm0 0v-3.675A55.378 55.378 0 0 1 12 8.443m-7.007 11.55A5.981 5.981 0 0 0 6.75 15.75v-1.5" />
                        </svg>
                    </div>
                </div>

                {/* Welcome Card */}
                <div className="welcome-box">
                    <h1 className="title">
                        {"Welcome to SmartCampus".split('').map((char, index) => (
                            <span
                                key={index}
                                className="char"
                                style={{ animationDelay: `${0.5 + index * 0.04}s` }}
                            >
                                {char === ' ' ? '\u00A0' : char}
                            </span>
                        ))}
                    </h1>
                    <p className="subtitle">The intelligent digital ecosystem for connected learning</p>
                    <div className="divider"></div>

                    <div className="loading-section">
                        <p className="loading-label">Initializing System</p>
                        <div className="loading-track">
                            <div
                                className="loading-fill"
                                style={{ width: `${progress}%` }}
                            ></div>
                        </div>
                        <p className="loading-percent">{progress}%</p>
                    </div>

                    <div className="status-dots">
                        <div className="status-dot"></div>
                        <div className="status-dot"></div>
                        <div className="status-dot"></div>
                    </div>
                </div>
            </div>

            {/* Version Tag */}
            <div className="version-tag">SMARTCAMPUS v2.0</div>
        </div>
    );
}
