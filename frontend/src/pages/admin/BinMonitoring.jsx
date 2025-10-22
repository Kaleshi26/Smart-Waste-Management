// File: frontend/src/pages/admin/BinMonitoring.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const BinMonitoring = ({ user }) => {
    const [bins, setBins] = useState([]);
    const [filteredBins, setFilteredBins] = useState([]);
    const [loading, setLoading] = useState(true);
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [typeFilter, setTypeFilter] = useState('ALL');
    const [showUpdateForm, setShowUpdateForm] = useState(false);
    const [selectedBin, setSelectedBin] = useState(null);
    const [updateData, setUpdateData] = useState({
        currentLevel: '',
        status: 'ACTIVE'
    });

    useEffect(() => {
        fetchBins();
    }, []);

    useEffect(() => {
        filterBins();
    }, [bins, statusFilter, typeFilter]);

    const fetchBins = async () => {
        try {
            setLoading(true);

            // Try direct bins endpoint first
            try {
                const response = await axios.get('http://localhost:8082/api/waste/bins');
                console.log('Bins direct response:', response.data);
                if (response.data && Array.isArray(response.data)) {
                    setBins(response.data);
                    return;
                }
            } catch (directError) {
                console.log('Direct bins endpoint failed, trying resident approach');
            }

            // Fallback: Get bins through residents
            const residentsResponse = await axios.get('http://localhost:8082/api/auth/users/role/ROLE_RESIDENT');
            const residents = residentsResponse.data || [];

            const allBins = [];
            for (const resident of residents.slice(0, 15)) {
                try {
                    const binsResponse = await axios.get(`http://localhost:8082/api/waste/bins/resident/${resident.id}`);
                    if (binsResponse.data && Array.isArray(binsResponse.data)) {
                        const binsWithResident = binsResponse.data.map(bin => ({
                            ...bin,
                            resident: {
                                id: resident.id,
                                name: resident.name,
                                email: resident.email,
                                phoneNumber: resident.phoneNumber
                            }
                        }));
                        allBins.push(...binsWithResident);
                    }
                } catch (binError) {
                    console.error(`Error fetching bins for resident ${resident.id}:`, binError);
                }
            }

            setBins(allBins);
        } catch (error) {
            console.error('Error fetching bins:', error);
            toast.error('Failed to load bins data');
            setBins([]);
        } finally {
            setLoading(false);
        }
    };

    const filterBins = () => {
        let filtered = bins;

        // Filter by status
        if (statusFilter !== 'ALL') {
            filtered = filtered.filter(bin => bin.status === statusFilter);
        }

        // Filter by type
        if (typeFilter !== 'ALL') {
            filtered = filtered.filter(bin => bin.binType === typeFilter);
        }

        setFilteredBins(filtered);
    };

    const getStatusBadge = (status) => {
        const statusConfig = {
            'ACTIVE': { label: 'Active', class: 'badge-success' },
            'NEEDS_EMPTYING': { label: 'Needs Emptying', class: 'badge-warning' },
            'NEEDS_MAINTENANCE': { label: 'Maintenance', class: 'badge-danger' },
            'INACTIVE': { label: 'Inactive', class: 'badge-secondary' }
        };

        const config = statusConfig[status] || { label: status, class: 'badge-secondary' };
        return <span className={`badge ${config.class}`}>{config.label}</span>;
    };

    const getLevelColor = (level) => {
        if (level >= 80) return 'level-high';
        if (level >= 50) return 'level-medium';
        return 'level-low';
    };

    const getTypeIcon = (type) => {
        const icons = {
            'GENERAL': '🗑️',
            'RECYCLABLE': '♻️',
            'ORGANIC': '🍂',
            'HAZARDOUS': '⚠️'
        };
        return icons[type] || '🗑️';
    };

    const calculateBinStats = () => {
        const totalBins = bins.length;
        const needsEmptying = bins.filter(bin => bin.status === 'NEEDS_EMPTYING' || (bin.currentLevel || 0) >= 80).length;
        const needsMaintenance = bins.filter(bin => bin.status === 'NEEDS_MAINTENANCE').length;
        const averageLevel = bins.length > 0 ?
            bins.reduce((sum, bin) => sum + (bin.currentLevel || 0), 0) / bins.length : 0;

        return { totalBins, needsEmptying, needsMaintenance, averageLevel };
    };

    const updateBinLevel = async (binId, level) => {
        try {
            await axios.put(`http://localhost:8082/api/waste/bins/${binId}/level`, {
                level: parseFloat(level)
            });
            toast.success('Bin level updated successfully');
            fetchBins();
            setShowUpdateForm(false);
            setSelectedBin(null);
        } catch (error) {
            console.error('Error updating bin level:', error);
            toast.error('Failed to update bin level');
        }
    };

    const handleUpdateSubmit = (e) => {
        e.preventDefault();
        if (selectedBin) {
            updateBinLevel(selectedBin.id, updateData.currentLevel);
        }
    };

    const schedulePickup = async (binId) => {
        try {
            const tomorrow = new Date();
            tomorrow.setDate(tomorrow.getDate() + 1);
            const scheduledDate = tomorrow.toISOString().split('T')[0];

            await axios.post('http://localhost:8082/api/waste/schedules/create', {
                binId: binId,
                scheduledDate: scheduledDate,
                notes: 'Scheduled from bin monitoring'
            });
            toast.success('Pickup scheduled for tomorrow');
        } catch (error) {
            console.error('Error scheduling pickup:', error);
            toast.error('Failed to schedule pickup');
        }
    };

    const stats = calculateBinStats();

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading bin monitoring data...</p>
            </div>
        );
    }

    return (
        <div className="bin-monitoring">
            <div className="page-header">
                <h1>Bin Monitoring</h1>
                <p>Real-time monitoring of waste bins across the city</p>
            </div>

            {/* Update Bin Modal */}
            {showUpdateForm && selectedBin && (
                <div className="modal-overlay">
                    <div className="modal">
                        <div className="modal-header">
                            <h3>Update Bin Level - {selectedBin.binId}</h3>
                            <button
                                onClick={() => {
                                    setShowUpdateForm(false);
                                    setSelectedBin(null);
                                }}
                                className="btn-close"
                            >
                                ×
                            </button>
                        </div>
                        <form onSubmit={handleUpdateSubmit}>
                            <div className="form-group">
                                <label>Current Fill Level (%)</label>
                                <input
                                    type="number"
                                    min="0"
                                    max="100"
                                    step="1"
                                    value={updateData.currentLevel}
                                    onChange={(e) => setUpdateData({...updateData, currentLevel: e.target.value})}
                                    required
                                    className="form-input"
                                />
                            </div>
                            <div className="form-group">
                                <label>Status</label>
                                <select
                                    value={updateData.status}
                                    onChange={(e) => setUpdateData({...updateData, status: e.target.value})}
                                    className="form-input"
                                >
                                    <option value="ACTIVE">Active</option>
                                    <option value="NEEDS_EMPTYING">Needs Emptying</option>
                                    <option value="NEEDS_MAINTENANCE">Needs Maintenance</option>
                                    <option value="INACTIVE">Inactive</option>
                                </select>
                            </div>
                            <div className="modal-actions">
                                <button
                                    type="button"
                                    onClick={() => {
                                        setShowUpdateForm(false);
                                        setSelectedBin(null);
                                    }}
                                    className="btn btn-secondary"
                                >
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary">
                                    Update Bin
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Stats Overview */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-icon">🗑️</div>
                    <div className="stat-content">
                        <div className="stat-value">{stats.totalBins}</div>
                        <div className="stat-title">Total Bins</div>
                        <div className="stat-breakdown">
                            Active waste bins
                        </div>
                    </div>
                </div>

                <div className="stat-card warning">
                    <div className="stat-icon">🚨</div>
                    <div className="stat-content">
                        <div className="stat-value">{stats.needsEmptying}</div>
                        <div className="stat-title">Need Emptying</div>
                        <div className="stat-breakdown">
                            Above 80% capacity
                        </div>
                    </div>
                </div>

                <div className="stat-card danger">
                    <div className="stat-icon">🔧</div>
                    <div className="stat-content">
                        <div className="stat-value">{stats.needsMaintenance}</div>
                        <div className="stat-title">Need Maintenance</div>
                        <div className="stat-breakdown">
                            Require service
                        </div>
                    </div>
                </div>

                <div className="stat-card info">
                    <div className="stat-icon">📊</div>
                    <div className="stat-content">
                        <div className="stat-value">{stats.averageLevel.toFixed(1)}%</div>
                        <div className="stat-title">Avg Fill Level</div>
                        <div className="stat-breakdown">
                            Across all bins
                        </div>
                    </div>
                </div>
            </div>

            {/* Filters */}
            <div className="filters-card">
                <div className="filter-group">
                    <select
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                        className="filter-select"
                    >
                        <option value="ALL">All Status</option>
                        <option value="ACTIVE">Active</option>
                        <option value="NEEDS_EMPTYING">Needs Emptying</option>
                        <option value="NEEDS_MAINTENANCE">Needs Maintenance</option>
                        <option value="INACTIVE">Inactive</option>
                    </select>
                </div>
                <div className="filter-group">
                    <select
                        value={typeFilter}
                        onChange={(e) => setTypeFilter(e.target.value)}
                        className="filter-select"
                    >
                        <option value="ALL">All Types</option>
                        <option value="GENERAL">General</option>
                        <option value="RECYCLABLE">Recyclable</option>
                        <option value="ORGANIC">Organic</option>
                        <option value="HAZARDOUS">Hazardous</option>
                    </select>
                </div>
                <div className="filter-group">
                    <span className="results-count">{filteredBins.length} bins found</span>
                </div>
            </div>

            {/* Bins Grid */}
            <div className="card">
                <div className="card-header">
                    <h3>Waste Bin Status</h3>
                    <div className="header-actions">
                        <button className="btn btn-secondary" onClick={fetchBins}>
                            Refresh Data
                        </button>
                    </div>
                </div>

                {filteredBins.length > 0 ? (
                    <div className="bins-grid">
                        {filteredBins.map((bin) => (
                            <div key={bin.id} className="bin-card">
                                <div className="bin-header">
                                    <div className="bin-type">
                                        <span className="bin-icon">{getTypeIcon(bin.binType)}</span>
                                        <span className="bin-type-label">{bin.binType}</span>
                                    </div>
                                    {getStatusBadge(bin.status)}
                                </div>

                                <div className="bin-info">
                                    <div className="bin-id">
                                        <strong>{bin.binId}</strong>
                                    </div>
                                    <div className="bin-location">
                                        <span>{bin.location || 'Unknown Location'}</span>
                                    </div>
                                    <div className="bin-owner">
                                        <small>Owner: {bin.resident?.name || 'Unassigned'}</small>
                                    </div>
                                </div>

                                <div className="bin-level">
                                    <div className="level-header">
                                        <span>Fill Level</span>
                                        <span className={`level-percent ${getLevelColor(bin.currentLevel)}`}>
                                            {bin.currentLevel || 0}%
                                        </span>
                                    </div>
                                    <div className="level-bar">
                                        <div
                                            className={`level-fill ${getLevelColor(bin.currentLevel)}`}
                                            style={{ width: `${bin.currentLevel || 0}%` }}
                                        ></div>
                                    </div>
                                </div>

                                <div className="bin-capacity">
                                    <small>Capacity: {bin.capacity || 'Standard'}L</small>
                                    <small>Last Updated: {
                                        bin.lastUpdated ?
                                            new Date(bin.lastUpdated).toLocaleDateString() :
                                            'Unknown'
                                    }</small>
                                </div>

                                <div className="bin-actions">
                                    <button
                                        className="btn btn-sm btn-primary"
                                        onClick={() => schedulePickup(bin.binId)}
                                    >
                                        Schedule Pickup
                                    </button>
                                    <button
                                        className="btn btn-sm btn-secondary"
                                        onClick={() => {
                                            setSelectedBin(bin);
                                            setUpdateData({
                                                currentLevel: bin.currentLevel || '0',
                                                status: bin.status || 'ACTIVE'
                                            });
                                            setShowUpdateForm(true);
                                        }}
                                    >
                                        Update Level
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="empty-state">
                        <h3>No Bins Found</h3>
                        <p>No bins match your filter criteria.</p>
                        <button
                            onClick={() => {
                                setStatusFilter('ALL');
                                setTypeFilter('ALL');
                            }}
                            className="btn btn-primary"
                        >
                            Clear Filters
                        </button>
                    </div>
                )}
            </div>

            {/* Bin Analytics */}
            <div className="card">
                <div className="card-header">
                    <h3>Bin Analytics</h3>
                </div>
                <div className="analytics-grid">
                    <div className="analytics-item">
                        <h4>Fill Level Distribution</h4>
                        <div className="distribution-bars">
                            <div className="distribution-bar">
                                <span className="bar-label">0-30%</span>
                                <div className="bar-container">
                                    <div
                                        className="bar-fill low"
                                        style={{ width: `${(bins.filter(b => (b.currentLevel || 0) <= 30).length / bins.length) * 100}%` }}
                                    ></div>
                                </div>
                                <span className="bar-count">{bins.filter(b => (b.currentLevel || 0) <= 30).length}</span>
                            </div>
                            <div className="distribution-bar">
                                <span className="bar-label">31-70%</span>
                                <div className="bar-container">
                                    <div
                                        className="bar-fill medium"
                                        style={{ width: `${(bins.filter(b => (b.currentLevel || 0) > 30 && (b.currentLevel || 0) <= 70).length / bins.length) * 100}%` }}
                                    ></div>
                                </div>
                                <span className="bar-count">{bins.filter(b => (b.currentLevel || 0) > 30 && (b.currentLevel || 0) <= 70).length}</span>
                            </div>
                            <div className="distribution-bar">
                                <span className="bar-label">71-100%</span>
                                <div className="bar-container">
                                    <div
                                        className="bar-fill high"
                                        style={{ width: `${(bins.filter(b => (b.currentLevel || 0) > 70).length / bins.length) * 100}%` }}
                                    ></div>
                                </div>
                                <span className="bar-count">{bins.filter(b => (b.currentLevel || 0) > 70).length}</span>
                            </div>
                        </div>
                    </div>
                    <div className="analytics-item">
                        <h4>Bin Types</h4>
                        <div className="type-stats">
                            <div className="type-stat">
                                <span className="type-label">General</span>
                                <span className="type-count">
                                    {bins.filter(b => b.binType === 'GENERAL').length}
                                </span>
                            </div>
                            <div className="type-stat">
                                <span className="type-label">Recyclable</span>
                                <span className="type-count">
                                    {bins.filter(b => b.binType === 'RECYCLABLE').length}
                                </span>
                            </div>
                            <div className="type-stat">
                                <span className="type-label">Organic</span>
                                <span className="type-count">
                                    {bins.filter(b => b.binType === 'ORGANIC').length}
                                </span>
                            </div>
                            <div className="type-stat">
                                <span className="type-label">Hazardous</span>
                                <span className="type-count">
                                    {bins.filter(b => b.binType === 'HAZARDOUS').length}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default BinMonitoring;