// File: frontend/src/pages/StaffDashboard.jsx
import React, { useState } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const StaffDashboard = ({ user }) => {
    const [activeTab, setActiveTab] = useState('collection');
    const [binId, setBinId] = useState('');
    const [binDetails, setBinDetails] = useState(null);
    const [collectionData, setCollectionData] = useState({
        weight: '',
        wasteType: 'GENERAL_WASTE'
    });
    const [loading, setLoading] = useState(false);

    const handleBinSearch = async (e) => {
        e.preventDefault();
        if (!binId.trim()) {
            toast.error('Please enter a Bin ID');
            return;
        }

        setLoading(true);
        try {
            const response = await axios.get(`http://localhost:8082/api/waste/bins/${binId}`);
            setBinDetails(response.data);
            toast.success('Bin found successfully!');
        } catch (error) {
            toast.error(error.response?.data?.error || 'Bin not found');
            setBinDetails(null);
        } finally {
            setLoading(false);
        }
    };

    const handleCollection = async (e) => {
        e.preventDefault();
        if (!binDetails || !collectionData.weight) {
            toast.error('Please fill all required fields');
            return;
        }

        setLoading(true);
        try {
            const collectionRequest = {
                binId: binDetails.binId,
                staffId: user.id,
                weight: parseFloat(collectionData.weight),
                wasteType: collectionData.wasteType
            };

            const response = await axios.post('http://localhost:8082/api/waste/collections/record', collectionRequest);
            toast.success(`Collection recorded! Charge: $${response.data.collection?.calculatedCharge || 0}`);

            // Reset form
            setBinId('');
            setBinDetails(null);
            setCollectionData({ weight: '', wasteType: 'GENERAL_WASTE' });
        } catch (error) {
            toast.error(error.response?.data?.error || 'Failed to record collection');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="staff-dashboard">
            <div className="page-header">
                <div>
                    <h1>Staff Dashboard</h1>
                    <p>Waste collection and management</p>
                </div>
                <div className="header-actions">
                    <span className="user-role">Staff Member</span>
                </div>
            </div>

            <div className="tab-navigation">
                <button
                    className={activeTab === 'collection' ? 'active' : ''}
                    onClick={() => setActiveTab('collection')}
                >
                    Record Collection
                </button>
                <button
                    className={activeTab === 'history' ? 'active' : ''}
                    onClick={() => setActiveTab('history')}
                >
                    Collection History
                </button>
            </div>

            {activeTab === 'collection' && (
                <div className="dashboard-grid">
                    {/* Bin Search */}
                    <div className="card">
                        <h3>Scan Waste Bin</h3>
                        <form onSubmit={handleBinSearch} className="collection-form">
                            <div className="form-group">
                                <label className="form-label">Bin ID</label>
                                <input
                                    type="text"
                                    className="form-input"
                                    placeholder="Enter Bin ID (e.g., BIN-COL-001)"
                                    value={binId}
                                    onChange={(e) => setBinId(e.target.value)}
                                />
                            </div>
                            <button
                                type="submit"
                                className="btn btn-primary"
                                disabled={loading}
                            >
                                {loading ? 'Searching...' : 'Find Bin'}
                            </button>
                        </form>
                    </div>

                    {/* Bin Details & Collection */}
                    <div className="card">
                        <h3>Collection Details</h3>

                        {binDetails ? (
                            <div className="bin-info-card">
                                <div className="bin-header">
                                    <h4>Bin Information</h4>
                                    <span className="bin-id">{binDetails.binId}</span>
                                </div>

                                <div className="bin-details-grid">
                                    <div className="detail-item">
                                        <label>Location:</label>
                                        <span>{binDetails.location}</span>
                                    </div>
                                    <div className="detail-item">
                                        <label>Type:</label>
                                        <span>{binDetails.binType}</span>
                                    </div>
                                    <div className="detail-item">
                                        <label>Capacity:</label>
                                        <span>{binDetails.capacity}L</span>
                                    </div>
                                    <div className="detail-item">
                                        <label>Current Level:</label>
                                        <span>{binDetails.currentLevel}%</span>
                                    </div>
                                </div>

                                <form onSubmit={handleCollection} className="collection-form">
                                    <div className="form-group">
                                        <label className="form-label">Weight (kg)</label>
                                        <input
                                            type="number"
                                            step="0.1"
                                            min="0.1"
                                            className="form-input"
                                            placeholder="Enter weight in kilograms"
                                            value={collectionData.weight}
                                            onChange={(e) => setCollectionData({
                                                ...collectionData,
                                                weight: e.target.value
                                            })}
                                            required
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label className="form-label">Waste Type</label>
                                        <select
                                            className="form-input"
                                            value={collectionData.wasteType}
                                            onChange={(e) => setCollectionData({
                                                ...collectionData,
                                                wasteType: e.target.value
                                            })}
                                        >
                                            <option value="GENERAL_WASTE">General Waste</option>
                                            <option value="RECYCLABLE_PLASTIC">Recyclable Plastic</option>
                                            <option value="RECYCLABLE_PAPER">Recyclable Paper</option>
                                            <option value="E_WASTE">E-Waste</option>
                                        </select>
                                    </div>

                                    <button
                                        type="submit"
                                        className="btn btn-primary btn-block"
                                        disabled={loading}
                                    >
                                        {loading ? 'Recording...' : 'Record Collection'}
                                    </button>
                                </form>
                            </div>
                        ) : (
                            <div className="empty-state">
                                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                </svg>
                                <h4>No Bin Selected</h4>
                                <p>Search for a bin to record collection</p>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {activeTab === 'history' && (
                <div className="card">
                    <h3>Recent Collections</h3>
                    <div className="empty-state">
                        <p>Collection history will appear here</p>
                    </div>
                </div>
            )}
        </div>
    );
};

export default StaffDashboard;