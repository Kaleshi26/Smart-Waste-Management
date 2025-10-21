// File: frontend/src/components/StaffDashboard.jsx
import React, { useState } from 'react';
import axios from 'axios';

function StaffDashboard({ user }) {
    const [view, setView] = useState('scan');
    const [inputValue, setInputValue] = useState('');
    const [binDetails, setBinDetails] = useState(null);
    const [weight, setWeight] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);

    // Step 1: Get bin details
    const handleScan = async (event) => {
        event.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(null);
        try {
            const response = await axios.get(`http://localhost:8082/api/waste/bins/${inputValue}`);
            setBinDetails(response.data);
            setView('confirm');
        } catch (err) {
            setError(err.response?.data?.error || 'Error fetching bin details.');
        } finally {
            setLoading(false);
        }
    };

    // Step 2: Record collection
    const handleConfirm = async (event) => {
        event.preventDefault();
        setLoading(true);
        setError(null);
        setSuccess(null);
        try {
            const collectionData = {
                binId: binDetails.binId,
                staffId: user.id,
                weight: parseFloat(weight),
                wasteType: binDetails.binType || 'GENERAL_WASTE'
            };

            const response = await axios.post('http://localhost:8082/api/waste/collections/record', collectionData);
            setSuccess(`Collection for bin ${binDetails.binId} recorded successfully! Charge: $${response.data.collection?.calculatedCharge || 0}`);
            handleCancel();
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to record collection.');
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = () => {
        setView('scan');
        setInputValue('');
        setBinDetails(null);
        setWeight('');
        setError(null);
    };

    // Scan View
    if (view === 'scan') {
        return (
            <div className="dashboard-container">
                <h2 className="dashboard-title">Scan Waste Bin</h2>
                <div className="dashboard-card">
                    <form onSubmit={handleScan} className="login-form" style={{ boxShadow: 'none', padding: 0 }}>
                        {success && <p className="success-message">{success}</p>}
                        {error && <p className="error-message">{error}</p>}
                        <div className="form-group">
                            <label htmlFor="binId">Enter Bin ID</label>
                            <input
                                id="binId"
                                type="text"
                                value={inputValue}
                                onChange={(e) => setInputValue(e.target.value)}
                                placeholder="e.g., BIN-COL-001"
                                required
                            />
                        </div>
                        <button type="submit" disabled={loading}>
                            {loading ? 'Searching...' : 'Get Bin Details'}
                        </button>
                    </form>
                </div>
            </div>
        );
    }

    // Confirm View
    if (view === 'confirm') {
        return (
            <div className="dashboard-container">
                <h2 className="dashboard-title">Confirm Collection</h2>
                <div className="dashboard-card">
                    <div className="bin-details">
                        <h3>Bin Details</h3>
                        <p><strong>Bin ID:</strong> {binDetails.binId}</p>
                        <p><strong>Location:</strong> {binDetails.location}</p>
                        <p><strong>Type:</strong> {binDetails.binType}</p>
                        <p><strong>Capacity:</strong> {binDetails.capacity}L</p>
                        {binDetails.resident && (
                            <p><strong>Resident:</strong> {binDetails.resident.name}</p>
                        )}
                    </div>
                    <form onSubmit={handleConfirm} className="login-form" style={{ boxShadow: 'none', padding: 0, marginTop: '1.5rem' }}>
                        {error && <p className="error-message">{error}</p>}
                        <div className="form-group">
                            <label htmlFor="weight">Enter Weight (Kg)</label>
                            <input
                                id="weight"
                                type="number"
                                value={weight}
                                onChange={(e) => setWeight(e.target.value)}
                                step="0.1"
                                min="0.1"
                                placeholder="e.g., 12.5"
                                required
                            />
                        </div>
                        <div className="button-group">
                            <button type="button" className="cancel-button" onClick={handleCancel}>
                                Cancel
                            </button>
                            <button type="submit" disabled={loading}>
                                {loading ? 'Confirming...' : 'Confirm Collection'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        );
    }
}

export default StaffDashboard;