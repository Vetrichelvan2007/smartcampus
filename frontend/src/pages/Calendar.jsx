import React, { useState, useEffect } from 'react';
import DashboardLayout from '../components/DashboardLayout';
import './Calendar.css';

export default function Calendar() {
    const [currentDate, setCurrentDate] = useState(new Date());
    const [selectedDate, setSelectedDate] = useState(new Date());
    const [events, setEvents] = useState([]);
    
    // Toggleable Calendars
    const [showAcademic, setShowAcademic] = useState(true);
    const [showInstitutional, setShowInstitutional] = useState(true);
    const [showEvents, setShowEvents] = useState(true);

    // Mock academic schedule and institutional events
    const allMockEvents = [
        { date: new Date().toDateString(), title: 'Theory of Computation Quiz', type: 'academic', desc: 'Active online MCQ quiz worth 10 marks.' },
        { date: new Date().toDateString(), title: 'Neural Networks Lab Submission', type: 'academic', desc: 'Submit final neural networks lab report by 11:59 PM.' },
        { date: new Date(new Date().setDate(new Date().getDate() + 2)).toDateString(), title: 'End-Semester Registration Starts', type: 'institutional', desc: 'Online portal registration for final exams.' },
        { date: new Date(new Date().setDate(new Date().getDate() - 3)).toDateString(), title: 'Guest Lecture: Artificial Intelligence', type: 'event', desc: 'Seminar on LLMs and transformer networks in Hall A.' },
        { date: new Date(new Date().setDate(new Date().getDate() + 5)).toDateString(), title: 'Sports Day Celebration', type: 'event', desc: 'Annual sports activities and track meets.' }
    ];

    useEffect(() => {
        // Filter events based on active filters
        const filtered = allMockEvents.filter(e => {
            if (e.type === 'academic' && !showAcademic) return false;
            if (e.type === 'institutional' && !showInstitutional) return false;
            if (e.type === 'event' && !showEvents) return false;
            return true;
        });
        setEvents(filtered);
    }, [showAcademic, showInstitutional, showEvents]);

    const getDaysInMonth = (year, month) => new Date(year, month + 1, 0).getDate();
    const getFirstDayOfMonth = (year, month) => new Date(year, month, 1).getDay();

    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const firstDay = getFirstDayOfMonth(year, month);
    const totalDays = getDaysInMonth(year, month);

    const changeMonth = (offset) => {
        const nextDate = new Date(currentDate);
        nextDate.setMonth(nextDate.getMonth() + offset);
        setCurrentDate(nextDate);
    };

    const goToToday = () => {
        setCurrentDate(new Date());
        setSelectedDate(new Date());
    };

    const handleDateSelect = (day) => {
        const d = new Date(year, month, day);
        setSelectedDate(d);
    };

    const getEventsForDate = (dateStr) => {
        return events.filter(e => e.date === dateStr);
    };

    const selectedDateEvents = getEventsForDate(selectedDate.toDateString());

    // Generate Calendar Day Cells
    const dayNames = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const cells = [];

    // Empty lead cells
    for (let i = 0; i < firstDay; i++) {
        cells.push(<div key={`empty-${i}`} className="calendar-cell empty"></div>);
    }

    // Actual day cells
    const today = new Date();
    for (let day = 1; day <= totalDays; day++) {
        const d = new Date(year, month, day);
        const dateStr = d.toDateString();
        const dateEvents = getEventsForDate(dateStr);
        
        const isToday = day === today.getDate() && month === today.getMonth() && year === today.getFullYear();
        const isSelected = day === selectedDate.getDate() && month === selectedDate.getMonth() && year === selectedDate.getFullYear();

        cells.push(
            <div
                key={`day-${day}`}
                className={`calendar-cell day ${isToday ? 'today' : ''} ${isSelected ? 'selected' : ''}`}
                onClick={() => handleDateSelect(day)}
            >
                <span className="cell-num">{day}</span>
                {dateEvents.length > 0 && (
                    <div className="cell-dots">
                        {dateEvents.map((e, idx) => (
                            <span key={idx} className={`dot-indicator ${e.type}`}></span>
                        ))}
                    </div>
                )}
            </div>
        );
    }

    return (
        <DashboardLayout role="student">
            <div className="calendar-page-content">
                <div className="container-body">
                    {/* Left Sidebar Filter */}
                    <aside className="sidebar-filter glass">
                        <div className="sidebar-header">
                            <h3>Calendars</h3>
                        </div>
                        <div className="filter-options">
                            <label className="checkbox-wrap">
                                <input
                                    type="checkbox"
                                    checked={showAcademic}
                                    onChange={(e) => setShowAcademic(e.target.checked)}
                                />
                                <span className="custom-box academic"></span>
                                <span className="label-text">Academic Calendar</span>
                            </label>
                            <label className="checkbox-wrap">
                                <input
                                    type="checkbox"
                                    checked={showInstitutional}
                                    onChange={(e) => setShowInstitutional(e.target.checked)}
                                />
                                <span className="custom-box institutional"></span>
                                <span className="label-text">Institutional Calendar</span>
                            </label>
                            <label className="checkbox-wrap">
                                <input
                                    type="checkbox"
                                    checked={showEvents}
                                    onChange={(e) => setShowEvents(e.target.checked)}
                                />
                                <span className="custom-box event"></span>
                                <span className="label-text">Events / Seminars</span>
                            </label>
                        </div>
                    </aside>

                    {/* Main Calendar Grid */}
                    <main className="calendar-main glass">
                        <div className="calendar-header-nav">
                            <div className="header-buttons">
                                <button className="nav-btn" onClick={() => changeMonth(-1)}>◀</button>
                                <button className="nav-btn" onClick={goToToday}>Today</button>
                                <button className="nav-btn" onClick={() => changeMonth(1)}>▶</button>
                            </div>
                            <h2 className="month-year-title">
                                {currentDate.toLocaleString('default', { month: 'long', year: 'numeric' })}
                            </h2>
                            <div className="spacer"></div>
                        </div>

                        <div className="calendar-grid-canvas">
                            {dayNames.map(d => (
                                <div key={d} className="day-name">{d}</div>
                            ))}
                            {cells}
                        </div>
                    </main>

                    {/* Right side Event Details Panel */}
                    <aside className="event-details-panel glass">
                        <div className="panel-header">
                            <h3>Event Details</h3>
                        </div>
                        <div className="panel-body">
                            <div className="selected-date-banner">
                                {selectedDate.toLocaleDateString('en-US', { weekday: 'long', month: 'short', day: 'numeric' })}
                            </div>

                            {selectedDateEvents.length > 0 ? (
                                <div className="event-cards-list">
                                    {selectedDateEvents.map((e, idx) => (
                                        <div key={idx} className={`event-details-card ${e.type}`}>
                                            <div className="event-header-row">
                                                <span className={`type-chip ${e.type}`}>{e.type}</span>
                                            </div>
                                            <h4>{e.title}</h4>
                                            <p>{e.desc}</p>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <p className="no-events-msg">No active events listed for this date.</p>
                            )}
                        </div>
                    </aside>
                </div>
            </div>
        </DashboardLayout>
    );
}
