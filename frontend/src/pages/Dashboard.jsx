// File: frontend/src/pages/Dashboard.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';

const Dashboard = ({ user }) => {
    const [stats, setStats] = useState({});
    const [recentInvoices, setRecentInvoices] = useState([]);
    const [bins, setBins] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchDashboardData();
    }, [user.id]);

// In Dashboard.jsx - improve error handling
    const fetchDashboardData = async () => {
        try {
            setLoading(true);

            // Check if user has an ID
            if (!user?.id) {
                console.error('User ID is undefined');
                toast.error('User information is incomplete. Please log in again.');
                return;
            }

            // Fetch user's bins with proper error handling
            const binsResponse = await axios.get(`http://localhost:8082/api/waste/bins/resident/${user.id}`);
            const userBins = binsResponse.data || [];

            // Fetch user's invoices
            const invoicesResponse = await axios.get(`http://localhost:8082/api/invoices/resident/${user.id}`);
            const userInvoices = invoicesResponse.data || [];

            setBins(userBins);
            setRecentInvoices(userInvoices.slice(0, 5));

            // Calculate stats
            const totalBins = userBins.length;
            const pendingInvoices = userInvoices.filter(inv => inv.status === 'PENDING').length;
            const totalDue = userInvoices
                .filter(inv => inv.status === 'PENDING')
                .reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);

            setStats({
                totalBins,
                pendingInvoices,
                totalDue,
                recentCollections: userBins.reduce((sum, bin) => sum + (bin.collections?.length || 0), 0)
            });

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

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading your dashboard...</p>
            </div>
        );
    }

    return (
        <div className="dashboard">
            {/* Welcome Section */}
            <div className="welcome-section">
                <h2>Welcome back, {user.name}! 👋</h2>
                <p>Here's what's happening with your waste management today.</p>
            </div>

            {/* Stats Grid */}
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

                <div className="stat-card success">
                    <div className="stat-title">Collections</div>
                    <div className="stat-value">{stats.recentCollections || 0}</div>
                    <div className="stat-change">This month</div>
                </div>
            </div>

            <div className="dashboard-grid">
                {/* Recent Invoices */}
                <div className="card">
                    <div className="card-header">
                        <h3>Recent Invoices</h3>
                        <Link to="/invoices" className="btn btn-sm btn-secondary">
                            View All
                        </Link>
                    </div>

                    {recentInvoices.length > 0 ? (
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>Invoice #</th>
                                <th>Amount</th>
                                <th>Due Date</th>
                                <th>Status</th>
                            </tr>
                            </thead>
                            <tbody>
                            {recentInvoices.map((invoice) => (
                                <tr key={invoice.id}>
                                    <td>{invoice.invoiceNumber}</td>
                                    <td>${(invoice.totalAmount || 0).toFixed(2)}</td>
                                    <td>{invoice.dueDate}</td>
                                    <td>
                      <span className={`status-badge status-${invoice.status?.toLowerCase()}`}>
                        {invoice.status}
                      </span>
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
                        <Link to="/bins" className="btn btn-sm btn-secondary">
                            Manage
                        </Link>
                    </div>

                    {bins.length > 0 ? (
                        <div className="bins-list">
                            {bins.slice(0, 3).map((bin) => (
                                <div key={bin.binId} className="bin-item">
                                    <div className="bin-info">
                                        <strong>{bin.binId}</strong>
                                        <span>{bin.location}</span>
                                    </div>
                                    <div className="bin-status">
                                        <div className="progress-bar">
                                            <div
                                                className={`progress-fill ${getBinStatus(bin)}`}
                                                style={{ width: `${bin.currentLevel || 0}%` }}
                                            ></div>
                                        </div>
                                        <span>{bin.currentLevel || 0}% full</span>
                                    </div>
                                </div>
                            ))}
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
                <h3>Quick Actions</h3>
                <div className="quick-actions">
                    <Link to="/invoices" className="action-card">
                        <div className="action-icon">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                        </div>
                        <div className="action-content">
                            <strong>Pay Invoice</strong>
                            <p>View and pay pending bills</p>
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
                            <p>Check bin status and history</p>
                        </div>
                    </Link>

                    <Link to="/profile" className="action-card">
                        <div className="action-icon">
                            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                            </svg>
                        </div>
                        <div className="action-content">
                            <strong>Update Profile</strong>
                            <p>Manage your account details</p>
                        </div>
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;