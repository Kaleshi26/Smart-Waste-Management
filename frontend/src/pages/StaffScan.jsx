// File: frontend/src/pages/StaffScan.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const StaffScan = ({ user }) => {
    const [scanData, setScanData] = useState({
        rfidTag: '',
        binId: '',
        weight: '',
        notes: '',
        truckId: 'TRUCK-001'
    });
    const [scanning, setScanning] = useState(false);
    const [currentBin, setCurrentBin] = useState(null);
    const [scanMode, setScanMode] = useState('rfid');
    const [todaySchedule, setTodaySchedule] = useState(null);
    const [billingInfo, setBillingInfo] = useState(null);
    const [showInvoicePreview, setShowInvoicePreview] = useState(false);

    // Check today's schedule when bin is found
    useEffect(() => {
        if (currentBin) {
            checkTodaySchedule();
        }
    }, [currentBin]);

    const checkTodaySchedule = async () => {
        try {
            const response = await axios.get(`http://localhost:8082/api/waste/bins/${currentBin.binId}/schedule/today`);
            setTodaySchedule(response.data);
        } catch (error) {
            console.error('Error checking schedule:', error);
            setTodaySchedule(null);
        }
    };

    const calculateCharges = (weight, binType) => {
        // Base rates - in real app, this would come from backend billing model
        const rates = {
            'GENERAL_WASTE': 0.5, // $0.5 per kg
            'RECYCLABLE_PLASTIC': 0.3,
            'RECYCLABLE_PAPER': 0.2,
            'E_WASTE': 1.0
        };

        const baseRate = rates[binType] || 0.5;
        const weightCharge = weight * baseRate;
        const serviceFee = 2.0; // Fixed service fee
        const totalCharge = weightCharge + serviceFee;

        return {
            baseRate,
            weightCharge,
            serviceFee,
            totalCharge,
            weight,
            binType
        };
    };

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

    const handleWeightChange = (weight) => {
        setScanData({ ...scanData, weight });

        if (weight && currentBin) {
            const charges = calculateCharges(parseFloat(weight), currentBin.binType);
            setBillingInfo(charges);
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

        // Check if bin is scheduled for today
        if (!todaySchedule?.scheduled) {
            const proceed = window.confirm(
                'This bin is not scheduled for collection today. Do you want to proceed anyway?'
            );
            if (!proceed) return;
        }

        setScanning(true);
        try {
            const collectionData = {
                binId: currentBin.binId,
                collectorId: user.id,
                weight: parseFloat(scanData.weight),
                truckId: scanData.truckId,
                notes: scanData.notes
            };

            const response = await axios.post('http://localhost:8082/api/waste/collections/record', collectionData);

            // Show invoice preview
            setShowInvoicePreview(true);
            toast.success(`Collection recorded for Bin ${currentBin.binId}!`);

        } catch (error) {
            console.error('Error recording collection:', error);
            toast.error(error.response?.data?.error || 'Failed to record collection');
        } finally {
            setScanning(false);
        }
    };

    const finishCollection = () => {
        // Reset form for next collection
        setScanData({
            rfidTag: '',
            binId: '',
            weight: '',
            notes: '',
            truckId: 'TRUCK-001'
        });
        setCurrentBin(null);
        setTodaySchedule(null);
        setBillingInfo(null);
        setShowInvoicePreview(false);
        toast.success('Ready for next collection!');
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
                        <div className="bin-header">
                            <h3>Bin Found ✅</h3>
                            {todaySchedule?.scheduled && (
                                <span className="schedule-badge">Scheduled for Today</span>
                            )}
                        </div>

                        <div className="bin-details-grid">
                            <div className="detail-item">
                                <label>Bin ID:</label>
                                <span>{currentBin.binId}</span>
                            </div>
                            <div className="detail-item">
                                <label>Location:</label>
                                <span>{currentBin.location}</span>
                            </div>
                            <div className="detail-item">
                                <label>Type:</label>
                                <span>{currentBin.binType?.replace(/_/g, ' ')}</span>
                            </div>
                            <div className="detail-item">
                                <label>Capacity:</label>
                                <span>{currentBin.capacity}L</span>
                            </div>
                            <div className="detail-item">
                                <label>Current Level:</label>
                                <span>{currentBin.currentLevel || 0}%</span>
                            </div>
                            <div className="detail-item">
                                <label>Resident:</label>
                                <span>{currentBin.resident?.name || 'Not assigned'}</span>
                            </div>
                        </div>

                        {/* Collection Data Form */}
                        <div className="collection-form">
                            <h4>Collection Data</h4>

                            <div className="form-group">
                                <label className="form-label">Truck ID</label>
                                <input
                                    type="text"
                                    name="truckId"
                                    className="form-input"
                                    placeholder="Enter truck ID"
                                    value={scanData.truckId}
                                    onChange={handleScanChange}
                                />
                            </div>

                            <div className="form-group">
                                <label className="form-label">Weight (kg)</label>
                                <input
                                    type="number"
                                    name="weight"
                                    className="form-input"
                                    placeholder="Enter weight in kilograms"
                                    value={scanData.weight}
                                    onChange={(e) => handleWeightChange(e.target.value)}
                                    step="0.1"
                                    min="0"
                                />
                            </div>

                            {/* Billing Preview */}
                            {billingInfo && (
                                <div className="billing-preview">
                                    <h5>Charge Calculation</h5>
                                    <div className="charge-breakdown">
                                        <div className="charge-item">
                                            <span>Weight ({billingInfo.weight} kg × ${billingInfo.baseRate}/kg):</span>
                                            <span>${billingInfo.weightCharge.toFixed(2)}</span>
                                        </div>
                                        <div className="charge-item">
                                            <span>Service Fee:</span>
                                            <span>${billingInfo.serviceFee.toFixed(2)}</span>
                                        </div>
                                        <div className="charge-item total">
                                            <span>Total Charge:</span>
                                            <span>${billingInfo.totalCharge.toFixed(2)}</span>
                                        </div>
                                    </div>
                                </div>
                            )}

                            <div className="form-group">
                                <label className="form-label">Notes (Optional)</label>
                                <textarea
                                    name="notes"
                                    className="form-input"
                                    placeholder="Any additional notes or issues..."
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

                {/* Invoice Preview Modal */}
                {showInvoicePreview && billingInfo && (
                    <div className="modal-overlay">
                        <div className="modal">
                            <div className="modal-header">
                                <h3>Collection Successful! 🎉</h3>
                            </div>
                            <div className="modal-body">
                                <div className="success-message">
                                    <div className="success-icon">✅</div>
                                    <h4>Collection Recorded Successfully</h4>
                                    <p>Bin {currentBin?.binId} collection has been recorded and invoice generated.</p>
                                </div>

                                <div className="invoice-summary">
                                    <h5>Invoice Summary</h5>
                                    <div className="invoice-details">
                                        <div className="invoice-item">
                                            <span>Bin ID:</span>
                                            <span>{currentBin?.binId}</span>
                                        </div>
                                        <div className="invoice-item">
                                            <span>Weight Collected:</span>
                                            <span>{billingInfo.weight} kg</span>
                                        </div>
                                        <div className="invoice-item">
                                            <span>Waste Type:</span>
                                            <span>{currentBin?.binType?.replace(/_/g, ' ')}</span>
                                        </div>
                                        <div className="invoice-item">
                                            <span>Total Charge:</span>
                                            <span className="total-amount">${billingInfo.totalCharge.toFixed(2)}</span>
                                        </div>
                                        <div className="invoice-item">
                                            <span>Resident:</span>
                                            <span>{currentBin?.resident?.name || 'Not assigned'}</span>
                                        </div>
                                    </div>
                                </div>

                                <div className="next-steps">
                                    <h5>Next Steps</h5>
                                    <p>The invoice has been automatically generated and added to the resident's account.</p>
                                    <ul>
                                        <li>✅ Collection recorded in system</li>
                                        <li>✅ Invoice generated for resident</li>
                                        <li>✅ Bin level reset to 0%</li>
                                        <li>✅ Collection marked as completed</li>
                                    </ul>
                                </div>
                            </div>
                            <div className="modal-actions">
                                <button
                                    onClick={finishCollection}
                                    className="btn btn-primary btn-block"
                                >
                                    Continue to Next Bin
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                {/* Quick Stats */}
                <div className="quick-stats">
                    <div className="stat-item">
                        <div className="stat-icon">📊</div>
                        <div className="stat-content">
                            <strong>Real-time Billing</strong>
                            <p>Charges calculated automatically based on weight and waste type</p>
                        </div>
                    </div>
                    <div className="stat-item">
                        <div className="stat-icon">⏰</div>
                        <div className="stat-content">
                            <strong>Schedule Check</strong>
                            <p>Automatically verifies if bin is scheduled for today</p>
                        </div>
                    </div>
                    <div className="stat-item">
                        <div className="stat-icon">🧾</div>
                        <div className="stat-content">
                            <strong>Auto Invoice</strong>
                            <p>Invoice generated automatically after collection</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StaffScan;