// File: frontend/src/pages/admin/AdminAnalyticsDashboard.jsx
import React, { useState, useEffect } from 'react';
import { Bar } from 'react-chartjs-2';
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    BarElement,
    Title,
    Tooltip,
    Legend,
} from 'chart.js';
import analyticsService from '../../services/analyticsService';
import toast from 'react-hot-toast';

// Register Chart.js components
ChartJS.register(
    CategoryScale,
    LinearScale,
    BarElement,
    Title,
    Tooltip,
    Legend
);

const AdminAnalyticsDashboard = ({ user }) => {
    // State management
    const [analyticsData, setAnalyticsData] = useState({
        kpis: {},
        monthlyData: [],
        collectionRecords: [],
        binStatusOverview: []
    });
    const [loading, setLoading] = useState(true);
    const [selectedFilter, setSelectedFilter] = useState('30'); // Default to 30 days
    const [lastUpdated, setLastUpdated] = useState(null);

    // Strategy Pattern Implementation for Filtering
    const filterStrategies = {
        '7': {
            name: 'Last 7 Days',
            getDateRange: () => {
                const end = new Date();
                const start = new Date();
                start.setDate(start.getDate() - 7);
                return { start, end };
            }
        },
        '30': {
            name: 'Last 30 Days',
            getDateRange: () => {
                const end = new Date();
                const start = new Date();
                start.setDate(start.getDate() - 30);
                return { start, end };
            }
        },
        'all': {
            name: 'All Time',
            getDateRange: () => {
                const end = new Date();
                const start = new Date('2024-01-01'); // System start date
                return { start, end };
            }
        }
    };

    // Observer Pattern Implementation for Data Updates
    const [observers, setObservers] = useState([]);
    
    const addObserver = (callback) => {
        setObservers(prev => [...prev, callback]);
    };

    const removeObserver = (callback) => {
        setObservers(prev => prev.filter(obs => obs !== callback));
    };

    const notifyObservers = (data) => {
        observers.forEach(observer => observer(data));
    };

    // Fetch analytics data
    const fetchAnalyticsData = async (filter = selectedFilter) => {
        try {
            setLoading(true);
            const data = await analyticsService.getAnalyticsData(filter);
            setAnalyticsData(data);
            setLastUpdated(new Date());
            
            // Notify all observers about data update
            notifyObservers(data);
        } catch (error) {
            console.error('Error fetching analytics data:', error);
            toast.error('Failed to load analytics data');
        } finally {
            setLoading(false);
        }
    };

    // Handle filter change
    const handleFilterChange = (filter) => {
        setSelectedFilter(filter);
        fetchAnalyticsData(filter);
    };

    // CSV Export functionality
    const exportToCSV = () => {
        const { collectionRecords, monthlyData } = analyticsData;
        
        // Prepare CSV data
        const csvData = [
            ['Collection ID', 'Bin ID', 'Location', 'Weight (kg)', 'Collection Date', 'Staff Member', 'Charge ($)'],
            ...collectionRecords.map(record => [
                record.id || 'N/A',
                record.binId || 'N/A',
                record.location || 'N/A',
                record.weight || 0,
                new Date(record.collectionTime).toLocaleDateString(),
                record.staffName || 'N/A',
                record.calculatedCharge || 0
            ])
        ];

        // Convert to CSV string
        const csvContent = csvData.map(row => row.join(',')).join('\n');
        
        // Create and download file
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        const url = URL.createObjectURL(blob);
        link.setAttribute('href', url);
        link.setAttribute('download', `waste_analytics_${selectedFilter}_days_${new Date().toISOString().split('T')[0]}.csv`);
        link.style.visibility = 'hidden';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        toast.success('CSV exported successfully!');
    };

    // Chart configuration
    const chartData = {
        labels: analyticsData.monthlyData.map(item => item.month),
        datasets: [
            {
                label: 'Waste Collected (kg)',
                data: analyticsData.monthlyData.map(item => item.totalWeight),
                backgroundColor: 'rgba(54, 162, 235, 0.6)',
                borderColor: 'rgba(54, 162, 235, 1)',
                borderWidth: 1,
            },
            {
                label: 'Collections Count',
                data: analyticsData.monthlyData.map(item => item.collectionCount),
                backgroundColor: 'rgba(255, 99, 132, 0.6)',
                borderColor: 'rgba(255, 99, 132, 1)',
                borderWidth: 1,
            }
        ]
    };

    const chartOptions = {
        responsive: true,
        plugins: {
            legend: {
                position: 'top',
            },
            title: {
                display: true,
                text: `Waste Collection Analytics - ${filterStrategies[selectedFilter].name}`
            },
        },
        scales: {
            y: {
                beginAtZero: true,
            },
        },
    };

    useEffect(() => {
        fetchAnalyticsData();
    }, []);

    if (loading && !lastUpdated) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading analytics dashboard...</p>
            </div>
        );
    }

    return (
        <div className="analytics-dashboard">
            {/* Header */}
            <div className="dashboard-header">
                <h1>📊 Analytics Dashboard</h1>
                <p>Comprehensive waste management analytics and insights</p>
                {lastUpdated && (
                    <div className="last-updated">
                        Last updated: {lastUpdated.toLocaleTimeString()}
                    </div>
                )}
            </div>

            {/* Filter Controls */}
            <div className="filter-controls">
                <div className="filter-buttons">
                    {Object.entries(filterStrategies).map(([key, strategy]) => (
                        <button
                            key={key}
                            className={`filter-btn ${selectedFilter === key ? 'active' : ''}`}
                            onClick={() => handleFilterChange(key)}
                        >
                            {strategy.name}
                        </button>
                    ))}
                </div>
                <div className="export-controls">
                    <button 
                        className="btn btn-primary"
                        onClick={exportToCSV}
                        disabled={loading}
                    >
                        📥 Export CSV
                    </button>
                    <button 
                        className="btn btn-secondary"
                        onClick={() => fetchAnalyticsData(selectedFilter)}
                        disabled={loading}
                    >
                        🔄 Refresh
                    </button>
                </div>
            </div>

            {/* KPIs Section */}
            <div className="kpis-section">
                <h2>Key Performance Indicators</h2>
                <div className="kpis-grid">
                    <div className="kpi-card">
                        <div className="kpi-icon">🗑️</div>
                        <div className="kpi-content">
                            <div className="kpi-value">{analyticsData.kpis.totalWasteCollected || 0}</div>
                            <div className="kpi-label">Total Waste Collected (kg)</div>
                        </div>
                    </div>
                    <div className="kpi-card">
                        <div className="kpi-icon">🚛</div>
                        <div className="kpi-content">
                            <div className="kpi-value">{analyticsData.kpis.totalCollections || 0}</div>
                            <div className="kpi-label">Total Collections</div>
                        </div>
                    </div>
                    <div className="kpi-card">
                        <div className="kpi-icon">📦</div>
                        <div className="kpi-content">
                            <div className="kpi-value">{analyticsData.kpis.registeredBins || 0}</div>
                            <div className="kpi-label">Registered Bins</div>
                        </div>
                    </div>
                    <div className="kpi-card">
                        <div className="kpi-icon">💰</div>
                        <div className="kpi-content">
                            <div className="kpi-value">${analyticsData.kpis.totalRevenue || 0}</div>
                            <div className="kpi-label">Total Revenue</div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Charts Section */}
            <div className="charts-section">
                <div className="chart-container">
                    <h3>Monthly Waste Collection Trends</h3>
                    <div className="chart-wrapper">
                        <Bar data={chartData} options={chartOptions} />
                    </div>
                </div>
            </div>

            {/* Collection Records Table */}
            <div className="collection-records-section">
                <div className="card">
                    <div className="card-header">
                        <h3>Collection Records</h3>
                        <span className="badge">{analyticsData.collectionRecords.length} records</span>
                    </div>
                    <div className="table-container">
                        <table className="data-table">
                            <thead>
                                <tr>
                                    <th>Collection ID</th>
                                    <th>Bin ID</th>
                                    <th>Location</th>
                                    <th>Weight (kg)</th>
                                    <th>Collection Date</th>
                                    <th>Staff Member</th>
                                    <th>Charge ($)</th>
                                </tr>
                            </thead>
                            <tbody>
                                {analyticsData.collectionRecords.map((record, index) => (
                                    <tr key={record.id || index}>
                                        <td>{record.id || 'N/A'}</td>
                                        <td>{record.binId || 'N/A'}</td>
                                        <td>{record.location || 'N/A'}</td>
                                        <td>{record.weight || 0}</td>
                                        <td>{new Date(record.collectionTime).toLocaleDateString()}</td>
                                        <td>{record.staffName || 'N/A'}</td>
                                        <td>${(record.calculatedCharge || 0).toFixed(2)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            {/* Bin Status Overview */}
            <div className="bin-status-section">
                <div className="card">
                    <div className="card-header">
                        <h3>Bin Status Overview</h3>
                        <span className="badge">{analyticsData.binStatusOverview.length} bins</span>
                    </div>
                    <div className="bin-status-grid">
                        {analyticsData.binStatusOverview.map((bin, index) => (
                            <div key={bin.binId || index} className="bin-status-card">
                                <div className="bin-info">
                                    <strong>{bin.binId}</strong>
                                    <span>{bin.location}</span>
                                </div>
                                <div className="bin-details">
                                    <div className="bin-status">
                                        <span className={`status-badge status-${bin.status?.toLowerCase()}`}>
                                            {bin.status}
                                        </span>
                                    </div>
                                    <div className="bin-level">
                                        <div className="progress-bar">
                                            <div 
                                                className={`progress-fill ${bin.currentLevel >= 80 ? 'danger' : bin.currentLevel >= 60 ? 'warning' : 'success'}`}
                                                style={{ width: `${bin.currentLevel || 0}%` }}
                                            ></div>
                                        </div>
                                        <span>{bin.currentLevel || 0}% full</span>
                                    </div>
                                    <div className="last-collection">
                                        <small>Last collection: {bin.lastCollectionDate || 'Never'}</small>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AdminAnalyticsDashboard;
