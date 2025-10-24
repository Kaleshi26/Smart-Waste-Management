import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const StaffScanOffline = ({ user }) => {
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
    const [isOnline, setIsOnline] = useState(navigator.onLine);
    const [pendingCollections, setPendingCollections] = useState([]);
    const [syncing, setSyncing] = useState(false);
    const [deviceId, setDeviceId] = useState('');

    // Generate device ID on component mount
    useEffect(() => {
        const storedDeviceId = localStorage.getItem('staffDeviceId') || `staff-${user.id}-${Date.now()}`;
        localStorage.setItem('staffDeviceId', storedDeviceId);
        setDeviceId(storedDeviceId);
    }, [user.id]);

    // Network status detection
    useEffect(() => {
        const handleOnline = () => {
            setIsOnline(true);
            toast.success('Back online! Syncing available.');
        };

        const handleOffline = () => {
            setIsOnline(false);
            toast.warning('Working offline. Collections will be saved locally.');
        };

        window.addEventListener('online', handleOnline);
        window.addEventListener('offline', handleOffline);

        return () => {
            window.removeEventListener('online', handleOnline);
            window.removeEventListener('offline', handleOffline);
        };
    }, []);

    // Load pending collections on mount
    useEffect(() => {
        if (isOnline) {
            fetchPendingCollections();
        } else {
            loadLocalPendingCollections();
        }
    }, [isOnline]);

    const fetchPendingCollections = async () => {
        try {
            const response = await axios.get('http://localhost:8082/api/waste/collections/offline/pending', {
                headers: { 'Device-Id': deviceId }
            });
            setPendingCollections(response.data.pendingCollections || []);
        } catch (error) {
            console.error('Error fetching pending collections:', error);
            loadLocalPendingCollections(); // Fallback to local storage
        }
    };

    const loadLocalPendingCollections = () => {
        const localCollections = localStorage.getItem(`offlineCollections-${deviceId}`);
        if (localCollections) {
            setPendingCollections(JSON.parse(localCollections));
        }
    };

    const saveLocalPendingCollection = (collection) => {
        const updatedCollections = [...pendingCollections, collection];
        setPendingCollections(updatedCollections);
        localStorage.setItem(`offlineCollections-${deviceId}`, JSON.stringify(updatedCollections));
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

            if (!bin.resident) {
                toast.error('Warning: This bin is not assigned to any resident', {
                    icon: '⚠️',
                    duration: 4000
                });
            }
        } catch (error) {
            console.error('Error scanning bin:', error);

            if (!isOnline) {
                // In offline mode, create a temporary bin object
                const tempBin = {
                    binId: scanData.rfidTag,
                    location: 'Unknown (Offline Mode)',
                    binType: 'GENERAL',
                    resident: null,
                    _isOffline: true
                };
                setCurrentBin(tempBin);
                toast.success(`Bin ${scanData.rfidTag} recorded in offline mode`);
            } else {
                toast.error('Bin not found. Please check RFID tag or use manual entry.');
                setCurrentBin(null);
            }
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

            if (!isOnline) {
                // In offline mode, create a temporary bin object
                const tempBin = {
                    binId: scanData.binId,
                    location: 'Unknown (Offline Mode)',
                    binType: 'GENERAL',
                    resident: null,
                    _isOffline: true
                };
                setCurrentBin(tempBin);
                toast.success(`Bin ${scanData.binId} recorded in offline mode`);
            } else {
                toast.error('Bin not found. Please check Bin ID.');
                setCurrentBin(null);
            }
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
                truckId: scanData.truckId,
                notes: scanData.notes,
                location: currentBin.location || 'Unknown',
                binType: currentBin.binType || 'GENERAL',
                residentId: currentBin.resident?.id || null
            };

            let result;

            if (isOnline) {
                // Try online first, fallback to offline if fails
                try {
                    result = await axios.post('http://localhost:8082/api/waste/collections/record', collectionData);
                    toast.success(`Collection recorded for Bin ${currentBin.binId}!`);
                } catch (onlineError) {
                    console.warn('Online recording failed, falling back to offline:', onlineError);
                    await recordOfflineCollection(collectionData);
                }
            } else {
                await recordOfflineCollection(collectionData);
            }

            // Reset form for next collection
            resetForm();

        } catch (error) {
            console.error('Error recording collection:', error);
            toast.error(error.response?.data?.error || 'Failed to record collection');
        } finally {
            setScanning(false);
        }
    };

    const recordOfflineCollection = async (collectionData) => {
        try {
            if (isOnline) {
                // Use backend offline endpoint
                const response = await axios.post('http://localhost:8082/api/waste/collections/record-offline',
                    collectionData,
                    { headers: { 'Device-Id': deviceId } }
                );
                toast.success('Collection saved offline! Will sync when ready.');
                fetchPendingCollections(); // Refresh pending list
            } else {
                // Store locally in browser
                const offlineCollection = {
                    ...collectionData,
                    id: `offline-${Date.now()}`,
                    recordedAt: new Date().toISOString(),
                    deviceId: deviceId,
                    status: 'pending'
                };
                saveLocalPendingCollection(offlineCollection);
                toast.success('Collection saved locally! Sync when back online.');
            }
        } catch (error) {
            console.error('Error recording offline collection:', error);
            throw error;
        }
    };

    const syncPendingCollections = async () => {
        if (!isOnline) {
            toast.error('Cannot sync while offline');
            return;
        }

        setSyncing(true);
        try {
            // First sync any locally stored collections
            const localCollections = localStorage.getItem(`offlineCollections-${deviceId}`);
            if (localCollections) {
                const collections = JSON.parse(localCollections);
                for (const collection of collections) {
                    try {
                        await axios.post('http://localhost:8082/api/waste/collections/record-offline',
                            collection,
                            { headers: { 'Device-Id': deviceId } }
                        );
                    } catch (error) {
                        console.error('Error syncing local collection:', error);
                    }
                }
                // Clear local storage after successful sync
                localStorage.removeItem(`offlineCollections-${deviceId}`);
            }

            // Now sync with backend
            const response = await axios.post('http://localhost:8082/api/waste/collections/sync-offline',
                {},
                { headers: { 'Device-Id': deviceId } }
            );

            toast.success(response.data.message || 'All collections synced successfully!');
            setPendingCollections([]);

        } catch (error) {
            console.error('Error syncing collections:', error);
            toast.error('Failed to sync collections');
        } finally {
            setSyncing(false);
        }
    };

    const resetForm = () => {
        setScanData({
            rfidTag: '',
            binId: '',
            weight: '',
            notes: '',
            truckId: 'TRUCK-001'
        });
        setCurrentBin(null);
    };

    const getTotalPendingWeight = () => {
        return pendingCollections.reduce((sum, coll) => sum + (coll.weight || 0), 0);
    };

    return (
        <div className="scan-offline-page">
            {/* Header with Network Status */}
            <div className="page-header">
                <div>
                    <h1>📱 Offline Collection</h1>
                    <p>Record collections without internet connection</p>
                    <div className={`network-status ${isOnline ? 'online' : 'offline'}`}>
                        <span className="status-indicator"></span>
                        {isOnline ? 'Online - Real-time Mode' : 'Offline - Local Storage Mode'}
                    </div>
                </div>
                <div className="header-actions">
                    <div className="offline-stats">
                        <span className="pending-count">
                            {pendingCollections.length} pending collections
                        </span>
                        {isOnline && pendingCollections.length > 0 && (
                            <button
                                onClick={syncPendingCollections}
                                disabled={syncing}
                                className="btn btn-success btn-sm"
                            >
                                {syncing ? '🔄 Syncing...' : '📤 Sync Now'}
                            </button>
                        )}
                    </div>
                </div>
            </div>

            {/* Quick Stats */}
            <div className="stats-grid compact">
                <div className="stat-card offline">
                    <div className="stat-icon">📱</div>
                    <div className="stat-content">
                        <div className="stat-value">{pendingCollections.length}</div>
                        <div className="stat-title">Pending Sync</div>
                        <div className="stat-change">{getTotalPendingWeight().toFixed(1)} kg total</div>
                    </div>
                </div>

                <div className="stat-card warning">
                    <div className="stat-icon">🌐</div>
                    <div className="stat-content">
                        <div className="stat-value">{isOnline ? 'Online' : 'Offline'}</div>
                        <div className="stat-title">Network Status</div>
                        <div className="stat-change">
                            {isOnline ? 'Real-time mode' : 'Local storage'}
                        </div>
                    </div>
                </div>

                <div className="stat-card info">
                    <div className="stat-icon">🆔</div>
                    <div className="stat-content">
                        <div className="stat-value">{deviceId.slice(0, 8)}...</div>
                        <div className="stat-title">Device ID</div>
                        <div className="stat-change">For sync tracking</div>
                    </div>
                </div>
            </div>

            <div className="offline-interface">
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
                    <div className="scan-card offline">
                        <h3>RFID Scan {!isOnline && ' (Offline Mode)'}</h3>
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
                    <div className="scan-card offline">
                        <h3>Manual Entry {!isOnline && ' (Offline Mode)'}</h3>
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

                {/* Collection Data Form */}
                {currentBin && (
                    <div className="collection-form-card offline">
                        <div className="card-header">
                            <h3>Collection Data</h3>
                            {currentBin._isOffline && (
                                <span className="offline-badge">Offline Record</span>
                            )}
                        </div>

                        <div className="bin-info">
                            <div className="bin-detail">
                                <strong>Bin ID:</strong> {currentBin.binId}
                            </div>
                            <div className="bin-detail">
                                <strong>Location:</strong> {currentBin.location}
                            </div>
                            {currentBin.resident && (
                                <div className="bin-detail">
                                    <strong>Resident:</strong> {currentBin.resident.name}
                                </div>
                            )}
                        </div>

                        <div className="form-group">
                            <label className="form-label">Truck ID</label>
                            <input
                                type="text"
                                name="truckId"
                                className="form-input"
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
                                onChange={handleScanChange}
                                step="0.1"
                                min="0"
                                required
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
                            {scanning ? '📱 Saving...' : '💾 Save Collection'}
                            {!isOnline && ' (Offline)'}
                        </button>
                    </div>
                )}

                {/* Pending Collections List */}
                {pendingCollections.length > 0 && (
                    <div className="pending-collections-card">
                        <div className="card-header">
                            <h3>⏳ Pending Collections</h3>
                            <span className="badge">{pendingCollections.length} waiting</span>
                        </div>

                        <div className="pending-list">
                            {pendingCollections.slice(0, 5).map((collection, index) => (
                                <div key={collection.id || index} className="pending-item">
                                    <div className="pending-info">
                                        <strong>Bin {collection.binId}</strong>
                                        <span>{collection.location}</span>
                                        <small>{collection.weight} kg • {new Date(collection.recordedAt).toLocaleTimeString()}</small>
                                    </div>
                                    <div className="pending-status">
                                        <span className="status-offline">Pending</span>
                                    </div>
                                </div>
                            ))}

                            {pendingCollections.length > 5 && (
                                <div className="view-more">
                                    <small>+ {pendingCollections.length - 5} more collections pending</small>
                                </div>
                            )}
                        </div>

                        {isOnline && (
                            <div className="sync-section">
                                <button
                                    onClick={syncPendingCollections}
                                    disabled={syncing}
                                    className="btn btn-primary btn-block"
                                >
                                    {syncing ? '🔄 Syncing Collections...' : '📤 Sync All to Server'}
                                </button>
                            </div>
                        )}
                    </div>
                )}

                {/* Offline Instructions */}
                <div className="info-card">
                    <h4>📋 Offline Mode Instructions</h4>
                    <div className="instructions">
                        <div className="instruction-item">
                            <span className="instruction-number">1</span>
                            <span>Collections are saved locally when offline</span>
                        </div>
                        <div className="instruction-item">
                            <span className="instruction-number">2</span>
                            <span>Sync automatically when back online</span>
                        </div>
                        <div className="instruction-item">
                            <span className="instruction-number">3</span>
                            <span>Data is safe even if app closes</span>
                        </div>
                        <div className="instruction-item">
                            <span className="instruction-number">4</span>
                            <span>Manual sync available in online mode</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StaffScanOffline;