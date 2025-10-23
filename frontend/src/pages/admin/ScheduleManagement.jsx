// File: frontend/src/pages/admin/ScheduleManagement.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const ScheduleManagement = ({ user }) => {
    const [schedules, setSchedules] = useState([]);
    const [filteredSchedules, setFilteredSchedules] = useState([]);
    const [loading, setLoading] = useState(true);
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [dateFilter, setDateFilter] = useState('');
    const [showCreateForm, setShowCreateForm] = useState(false);
    const [bins, setBins] = useState([]);
    const [formData, setFormData] = useState({
        binId: '',
        scheduledDate: '',
        notes: ''
    });

    useEffect(() => {
        fetchSchedules();
        fetchAvailableBins();
    }, []);

    useEffect(() => {
        filterSchedules();
    }, [schedules, statusFilter, dateFilter]);

    const fetchSchedules = async () => {
        try {
            setLoading(true);

            // Try to get all schedules first
            try {
                const allSchedulesResponse = await axios.get('http://localhost:8082/api/waste/schedules');
                console.log('All schedules:', allSchedulesResponse.data);
                if (allSchedulesResponse.data && Array.isArray(allSchedulesResponse.data)) {
                    setSchedules(allSchedulesResponse.data);
                    return;
                }
            } catch (allError) {
                console.log('All schedules endpoint not available, trying today\'s schedules');
            }

            // Fallback: Get today's schedules and expand
            const todayResponse = await axios.get('http://localhost:8082/api/waste/schedules/pending/today');
            console.log('Today schedules:', todayResponse.data);

            if (todayResponse.data && Array.isArray(todayResponse.data)) {
                setSchedules(todayResponse.data);
            } else {
                setSchedules([]);
            }
        } catch (error) {
            console.error('Error fetching schedules:', error);
            toast.error('Failed to load schedules');
            setSchedules([]);
        } finally {
            setLoading(false);
        }
    };

    const fetchAvailableBins = async () => {
        try {
            // Get bins that might need scheduling
            const binsResponse = await axios.get('http://localhost:8082/api/waste/bins').catch(() => ({ data: [] }));
            if (binsResponse.data && Array.isArray(binsResponse.data)) {
                // Filter bins that are active and have reasonable fill levels
                const availableBins = binsResponse.data.filter(bin =>
                    bin.status === 'ACTIVE' && (bin.currentLevel || 0) > 0
                );
                setBins(availableBins);
            }
        } catch (error) {
            console.error('Error fetching bins:', error);
        }
    };

    const filterSchedules = () => {
        let filtered = schedules;

        // Filter by status
        if (statusFilter !== 'ALL') {
            filtered = filtered.filter(schedule => schedule.status === statusFilter);
        }

        // Filter by date
        if (dateFilter) {
            filtered = filtered.filter(schedule =>
                schedule.scheduledDate === dateFilter
            );
        }

        setFilteredSchedules(filtered);
    };

    const getStatusBadge = (status) => {
        const statusConfig = {
            'PENDING': { label: 'Pending', class: 'badge-warning' },
            'COMPLETED': { label: 'Completed', class: 'badge-success' },
            'CANCELLED': { label: 'Cancelled', class: 'badge-secondary' },
            'IN_PROGRESS': { label: 'In Progress', class: 'badge-info' }
        };

        const config = statusConfig[status] || { label: status, class: 'badge-secondary' };
        return <span className={`badge ${config.class}`}>{config.label}</span>;
    };

    const calculateScheduleStats = () => {
        const totalSchedules = schedules.length;
        const pendingSchedules = schedules.filter(s => s.status === 'PENDING').length;
        const completedSchedules = schedules.filter(s => s.status === 'COMPLETED').length;
        const today = new Date().toISOString().split('T')[0];
        const todaySchedules = schedules.filter(s => s.scheduledDate === today).length;

        return { totalSchedules, pendingSchedules, completedSchedules, todaySchedules };
    };

    const cancelSchedule = async (scheduleId) => {
        try {
            await axios.put(`http://localhost:8082/api/waste/schedules/${scheduleId}/cancel`);
            toast.success('Schedule cancelled successfully');
            fetchSchedules();
        } catch (error) {
            console.error('Error cancelling schedule:', error);
            toast.error('Failed to cancel schedule');
        }
    };

    const markAsCompleted = async (scheduleId) => {
        try {
            // Create a collection event to mark as completed
            const schedule = schedules.find(s => s.id === scheduleId);
            if (schedule && schedule.wasteBin) {
                await axios.post('http://localhost:8082/api/waste/collections/record', {
                    binId: schedule.wasteBin.binId,
                    weight: 10, // Default weight
                    notes: `Completed from schedule ${scheduleId}`
                });
                toast.success('Schedule completed and collection recorded');
                fetchSchedules();
            }
        } catch (error) {
            console.error('Error completing schedule:', error);
            toast.error('Failed to complete schedule');
        }
    };

    const createSchedule = async (e) => {
        e.preventDefault();
        try {
            await axios.post('http://localhost:8082/api/waste/schedules/create', {
                binId: formData.binId,
                scheduledDate: formData.scheduledDate,
                notes: formData.notes
            });
            toast.success('Schedule created successfully');
            setShowCreateForm(false);
            setFormData({ binId: '', scheduledDate: '', notes: '' });
            fetchSchedules();
        } catch (error) {
            console.error('Error creating schedule:', error);
            toast.error(error.response?.data?.message || 'Failed to create schedule');
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const stats = calculateScheduleStats();

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading schedules...</p>
            </div>
        );
    }

    return (
        <div className="schedule-management">
            <div className="page-header">
                <h1>Schedule Management</h1>
                <p>Manage and monitor waste collection schedules</p>
                <button
                    onClick={() => setShowCreateForm(true)}
                    className="btn btn-primary"
                >
                    + Create Schedule
                </button>
            </div>

            {/* Create Schedule Modal */}
            {showCreateForm && (
                <div className="modal-overlay">
                    <div className="modal">
                        <div className="modal-header">
                            <h3>Create New Schedule</h3>
                            <button
                                onClick={() => setShowCreateForm(false)}
                                className="btn-close"
                            >
                                ×
                            </button>
                        </div>
                        <form onSubmit={createSchedule}>
                            <div className="form-group">
                                <label>Select Bin</label>
                                <select
                                    name="binId"
                                    value={formData.binId}
                                    onChange={handleInputChange}
                                    required
                                    className="form-input"
                                >
                                    <option value="">Choose a bin</option>
                                    {bins.map(bin => (
                                        <option key={bin.binId} value={bin.binId}>
                                            {bin.binId} - {bin.location} ({bin.currentLevel}% full)
                                        </option>
                                    ))}
                                </select>
                            </div>
                            <div className="form-group">
                                <label>Scheduled Date</label>
                                <input
                                    type="date"
                                    name="scheduledDate"
                                    value={formData.scheduledDate}
                                    onChange={handleInputChange}
                                    min={new Date().toISOString().split('T')[0]}
                                    required
                                    className="form-input"
                                />
                            </div>
                            <div className="form-group">
                                <label>Notes (Optional)</label>
                                <textarea
                                    name="notes"
                                    value={formData.notes}
                                    onChange={handleInputChange}
                                    className="form-input"
                                    rows="3"
                                    placeholder="Additional notes for the collection..."
                                />
                            </div>
                            <div className="modal-actions">
                                <button
                                    type="button"
                                    onClick={() => setShowCreateForm(false)}
                                    className="btn btn-secondary"
                                >
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary">
                                    Create Schedule
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Stats Overview */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-icon">📅</div>
                    <div className="stat-content">
                        <div className="stat-value">{stats.totalSchedules}</div>
                        <div className="stat-title">Total Schedules</div>
                        <div className="stat-breakdown">
                            All time
                        </div>
                    </div>
                </div>

                <div className="stat-card warning">
                    <div className="stat-icon">⏰</div>
                    <div className="stat-content">
                        <div className="stat-value">{stats.pendingSchedules}</div>
                        <div className="stat-title">Pending</div>
                        <div className="stat-breakdown">
                            Awaiting collection
                        </div>
                    </div>
                </div>

                <div className="stat-card success">
                    <div className="stat-icon">✅</div>
                    <div className="stat-content">
                        <div className="stat-value">{stats.completedSchedules}</div>
                        <div className="stat-title">Completed</div>
                        <div className="stat-breakdown">
                            Finished collections
                        </div>
                    </div>
                </div>

                <div className="stat-card info">
                    <div className="stat-icon">📌</div>
                    <div className="stat-content">
                        <div className="stat-value">{stats.todaySchedules}</div>
                        <div className="stat-title">Today</div>
                        <div className="stat-breakdown">
                            Scheduled for today
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
                        <option value="PENDING">Pending</option>
                        <option value="COMPLETED">Completed</option>
                        <option value="CANCELLED">Cancelled</option>
                    </select>
                </div>
                <div className="filter-group">
                    <input
                        type="date"
                        value={dateFilter}
                        onChange={(e) => setDateFilter(e.target.value)}
                        className="form-input"
                    />
                </div>
                <div className="filter-group">
                    <button
                        onClick={() => {
                            setStatusFilter('ALL');
                            setDateFilter('');
                        }}
                        className="btn btn-secondary"
                    >
                        Clear Filters
                    </button>
                </div>
                <div className="filter-group">
                    <span className="results-count">{filteredSchedules.length} schedules found</span>
                </div>
            </div>

            {/* Schedules Table */}
            <div className="card">
                <div className="card-header">
                    <h3>Collection Schedules</h3>
                    <div className="header-actions">
                        <button
                            onClick={() => setShowCreateForm(true)}
                            className="btn btn-primary"
                        >
                            Create Schedule
                        </button>
                    </div>
                </div>

                {filteredSchedules.length > 0 ? (
                    <div className="table-container">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>Schedule Details</th>
                                <th>Bin Information</th>
                                <th>Resident</th>
                                <th>Scheduled Date</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredSchedules.map((schedule) => (
                                <tr key={schedule.id}>
                                    <td>
                                        <div className="schedule-cell">
                                            <strong>Schedule #{schedule.id}</strong>
                                            {schedule.notes && (
                                                <small>{schedule.notes}</small>
                                            )}
                                        </div>
                                    </td>
                                    <td>
                                        <div className="bin-cell">
                                            <strong>{schedule.wasteBin?.binId || 'N/A'}</strong>
                                            <span>{schedule.wasteBin?.location || 'Unknown Location'}</span>
                                            <small>Level: {schedule.wasteBin?.currentLevel || 0}%</small>
                                        </div>
                                    </td>
                                    <td>
                                        <div className="resident-cell">
                                            <strong>{schedule.wasteBin?.resident?.name || 'Unknown Resident'}</strong>
                                            <span>{schedule.wasteBin?.resident?.email || 'No email'}</span>
                                        </div>
                                    </td>
                                    <td>
                                        {schedule.scheduledDate ?
                                            new Date(schedule.scheduledDate).toLocaleDateString() :
                                            'N/A'
                                        }
                                        {schedule.scheduledDate === new Date().toISOString().split('T')[0] && (
                                            <div className="today-indicator">Today</div>
                                        )}
                                    </td>
                                    <td>{getStatusBadge(schedule.status)}</td>
                                    <td>
                                        <div className="action-buttons">
                                            {schedule.status === 'PENDING' && (
                                                <>
                                                    <button
                                                        className="btn btn-sm btn-success"
                                                        onClick={() => markAsCompleted(schedule.id)}
                                                    >
                                                        Complete
                                                    </button>
                                                    <button
                                                        className="btn btn-sm btn-danger"
                                                        onClick={() => cancelSchedule(schedule.id)}
                                                    >
                                                        Cancel
                                                    </button>
                                                </>
                                            )}
                                            <button className="btn btn-sm btn-info">View</button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="empty-state">
                        <h3>No Schedules Found</h3>
                        <p>No schedules match your filter criteria.</p>
                        <button
                            onClick={() => {
                                setStatusFilter('ALL');
                                setDateFilter('');
                            }}
                            className="btn btn-primary"
                        >
                            Clear Filters
                        </button>
                    </div>
                )}
            </div>

            {/* Quick Stats */}
            <div className="card">
                <div className="card-header">
                    <h3>Schedule Overview</h3>
                </div>
                <div className="schedule-overview">
                    <div className="overview-item">
                        <span className="overview-label">Next 7 Days</span>
                        <span className="overview-value">
                            {schedules.filter(s => {
                                const scheduleDate = new Date(s.scheduledDate);
                                const nextWeek = new Date();
                                nextWeek.setDate(nextWeek.getDate() + 7);
                                return scheduleDate <= nextWeek && scheduleDate >= new Date();
                            }).length}
                        </span>
                    </div>
                    <div className="overview-item">
                        <span className="overview-label">High Priority</span>
                        <span className="overview-value">
                            {schedules.filter(s =>
                                s.wasteBin?.currentLevel >= 80 && s.status === 'PENDING'
                            ).length}
                        </span>
                    </div>
                    <div className="overview-item">
                        <span className="overview-label">Completion Rate</span>
                        <span className="overview-value">
                            {stats.totalSchedules > 0
                                ? `${((stats.completedSchedules / stats.totalSchedules) * 100).toFixed(1)}%`
                                : '0%'
                            }
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ScheduleManagement;