import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const CollectionReports = ({ user }) => {
    const [collections, setCollections] = useState([]);
    const [loading, setLoading] = useState(true);
    const [dateRange, setDateRange] = useState({
        start: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0],
        end: new Date().toISOString().split('T')[0]
    });

    useEffect(() => {
        fetchCollectionData();
    }, [dateRange]);

    const fetchCollectionData = async () => {
        try {
            setLoading(true);

            // Fetch all collections within date range
            const collectionsResponse = await axios.get(
                `http://localhost:8082/api/waste/collections?start=${dateRange.start}T00:00:00&end=${dateRange.end}T23:59:59`
            );

            console.log('Collections API Response:', collectionsResponse.data);

            if (collectionsResponse.data && Array.isArray(collectionsResponse.data)) {
                setCollections(collectionsResponse.data);
            } else {
                console.warn('Unexpected collections response format:', collectionsResponse.data);
                setCollections([]);
            }

        } catch (error) {
            console.error('Error fetching collection data:', error);

            // Fallback: Try to get collections from staff members
            try {
                const staffResponse = await axios.get('http://localhost:8082/api/auth/users/role/ROLE_STAFF');
                const staffMembers = staffResponse.data || [];

                const allCollections = [];
                for (const staff of staffMembers.slice(0, 5)) { // Limit to prevent too many requests
                    try {
                        const staffCollections = await axios.get(
                            `http://localhost:8082/api/waste/collections/collector/${staff.id}`
                        );
                        if (staffCollections.data && Array.isArray(staffCollections.data)) {
                            // Filter by date range
                            const filteredCollections = staffCollections.data.filter(collection => {
                                const collectionDate = new Date(collection.collectionTime).toISOString().split('T')[0];
                                return collectionDate >= dateRange.start && collectionDate <= dateRange.end;
                            });
                            allCollections.push(...filteredCollections);
                        }
                    } catch (staffError) {
                        console.error(`Error fetching collections for staff ${staff.id}:`, staffError);
                    }
                }

                setCollections(allCollections);
            } catch (fallbackError) {
                console.error('Fallback approach failed:', fallbackError);
                toast.error('Failed to load collection data');
                setCollections([]);
            }
        } finally {
            setLoading(false);
        }
    };

    // Calculate metrics from real data
    const calculateMetrics = () => {
        const totalCollections = collections.length;
        const totalWeight = collections.reduce((sum, coll) => sum + (coll.weight || 0), 0);
        const totalRevenue = collections.reduce((sum, coll) => sum + (coll.calculatedCharge || 0), 0);
        const avgWeight = totalCollections > 0 ? totalWeight / totalCollections : 0;

        return { totalCollections, totalWeight, totalRevenue, avgWeight };
    };
    const { totalCollections, totalWeight, totalRevenue, avgWeight } = calculateMetrics();

    const getTopCollectors = () => {
        const collectorMap = new Map();

        collections.forEach(collection => {
            if (collection.collector) {
                const collectorId = collection.collector.id;
                const collectorName = collection.collector.name || 'Unknown Staff';

                if (!collectorMap.has(collectorId)) {
                    collectorMap.set(collectorId, {
                        name: collectorName,
                        collections: 0,
                        totalWeight: 0,
                        totalRevenue: 0
                    });
                }

                const collectorData = collectorMap.get(collectorId);
                collectorData.collections += 1;
                collectorData.totalWeight += collection.weight || 0;
                collectorData.totalRevenue += collection.calculatedCharge || 0;
            }
        });

        return Array.from(collectorMap.values())
            .sort((a, b) => b.collections - a.collections)
            .slice(0, 5);
    };

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading collection reports...</p>
            </div>
        );
    }

    return (
        <div className="collection-reports">
            <div className="page-header">
                <h1>Collection Reports</h1>
                <p>Monitor collection performance and generate insights</p>
            </div>

            {/* Date Range Filter */}
            <div className="filters-card">
                <div className="filter-group">
                    <label>Start Date</label>
                    <input
                        type="date"
                        value={dateRange.start}
                        onChange={(e) => setDateRange(prev => ({ ...prev, start: e.target.value }))}
                        className="form-input"
                    />
                </div>
                <div className="filter-group">
                    <label>End Date</label>
                    <input
                        type="date"
                        value={dateRange.end}
                        onChange={(e) => setDateRange(prev => ({ ...prev, end: e.target.value }))}
                        className="form-input"
                    />
                </div>
                <div className="filter-group">
                    <button onClick={fetchCollectionData} className="btn btn-primary">
                        Apply Filter
                    </button>
                </div>
            </div>

            {/* Key Metrics */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-icon">🚛</div>
                    <div className="stat-content">
                        <div className="stat-value">{totalCollections}</div>
                        <div className="stat-title">Total Collections</div>
                        <div className="stat-breakdown">
                            {dateRange.start} to {dateRange.end}
                        </div>
                    </div>
                </div>

                <div className="stat-card success">
                    <div className="stat-icon">⚖️</div>
                    <div className="stat-content">
                        <div className="stat-value">{totalWeight.toFixed(1)} kg</div>
                        <div className="stat-title">Total Waste</div>
                        <div className="stat-breakdown">
                            Avg: {avgWeight.toFixed(1)} kg per collection
                        </div>
                    </div>
                </div>

                <div className="stat-card warning">
                    <div className="stat-icon">💰</div>
                    <div className="stat-content">
                        <div className="stat-value">${totalRevenue.toFixed(2)}</div>
                        <div className="stat-title">Total Revenue</div>
                        <div className="stat-breakdown">
                            From collections
                        </div>
                    </div>
                </div>

                <div className="stat-card info">
                    <div className="stat-icon">👷</div>
                    <div className="stat-content">
                        <div className="stat-value">{new Set(collections.map(c => c.collector?.id)).size}</div>
                        <div className="stat-title">Active Staff</div>
                        <div className="stat-breakdown">
                            Involved in collections
                        </div>
                    </div>
                </div>
            </div>

            <div className="dashboard-grid">
                {/* Top Collectors */}
                <div className="card">
                    <div className="card-header">
                        <h3>Top Collectors</h3>
                        <span className="badge">Performance</span>
                    </div>
                    <div className="collectors-list">
                        {getTopCollectors().map((collector, index) => (
                            <div key={index} className="collector-item">
                                <div className="collector-rank">
                                    <span className={`rank-badge ${index < 3 ? 'top-three' : ''}`}>
                                        #{index + 1}
                                    </span>
                                </div>
                                <div className="collector-info">
                                    <strong>{collector.name}</strong>
                                    <div className="collector-stats">
                                        <span>{collector.collections} collections</span>
                                        <span>{collector.totalWeight.toFixed(1)} kg</span>
                                        <span>${collector.totalRevenue.toFixed(2)}</span>
                                    </div>
                                </div>
                                <div className="collector-performance">
                                    <div className="performance-bar">
                                        <div
                                            className="performance-fill"
                                            style={{ width: `${(collector.collections / totalCollections) * 100}%` }}
                                        ></div>
                                    </div>
                                    <span>{((collector.collections / totalCollections) * 100).toFixed(1)}%</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Recent Collections */}
                <div className="card">
                    <div className="card-header">
                        <h3>Recent Collections</h3>
                        <span className="badge">{collections.length} total</span>
                    </div>
                    <div className="collections-list">
                        {collections.slice(0, 8).map((collection, index) => (
                            <div key={collection.id || index} className="collection-item">
                                <div className="collection-info">
                                    <strong>Bin {collection.binId}</strong>
                                    <span>{collection.location || 'Unknown Location'}</span>
                                    <small>
                                        By {collection.collector?.name || 'Unknown Staff'} •
                                        {new Date(collection.collectionTime).toLocaleDateString()}
                                    </small>
                                </div>
                                <div className="collection-details">
                                    <span className="weight">{collection.weight} kg</span>
                                    <span className="charge">${collection.calculatedCharge?.toFixed(2)}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Export Options */}
                <div className="card">
                    <div className="card-header">
                        <h3>Export Reports</h3>
                    </div>
                    <div className="export-options">
                        <button className="export-btn">
                            <span className="export-icon">📊</span>
                            Export Collection Data (CSV)
                        </button>
                        <button className="export-btn">
                            <span className="export-icon">💰</span>
                            Export Revenue Report (PDF)
                        </button>
                        <button className="export-btn">
                            <span className="export-icon">👷</span>
                            Export Staff Performance
                        </button>
                        <button className="export-btn">
                            <span className="export-icon">📈</span>
                            Generate Analytics Report
                        </button>
                    </div>
                </div>

                {/* Collection Trends */}
                <div className="card">
                    <div className="card-header">
                        <h3>Collection Trends</h3>
                    </div>
                    <div className="trends-placeholder">
                        <div className="trend-chart">
                            <div className="chart-placeholder">
                                <p>📈 Collection Analytics Chart</p>
                                <small>Weight trends, revenue patterns, and performance metrics would be displayed here</small>
                            </div>
                        </div>
                        <div className="trend-stats">
                            <div className="trend-stat">
                                <span className="trend-label">Peak Collection Day</span>
                                <span className="trend-value">Monday</span>
                            </div>
                            <div className="trend-stat">
                                <span className="trend-label">Avg Daily Collections</span>
                                <span className="trend-value">{(totalCollections / 30).toFixed(1)}</span>
                            </div>
                            <div className="trend-stat">
                                <span className="trend-label">Most Active Area</span>
                                <span className="trend-value">Downtown</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CollectionReports;