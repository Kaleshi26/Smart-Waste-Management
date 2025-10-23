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

    // Recycling state
    const [recyclables, setRecyclables] = useState([]);
    const [showRecyclingSection, setShowRecyclingSection] = useState(false);

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

    // FIXED: Calculate charges with proper recycling refunds
    const calculateCharges = (weight, binType, recyclables = []) => {
        // Parse weight safely
        const parsedWeight = parseFloat(weight) || 0;

        // Base rates for waste collection
        const rates = {
            'GENERAL_WASTE': 0.5,
            'RECYCLABLE_PLASTIC': 0.3,
            'RECYCLABLE_PAPER': 0.2,
            'E_WASTE': 1.0
        };

        // Recycling refund rates per kg (what resident gets back)
        const recyclingRefundRates = {
            'PLASTIC': 0.8,
            'METAL': 1.2,
            'PAPER': 0.4,
            'GLASS': 0.3,
            'ELECTRONICS': 2.5
        };

        const baseRate = rates[binType] || 0.5;
        const weightCharge = parsedWeight * baseRate;
        const serviceFee = 2.0;

        // Calculate recycling refunds - only for items with positive weight
        let totalRecyclingRefund = 0;
        const recyclingDetails = recyclables
            .filter(item => item.weightKg > 0)
            .map(item => {
                const refundAmount = (item.weightKg || 0) * recyclingRefundRates[item.type];
                totalRecyclingRefund += refundAmount;
                return {
                    type: item.type,
                    weightKg: item.weightKg || 0,
                    quality: item.quality || 'GOOD',
                    notes: item.notes || '',
                    refundAmount: refundAmount,
                    ratePerKg: recyclingRefundRates[item.type]
                };
            });

        const totalCharge = Math.max(0, weightCharge + serviceFee - totalRecyclingRefund);

        return {
            baseRate,
            weightCharge,
            serviceFee,
            totalRecyclingRefund,
            recyclingDetails,
            totalCharge,
            weight: parsedWeight,
            binType,
            hasRecycling: totalRecyclingRefund > 0
        };
    };

    const handleScanChange = (e) => {
        setScanData({
            ...scanData,
            [e.target.name]: e.target.value
        });
    };

    // FIXED: Handle recyclable input changes with proper validation
    const handleRecyclableChange = (index, field, value) => {
        const updatedRecyclables = [...recyclables];

        // Ensure we have an object at this index
        if (!updatedRecyclables[index]) {
            updatedRecyclables[index] = {
                type: 'PLASTIC',
                weightKg: 0,
                quality: 'GOOD',
                notes: ''
            };
        }

        // Update the field
        updatedRecyclables[index] = {
            ...updatedRecyclables[index],
            [field]: field === 'weightKg' ? (parseFloat(value) || 0) : value
        };

        setRecyclables(updatedRecyclables);

        // Recalculate charges when recyclables change
        if (scanData.weight && currentBin) {
            const charges = calculateCharges(scanData.weight, currentBin.binType, updatedRecyclables);
            setBillingInfo(charges);
        }
    };

    // Add new recyclable type
    const addRecyclable = () => {
        setRecyclables([...recyclables, {
            type: 'PLASTIC',
            weightKg: 0,
            quality: 'GOOD',
            notes: ''
        }]);
    };

    // Remove recyclable
    const removeRecyclable = (index) => {
        const updatedRecyclables = recyclables.filter((_, i) => i !== index);
        setRecyclables(updatedRecyclables);

        if (scanData.weight && currentBin) {
            const charges = calculateCharges(scanData.weight, currentBin.binType, updatedRecyclables);
            setBillingInfo(charges);
        }
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
            const charges = calculateCharges(weight, currentBin.binType, recyclables);
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
            // Filter out recyclables with 0 weight
            const validRecyclables = recyclables.filter(item => item.weightKg > 0);

            const collectionData = {
                binId: currentBin.binId,
                collectorId: user.id,
                weight: parseFloat(scanData.weight),
                truckId: scanData.truckId,
                notes: scanData.notes,
                // Include only recyclables with positive weight
                recyclables: validRecyclables
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
        setRecyclables([]);
        setShowRecyclingSection(false);
        setShowInvoicePreview(false);
        toast.success('Ready for next collection!');
    };

    // FIXED: Get recycling rates for display
    const getRecyclingRatesInfo = () => {
        return {
            'PLASTIC': 0.8,
            'METAL': 1.2,
            'PAPER': 0.4,
            'GLASS': 0.3,
            'ELECTRONICS': 2.5
        };
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
                                <label className="form-label">Total Waste Weight (kg)</label>
                                <input
                                    type="number"
                                    name="weight"
                                    className="form-input"
                                    placeholder="Enter weight in kilograms"
                                    value={scanData.weight}
                                    onChange={(e) => handleWeightChange(e.target.value)}
                                    step="0.1"
                                    min="0"
                                    required
                                />
                            </div>

                            {/* Recycling Section Toggle */}
                            <div className="recycling-toggle">
                                <button
                                    type="button"
                                    className={`btn btn-sm ${showRecyclingSection ? 'btn-success' : 'btn-outline'}`}
                                    onClick={() => setShowRecyclingSection(!showRecyclingSection)}
                                >
                                    ♻️ {showRecyclingSection ? 'Hide' : 'Add'} Recyclables
                                </button>
                            </div>

                            {/* Recyclables Input Section */}
                            {showRecyclingSection && (
                                <div className="recyclables-section">
                                    <h5>♻️ Recyclable Materials</h5>
                                    <p className="section-description">
                                        Record any recyclable materials found in the waste. This will provide refunds to the resident.
                                    </p>

                                    {/* NEW: Recycling Rates Info */}
                                    <div className="recycling-rates-info">
                                        <h6>💰 Recycling Refund Rates (per kg):</h6>
                                        <div className="rates-grid">
                                            {Object.entries(getRecyclingRatesInfo()).map(([type, rate]) => (
                                                <div key={type} className="rate-item">
                                                    <span className="material-type">{type}:</span>
                                                    <span className="rate-amount">${rate.toFixed(2)}</span>
                                                </div>
                                            ))}
                                        </div>
                                    </div>

                                    {recyclables.map((item, index) => (
                                        <div key={index} className="recyclable-item">
                                            <div className="recyclable-header">
                                                <h6>Recyclable #{index + 1}</h6>
                                                <button
                                                    type="button"
                                                    className="btn btn-sm btn-danger"
                                                    onClick={() => removeRecyclable(index)}
                                                >
                                                    Remove
                                                </button>
                                            </div>
                                            <div className="recyclable-form">
                                                <div className="form-group">
                                                    <label>Material Type</label>
                                                    <select
                                                        value={item.type}
                                                        onChange={(e) => handleRecyclableChange(index, 'type', e.target.value)}
                                                        className="form-input"
                                                    >
                                                        <option value="PLASTIC">Plastic (${getRecyclingRatesInfo().PLASTIC}/kg)</option>
                                                        <option value="METAL">Metal (${getRecyclingRatesInfo().METAL}/kg)</option>
                                                        <option value="PAPER">Paper (${getRecyclingRatesInfo().PAPER}/kg)</option>
                                                        <option value="GLASS">Glass (${getRecyclingRatesInfo().GLASS}/kg)</option>
                                                        <option value="ELECTRONICS">Electronics (${getRecyclingRatesInfo().ELECTRONICS}/kg)</option>
                                                    </select>
                                                </div>
                                                <div className="form-group">
                                                    <label>Weight (kg)</label>
                                                    <input
                                                        type="number"
                                                        step="0.1"
                                                        min="0"
                                                        value={item.weightKg || ''}
                                                        onChange={(e) => handleRecyclableChange(index, 'weightKg', e.target.value)}
                                                        className="form-input"
                                                        placeholder="0.0"
                                                    />
                                                </div>
                                                <div className="form-group">
                                                    <label>Quality</label>
                                                    <select
                                                        value={item.quality}
                                                        onChange={(e) => handleRecyclableChange(index, 'quality', e.target.value)}
                                                        className="form-input"
                                                    >
                                                        <option value="EXCELLENT">Excellent</option>
                                                        <option value="GOOD">Good</option>
                                                        <option value="AVERAGE">Average</option>
                                                        <option value="POOR">Poor</option>
                                                    </select>
                                                </div>
                                                <div className="form-group">
                                                    <label>Notes (Optional)</label>
                                                    <input
                                                        type="text"
                                                        value={item.notes}
                                                        onChange={(e) => handleRecyclableChange(index, 'notes', e.target.value)}
                                                        className="form-input"
                                                        placeholder="e.g., Clean bottles, aluminum cans..."
                                                    />
                                                </div>

                                                {/* NEW: Real-time recycling refund preview */}
                                                {item.weightKg > 0 && (
                                                    <div className="recycling-refund-preview">
                                                        <div className="refund-amount">
                                                            <strong>Refund for this item:</strong>
                                                            <span className="text-success">
                                                                ${((item.weightKg || 0) * getRecyclingRatesInfo()[item.type]).toFixed(2)}
                                                            </span>
                                                        </div>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    ))}

                                    <button
                                        type="button"
                                        className="btn btn-secondary btn-sm"
                                        onClick={addRecyclable}
                                    >
                                        + Add Another Recyclable
                                    </button>
                                </div>
                            )}

                            {/* Billing Preview */}
                            {billingInfo && (
                                <div className="billing-preview">
                                    <h5>💰 Charge Calculation</h5>
                                    <div className="charge-breakdown">
                                        <div className="charge-item">
                                            <span>Weight ({billingInfo.weight} kg × ${billingInfo.baseRate}/kg):</span>
                                            <span>${billingInfo.weightCharge.toFixed(2)}</span>
                                        </div>
                                        <div className="charge-item">
                                            <span>Service Fee:</span>
                                            <span>${billingInfo.serviceFee.toFixed(2)}</span>
                                        </div>

                                        {/* Recycling Refunds */}
                                        {billingInfo.hasRecycling && (
                                            <>
                                                <div className="charge-section-divider"></div>
                                                <div className="charge-item text-success">
                                                    <span>
                                                        <strong>♻️ Recycling Refunds:</strong>
                                                    </span>
                                                    <span><strong>-${billingInfo.totalRecyclingRefund.toFixed(2)}</strong></span>
                                                </div>
                                                {billingInfo.recyclingDetails.map((item, index) => (
                                                    <div key={index} className="charge-subitem text-success">
                                                        <span>  └ {item.type}: {item.weightKg}kg × ${item.ratePerKg}/kg</span>
                                                        <span>-${item.refundAmount.toFixed(2)}</span>
                                                    </div>
                                                ))}
                                            </>
                                        )}

                                        <div className="charge-section-divider"></div>
                                        <div className="charge-item total">
                                            <span><strong>Final Charge:</strong></span>
                                            <span><strong>${billingInfo.totalCharge.toFixed(2)}</strong></span>
                                        </div>

                                        {/* NEW: Savings Summary */}
                                        {billingInfo.hasRecycling && (
                                            <div className="savings-summary">
                                                <div className="savings-item">
                                                    <span>Resident Saves:</span>
                                                    <span className="text-success">${billingInfo.totalRecyclingRefund.toFixed(2)}</span>
                                                </div>
                                                <div className="savings-item">
                                                    <span>Original Amount:</span>
                                                    <span className="text-muted">${(billingInfo.weightCharge + billingInfo.serviceFee).toFixed(2)}</span>
                                                </div>
                                            </div>
                                        )}
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

                                        {/* Recycling Summary in Invoice */}
                                        {billingInfo.hasRecycling && (
                                            <>
                                                <div className="invoice-item text-success">
                                                    <span>Recycling Refunds:</span>
                                                    <span>-${billingInfo.totalRecyclingRefund.toFixed(2)}</span>
                                                </div>
                                                {billingInfo.recyclingDetails.map((item, index) => (
                                                    <div key={index} className="invoice-subitem text-success">
                                                        <span>  └ {item.type}: {item.weightKg}kg</span>
                                                        <span>-${item.refundAmount.toFixed(2)}</span>
                                                    </div>
                                                ))}
                                            </>
                                        )}

                                        <div className="invoice-item total">
                                            <span>Total Charge:</span>
                                            <span className="total-amount">${billingInfo.totalCharge.toFixed(2)}</span>
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
                                        {billingInfo.hasRecycling && (
                                            <li>✅ Recycling refunds applied: <strong>${billingInfo.totalRecyclingRefund.toFixed(2)}</strong></li>
                                        )}
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
                        <div className="stat-icon">♻️</div>
                        <div className="stat-content">
                            <strong>Recycling Refunds</strong>
                            <p>Residents get refunds for recyclable materials</p>
                        </div>
                    </div>
                    <div className="stat-item">
                        <div className="stat-icon">💰</div>
                        <div className="stat-content">
                            <strong>Instant Savings</strong>
                            <p>Recycling refunds applied immediately to invoices</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StaffScan;