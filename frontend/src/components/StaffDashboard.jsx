// File: frontend/src/pages/StaffDashboard.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';

const StaffDashboard = ({ user }) => {
    const [stats, setStats] = useState({});
    const [todayCollections, setTodayCollections] = useState([]);
    const [assignedRoute, setAssignedRoute] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchStaffDashboardData();
    }, [user.id]);

    const fetchStaffDashboardData = async () => {
        try {
            setLoading(true);

            // Fetch today's collections
            const collectionsResponse = await axios.get(`http://localhost:8082/api/waste/collections/collector/${user.id}`);
            const allCollections = collectionsResponse.data || [];

            // Filter today's collections
            const today = new Date().toISOString().split('T')[0];
            const todayCollections = allCollections.filter(collection =>
                collection.collectionTime?.includes(today)
            );

            setTodayCollections(todayCollections);

            // Calculate stats
            const totalCollections = allCollections.length;
            const todayWeight = todayCollections.reduce((sum, coll) => sum + (coll.weight || 0), 0);
            const totalWeight = allCollections.reduce((sum, coll) => sum + (coll.weight || 0), 0);

            setStats({
                totalCollections,
                todayCollections: todayCollections.length,
                todayWeight,
                totalWeight,
                efficiency: todayCollections.length > 0 ? Math.round((todayCollections.length / 50) * 100) : 0 // Assuming 50 is target
            });

        } catch (error) {
            console.error('Error fetching staff dashboard data:', error);
            toast.error('Failed to load dashboard data');
        } finally {
            setLoading(false);
        }
    };

    const startCollection = () => {
        toast.success('Starting collection route...');
        // This would navigate to collection interface
    };

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading your dashboard...</p>
            </div>
        );
    }

    return (
        <div className="dashboard">
            {/* Welcome Section */}
            <div className="welcome-section">
                <h2>Welcome, {user.name}! 🚛</h2>
                <p>Ready to start your waste collection route for today.</p>
            </div>

            {/* Quick Actions */}
            <div className="quick-actions-staff">
                <button onClick={startCollection} className="action-card primary">
                    <div className="action-icon">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                        </svg>
                    </div>
                    <div className="action-content">
                        <strong>Start Collection</strong>
                        <p>Begin today's waste collection route</p>
                    </div>
                </button>

                <Link to="/staff/scan" className="action-card">
                    <div className="action-icon">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
                        </svg>
                    </div>
                    <div className="action-content">
                        <strong>Scan Bin</strong>
                        <p>Record individual bin collection</p>
                    </div>
                </Link>

                <Link to="/staff/collections" className="action-card">
                    <div className="action-icon">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                    </div>
                    <div className="action-content">
                        <strong>View History</strong>
                        <p>Check previous collection records</p>
                    </div>
                </Link>
            </div>

            {/* Stats Grid */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-title">Today's Collections</div>
                    <div className="stat-value">{stats.todayCollections || 0}</div>
                    <div className="stat-change">Bins collected today</div>
                </div>

                <div className="stat-card warning">
                    <div className="stat-title">Today's Weight</div>
                    <div className="stat-value">{stats.todayWeight || 0}kg</div>
                    <div className="stat-change">Total waste collected</div>
                </div>

                <div className="stat-card success">
                    <div className="stat-title">Efficiency</div>
                    <div className="stat-value">{stats.efficiency || 0}%</div>
                    <div className="stat-change">Route completion</div>
                </div>

                <div className="stat-card">
                    <div className="stat-title">Total Collections</div>
                    <div className="stat-value">{stats.totalCollections || 0}</div>
                    <div className="stat-change">All time records</div>
                </div>
            </div>

            <div className="dashboard-grid">
                {/* Today's Collections */}
                <div className="card">
                    <div className="card-header">
                        <h3>Today's Collections</h3>
                        <span className="badge">{todayCollections.length} bins</span>
                    </div>

                    {todayCollections.length > 0 ? (
                        <div className="collections-list">
                            {todayCollections.map((collection, index) => (
                                <div key={collection.id || index} className="collection-item">
                                    <div className="collection-info">
                                        <strong>Bin {collection.wasteBin?.binId}</strong>
                                        <span>{collection.wasteBin?.location}</span>
                                    </div>
                                    <div className="collection-details">
                                        <span className="weight">{collection.weight} kg</span>
                                        <span className="time">
                                            {new Date(collection.collectionTime).toLocaleTimeString()}
                                        </span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="empty-state">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                            </svg>
                            <h3>No Collections Today</h3>
                            <p>Start your collection route to record today's work</p>
                        </div>
                    )}
                </div>

                {/* Quick Tips */}
                <div className="card">
                    <div className="card-header">
                        <h3>Collection Tips</h3>
                    </div>
                    <div className="tips-list">
                        <div className="tip-item">
                            <div className="tip-icon">📱</div>
                            <div className="tip-content">
                                <strong>Scan Properly</strong>
                                <p>Ensure RFID/QR code is clearly visible before scanning</p>
                            </div>
                        </div>
                        <div className="tip-item">
                            <div className="tip-icon">⚖️</div>
                            <div className="tip-content">
                                <strong>Accurate Weights</strong>
                                <p>Record precise weights for accurate billing</p>
                            </div>
                        </div>
                        <div className="tip-item">
                            <div className="tip-icon">🔄</div>
                            <div className="tip-content">
                                <strong>Offline Mode</strong>
                                <p>Collections sync automatically when back online</p>
                            </div>
                        </div>
                        <div className="tip-item">
                            <div className="tip-icon">🚨</div>
                            <div className="tip-content">
                                <strong>Report Issues</strong>
                                <p>Report damaged bins or tags immediately</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StaffDashboard;