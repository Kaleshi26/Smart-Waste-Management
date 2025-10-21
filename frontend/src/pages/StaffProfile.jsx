// File: frontend/src/pages/StaffProfile.jsx
import React, { useState } from 'react';
import toast from 'react-hot-toast';

const StaffProfile = ({ user }) => {
    const [activeTab, setActiveTab] = useState('profile');
    const [shiftData, setShiftData] = useState({
        startTime: '08:00',
        endTime: '17:00',
        vehicle: 'Truck-001',
        route: 'Route A'
    });

    const performanceStats = {
        totalCollections: 1247,
        avgDailyCollections: 45,
        totalWeight: 12560.5,
        efficiency: 92
    };

    return (
        <div className="profile-page">
            <div className="page-header">
                <div>
                    <h1>Staff Profile</h1>
                    <p>Manage your staff account and view performance</p>
                </div>
                <div className="header-actions">
                    <span className="staff-badge">Waste Collection Staff</span>
                </div>
            </div>

            <div className="profile-layout">
                {/* Sidebar */}
                <div className="profile-sidebar">
                    <div className="profile-summary staff">
                        <div className="profile-avatar staff">
                            {user.name?.charAt(0).toUpperCase()}
                        </div>
                        <div className="profile-info">
                            <h3>{user.name}</h3>
                            <p className="profile-role">Collection Staff</p>
                            <p className="profile-email">{user.email}</p>
                            <p className="profile-id">ID: {user.id}</p>
                        </div>
                    </div>

                    <nav className="profile-nav">
                        <button
                            className={activeTab === 'profile' ? 'active' : ''}
                            onClick={() => setActiveTab('profile')}
                        >
                            Personal Info
                        </button>
                        <button
                            className={activeTab === 'shift' ? 'active' : ''}
                            onClick={() => setActiveTab('shift')}
                        >
                            Shift Details
                        </button>
                        <button
                            className={activeTab === 'performance' ? 'active' : ''}
                            onClick={() => setActiveTab('performance')}
                        >
                            Performance
                        </button>
                    </nav>
                </div>

                {/* Main Content */}
                <div className="profile-content">
                    {/* Personal Information */}
                    {activeTab === 'profile' && (
                        <div className="profile-card">
                            <h3>Personal Information</h3>

                            <div className="info-grid">
                                <div className="info-item">
                                    <label>Full Name:</label>
                                    <span>{user.name}</span>
                                </div>
                                <div className="info-item">
                                    <label>Email:</label>
                                    <span>{user.email}</span>
                                </div>
                                <div className="info-item">
                                    <label>Phone:</label>
                                    <span>{user.phone || 'Not provided'}</span>
                                </div>
                                <div className="info-item">
                                    <label>Address:</label>
                                    <span>{user.address || 'Not provided'}</span>
                                </div>
                                <div className="info-item">
                                    <label>Staff ID:</label>
                                    <span>{user.id}</span>
                                </div>
                                <div className="info-item">
                                    <label>Employment Status:</label>
                                    <span className="status-badge status-active">Active</span>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Shift Information */}
                    {activeTab === 'shift' && (
                        <div className="profile-card">
                            <h3>Shift & Assignment</h3>

                            <div className="shift-info">
                                <div className="shift-item">
                                    <div className="shift-icon">🕐</div>
                                    <div className="shift-details">
                                        <strong>Shift Hours</strong>
                                        <p>{shiftData.startTime} - {shiftData.endTime}</p>
                                    </div>
                                </div>

                                <div className="shift-item">
                                    <div className="shift-icon">🚛</div>
                                    <div className="shift-details">
                                        <strong>Assigned Vehicle</strong>
                                        <p>{shiftData.vehicle}</p>
                                    </div>
                                </div>

                                <div className="shift-item">
                                    <div className="shift-icon">🗺️</div>
                                    <div className="shift-details">
                                        <strong>Collection Route</strong>
                                        <p>{shiftData.route}</p>
                                    </div>
                                </div>

                                <div className="shift-item">
                                    <div className="shift-icon">✅</div>
                                    <div className="shift-details">
                                        <strong>Today's Status</strong>
                                        <p>Ready for collection</p>
                                    </div>
                                </div>
                            </div>

                            <div className="today-tasks">
                                <h4>Today's Tasks</h4>
                                <div className="task-list">
                                    <div className="task-item completed">
                                        <span className="task-check">✓</span>
                                        <span>Vehicle inspection</span>
                                    </div>
                                    <div className="task-item completed">
                                        <span className="task-check">✓</span>
                                        <span>Equipment check</span>
                                    </div>
                                    <div className="task-item pending">
                                        <span className="task-check">○</span>
                                        <span>Start collection route</span>
                                    </div>
                                    <div className="task-item pending">
                                        <span className="task-check">○</span>
                                        <span>Complete daily report</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Performance */}
                    {activeTab === 'performance' && (
                        <div className="profile-card">
                            <h3>Performance Metrics</h3>

                            <div className="performance-stats">
                                <div className="performance-card">
                                    <div className="metric-value">{performanceStats.totalCollections}</div>
                                    <div className="metric-label">Total Collections</div>
                                </div>
                                <div className="performance-card">
                                    <div className="metric-value">{performanceStats.avgDailyCollections}</div>
                                    <div className="metric-label">Avg Daily</div>
                                </div>
                                <div className="performance-card">
                                    <div className="metric-value">{performanceStats.totalWeight.toFixed(0)}kg</div>
                                    <div className="metric-label">Total Weight</div>
                                </div>
                                <div className="performance-card">
                                    <div className="metric-value">{performanceStats.efficiency}%</div>
                                    <div className="metric-label">Efficiency</div>
                                </div>
                            </div>

                            <div className="performance-breakdown">
                                <h4>Recent Achievements</h4>
                                <div className="achievements-list">
                                    <div className="achievement-item">
                                        <div className="achievement-icon">🏆</div>
                                        <div className="achievement-content">
                                            <strong>Top Collector</strong>
                                            <p>Highest collections in November</p>
                                        </div>
                                    </div>
                                    <div className="achievement-item">
                                        <div className="achievement-icon">⭐</div>
                                        <div className="achievement-content">
                                            <strong>Perfect Week</strong>
                                            <p>100% route completion last week</p>
                                        </div>
                                    </div>
                                    <div className="achievement-item">
                                        <div className="achievement-icon">📈</div>
                                        <div className="achievement-content">
                                            <strong>Efficiency Boost</strong>
                                            <p>15% improvement in daily average</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default StaffProfile;