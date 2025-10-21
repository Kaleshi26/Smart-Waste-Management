// File: frontend/src/pages/StaffScan.jsx
import React, { useState } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const StaffScan = ({ user }) => {
    const [scanData, setScanData] = useState({
        rfidTag: '',
        binId: '',
        weight: '',
        notes: ''
    });
    const [scanning, setScanning] = useState(false);
    const [currentBin, setCurrentBin] = useState(null);
    const [scanMode, setScanMode] = useState('rfid'); // 'rfid' or 'manual'

    const handleScanChange = (e) => {
        setScanData({
            ...scanData,
            [e.target.name]: e.target.value
        });
    };

    const handleRFIDScan = async () => {
        if (!scanData.rfidTag.trim()) {
            toast.error('Please enter an RFID tag');
            return;
        }

        setScanning(true);
        try {
            // Look up bin by RFID
            const response = await axios.get(`http://localhost:8082/api/waste/bins/rfid/${scanData.rfidTag}`);
            const bin = response.data;
            setCurrentBin(bin);
            toast.success(`Bin ${bin.binId} found!`);
        } catch (error) {
            console.error('Error scanning bin:', error);
            toast.error('Bin not found. Please check RFID tag or use manual entry.');
            setCurrentBin(null);
        } finally {
            setScanning(false);
        }
    };

    const handleManualLookup = async () => {
        if (!scanData.binId.trim()) {
            toast.error('Please enter a Bin ID');
            return;
        }

        setScanning(true);
        try {
            const response = await axios.get(`http://localhost:8082/api/waste/bins/${scanData.binId}`);
            const bin = response.data;
            setCurrentBin(bin);
            toast.success(`Bin ${bin.binId} found!`);
        } catch (error) {
            console.error('Error looking up bin:', error);
            toast.error('Bin not found. Please check Bin ID.');
            setCurrentBin(null);
        } finally {
            setScanning(false);
        }
    };

    const recordCollection = async () => {
        if (!currentBin) {
            toast.error('Please scan or lookup a bin first');
            return;
        }

        if (!scanData.weight || parseFloat(scanData.weight) <= 0) {
            toast.error('Please enter a valid weight');
            return;
        }

        setScanning(true);
        try {
            const collectionData = {
                binId: currentBin.binId,
                collectorId: user.id,
                weight: parseFloat(scanData.weight),
                notes: scanData.notes
            };

            await axios.post('http://localhost:8082/api/waste/collections/record', collectionData);

            toast.success(`Collection recorded for Bin ${currentBin.binId}!`);

            // Reset form
            setScanData({
                rfidTag: '',
                binId: '',
                weight: '',
                notes: ''
            });
            setCurrentBin(null);

        } catch (error) {
            console.error('Error recording collection:', error);
            toast.error(error.response?.data?.error || 'Failed to record collection');
        } finally {
            setScanning(false);
        }
    };

    return (
        <div className="scan-page">
            <div className="page-header">
                <div>
                    <h1>Record Collection</h1>
                    <p>Scan bins and record waste collection data</p>
                </div>
                <div className="header-actions">
                    <span className="staff-badge">Staff Mode</span>
                </div>
            </div>

            <div className="scan-interface">
                {/* Scan Mode Toggle */}
                <div className="scan-mode-toggle">
                    <button
                        className={scanMode === 'rfid' ? 'active' : ''}
                        onClick={() => setScanMode('rfid')}
                    >
                        📱 RFID Scan
                    </button>
                    <button
                        className={scanMode === 'manual' ? 'active' : ''}
                        onClick={() => setScanMode('manual')}
                    >
                        ⌨️ Manual Entry
                    </button>
                </div>

                {/* RFID Scan Mode */}
                {scanMode === 'rfid' && (
                    <div className="scan-card">
                        <h3>RFID Scan</h3>
                        <div className="form-group">
                            <label className="form-label">RFID Tag</label>
                            <input
                                type="text"
                                name="rfidTag"
                                className="form-input"
                                placeholder="Scan or enter RFID tag"
                                value={scanData.rfidTag}
                                onChange={handleScanChange}
                                disabled={scanning}
                            />
                        </div>
                        <button
                            onClick={handleRFIDScan}
                            disabled={scanning}
                            className="btn btn-primary btn-block"
                        >
                            {scanning ? 'Scanning...' : 'Scan Bin'}
                        </button>
                    </div>
                )}

                {/* Manual Entry Mode */}
                {scanMode === 'manual' && (
                    <div className="scan-card">
                        <h3>Manual Entry</h3>
                        <div className="form-group">
                            <label className="form-label">Bin ID</label>
                            <input
                                type="text"
                                name="binId"
                                className="form-input"
                                placeholder="Enter Bin ID"
                                value={scanData.binId}
                                onChange={handleScanChange}
                                disabled={scanning}
                            />
                        </div>
                        <button
                            onClick={handleManualLookup}
                            disabled={scanning}
                            className="btn btn-primary btn-block"
                        >
                            {scanning ? 'Looking up...' : 'Lookup Bin'}
                        </button>
                    </div>
                )}

                {/* Bin Information */}
                {currentBin && (
                    <div className="bin-info-card">
                        <h3>Bin Found ✅</h3>
                        <div className="bin-details">
                            <div className="detail-row">
                                <label>Bin ID:</label>
                                <span>{currentBin.binId}</span>
                            </div>
                            <div className="detail-row">
                                <label>Location:</label>
                                <span>{currentBin.location}</span>
                            </div>
                            <div className="detail-row">
                                <label>Type:</label>
                                <span>{currentBin.binType?.replace(/_/g, ' ')}</span>
                            </div>
                            <div className="detail-row">
                                <label>Capacity:</label>
                                <span>{currentBin.capacity}L</span>
                            </div>
                            <div className="detail-row">
                                <label>Current Level:</label>
                                <span>{currentBin.currentLevel || 0}%</span>
                            </div>
                        </div>

                        {/* Collection Data Form */}
                        <div className="collection-form">
                            <h4>Collection Data</h4>
                            <div className="form-group">
                                <label className="form-label">Weight (kg)</label>
                                <input
                                    type="number"
                                    name="weight"
                                    className="form-input"
                                    placeholder="Enter weight in kilograms"
                                    value={scanData.weight}
                                    onChange={handleScanChange}
                                    step="0.1"
                                    min="0"
                                />
                            </div>
                            <div className="form-group">
                                <label className="form-label">Notes (Optional)</label>
                                <textarea
                                    name="notes"
                                    className="form-input"
                                    placeholder="Any additional notes..."
                                    rows="3"
                                    value={scanData.notes}
                                    onChange={handleScanChange}
                                />
                            </div>
                            <button
                                onClick={recordCollection}
                                disabled={scanning || !scanData.weight}
                                className="btn btn-success btn-block"
                            >
                                {scanning ? 'Recording...' : 'Record Collection'}
                            </button>
                        </div>
                    </div>
                )}

                {/* Quick Actions */}
                <div className="quick-actions-scan">
                    <div className="action-item">
                        <div className="action-icon">🔍</div>
                        <div className="action-content">
                            <strong>Scan Tips</strong>
                            <p>Ensure RFID tag is clean and visible</p>
                        </div>
                    </div>
                    <div className="action-item">
                        <div className="action-icon">⚖️</div>
                        <div className="action-content">
                            <strong>Weight Accuracy</strong>
                            <p>Record precise weights for billing</p>
                        </div>
                    </div>
                    <div className="action-item">
                        <div className="action-icon">📝</div>
                        <div className="action-content">
                            <strong>Note Issues</strong>
                            <p>Report damaged bins in notes</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StaffScan;