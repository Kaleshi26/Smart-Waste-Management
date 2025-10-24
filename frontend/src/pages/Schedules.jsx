import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const Schedules = ({ user }) => {
    const [schedules, setSchedules] = useState([]);
    const [bins, setBins] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [newSchedule, setNewSchedule] = useState({
        binId: '',
        scheduledDate: '',
        scheduledTime: '09:00',
        notes: ''
    });

    useEffect(() => {
        fetchSchedules();
        fetchBins();
    }, [user.id]);

    const fetchSchedules = async () => {
        try {
            console.log('Fetching schedules for resident ID:', user.id);
            const response = await axios.get(`http://localhost:8082/api/waste/schedules/resident/${user.id}`);
            console.log('Schedules response:', response.data);

            // The response data now has the DTO structure with bin details
            setSchedules(response.data || []);
        } catch (error) {
            console.error('Error fetching schedules:', error);
            toast.error('Failed to load schedules');
        } finally {
            setLoading(false);
        }
    };

    const fetchBins = async () => {
        try {
            const response = await axios.get(`http://localhost:8082/api/waste/bins/resident/${user.id}`);
            setBins(response.data || []);
        } catch (error) {
            console.error('Error fetching bins:', error);
        }
    };

    const handleCreateSchedule = async (e) => {
        e.preventDefault();
        try {
            await axios.post('http://localhost:8082/api/waste/schedules/create', newSchedule);
            toast.success('Collection scheduled successfully!');
            setShowCreateModal(false);
            setNewSchedule({
                binId: '',
                scheduledDate: '',
                scheduledTime: '09:00',
                notes: ''
            });
            fetchSchedules();
        } catch (error) {
            toast.error(error.response?.data?.error || 'Failed to schedule collection');
        }
    };

    const getStatusBadge = (status) => {
        const statusConfig = {
            PENDING: { class: 'pending', label: 'Pending' },
            COMPLETED: { class: 'success', label: 'Completed' },
            CANCELLED: { class: 'danger', label: 'Cancelled' },
            IN_PROGRESS: { class: 'warning', label: 'In Progress' }
        };
        const config = statusConfig[status] || { class: 'pending', label: status };
        return <span className={`status-badge status-${config.class}`}>{config.label}</span>;
    };

    const formatDateTime = (date, time) => {
        return `${date} at ${time}`;
    };

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading your schedules...</p>
            </div>
        );
    }

    return (
        <div className="schedules-page">
            <div className="page-header">
                <div>
                    <h1>Collection Schedules</h1>
                    <p>Manage your waste collection appointments</p>
                </div>
                <div className="header-actions">
                    <button
                        onClick={() => setShowCreateModal(true)}
                        className="btn btn-primary"
                        disabled={bins.length === 0}
                    >
                        Schedule Collection
                    </button>
                </div>
            </div>

            {/* Stats */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-title">Total Schedules</div>
                    <div className="stat-value">{schedules.length}</div>
                    <div className="stat-change">All time</div>
                </div>
                <div className="stat-card warning">
                    <div className="stat-title">Pending</div>
                    <div className="stat-value">
                        {schedules.filter(s => s.status === 'PENDING').length}
                    </div>
                    <div className="stat-change">Awaiting collection</div>
                </div>
                <div className="stat-card success">
                    <div className="stat-title">Completed</div>
                    <div className="stat-value">
                        {schedules.filter(s => s.status === 'COMPLETED').length}
                    </div>
                    <div className="stat-change">Finished collections</div>
                </div>
            </div>

            {/* Schedules List */}
            <div className="card">
                <div className="card-header">
                    <h3>Your Collection Schedules</h3>
                </div>

                {schedules.length > 0 ? (
                    <div className="table-container">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>Schedule ID</th>
                                <th>Bin ID</th>
                                <th>Scheduled Date & Time</th>
                                <th>Status</th>
                                <th>Notes</th>
                                <th>Created Date</th>
                            </tr>
                            </thead>
                            <tbody>
                            {schedules.map((schedule) => (
                                <tr key={schedule.id}>
                                    <td>
                                        <strong>#{schedule.id}</strong>
                                    </td>
                                    <td>{schedule.binId}</td>
                                    <td>
                                        {formatDateTime(schedule.scheduledDate, schedule.scheduledTime)}
                                    </td>
                                    <td>{getStatusBadge(schedule.status)}</td>
                                    <td>{schedule.notes || '-'}</td>
                                    <td>{schedule.createdDate}</td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="empty-state">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                        <h3>No Schedules Found</h3>
                        <p>You haven't scheduled any waste collections yet.</p>
                        {bins.length > 0 ? (
                            <button
                                onClick={() => setShowCreateModal(true)}
                                className="btn btn-primary"
                            >
                                Schedule Your First Collection
                            </button>
                        ) : (
                            <p className="empty-state-help">
                                You need to have waste bins registered before scheduling collections.
                            </p>
                        )}
                    </div>
                )}
            </div>

            {/* Create Schedule Modal */}
            {showCreateModal && (
                <div className="modal-overlay">
                    <div className="modal">
                        <div className="modal-header">
                            <h3>Schedule Waste Collection</h3>
                            <button
                                onClick={() => setShowCreateModal(false)}
                                className="btn btn-sm btn-secondary"
                            >
                                Cancel
                            </button>
                        </div>

                        <form onSubmit={handleCreateSchedule}>
                            <div className="modal-body">
                                <div className="form-group">
                                    <label>Select Bin *</label>
                                    <select
                                        value={newSchedule.binId}
                                        onChange={(e) => setNewSchedule({...newSchedule, binId: e.target.value})}
                                        required
                                    >
                                        <option value="">Choose a bin</option>
                                        {bins.map(bin => (
                                            <option key={bin.binId} value={bin.binId}>
                                                {bin.binId} - {bin.binType?.replace(/_/g, ' ')} ({bin.currentLevel}% full)
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="form-group">
                                    <label>Collection Date *</label>
                                    <input
                                        type="date"
                                        value={newSchedule.scheduledDate}
                                        onChange={(e) => setNewSchedule({...newSchedule, scheduledDate: e.target.value})}
                                        min={new Date().toISOString().split('T')[0]}
                                        required
                                    />
                                </div>

                                <div className="form-group">
                                    <label>Preferred Time *</label>
                                    <select
                                        value={newSchedule.scheduledTime}
                                        onChange={(e) => setNewSchedule({...newSchedule, scheduledTime: e.target.value})}
                                        required
                                    >
                                        <option value="08:00:00">8:00 AM</option>
                                        <option value="09:00:00">9:00 AM</option>
                                        <option value="10:00:00">10:00 AM</option>
                                        <option value="11:00:00">11:00 AM</option>
                                        <option value="14:00:00">2:00 PM</option>
                                        <option value="15:00:00">3:00 PM</option>
                                    </select>
                                </div>

                                <div className="form-group">
                                    <label>Special Instructions (Optional)</label>
                                    <textarea
                                        value={newSchedule.notes}
                                        onChange={(e) => setNewSchedule({...newSchedule, notes: e.target.value})}
                                        placeholder="Any special instructions for the collection team..."
                                        rows="3"
                                    />
                                </div>

                                {bins.length > 0 && newSchedule.binId && (
                                    <div className="info-box">
                                        <h4>Selected Bin Information</h4>
                                        {bins.find(b => b.binId === newSchedule.binId) && (
                                            <div className="bin-preview">
                                                <p><strong>Type:</strong> {bins.find(b => b.binId === newSchedule.binId).binType?.replace(/_/g, ' ')}</p>
                                                <p><strong>Current Level:</strong> {bins.find(b => b.binId === newSchedule.binId).currentLevel}%</p>
                                                <p><strong>Location:</strong> {bins.find(b => b.binId === newSchedule.binId).location}</p>
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>

                            <div className="modal-actions">
                                <button type="submit" className="btn btn-primary btn-block">
                                    Schedule Collection
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Schedules;