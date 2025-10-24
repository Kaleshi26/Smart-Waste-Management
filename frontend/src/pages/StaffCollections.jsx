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

    // 🎯 ADD THIS FUNCTION: Normalize collection data from backend
    const normalizeCollection = (collection) => {
        console.log('🔄 Normalizing collection:', collection.id);

        return {
            ...collection,
            // Map backend fields to frontend expected fields
            calculatedCharge: collection.charge || collection.calculatedCharge || 0,
            recyclingRefund: collection.recyclingRefund || collection.refundAmount || 0,
            finalAmount: collection.finalAmount || collection.totalAmount || 0,
            // Ensure all required fields have values
            weight: collection.weight || 0
        };
    };

    const fetchCollections = async () => {
        try {
            console.log('🔄 Fetching collections for staff:', user.id);
            const response = await axios.get(`http://localhost:8082/api/waste/collections/collector/${user.id}`);

            // 🎯 NORMALIZE the collection data
            const normalizedCollections = (response.data || []).map(normalizeCollection);

            // 🔍 DEBUG: Log collection data for verification
            if (normalizedCollections.length > 0) {
                console.log('📊 First collection after normalization:', {
                    id: normalizedCollections[0].id,
                    weight: normalizedCollections[0].weight,
                    calculatedCharge: normalizedCollections[0].calculatedCharge,
                    recyclingRefund: normalizedCollections[0].recyclingRefund,
                    finalAmount: normalizedCollections[0].finalAmount
                });
            }

            setCollections(normalizedCollections);
        } catch (error) {
            console.error('Error fetching collections:', error);
            console.error('Error details:', error.response?.data);
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
        if (filter === 'week') {
            const weekAgo = new Date();
            weekAgo.setDate(weekAgo.getDate() - 7);
            const collectionDate = new Date(collection.collectionTime);
            return collectionDate >= weekAgo;
        }
        return true;
    });

    // 🎯 UPDATED: Enhanced statistics calculations
    const getTotalWeight = () => {
        return filteredCollections.reduce((sum, coll) => sum + (coll.weight || 0), 0);
    };

    const getTotalRevenue = () => {
        return filteredCollections.reduce((sum, coll) => sum + (coll.calculatedCharge || 0), 0);
    };

    const getTotalRecyclingRefunds = () => {
        return filteredCollections.reduce((sum, coll) => sum + (coll.recyclingRefund || 0), 0);
    };

    const getNetRevenue = () => {
        return getTotalRevenue() - getTotalRecyclingRefunds();
    };

    // 🎯 NEW: Calculate performance metrics
    const getPerformanceMetrics = () => {
        const totalCollections = filteredCollections.length;
        const avgWeight = totalCollections > 0 ? getTotalWeight() / totalCollections : 0;
        const avgRevenue = totalCollections > 0 ? getNetRevenue() / totalCollections : 0;

        return {
            totalCollections,
            avgWeight,
            avgRevenue
        };
    };

    // 🎯 NEW: Get collections with recycling
    const getCollectionsWithRecycling = () => {
        return filteredCollections.filter(coll => coll.recyclingRefund > 0);
    };

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading collection history...</p>
            </div>
        );
    }

    const performance = getPerformanceMetrics();
    const collectionsWithRecycling = getCollectionsWithRecycling();

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

            {/* 🎯 UPDATED: Enhanced Summary Cards */}
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
                    <div className="stat-title">Recycling Refunds</div>
                    <div className="stat-value">${getTotalRecyclingRefunds().toFixed(2)}</div>
                    <div className="stat-change">Total savings</div>
                </div>

                <div className="stat-card info">
                    <div className="stat-title">Net Revenue</div>
                    <div className="stat-value">${getNetRevenue().toFixed(2)}</div>
                    <div className="stat-change">After refunds</div>
                </div>

                {/* 🎯 NEW: Additional Performance Cards */}
                <div className="stat-card secondary">
                    <div className="stat-title">Avg. Weight</div>
                    <div className="stat-value">{performance.avgWeight.toFixed(1)}kg</div>
                    <div className="stat-change">Per collection</div>
                </div>

                <div className="stat-card primary">
                    <div className="stat-title">Green Collections</div>
                    <div className="stat-value">{collectionsWithRecycling.length}</div>
                    <div className="stat-change">With recycling</div>
                </div>
            </div>

            {/* 🎯 UPDATED: Enhanced Filters */}
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
                    <button
                        className={filter === 'week' ? 'active' : ''}
                        onClick={() => setFilter('week')}
                    >
                        This Week
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

            {/* 🎯 UPDATED: Enhanced Collections Table */}
            <div className="card">
                <div className="card-header">
                    <h3>Collection Records</h3>
                    <div className="revenue-summary">
                        <span className="revenue-item">
                            Gross: <strong>${getTotalRevenue().toFixed(2)}</strong>
                        </span>
                        <span className="revenue-item text-success">
                            Refunds: <strong>-${getTotalRecyclingRefunds().toFixed(2)}</strong>
                        </span>
                        <span className="revenue-item total">
                            Net: <strong>${getNetRevenue().toFixed(2)}</strong>
                        </span>
                    </div>
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
                                <th>Recycling Refund</th>
                                <th>Net Amount</th>
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
                                        <strong>${(collection.calculatedCharge || 0).toFixed(2)}</strong>
                                    </td>
                                    <td className={collection.recyclingRefund > 0 ? 'text-success' : ''}>
                                        {collection.recyclingRefund > 0 ? `-$${collection.recyclingRefund.toFixed(2)}` : '-'}
                                    </td>
                                    <td>
                                        <strong className={
                                            collection.recyclingRefund > 0 ? 'text-info' : ''
                                        }>
                                            ${((collection.calculatedCharge || 0) - (collection.recyclingRefund || 0)).toFixed(2)}
                                        </strong>
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



        </div>
    );
};

export default StaffCollections;