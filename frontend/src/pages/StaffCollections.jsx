// File: frontend/src/pages/StaffCollections.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const StaffCollections = ({ user }) => {
    const [collections, setCollections] = useState([]);
    const [filter, setFilter] = useState('all');
    const [dateRange, setDateRange] = useState({
        start: new Date().toISOString().split('T')[0],
        end: new Date().toISOString().split('T')[0]
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchCollections();
    }, [user.id]);

    const fetchCollections = async () => {
        try {
            const response = await axios.get(`http://localhost:8082/api/waste/collections/collector/${user.id}`);

            // The response now includes proper bin details
            setCollections(response.data || []);
        } catch (error) {
            console.error('Error fetching collections:', error);
            toast.error('Failed to load collection history');
        } finally {
            setLoading(false);
        }
    };
    const filteredCollections = collections.filter(collection => {
        if (filter === 'all') return true;
        if (filter === 'today') {
            const today = new Date().toISOString().split('T')[0];
            return collection.collectionTime?.includes(today);
        }
        return true;
    });

    const getTotalWeight = () => {
        return filteredCollections.reduce((sum, coll) => sum + (coll.weight || 0), 0);
    };

    const getEarnings = () => {
        // Simple calculation - in real app, this would come from backend
        return filteredCollections.length * 2.5; // $2.5 per collection example
    };

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading collection history...</p>
            </div>
        );
    }

    return (
        <div className="collections-page">
            <div className="page-header">
                <div>
                    <h1>Collection History</h1>
                    <p>View your waste collection records and performance</p>
                </div>
                <div className="header-actions">
                    <span className="collection-count">
                        {collections.length} total collections
                    </span>
                </div>
            </div>

            {/* Summary Cards */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-title">Total Collections</div>
                    <div className="stat-value">{filteredCollections.length}</div>
                    <div className="stat-change">Filtered records</div>
                </div>

                <div className="stat-card warning">
                    <div className="stat-title">Total Weight</div>
                    <div className="stat-value">{getTotalWeight().toFixed(1)}kg</div>
                    <div className="stat-change">Waste collected</div>
                </div>

                <div className="stat-card success">
                    <div className="stat-title">Estimated Earnings</div>
                    <div className="stat-value">${getEarnings().toFixed(2)}</div>
                    <div className="stat-change">This period</div>
                </div>

                <div className="stat-card">
                    <div className="stat-title">Avg. Weight</div>
                    <div className="stat-value">
                        {filteredCollections.length > 0 ? (getTotalWeight() / filteredCollections.length).toFixed(1) : '0.0'}kg
                    </div>
                    <div className="stat-change">Per collection</div>
                </div>
            </div>

            {/* Filters */}
            <div className="filters-section">
                <div className="filter-buttons">
                    <button
                        className={filter === 'all' ? 'active' : ''}
                        onClick={() => setFilter('all')}
                    >
                        All Time
                    </button>
                    <button
                        className={filter === 'today' ? 'active' : ''}
                        onClick={() => setFilter('today')}
                    >
                        Today
                    </button>
                </div>

                <div className="date-filters">
                    <div className="form-group">
                        <label>From</label>
                        <input
                            type="date"
                            value={dateRange.start}
                            onChange={(e) => setDateRange({...dateRange, start: e.target.value})}
                            className="form-input"
                        />
                    </div>
                    <div className="form-group">
                        <label>To</label>
                        <input
                            type="date"
                            value={dateRange.end}
                            onChange={(e) => setDateRange({...dateRange, end: e.target.value})}
                            className="form-input"
                        />
                    </div>
                </div>
            </div>

            {/* Collections Table */}
            <div className="card">
                <div className="card-header">
                    <h3>Collection Records</h3>
                </div>

                {filteredCollections.length > 0 ? (
                    <div className="table-container">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>Date & Time</th>
                                <th>Bin ID</th>
                                <th>Location</th>
                                <th>Weight</th>
                                <th>Charge</th>
                                <th>Status</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredCollections.map((collection) => (
                                <tr key={collection.id}>
                                    <td>
                                        <div className="datetime-cell">
                                            <div className="date">
                                                {new Date(collection.collectionTime).toLocaleDateString()}
                                            </div>
                                            <div className="time">
                                                {new Date(collection.collectionTime).toLocaleTimeString()}
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <strong>{collection.wasteBin?.binId}</strong>
                                    </td>
                                    <td>{collection.wasteBin?.location}</td>
                                    <td>
                                            <span className="weight-badge">
                                                {collection.weight} kg
                                            </span>
                                    </td>
                                    <td>
                                        ${collection.calculatedCharge?.toFixed(2) || '0.00'}
                                    </td>
                                    <td>
                                            <span className="status-badge status-success">
                                                Completed
                                            </span>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="empty-state">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                        <h3>No Collections Found</h3>
                        <p>
                            {filter === 'today'
                                ? "You haven't recorded any collections today."
                                : "You haven't recorded any collections yet."
                            }
                        </p>
                    </div>
                )}
            </div>

            {/* Performance Chart Placeholder */}
            <div className="card">
                <h3>Performance Overview</h3>
                <div className="performance-placeholder">
                    <div className="placeholder-content">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                        </svg>
                        <p>Collection performance charts will be displayed here</p>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StaffCollections;