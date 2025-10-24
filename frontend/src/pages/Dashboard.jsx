// File: frontend/src/pages/Dashboard.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';

const Dashboard = ({ user }) => {
    const [stats, setStats] = useState({});
    const [recentInvoices, setRecentInvoices] = useState([]);
    const [bins, setBins] = useState([]);
    const [recentSchedules, setRecentSchedules] = useState([]);
    const [loading, setLoading] = useState(true);
    const [lastUpdated, setLastUpdated] = useState(null);
    const [autoRefresh, setAutoRefresh] = useState(true);

    useEffect(() => {
        fetchDashboardData();

        // Auto-refresh every 30 seconds if enabled
        let interval;
        if (autoRefresh) {
            interval = setInterval(fetchDashboardData, 30000);
        }

        return () => clearInterval(interval);
    }, [user.id, autoRefresh]);

    // ADD THIS FUNCTION: Normalize invoice data from backend
    const normalizeInvoice = (invoice) => {
        console.log('🔄 Normalizing invoice:', invoice.invoiceNumber);

        return {
            ...invoice,
            // Map backend fields to frontend expected fields
            recyclingCredits: invoice.refundAmount || invoice.recyclingCredits || 0,
            weightBasedCharge: invoice.weightBasedCharge || 0,
            baseCharge: invoice.baseCharge || 0,
            totalAmount: invoice.finalAmount || invoice.totalAmount || 0,
            // Ensure all required fields have values
            otherCharges: invoice.otherCharges || 0
        };
    };

    const fetchDashboardData = async () => {
        try {
            setLoading(true);

            if (!user?.id) {
                console.error('User ID is undefined');
                toast.error('User information is incomplete. Please log in again.');
                return;
            }

            // Fetch user's bins
            const binsResponse = await axios.get(`http://localhost:8082/api/waste/bins/resident/${user.id}`);
            const userBins = binsResponse.data || [];

            // Fetch user's invoices
            const invoicesResponse = await axios.get(`http://localhost:8082/api/invoices/resident/${user.id}`);
            let userInvoices = invoicesResponse.data || [];

            // 🎯 NORMALIZE the invoice data
            userInvoices = userInvoices.map(normalizeInvoice);

            // 🔍 DEBUG: Log invoice data for verification
            if (userInvoices.length > 0) {
                console.log('📊 Dashboard - First invoice after normalization:', {
                    invoiceNumber: userInvoices[0].invoiceNumber,
                    totalAmount: userInvoices[0].totalAmount,
                    recyclingCredits: userInvoices[0].recyclingCredits,
                    finalAmount: userInvoices[0].finalAmount
                });
            }

            // Fetch user's schedules
            try {
                const schedulesResponse = await axios.get(`http://localhost:8082/api/waste/schedules/resident/${user.id}`);
                const pendingSchedules = schedulesResponse.data
                    .filter(s => s.status === 'PENDING')
                    .slice(0, 3);
                setRecentSchedules(pendingSchedules);
            } catch (scheduleError) {
                console.error('Error fetching schedules:', scheduleError);
                setRecentSchedules([]);
            }

            setBins(userBins);
            setRecentInvoices(userInvoices.slice(0, 5));

            // 🎯 CALCULATE STATS WITH NORMALIZED DATA
            const totalBins = userBins.length;
            const pendingInvoices = userInvoices.filter(inv => inv.status === 'PENDING').length;

            // FIXED: Use normalized totalAmount for calculations
            const totalDue = userInvoices
                .filter(inv => inv.status === 'PENDING')
                .reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);

            // 🎯 NEW: Calculate total recycling savings
            const totalRecyclingSavings = userInvoices.reduce((sum, inv) => sum + (inv.recyclingCredits || 0), 0);

            // Calculate bins needing attention
            const binsNeedingAttention = userBins.filter(bin => {
                const level = bin.currentLevel || 0;
                return level >= 60;
            }).length;

            // 🎯 NEW: Calculate monthly statistics
            const currentMonth = new Date().getMonth();
            const currentYear = new Date().getFullYear();

            const monthlyInvoices = userInvoices.filter(inv => {
                if (!inv.invoiceDate) return false;
                const invoiceDate = new Date(inv.invoiceDate);
                return invoiceDate.getMonth() === currentMonth && invoiceDate.getFullYear() === currentYear;
            });

            const monthlySpending = monthlyInvoices.reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);
            const monthlySavings = monthlyInvoices.reduce((sum, inv) => sum + (inv.recyclingCredits || 0), 0);

            setStats({
                totalBins,
                pendingInvoices,
                totalDue,
                recentCollections: userBins.reduce((sum, bin) => sum + (bin.collections?.length || 0), 0),
                binsNeedingAttention,
                // 🎯 NEW STATS
                totalRecyclingSavings,
                monthlySpending,
                monthlySavings,
                totalInvoices: userInvoices.length
            });

            setLastUpdated(new Date());

        } catch (error) {
            console.error('Error fetching dashboard data:', error);
            if (error.response?.status === 400) {
                toast.error('Invalid user ID. Please log in again.');
            } else if (error.code === 'ERR_NETWORK') {
                toast.error('Network error. Please check if the backend server is running.');
            } else {
                toast.error('Failed to load dashboard data');
            }
        } finally {
            setLoading(false);
        }
    };

    const getBinStatus = (bin) => {
        const level = bin.currentLevel || 0;
        if (level >= 80) return 'danger';
        if (level >= 60) return 'warning';
        return 'active';
    };

    const getUrgentBins = () => {
        return bins.filter(bin => {
            const level = bin.currentLevel || 0;
            return level >= 80;
        });
    };

    const getStatusBadge = (status) => {
        const statusConfig = {
            PENDING: { class: 'pending', label: 'Pending' },
            PAID: { class: 'paid', label: 'Paid' },
            OVERDUE: { class: 'overdue', label: 'Overdue' },
            PARTIALLY_PAID: { class: 'warning', label: 'Partial' },
            CANCELLED: { class: 'inactive', label: 'Cancelled' }
        };

        const config = statusConfig[status] || { class: 'pending', label: status };
        return <span className={`status-badge status-${config.class}`}>{config.label}</span>;
    };

    if (loading && !lastUpdated) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading your dashboard...</p>
            </div>
        );
    }

    const urgentBins = getUrgentBins();

    return (
        <div className="dashboard">
            {/* Welcome Section */}
            <div className="welcome-section">
                <h2>Welcome back, {user.name}! 👋</h2>
                <p>Here's what's happening with your waste management today.</p>
                {lastUpdated && (
                    <div className="last-updated">
                        Last updated: {lastUpdated.toLocaleTimeString()}
                    </div>
                )}
            </div>

            {/* 🎯 UPDATED Stats Grid with Enhanced Metrics */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-title">Active Bins</div>
                    <div className="stat-value">{stats.totalBins || 0}</div>
                    <div className="stat-change">Registered waste bins</div>
                </div>

                <div className="stat-card warning">
                    <div className="stat-title">Pending Invoices</div>
                    <div className="stat-value">{stats.pendingInvoices || 0}</div>
                    <div className="stat-change">Awaiting payment</div>
                </div>

                <div className="stat-card danger">
                    <div className="stat-title">Total Due</div>
                    <div className="stat-value">${(stats.totalDue || 0).toFixed(2)}</div>
                    <div className="stat-change">Outstanding balance</div>
                </div>

                {/* 🎯 NEW: Recycling Savings Card */}
                <div className="stat-card success">
                    <div className="stat-title">Recycling Savings</div>
                    <div className="stat-value">${(stats.totalRecyclingSavings || 0).toFixed(2)}</div>
                    <div className="stat-change">Total refunds earned</div>
                </div>

                {/* 🎯 NEW: Monthly Spending Card */}
                <div className="stat-card info">
                    <div className="stat-title">Monthly Spending</div>
                    <div className="stat-value">${(stats.monthlySpending || 0).toFixed(2)}</div>
                    <div className="stat-change">This month</div>
                </div>

                {/* 🎯 NEW: Total Collections Card */}
                <div className="stat-card secondary">
                    <div className="stat-title">Collections</div>
                    <div className="stat-value">{stats.recentCollections || 0}</div>
                    <div className="stat-change">All time</div>
                </div>
            </div>

            {/* Urgent Alert */}
            {urgentBins.length > 0 && (
                <div className="alert alert-danger">
                    <div className="alert-icon">⚠️</div>
                    <div className="alert-content">
                        <strong>Attention Needed</strong>
                        <p>
                            {urgentBins.length} bin{urgentBins.length !== 1 ? 's' : ''} {urgentBins.length === 1 ? 'is' : 'are'} over 80% full and needs immediate collection.
                        </p>
                    </div>
                    <Link to="/schedules" className="btn btn-sm btn-danger">
                        Schedule Collection
                    </Link>
                </div>
            )}

            {/* 🎯 NEW: Financial Summary Alert */}
            {stats.totalRecyclingSavings > 0 && (
                <div className="alert alert-success">
                    <div className="alert-icon">💰</div>
                    <div className="alert-content">
                        <strong>Great Job Recycling!</strong>
                        <p>
                            You've saved <strong>${stats.totalRecyclingSavings.toFixed(2)}</strong> through recycling credits.
                            {stats.monthlySavings > 0 && ` $${stats.monthlySavings.toFixed(2)} saved this month alone!`}
                        </p>
                    </div>
                    <Link to="/invoices" className="btn btn-sm btn-success">
                        View Details
                    </Link>
                </div>
            )}

            <div className="dashboard-grid">
                {/* Recent Invoices - UPDATED with better display */}
                <div className="card">
                    <div className="card-header">
                        <h3>Recent Invoices</h3>
                        <div className="card-actions">
                            <span className="badge">{recentInvoices.length}</span>
                            <Link to="/invoices" className="btn btn-sm btn-secondary">
                                View All
                            </Link>
                        </div>
                    </div>

                    {recentInvoices.length > 0 ? (
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>Invoice #</th>
                                <th>Amount</th>
                                <th>Recycling Credits</th>
                                <th>Due Date</th>
                                <th>Status</th>
                            </tr>
                            </thead>
                            <tbody>
                            {recentInvoices.map((invoice) => (
                                <tr key={invoice.id}>
                                    <td>
                                        <strong>{invoice.invoiceNumber}</strong>
                                    </td>
                                    <td>
                                        <strong>${(invoice.totalAmount || 0).toFixed(2)}</strong>
                                    </td>
                                    <td className="text-success">
                                        {invoice.recyclingCredits > 0 ? `-$${invoice.recyclingCredits.toFixed(2)}` : '-'}
                                    </td>
                                    <td>
                                        <span className={
                                            new Date(invoice.dueDate) < new Date() && invoice.status === 'PENDING'
                                                ? 'text-danger'
                                                : ''
                                        }>
                                            {invoice.dueDate}
                                        </span>
                                    </td>
                                    <td>
                                        {getStatusBadge(invoice.status)}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    ) : (
                        <div className="empty-state">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                            <h3>No invoices yet</h3>
                            <p>Your billing information will appear here</p>
                        </div>
                    )}
                </div>

                {/* Bin Status */}
                <div className="card">
                    <div className="card-header">
                        <h3>Bin Status</h3>
                        <div className="card-actions">
                            <span className="badge">{bins.length}</span>
                            <Link to="/bins" className="btn btn-sm btn-secondary">
                                Manage
                            </Link>
                        </div>
                    </div>

                    {bins.length > 0 ? (
                        <div className="bins-list">
                            {bins.slice(0, 5).map((bin) => (
                                <div key={bin.binId} className="bin-item">
                                    <div className="bin-info">
                                        <strong>{bin.binId}</strong>
                                        <span>{bin.location}</span>
                                        <small>{bin.binType?.replace(/_/g, ' ')}</small>
                                    </div>
                                    <div className="bin-status">
                                        <div className="progress-bar">
                                            <div
                                                className={`progress-fill ${getBinStatus(bin)}`}
                                                style={{ width: `${bin.currentLevel || 0}%` }}
                                            ></div>
                                        </div>
                                        <span className={`level-text ${getBinStatus(bin)}`}>
                                            {bin.currentLevel || 0}% full
                                        </span>
                                    </div>
                                </div>
                            ))}
                            {bins.length > 5 && (
                                <div className="view-more">
                                    <Link to="/bins" className="btn btn-sm btn-secondary">
                                        View all {bins.length} bins
                                    </Link>
                                </div>
                            )}
                        </div>
                    ) : (
                        <div className="empty-state">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                            </svg>
                            <h3>No bins registered</h3>
                            <p>Contact admin to get your waste bins registered</p>
                        </div>
                    )}
                </div>
            </div>

            {/* Quick Actions */}
            <div className="card">
                <div className="card-header">
                    <h3>Quick Actions</h3>
                    <div className="auto-refresh-toggle">
                        <label className="toggle-label">
                            <input
                                type="checkbox"
                                checked={autoRefresh}
                                onChange={(e) => setAutoRefresh(e.target.checked)}
                            />
                            <span className="toggle-slider"></span>
                            Auto-refresh
                        </label>
                    </div>
                </div>
                <div className="quick-actions">
                    <Link to="/invoices" className="action-card">
                        <div className="action-icon">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                        </div>
                        <div className="action-content">
                            <strong>Pay Invoice</strong>
                            <p>
                                {stats.pendingInvoices > 0
                                    ? `${stats.pendingInvoices} pending - $${(stats.totalDue || 0).toFixed(2)} due`
                                    : 'View and pay bills'
                                }
                            </p>
                        </div>
                    </Link>

                    <Link to="/bins" className="action-card">
                        <div className="action-icon">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                            </svg>
                        </div>
                        <div className="action-content">
                            <strong>View Bins</strong>
                            <p>
                                {stats.binsNeedingAttention > 0
                                    ? `${stats.binsNeedingAttention} need attention`
                                    : 'Check bin status and history'
                                }
                            </p>
                        </div>
                    </Link>

                    <Link to="/schedules" className="action-card">
                        <div className="action-icon">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                            </svg>
                        </div>
                        <div className="action-content">
                            <strong>Schedule Collection</strong>
                            <p>Book waste collection pickup</p>
                        </div>
                    </Link>

                    <Link to="/invoices" className="action-card">
                        <div className="action-icon">💰</div>
                        <div className="action-content">
                            <strong>Recycling Savings</strong>
                            <p>You saved ${(stats.totalRecyclingSavings || 0).toFixed(2)}</p>
                        </div>
                    </Link>
                </div>
            </div>

            {/* Recent Schedules */}
            <div className="card">
                <div className="card-header">
                    <h3>Upcoming Collections</h3>
                    <div className="card-actions">
                        <span className="badge">{recentSchedules.length}</span>
                        <Link to="/schedules" className="btn btn-sm btn-secondary">
                            View All
                        </Link>
                    </div>
                </div>

                {recentSchedules.length > 0 ? (
                    <div className="schedules-list">
                        {recentSchedules.map((schedule) => (
                            <div key={schedule.id} className="schedule-item">
                                <div className="schedule-info">
                                    <strong>Bin {schedule.binId}</strong>
                                    <span>{schedule.scheduledDate} at {schedule.scheduledTime}</span>
                                    {schedule.binType && (
                                        <small>{schedule.binType.replace(/_/g, ' ')} • {schedule.location}</small>
                                    )}
                                </div>
                                <span className={`status-badge status-pending`}>
                                    Scheduled
                                </span>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="empty-state mini">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                        </svg>
                        <p>No upcoming collections</p>
                        <Link to="/schedules" className="btn btn-sm btn-primary">
                            Schedule Now
                        </Link>
                    </div>
                )}
            </div>

            {/* Dashboard Controls */}
            <div className="dashboard-controls">
                <button
                    onClick={fetchDashboardData}
                    className="btn btn-secondary"
                    disabled={loading}
                >
                    {loading ? 'Refreshing...' : 'Refresh Data'}
                </button>
                {lastUpdated && (
                    <span className="last-updated-text">
                        Last updated: {lastUpdated.toLocaleString()}
                    </span>
                )}
            </div>
        </div>
    );
};

export default Dashboard;