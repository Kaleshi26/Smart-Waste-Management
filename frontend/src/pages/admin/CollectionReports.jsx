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
            console.log('🔄 Fetching collection data for date range:', dateRange);

            // Get all staff members first
            const staffResponse = await axios.get('http://localhost:8082/api/auth/users/role/ROLE_STAFF');
            const staffMembers = staffResponse.data || [];
            console.log('👷 Found staff members:', staffMembers.length);

            // Fetch collections for each staff member
            const allCollections = [];
            const staffPromises = staffMembers.map(async (staff) => {
                try {
                    console.log(`📦 Fetching collections for staff ${staff.id}: ${staff.name}`);
                    const collectionsResponse = await axios.get(
                        `http://localhost:8082/api/waste/collections/collector/${staff.id}`
                    );

                    if (collectionsResponse.data && Array.isArray(collectionsResponse.data)) {
                        // Add staff info to each collection
                        const collectionsWithStaff = collectionsResponse.data.map(collection => ({
                            ...collection,
                            collector: {
                                id: staff.id,
                                name: staff.name,
                                email: staff.email
                            }
                        }));

                        console.log(`✅ Found ${collectionsWithStaff.length} collections for ${staff.name}`);
                        return collectionsWithStaff;
                    }
                    return [];
                } catch (error) {
                    console.error(`❌ Error fetching collections for staff ${staff.id}:`, error);
                    return [];
                }
            });

            // Wait for all staff collections to be fetched
            const staffCollectionsArrays = await Promise.all(staffPromises);

            // Flatten all collections
            const allFetchedCollections = staffCollectionsArrays.flat();

            // Filter by date range
            const filteredCollections = allFetchedCollections.filter(collection => {
                if (!collection.collectionTime) return false;

                const collectionDate = new Date(collection.collectionTime).toISOString().split('T')[0];
                const isInRange = collectionDate >= dateRange.start && collectionDate <= dateRange.end;

                return isInRange;
            });

            console.log('📊 Final filtered collections:', filteredCollections.length);
            setCollections(filteredCollections);

        } catch (error) {
            console.error('❌ Error fetching collection data:', error);
            toast.error('Failed to load collection data');
            setCollections([]);
        } finally {
            setLoading(false);
        }
    };

    // Calculate metrics from real data
    const calculateMetrics = () => {
        const totalCollections = collections.length;
        const totalWeight = collections.reduce((sum, coll) => sum + (coll.weight || 0), 0);
        const totalRevenue = collections.reduce((sum, coll) => sum + (coll.calculatedCharge || 0), 0);
        const totalRecyclingWeight = collections.reduce((sum, coll) => sum + (coll.recyclableWeight || 0), 0);
        const totalRefunds = collections.reduce((sum, coll) => sum + (coll.refundAmount || 0), 0);
        const avgWeight = totalCollections > 0 ? totalWeight / totalCollections : 0;

        return {
            totalCollections,
            totalWeight,
            totalRevenue,
            avgWeight,
            totalRecyclingWeight,
            totalRefunds
        };
    };

    const { totalCollections, totalWeight, totalRevenue, avgWeight, totalRecyclingWeight, totalRefunds } = calculateMetrics();

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
                        totalRevenue: 0,
                        recyclingWeight: 0,
                        refunds: 0
                    });
                }

                const collectorData = collectorMap.get(collectorId);
                collectorData.collections += 1;
                collectorData.totalWeight += collection.weight || 0;
                collectorData.totalRevenue += collection.calculatedCharge || 0;
                collectorData.recyclingWeight += collection.recyclableWeight || 0;
                collectorData.refunds += collection.refundAmount || 0;
            }
        });

        return Array.from(collectorMap.values())
            .sort((a, b) => b.collections - a.collections)
            .slice(0, 5);
    };

    const getCollectionTrends = () => {
        const dailyStats = {};

        collections.forEach(collection => {
            if (collection.collectionTime) {
                const date = new Date(collection.collectionTime).toDateString();
                if (!dailyStats[date]) {
                    dailyStats[date] = {
                        date,
                        collections: 0,
                        weight: 0,
                        revenue: 0
                    };
                }
                dailyStats[date].collections += 1;
                dailyStats[date].weight += collection.weight || 0;
                dailyStats[date].revenue += collection.calculatedCharge || 0;
            }
        });

        return Object.values(dailyStats)
            .sort((a, b) => new Date(b.date) - new Date(a.date))
            .slice(0, 7); // Last 7 days
    };

    const exportToCSV = () => {
        const csvData = [
            ['Collection ID', 'Bin ID', 'Location', 'Collector', 'Weight (kg)', 'Charge ($)', 'Recycling Weight', 'Refund ($)', 'Collection Time'],
            ...collections.map(collection => [
                collection.id || 'N/A',
                collection.binId || 'N/A',
                collection.location || 'N/A',
                collection.collector?.name || 'Unknown',
                collection.weight || 0,
                collection.calculatedCharge || 0,
                collection.recyclableWeight || 0,
                collection.refundAmount || 0,
                new Date(collection.collectionTime).toLocaleString()
            ])
        ];

        const csvContent = csvData.map(row => row.join(',')).join('\n');
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', `collection_report_${dateRange.start}_to_${dateRange.end}.csv`);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        toast.success('Collection report exported as CSV!');
    };

    const exportPerformanceReport = () => {
        const topCollectors = getTopCollectors();
        const csvData = [
            ['Rank', 'Collector Name', 'Collections', 'Total Weight (kg)', 'Total Revenue ($)', 'Recycling Weight (kg)', 'Refunds ($)'],
            ...topCollectors.map((collector, index) => [
                index + 1,
                collector.name,
                collector.collections,
                collector.totalWeight.toFixed(2),
                collector.totalRevenue.toFixed(2),
                collector.recyclingWeight.toFixed(2),
                collector.refunds.toFixed(2)
            ])
        ];

        const csvContent = csvData.map(row => row.join(',')).join('\n');
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', `collector_performance_${dateRange.start}_to_${dateRange.end}.csv`);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        toast.success('Performance report exported!');
    };

    const getPeakDay = () => {
        const dayCount = {};
        collections.forEach(collection => {
            if (collection.collectionTime) {
                const day = new Date(collection.collectionTime).toLocaleDateString('en-US', { weekday: 'long' });
                dayCount[day] = (dayCount[day] || 0) + 1;
            }
        });

        return Object.entries(dayCount).sort((a, b) => b[1] - a[1])[0]?.[0] || 'No data';
    };

    const getMostActiveArea = () => {
        const areaCount = {};
        collections.forEach(collection => {
            if (collection.location) {
                areaCount[collection.location] = (areaCount[collection.location] || 0) + 1;
            }
        });

        return Object.entries(areaCount).sort((a, b) => b[1] - a[1])[0]?.[0] || 'No data';
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
                <h1>📊 Collection Reports</h1>
                <p>Monitor collection performance and generate insights</p>
                <button
                    onClick={fetchCollectionData}
                    className="btn btn-secondary"
                    disabled={loading}
                >
                    {loading ? 'Refreshing...' : '🔄 Refresh Data'}
                </button>
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
                    <span className="results-count">
                        {collections.length} collections in selected period
                    </span>
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
                    <div className="stat-icon">♻️</div>
                    <div className="stat-content">
                        <div className="stat-value">{totalRecyclingWeight.toFixed(1)} kg</div>
                        <div className="stat-title">Recycling</div>
                        <div className="stat-breakdown">
                            ${totalRefunds.toFixed(2)} refunded
                        </div>
                    </div>
                </div>
            </div>

            <div className="dashboard-grid">
                {/* Top Collectors */}
                <div className="card">
                    <div className="card-header">
                        <h3>🏆 Top Collectors</h3>
                        <span className="badge">Performance Ranking</span>
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
                                        <span>📦 {collector.collections} collections</span>
                                        <span>⚖️ {collector.totalWeight.toFixed(1)} kg</span>
                                        <span>💰 ${collector.totalRevenue.toFixed(2)}</span>
                                        {collector.recyclingWeight > 0 && (
                                            <span>♻️ {collector.recyclingWeight.toFixed(1)} kg</span>
                                        )}
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
                        {getTopCollectors().length === 0 && (
                            <div className="empty-state">
                                <p>No collection data available for the selected period</p>
                            </div>
                        )}
                    </div>
                </div>

                {/* Recent Collections */}
                <div className="card">
                    <div className="card-header">
                        <h3>📋 Recent Collections</h3>
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
                                    {collection.recyclableItemsCount > 0 && (
                                        <small className="recycling-badge">
                                            ♻️ {collection.recyclableItemsCount} recyclables
                                        </small>
                                    )}
                                </div>
                                <div className="collection-details">
                                    <span className="weight">{collection.weight} kg</span>
                                    <span className="charge">${collection.calculatedCharge?.toFixed(2)}</span>
                                </div>
                            </div>
                        ))}
                        {collections.length === 0 && (
                            <div className="empty-state">
                                <p>No collections found for the selected period</p>
                            </div>
                        )}
                    </div>
                </div>

                {/* Export Options */}
                <div className="card">
                    <div className="card-header">
                        <h3>📤 Export Reports</h3>
                    </div>
                    <div className="export-options">
                        <button className="export-btn" onClick={exportToCSV}>
                            <span className="export-icon">📊</span>
                            Export Collection Data (CSV)
                        </button>
                        <button className="export-btn" onClick={exportPerformanceReport}>
                            <span className="export-icon">👷</span>
                            Export Staff Performance (CSV)
                        </button>
                        <button className="export-btn" onClick={() => toast.info('PDF export coming soon!')}>
                            <span className="export-icon">💰</span>
                            Export Revenue Report (PDF)
                        </button>
                        <button className="export-btn" onClick={() => toast.info('Analytics report coming soon!')}>
                            <span className="export-icon">📈</span>
                            Generate Analytics Report
                        </button>
                    </div>
                </div>

                {/* Collection Trends */}
                <div className="card">
                    <div className="card-header">
                        <h3>📈 Collection Trends</h3>
                    </div>
                    <div className="trends-content">
                        <div className="trend-stats">
                            <div className="trend-stat">
                                <span className="trend-label">Peak Collection Day</span>
                                <span className="trend-value">{getPeakDay()}</span>
                            </div>
                            <div className="trend-stat">
                                <span className="trend-label">Avg Daily Collections</span>
                                <span className="trend-value">
                                    {totalCollections > 0 ? (totalCollections / 30).toFixed(1) : 0}
                                </span>
                            </div>
                            <div className="trend-stat">
                                <span className="trend-label">Most Active Area</span>
                                <span className="trend-value">{getMostActiveArea()}</span>
                            </div>
                            <div className="trend-stat">
                                <span className="trend-label">Recycling Rate</span>
                                <span className="trend-value">
                                    {totalWeight > 0 ? ((totalRecyclingWeight / totalWeight) * 100).toFixed(1) : 0}%
                                </span>
                            </div>
                        </div>

                        {/* Recent Daily Stats */}
                        <div className="daily-trends">
                            <h4>Last 7 Days Activity</h4>
                            {getCollectionTrends().map((day, index) => (
                                <div key={index} className="daily-stat">
                                    <span className="day">{new Date(day.date).toLocaleDateString('en-US', { weekday: 'short' })}</span>
                                    <div className="day-bar">
                                        <div
                                            className="day-fill"
                                            style={{ width: `${(day.collections / Math.max(...getCollectionTrends().map(d => d.collections))) * 100}%` }}
                                        ></div>
                                    </div>
                                    <span className="day-count">{day.collections} collections</span>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>

            {/* Debug Info - Remove in production */}
            {process.env.NODE_ENV === 'development' && collections.length > 0 && (
                <div className="card debug-info">
                    <h4>🔧 Debug Information</h4>
                    <details>
                        <summary>Show sample collection data ({collections.length} total)</summary>
                        <pre>{JSON.stringify(collections.slice(0, 2), null, 2)}</pre>
                    </details>
                </div>
            )}
        </div>
    );
};

export default CollectionReports;