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

    // Store actual billing rates from backend
    const [billingRates, setBillingRates] = useState(null);
    const [recyclingRates, setRecyclingRates] = useState(null);

    // Fetch billing rates when component loads
    useEffect(() => {
        fetchBillingRates();
    }, []);

    // Check today's schedule when bin is found
    useEffect(() => {
        if (currentBin) {
            checkTodaySchedule();
        }
    }, [currentBin]);

    // UPDATED: Fetch all billing model fields
    const fetchBillingRates = async () => {
        try {
            console.log('🔄 Fetching billing rates...');

            // Fetch billing models
            const billingResponse = await axios.get('http://localhost:8082/api/billing/models');
            const billingModels = billingResponse.data;

            console.log('📋 All billing models:', billingModels);

            // Find the active billing model
            let activeModel = billingModels.find(model => model.active === true);

            console.log('✅ Active billing model:', activeModel);

            if (activeModel) {
                // DEBUG: Log the actual rates from backend
                console.log('💰 Rate details from backend:', {
                    ratePerKg: activeModel.ratePerKg,
                    monthlyFlatFee: activeModel.monthlyFlatFee,
                    baseFee: activeModel.baseFee,
                    additionalRatePerKg: activeModel.additionalRatePerKg,
                    billingType: activeModel.billingType
                });

                const rates = {
                    billingType: activeModel.billingType || 'WEIGHT_BASED',
                    ratePerKg: parseFloat(activeModel.ratePerKg) || 0,
                    monthlyFlatFee: parseFloat(activeModel.monthlyFlatFee) || 0,
                    baseFee: parseFloat(activeModel.baseFee) || 0,
                    additionalRatePerKg: parseFloat(activeModel.additionalRatePerKg) || 0,
                    modelName: activeModel.city || 'Default'
                };

                console.log('🎯 Final rates being used:', rates);
                setBillingRates(rates);
            } else {
                // Fallback to safe default
                console.warn('⚠️ No active billing model found, using defaults');
                setBillingRates({
                    billingType: 'WEIGHT_BASED',
                    ratePerKg: 5.0, // Make sure this is reasonable
                    monthlyFlatFee: 0,
                    baseFee: 0,
                    additionalRatePerKg: 0,
                    modelName: 'Default'
                });
            }

            // Recycling rates (these are fixed)
            const recyclingRatesData = {
                'PLASTIC': 0.8,
                'METAL': 1.2,
                'PAPER': 0.4,
                'GLASS': 0.3,
                'ELECTRONICS': 2.5
            };
            setRecyclingRates(recyclingRatesData);

        } catch (error) {
            console.error('❌ Error fetching billing rates:', error);
            // Fallback to safe defaults
            setBillingRates({
                billingType: 'WEIGHT_BASED',
                ratePerKg: 5.0,
                monthlyFlatFee: 0,
                baseFee: 0,
                additionalRatePerKg: 0,
                modelName: 'Default'
            });
            setRecyclingRates({
                'PLASTIC': 0.8,
                'METAL': 1.2,
                'PAPER': 0.4,
                'GLASS': 0.3,
                'ELECTRONICS': 2.5
            });
        }
    };
    const checkTodaySchedule = async () => {
        try {
            const response = await axios.get(`http://localhost:8082/api/waste/bins/${currentBin.binId}/schedule/today`);
            setTodaySchedule(response.data);
        } catch (error) {
            console.error('Error checking schedule:', error);
            setTodaySchedule(null);
        }
    };

    // UPDATED: Calculate charges to handle ALL billing types
    const calculateCharges = (weight, binType, recyclables = []) => {
        if (!billingRates || !recyclingRates) {
            console.log('❌ Billing rates not loaded yet');
            return null;
        }

        // Parse weight safely
        const parsedWeight = parseFloat(weight) || 0;

        console.log('🧮 Starting calculation:', {
            weight: parsedWeight,
            billingRates: billingRates,
            recyclablesCount: recyclables.length
        });

        const baseRate = billingRates.ratePerKg;
        const billingType = billingRates.billingType || 'WEIGHT_BASED';

        console.log('💰 Using base rate:', baseRate, 'for type:', billingType);

        let baseCharge = 0;
        let weightBasedCharge = 0;
        let otherCharges = 0;

        switch(billingType) {
            case 'WEIGHT_BASED':
                weightBasedCharge = parsedWeight * baseRate;
                console.log('📊 Weight based charge:', parsedWeight, '×', baseRate, '=', weightBasedCharge);
                break;

            case 'FLAT_FEE':
                baseCharge = billingRates.monthlyFlatFee || 0;
                console.log('📊 Flat fee charge:', baseCharge);
                break;

            case 'HYBRID':
                baseCharge = billingRates.baseFee || 0;
                weightBasedCharge = parsedWeight * (billingRates.additionalRatePerKg || 0);
                console.log('📊 Hybrid charge - Base:', baseCharge, '+ Weight:', weightBasedCharge);
                break;

            default:
                weightBasedCharge = parsedWeight * baseRate;
                console.log('📊 Default weight based charge:', weightBasedCharge);
        }

        // Calculate recycling refunds
        let totalRecyclingRefund = 0;
        const recyclingDetails = recyclables
            .filter(item => item.weightKg > 0)
            .map(item => {
                const itemWeight = item.weightKg || 0;
                const itemRate = recyclingRates[item.type] || 0;
                const refundAmount = itemWeight * itemRate;
                totalRecyclingRefund += refundAmount;

                console.log('♻️ Recycling item:', {
                    type: item.type,
                    weight: itemWeight,
                    rate: itemRate,
                    refund: refundAmount
                });

                return {
                    type: item.type,
                    weightKg: itemWeight,
                    quality: item.quality || 'GOOD',
                    notes: item.notes || '',
                    refundAmount: refundAmount,
                    ratePerKg: itemRate
                };
            });

        const totalCharge = Math.max(0, baseCharge + weightBasedCharge + otherCharges - totalRecyclingRefund);

        console.log('🎯 Final calculation:', {
            baseCharge,
            weightBasedCharge,
            otherCharges,
            totalRecyclingRefund,
            totalCharge
        });

        return {
            billingType,
            baseRate,
            baseCharge,
            weightBasedCharge,
            otherCharges,
            totalRecyclingRefund,
            recyclingDetails,
            totalCharge,
            weight: parsedWeight,
            binType,
            hasRecycling: totalRecyclingRefund > 0,
            modelName: billingRates.modelName
        };
    };
    const handleScanChange = (e) => {
        setScanData({
            ...scanData,
            [e.target.name]: e.target.value
        });
    };

    // Handle recyclable input changes
    const handleRecyclableChange = (index, field, value) => {
        const updatedRecyclables = [...recyclables];

        if (!updatedRecyclables[index]) {
            updatedRecyclables[index] = {
                type: 'PLASTIC',
                weightKg: 0,
                quality: 'GOOD',
                notes: ''
            };
        }

        updatedRecyclables[index] = {
            ...updatedRecyclables[index],
            [field]: field === 'weightKg' ? (parseFloat(value) || 0) : value
        };

        setRecyclables(updatedRecyclables);

        if (scanData.weight && currentBin && billingRates) {
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

        if (scanData.weight && currentBin && billingRates) {
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
        if (weight && currentBin && billingRates) {
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

    // Show loading if rates aren't loaded yet
    if (!billingRates || !recyclingRates) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading billing rates...</p>
            </div>
        );
    }

    return (
        <div className="scan-page">
            <div className="page-header">
                <div>
                    <h1>Record Collection</h1>
                    <p>Scan bins and record waste collection data</p>
                </div>
                <div className="header-actions">
                    <span className="staff-badge">Staff Mode</span>
                    {/* Show current billing rate and type */}
                    <div className="current-rate-badge">
                        {billingRates.billingType === 'WEIGHT_BASED' && `Rate: $${billingRates.ratePerKg}/kg`}
                        {billingRates.billingType === 'FLAT_FEE' && `Flat Fee: $${billingRates.monthlyFlatFee}`}
                        {billingRates.billingType === 'HYBRID' && `Hybrid: $${billingRates.baseFee} + $${billingRates.additionalRatePerKg}/kg`}
                    </div>
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
                                <label className="form-label">
                                    {billingRates.billingType === 'FLAT_FEE'
                                        ? 'Waste Weight (kg) - For Reporting Only'
                                        : 'Total Waste Weight (kg)'
                                    }
                                </label>
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
                                {billingRates.billingType === 'FLAT_FEE' && (
                                    <small className="form-help">
                                        Weight is recorded for reporting purposes only. Charge is based on flat fee.
                                    </small>
                                )}
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

                                    {/* Recycling Rates Info */}
                                    <div className="recycling-rates-info">
                                        <h6>💰 Recycling Refund Rates (per kg):</h6>
                                        <div className="rates-grid">
                                            {Object.entries(recyclingRates).map(([type, rate]) => (
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
                                                        <option value="PLASTIC">Plastic (${recyclingRates.PLASTIC}/kg)</option>
                                                        <option value="METAL">Metal (${recyclingRates.METAL}/kg)</option>
                                                        <option value="PAPER">Paper (${recyclingRates.PAPER}/kg)</option>
                                                        <option value="GLASS">Glass (${recyclingRates.GLASS}/kg)</option>
                                                        <option value="ELECTRONICS">Electronics (${recyclingRates.ELECTRONICS}/kg)</option>
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

                                                {/* Real-time recycling refund preview */}
                                                {item.weightKg > 0 && (
                                                    <div className="recycling-refund-preview">
                                                        <div className="refund-amount">
                                                            <strong>Refund for this item:</strong>
                                                            <span className="text-success">
                                                                ${((item.weightKg || 0) * recyclingRates[item.type]).toFixed(2)}
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

                            {/* UPDATED: Billing Preview - Handles all billing types */}
                            {billingInfo && (
                                <div className="billing-preview">
                                    <h5>💰 Charge Calculation ({billingInfo.billingType})</h5>
                                    <div className="charge-breakdown">
                                        {/* Show components based on billing type */}
                                        {billingInfo.baseCharge > 0 && (
                                            <div className="charge-item">
                                                <span>
                                                    {billingInfo.billingType === 'FLAT_FEE'
                                                        ? 'Monthly Flat Fee:'
                                                        : 'Base Charge:'
                                                    }
                                                </span>
                                                <span>${billingInfo.baseCharge.toFixed(2)}</span>
                                            </div>
                                        )}

                                        {billingInfo.weightBasedCharge > 0 && (
                                            <div className="charge-item">
                                                <span>
                                                    {billingInfo.billingType === 'WEIGHT_BASED'
                                                        ? `Weight Charge (${billingInfo.weight} kg × $${billingInfo.baseRate}/kg):`
                                                        : `Additional Weight Charge (${billingInfo.weight} kg × $${billingRates.additionalRatePerKg}/kg):`
                                                    }
                                                </span>
                                                <span>${billingInfo.weightBasedCharge.toFixed(2)}</span>
                                            </div>
                                        )}

                                        {billingInfo.otherCharges > 0 && (
                                            <div className="charge-item">
                                                <span>Other Charges:</span>
                                                <span>${billingInfo.otherCharges.toFixed(2)}</span>
                                            </div>
                                        )}

                                        {/* Recycling Refunds */}
                                        {billingInfo.hasRecycling && (
                                            <>
                                                <div className="charge-section-divider"></div>
                                                <div className="charge-item text-success">
                                                    <span>
                                                        <strong>♻️ Recycling Credits:</strong>
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
                                            <span><strong>Total Amount:</strong></span>
                                            <span><strong>${billingInfo.totalCharge.toFixed(2)}</strong></span>
                                        </div>

                                        {/* Savings Summary */}
                                        {billingInfo.hasRecycling && (
                                            <div className="savings-summary">
                                                <div className="savings-item">
                                                    <span>Resident Saves:</span>
                                                    <span className="text-success">${billingInfo.totalRecyclingRefund.toFixed(2)}</span>
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

                {/* UPDATED: Invoice Preview Modal */}
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
                                    <h5>Invoice Summary ({billingInfo.billingType})</h5>
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

                                        {/* UPDATED: Match invoice structure for all billing types */}
                                        {billingInfo.baseCharge > 0 && (
                                            <div className="invoice-item">
                                                <span>
                                                    {billingInfo.billingType === 'FLAT_FEE'
                                                        ? 'Monthly Flat Fee:'
                                                        : 'Base Charge:'
                                                    }
                                                </span>
                                                <span>${billingInfo.baseCharge.toFixed(2)}</span>
                                            </div>
                                        )}
                                        {billingInfo.weightBasedCharge > 0 && (
                                            <div className="invoice-item">
                                                <span>Weight-Based Charge:</span>
                                                <span>${billingInfo.weightBasedCharge.toFixed(2)}</span>
                                            </div>
                                        )}
                                        {billingInfo.otherCharges > 0 && (
                                            <div className="invoice-item">
                                                <span>Other Charges:</span>
                                                <span>${billingInfo.otherCharges.toFixed(2)}</span>
                                            </div>
                                        )}

                                        {/* Recycling Summary in Invoice */}
                                        {billingInfo.hasRecycling && (
                                            <>
                                                <div className="invoice-item text-success">
                                                    <span>Recycling Credits:</span>
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
                                            <span>Total Amount:</span>
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
                                            <li>✅ Recycling credits applied: <strong>${billingInfo.totalRecyclingRefund.toFixed(2)}</strong></li>
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
                            <p>Charges calculated using current billing model</p>
                        </div>
                    </div>
                    <div className="stat-item">
                        <div className="stat-icon">♻️</div>
                        <div className="stat-content">
                            <strong>Recycling Credits</strong>
                            <p>Residents get credits for recyclable materials</p>
                        </div>
                    </div>
                    <div className="stat-item">
                        <div className="stat-icon">💰</div>
                        <div className="stat-content">
                            <strong>Instant Savings</strong>
                            <p>Recycling credits applied immediately to invoices</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StaffScan;