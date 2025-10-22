// File: frontend/src/pages/admin/AdminDashboard.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const AdminDashboard = ({ user }) => {
    const [stats, setStats] = useState({});
    const [recentCollections, setRecentCollections] = useState([]);
    const [pendingInvoices, setPendingInvoices] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchDashboardData();
    }, []);

    const fetchDashboardData = async () => {
        try {
            setLoading(true);

            // Fetch all users by role
            const [residentsResponse, staffResponse] = await Promise.all([
                axios.get('http://localhost:8082/api/auth/users/role/ROLE_RESIDENT').catch(() => ({ data: [] })),
                axios.get('http://localhost:8082/api/auth/users/role/ROLE_STAFF').catch(() => ({ data: [] }))
            ]);

            const residents = residentsResponse.data || [];
            const staff = staffResponse.data || [];

            // Fetch pending invoices
            const invoicesResponse = await axios.get('http://localhost:8082/api/invoices/pending').catch(() => ({ data: [] }));
            const pendingInvoicesData = invoicesResponse.data || [];

            // Fetch collection statistics
            const today = new Date().toISOString().split('T')[0];
            const startOfMonth = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0];
            const endOfMonth = new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0).toISOString().split('T')[0];

            const collectionsResponse = await axios.get(`http://localhost:8082/api/waste/collections/stats/total-waste?start=${startOfMonth}T00:00:00&end=${endOfMonth}T23:59:59`)
                .catch(() => ({ data: {} }));
            const collectionsData = collectionsResponse.data || {};

            // Calculate stats
            const totalPendingAmount = pendingInvoicesData.reduce((sum, invoice) => sum + (invoice.totalAmount || 0), 0);

            setStats({
                totalUsers: residents.length + staff.length,
                totalResidents: residents.length,
                totalStaff: staff.length,
                pendingInvoicesCount: pendingInvoicesData.length,
                pendingAmount: totalPendingAmount,
                totalCollections: collectionsData.collectionCount || 0,
                totalWeight: collectionsData.totalWeight || 0
            });

            setPendingInvoices(pendingInvoicesData.slice(0, 5));

            // Get recent collections from all staff
            const allStaffCollections = await Promise.all(
                staff.map(async (staffMember) => {
                    try {
                        const response = await axios.get(`http://localhost:8082/api/waste/collections/collector/${staffMember.id}`);
                        const collections = response.data || [];
                        return collections.map(collection => ({
                            ...collection,
                            staffName: staffMember.name
                        }));
                    } catch (error) {
                        return [];
                    }
                })
            );

            // Flatten and sort collections
            const flattenedCollections = allStaffCollections.flat();
            const sortedCollections = flattenedCollections
                .sort((a, b) => new Date(b.collectionTime) - new Date(a.collectionTime))
                .slice(0, 5);

            setRecentCollections(sortedCollections);

        } catch (error) {
            console.error('Error fetching admin dashboard data:', error);
            toast.error('Failed to load dashboard data');
        } finally {
            setLoading(false);
        }
    };

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading admin dashboard...</p>
            </div>
        );
    }

    return (
        <div className="admin-dashboard">
            {/* Header */}
            <div className="dashboard-header">
                <h1>Admin Dashboard</h1>
                <p>Welcome back, {user.name}! Here's your system overview.</p>
            </div>

            {/* Stats Grid */}
            <div className="stats-grid admin-stats">
                <div className="stat-card">
                    <div className="stat-icon">👥</div>
                    <div className="stat-content">
                        <div className="stat-value">{stats.totalUsers || 0}</div>
                        <div className="stat-title">Total Users</div>
                        <div className="stat-breakdown">
                            {stats.totalResidents || 0} residents • {stats.totalStaff || 0} staff
                        </div>
                    </div>
                </div>

                <div className="stat-card warning">
                    <div className="stat-icon">🚛</div>
                    <div className="stat-content">
                        <div className="stat-value">{stats.totalCollections || 0}</div>
                        <div className="stat-title">Collections</div>
                        <div className="stat-breakdown">
                            {stats.totalWeight || 0} kg this month
                        </div>
                    </div>
                </div>

                <div className="stat-card danger">
                    <div className="stat-icon">🧾</div>
                    <div className="stat-content">
                        <div className="stat-value">{stats.pendingInvoicesCount || 0}</div>
                        <div className="stat-title">Pending Invoices</div>
                        <div className="stat-breakdown">
                            ${(stats.pendingAmount || 0).toFixed(2)} total
                        </div>
                    </div>
                </div>

                <div className="stat-card success">
                    <div className="stat-icon">💰</div>
                    <div className="stat-content">
                        <div className="stat-value">${((stats.pendingAmount || 0) + (stats.totalCollections || 0) * 2).toFixed(2)}</div>
                        <div className="stat-title">Estimated Revenue</div>
                        <div className="stat-breakdown">
                            This month
                        </div>
                    </div>
                </div>
            </div>

            <div className="dashboard-grid admin-grid">
                {/* Recent Collections */}
                <div className="card">
                    <div className="card-header">
                        <h3>Recent Collections</h3>
                        <span className="badge">{recentCollections.length} collections</span>
                    </div>

                    {recentCollections.length > 0 ? (
                        <div className="collections-list">
                            {recentCollections.map((collection, index) => (
                                <div key={collection.id || index} className="collection-item">
                                    <div className="collection-info">
                                        <strong>Bin {collection.binId}</strong>
                                        <span>{collection.location}</span>
                                        <small>By {collection.staffName}</small>
                                    </div>
                                    <div className="collection-details">
                                        <span className="weight">{collection.weight} kg</span>
                                        <span className="charge">${collection.calculatedCharge?.toFixed(2)}</span>
                                        <span className="time">
                                            {new Date(collection.collectionTime).toLocaleDateString()}
                                        </span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="empty-state">
                            <p>No recent collections found</p>
                        </div>
                    )}
                </div>

                {/* Pending Invoices */}
                <div className="card">
                    <div className="card-header">
                        <h3>Pending Invoices</h3>
                        <span className="badge badge-danger">{pendingInvoices.length} pending</span>
                    </div>

                    {pendingInvoices.length > 0 ? (
                        <div className="invoices-list">
                            {pendingInvoices.map((invoice, index) => (
                                <div key={invoice.id || index} className="invoice-item">
                                    <div className="invoice-info">
                                        <strong>{invoice.invoiceNumber}</strong>
                                        <span>{invoice.resident?.name || 'Unknown Resident'}</span>
                                        <small>Due {new Date(invoice.dueDate).toLocaleDateString()}</small>
                                    </div>
                                    <div className="invoice-amount">
                                        <span className="amount">${invoice.totalAmount?.toFixed(2)}</span>
                                        <span className="status-badge status-pending">Pending</span>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="empty-state">
                            <p>No pending invoices</p>
                        </div>
                    )}
                </div>

                {/* Quick Actions */}
                <div className="card">
                    <div className="card-header">
                        <h3>Quick Actions</h3>
                    </div>
                    <div className="quick-actions-admin">
                        <button
                            className="action-btn primary"
                            onClick={() => window.location.href = '/admin/billing'}
                        >
                            <span className="action-icon">💰</span>
                            Create Billing Model
                        </button>
                        <button
                            className="action-btn"
                            onClick={() => window.location.href = '/admin/users'}
                        >
                            <span className="action-icon">👥</span>
                            Manage Users
                        </button>
                        <button
                            className="action-btn"
                            onClick={() => window.location.href = '/admin/collections'}
                        >
                            <span className="action-icon">📊</span>
                            View Reports
                        </button>
                        <button
                            className="action-btn"
                            onClick={() => window.location.href = '/admin/invoices'}
                        >
                            <span className="action-icon">🧾</span>
                            View All Invoices
                        </button>
                    </div>
                </div>

                {/* System Status */}
                <div className="card">
                    <div className="card-header">
                        <h3>System Status</h3>
                    </div>
                    <div className="system-status">
                        <div className="status-item">
                            <span className="status-indicator online"></span>
                            <span>Database Connection</span>
                            <span className="status-text">Online</span>
                        </div>
                        <div className="status-item">
                            <span className="status-indicator online"></span>
                            <span>API Services</span>
                            <span className="status-text">Operational</span>
                        </div>
                        <div className="status-item">
                            <span className="status-indicator online"></span>
                            <span>Collection System</span>
                            <span className="status-text">Active</span>
                        </div>
                        <div className="status-item">
                            <span className="status-indicator online"></span>
                            <span>Billing System</span>
                            <span className="status-text">Running</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AdminDashboard;