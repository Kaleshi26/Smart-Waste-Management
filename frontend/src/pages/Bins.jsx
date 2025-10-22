// File: frontend/src/pages/Bins.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const Bins = ({ user }) => {
    const [bins, setBins] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedBin, setSelectedBin] = useState(null);
    const [showDetails, setShowDetails] = useState(false);
    const [showLevelUpdate, setShowLevelUpdate] = useState(false);
    const [updateLevel, setUpdateLevel] = useState(0);
    const [updatingLevel, setUpdatingLevel] = useState(false);

    useEffect(() => {
        fetchBins();
    }, [user.id]);

    const fetchBins = async () => {
        try {
            const response = await axios.get(`http://localhost:8082/api/waste/bins/resident/${user.id}`);
            setBins(response.data || []);
        } catch (error) {
            console.error('Error fetching bins:', error);
            toast.error('Failed to load waste bins');
        } finally {
            setLoading(false);
        }
    };

    const getBinStatus = (bin) => {
        const level = bin.currentLevel || 0;
        if (level >= 80) return { type: 'danger', label: 'Needs Emptying' };
        if (level >= 60) return { type: 'warning', label: 'Getting Full' };
        return { type: 'active', label: 'Normal' };
    };

    const getBinTypeColor = (type) => {
        switch (type) {
            case 'GENERAL_WASTE': return '#ef4444';
            case 'RECYCLABLE_PLASTIC': return '#3b82f6';
            case 'RECYCLABLE_PAPER': return '#10b981';
            case 'E_WASTE': return '#f59e0b';
            default: return '#6b7280';
        }
    };

    const handleViewDetails = (bin) => {
        setSelectedBin(bin);
        setShowDetails(true);
    };

    const handleUpdateLevel = async () => {
        if (updateLevel < 0 || updateLevel > 100) {
            toast.error('Level must be between 0 and 100');
            return;
        }

        setUpdatingLevel(true);
        try {
            await axios.put(`http://localhost:8082/api/waste/bins/${selectedBin.binId}/level`, {
                currentLevel: parseFloat(updateLevel)
            });
            toast.success('Bin level updated successfully!');
            setShowLevelUpdate(false);
            setShowDetails(false);
            fetchBins();
        } catch (error) {
            toast.error(error.response?.data?.error || 'Failed to update bin level');
        } finally {
            setUpdatingLevel(false);
        }
    };

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading your waste bins...</p>
            </div>
        );
    }

    return (
        <div className="bins-page">
            <div className="page-header">
                <div>
                    <h1>My Waste Bins</h1>
                    <p>Manage and monitor your registered waste bins</p>
                </div>
                <div className="header-actions">
                    <span className="bin-count">{bins.length} bin{bins.length !== 1 ? 's' : ''} registered</span>
                </div>
            </div>

            {bins.length > 0 ? (
                <div className="bins-grid">
                    {bins.map((bin) => {
                        const status = getBinStatus(bin);

                        return (
                            <div key={bin.binId} className={`bin-card ${status.type}`}>
                                <div className="bin-header">
                                    <div className="bin-id">
                                        <div
                                            className="bin-type-indicator"
                                            style={{ backgroundColor: getBinTypeColor(bin.binType) }}
                                        ></div>
                                        {bin.binId}
                                    </div>
                                    <span className={`status-badge status-${status.type}`}>
                                        {status.label}
                                    </span>
                                </div>

                                <div className="bin-details">
                                    <div className="bin-detail">
                                        <label>Location:</label>
                                        <span>{bin.location}</span>
                                    </div>
                                    <div className="bin-detail">
                                        <label>Type:</label>
                                        <span>{bin.binType?.replace(/_/g, ' ')}</span>
                                    </div>
                                    <div className="bin-detail">
                                        <label>Capacity:</label>
                                        <span>{bin.capacity}L</span>
                                    </div>
                                    <div className="bin-detail">
                                        <label>Current Level:</label>
                                        <span>{bin.currentLevel || 0}%</span>
                                    </div>
                                </div>

                                <div className="progress-section">
                                    <div className="progress-bar">
                                        <div
                                            className={`progress-fill ${status.type}`}
                                            style={{ width: `${bin.currentLevel || 0}%` }}
                                        ></div>
                                    </div>
                                    <div className="progress-labels">
                                        <span>Empty</span>
                                        <span>Full</span>
                                    </div>
                                </div>

                                <div className="bin-footer">
                                    <div className="bin-meta">
                                        <small>Installed: {bin.installationDate}</small>
                                    </div>
                                    <button
                                        onClick={() => handleViewDetails(bin)}
                                        className="btn btn-sm btn-secondary"
                                    >
                                        View Details
                                    </button>
                                </div>
                            </div>
                        );
                    })}
                </div>
            ) : (
                <div className="empty-state">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                    <h3>No Waste Bins Registered</h3>
                    <p>You don't have any waste bins registered to your account yet.</p>
                    <p className="empty-state-help">
                        Contact your waste management provider to get bins assigned to your property.
                    </p>
                </div>
            )}

            {/* Bin Details Modal */}
            {showDetails && selectedBin && (
                <div className="modal-overlay">
                    <div className="modal">
                        <div className="modal-header">
                            <h3>Bin Details - {selectedBin.binId}</h3>
                            <button
                                onClick={() => setShowDetails(false)}
                                className="btn btn-sm btn-secondary"
                            >
                                Close
                            </button>
                        </div>

                        <div className="modal-body">
                            <div className="details-grid">
                                <div className="detail-item">
                                    <label>Bin ID:</label>
                                    <span>{selectedBin.binId}</span>
                                </div>
                                <div className="detail-item">
                                    <label>Location:</label>
                                    <span>{selectedBin.location}</span>
                                </div>
                                <div className="detail-item">
                                    <label>Type:</label>
                                    <span>{selectedBin.binType?.replace(/_/g, ' ')}</span>
                                </div>
                                <div className="detail-item">
                                    <label>Capacity:</label>
                                    <span>{selectedBin.capacity} Liters</span>
                                </div>
                                <div className="detail-item">
                                    <label>Current Level:</label>
                                    <span>{selectedBin.currentLevel || 0}%</span>
                                </div>
                                <div className="detail-item">
                                    <label>Status:</label>
                                    <span className={`status-badge status-${getBinStatus(selectedBin).type}`}>
                                        {getBinStatus(selectedBin).label}
                                    </span>
                                </div>
                                <div className="detail-item">
                                    <label>RFID Tag:</label>
                                    <span>{selectedBin.rfidTag || 'Not assigned'}</span>
                                </div>
                                <div className="detail-item">
                                    <label>QR Code:</label>
                                    <span>{selectedBin.qrCode || 'Not assigned'}</span>
                                </div>
                                <div className="detail-item">
                                    <label>Installation Date:</label>
                                    <span>{selectedBin.installationDate}</span>
                                </div>
                            </div>

                            {/* Bin Actions */}
                            <div className="bin-actions">
                                <h4>Bin Management</h4>
                                <button
                                    onClick={() => {
                                        setShowLevelUpdate(true);
                                        setUpdateLevel(selectedBin.currentLevel || 0);
                                    }}
                                    className="btn btn-sm btn-primary"
                                >
                                    Update Bin Level
                                </button>
                            </div>

                            {selectedBin.collections && selectedBin.collections.length > 0 && (
                                <div className="collections-section">
                                    <h4>Recent Collections</h4>
                                    <div className="collections-list">
                                        {selectedBin.collections.slice(0, 3).map((collection, index) => (
                                            <div key={index} className="collection-item">
                                                <div className="collection-date">
                                                    {new Date(collection.collectionTime).toLocaleDateString()}
                                                </div>
                                                <div className="collection-weight">
                                                    {collection.weight} kg
                                                </div>
                                                <div className="collection-charge">
                                                    ${collection.calculatedCharge || 0}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* Bin Level Update Modal - Separate from details modal */}
            {showLevelUpdate && selectedBin && (
                <div className="modal-overlay">
                    <div className="modal">
                        <div className="modal-header">
                            <h3>Update Bin Level - {selectedBin.binId}</h3>
                            <button
                                onClick={() => setShowLevelUpdate(false)}
                                className="btn btn-sm btn-secondary"
                                disabled={updatingLevel}
                            >
                                Cancel
                            </button>
                        </div>
                        <div className="modal-body">
                            <div className="form-group">
                                <label>Current Fill Level (%)</label>
                                <input
                                    type="number"
                                    min="0"
                                    max="100"
                                    step="1"
                                    value={updateLevel}
                                    onChange={(e) => setUpdateLevel(e.target.value)}
                                    className="form-input"
                                    disabled={updatingLevel}
                                />
                                <small>Enter the current fill percentage (0-100%)</small>
                            </div>
                        </div>
                        <div className="modal-actions">
                            <button
                                onClick={handleUpdateLevel}
                                className="btn btn-primary btn-block"
                                disabled={updatingLevel}
                            >
                                {updatingLevel ? 'Updating...' : 'Update Level'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Bins;