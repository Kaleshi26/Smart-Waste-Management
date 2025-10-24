import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const AdminDashboard = ({ user }) => {
    const [stats, setStats] = useState({});
    const [recentCollections, setRecentCollections] = useState([]);
    const [pendingInvoices, setPendingInvoices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [collectionStats, setCollectionStats] = useState({});

    useEffect(() => {
        fetchDashboardData();
    }, []);

    const fetchDashboardData = async () => {
        try {
            setLoading(true);

            // Fetch users
            const [residentsResponse, staffResponse] = await Promise.all([
                axios.get('http://localhost:8082/api/auth/users/role/ROLE_RESIDENT').catch(() => ({ data: [] })),
                axios.get('http://localhost:8082/api/auth/users/role/ROLE_STAFF').catch(() => ({ data: [] }))
            ]);

            const residents = residentsResponse.data || [];
            const staff = staffResponse.data || [];

            // Fetch pending invoices
            const invoicesResponse = await axios.get('http://localhost:8082/api/invoices/admin/pending').catch(() => ({ data: [] }));
            const pendingInvoicesData = invoicesResponse.data || [];

            // Fetch collection statistics using your actual endpoint
            const today = new Date();
            const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
            const endOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0);

            const collectionsResponse = await axios.get(
                `http://localhost:8082/api/waste/collections/stats/total-waste?start=${startOfMonth.toISOString().split('T')[0]}T00:00:00&end=${endOfMonth.toISOString().split('T')[0]}T23:59:59`
            ).catch(() => ({ data: {} }));

            const collectionsData = collectionsResponse.data || {};

            // 🆕 FETCH REAL RECENT COLLECTIONS
            const recentCollectionsData = await fetchRecentCollections(staff);

            // Calculate financial stats
            const totalPendingAmount = pendingInvoicesData.reduce((sum, invoice) =>
                sum + (invoice.finalAmount || invoice.totalAmount || 0), 0
            );

            // Calculate collection revenue from recent collections
            const totalCollectionRevenue = recentCollectionsData.reduce((sum, collection) =>
                sum + (collection.calculatedCharge || 0), 0
            );

            // Calculate today's stats
            const todayCollections = recentCollectionsData.filter(collection => {
                if (!collection.collectionTime) return false;
                const collectionDate = new Date(collection.collectionTime).toDateString();
                return collectionDate === today.toDateString();
            });

            const todayWeight = todayCollections.reduce((sum, coll) => sum + (coll.weight || 0), 0);
            const todayRevenue = todayCollections.reduce((sum, coll) => sum + (coll.calculatedCharge || 0), 0);

            setStats({
                totalUsers: residents.length + staff.length,
                totalResidents: residents.length,
                totalStaff: staff.length,
                pendingInvoicesCount: pendingInvoicesData.length,
                pendingAmount: totalPendingAmount,
                totalCollections: collectionsData.collectionCount || recentCollectionsData.length,
                totalWeight: collectionsData.totalWeight || 0,
                collectionRevenue: totalCollectionRevenue,
                todayCollections: todayCollections.length,
                todayWeight: todayWeight,
                todayRevenue: todayRevenue
            });

            setPendingInvoices(pendingInvoicesData.slice(0, 5));
            setRecentCollections(recentCollectionsData.slice(0, 6)); // Show 6 most recent
            setCollectionStats({
                total: recentCollectionsData.length,
                today: todayCollections.length,
                recycling: recentCollectionsData.filter(c => c.recyclableItemsCount > 0).length
            });

        } catch (error) {
            console.error('Error fetching admin dashboard data:', error);
            toast.error('Failed to load dashboard data');
        } finally {
            setLoading(false);
        }
    };

    // 🆕 NEW FUNCTION: Fetch recent collections from all staff
    const fetchRecentCollections = async (staffMembers) => {
        try {
            const allCollections = [];

            // Fetch collections for each staff member (limit to 3 to prevent too many requests)
            const staffToFetch = staffMembers.slice(0, 3);

            for (const staff of staffToFetch) {
                try {
                    const collectionsResponse = await axios.get(
                        `http://localhost:8082/api/waste/collections/collector/${staff.id}`
                    );

                    if (collectionsResponse.data && Array.isArray(collectionsResponse.data)) {
                        // Add staff info and limit to recent 10 collections per staff
                        const staffCollections = collectionsResponse.data
                            .slice(0, 10)
                            .map(collection => ({
                                ...collection,
                                staffName: staff.name,
                                collectorId: staff.id,
                                // Ensure we have all required fields
                                weight: collection.weight || 0,
                                calculatedCharge: collection.calculatedCharge || 0,
                                recyclableItemsCount: collection.recyclableItemsCount || 0,
                                recyclableWeight: collection.recyclableWeight || 0,
                                refundAmount: collection.refundAmount || 0
                            }));

                        allCollections.push(...staffCollections);
                    }
                } catch (error) {
                    console.error(`Error fetching collections for staff ${staff.id}:`, error);
                }
            }

            // Sort by collection time (newest first) and return
            return allCollections
                .filter(collection => collection.collectionTime) // Only collections with timestamps
                .sort((a, b) => new Date(b.collectionTime) - new Date(a.collectionTime))
                .slice(0, 20); // Limit to 20 most recent overall

        } catch (error) {
            console.error('Error fetching recent collections:', error);
            return [];
        }
    };

    // 🆕 Format date for display
    const formatCollectionTime = (dateString) => {
        if (!dateString) return 'Unknown time';

        const date = new Date(dateString);
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / (1000 * 60));
        const diffHours = Math.floor(diffMs / (1000 * 60 * 60));

        if (diffMins < 60) {
            return `${diffMins}m ago`;
        } else if (diffHours < 24) {
            return `${diffHours}h ago`;
        } else {
            return date.toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        }
    };

    // 🆕 Get status badge for collection
    const getCollectionStatus = (collection) => {
        const hasRecycling = collection.recyclableItemsCount > 0;
        const isRecent = new Date(collection.collectionTime) > new Date(Date.now() - 2 * 60 * 60 * 1000); // Within 2 hours

        if (isRecent) {
            return <span className="badge badge-success">🟢 Just Now</span>;
        }
        if (hasRecycling) {
            return <span className="badge badge-info">♻️ Recycling</span>;
        }
        return <span className="badge badge-secondary">Completed</span>;
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
                <button
                    onClick={fetchDashboardData}
                    className="btn btn-secondary btn-sm"
                    disabled={loading}
                >
                    {loading ? 'Refreshing...' : '🔄 Refresh'}
                </button>
            </div>

            {/* Stats Grid - UPDATED WITH TODAY'S COLLECTIONS */}
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
                        <div className="stat-value">{stats.todayCollections || 0}</div>
                        <div className="stat-title">Today's Collections</div>
                        <div className="stat-breakdown">
                            {stats.todayWeight || 0} kg • ${(stats.todayRevenue || 0).toFixed(2)}
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
                        <div className="stat-value">${(stats.collectionRevenue || 0).toFixed(2)}</div>
                        <div className="stat-title">Collection Revenue</div>
                        <div className="stat-breakdown">
                            {stats.totalCollections || 0} total collections
                        </div>
                    </div>
                </div>
            </div>

            <div className="dashboard-grid admin-grid">
                {/* 🆕 UPDATED: Recent Collections with Real Data */}
                <div className="card">
                    <div className="card-header">
                        <h3>📦 Recent Collections</h3>
                        <div className="header-stats">
                            <span className="badge badge-primary">{recentCollections.length} recent</span>
                            <span className="badge badge-success">{collectionStats.today || 0} today</span>
                            {collectionStats.recycling > 0 && (
                                <span className="badge badge-info">{collectionStats.recycling} with recycling</span>
                            )}
                        </div>
                    </div>

                    {recentCollections.length > 0 ? (
                        <div className="collections-list enhanced">
                            {recentCollections.map((collection, index) => (
                                <div key={collection.id || index} className="collection-item enhanced">
                                    <div className="collection-icon">
                                        {collection.recyclableItemsCount > 0 ? '♻️' : '🗑️'}
                                    </div>
                                    <div className="collection-details">
                                        <div className="collection-main">
                                            <strong>Bin {collection.binId}</strong>
                                            <span className="location">{collection.location || 'Unknown Location'}</span>
                                        </div>
                                        <div className="collection-meta">
                                            <span className="collector">By {collection.staffName || 'Unknown Staff'}</span>
                                            <span className="time">{formatCollectionTime(collection.collectionTime)}</span>
                                        </div>
                                        <div className="collection-stats">
                                            <span className="weight">{collection.weight} kg</span>
                                            <span className="charge">${collection.calculatedCharge?.toFixed(2)}</span>
                                            {collection.recyclableItemsCount > 0 && (
                                                <span className="recycling">
                                                    ♻️ {collection.recyclableItemsCount} items
                                                    {collection.refundAmount > 0 && (
                                                        <span className="refund"> (${collection.refundAmount?.toFixed(2)})</span>
                                                    )}
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                    <div className="collection-status">
                                        {getCollectionStatus(collection)}
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="empty-state">
                            <div className="empty-icon">🚛</div>
                            <h4>No Collections Yet</h4>
                            <p>Collection data will appear here as staff members record collections.</p>
                            <button
                                onClick={fetchDashboardData}
                                className="btn btn-primary"
                            >
                                Check for New Collections
                            </button>
                        </div>
                    )}

                    {/* View All Link */}
                    {recentCollections.length > 0 && (
                        <div className="card-footer">
                            <a href="/admin/collections" className="view-all-link">
                                View All Collections →
                            </a>
                        </div>
                    )}
                </div>

                {/* Pending Invoices */}
                <div className="card">
                    <div className="card-header">
                        <h3>🧾 Pending Invoices</h3>
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
                                        <span className="amount">${invoice.finalAmount?.toFixed(2)}</span>
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
                        <h3>⚡ Quick Actions</h3>
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
                            View Collection Reports
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

                {/* Collection Performance Summary */}
                <div className="card">
                    <div className="card-header">
                        <h3>📈 Collection Performance</h3>
                    </div>
                    <div className="performance-summary">
                        <div className="performance-item">
                            <div className="performance-label">Today's Efficiency</div>
                            <div className="performance-value">
                                {stats.todayCollections > 0 ? '🟢 Good' : '⚪ No Data'}
                            </div>
                            <div className="performance-details">
                                {stats.todayCollections || 0} collections today
                            </div>
                        </div>
                        <div className="performance-item">
                            <div className="performance-label">Avg Collection Weight</div>
                            <div className="performance-value">
                                {stats.totalCollections > 0 ?
                                    `${(stats.totalWeight / stats.totalCollections).toFixed(1)} kg` :
                                    '0 kg'
                                }
                            </div>
                            <div className="performance-details">
                                per collection
                            </div>
                        </div>
                        <div className="performance-item">
                            <div className="performance-label">Active Staff</div>
                            <div className="performance-value">
                                {stats.totalStaff || 0}
                            </div>
                            <div className="performance-details">
                                collectors available
                            </div>
                        </div>
                        <div className="performance-item">
                            <div className="performance-label">Revenue Rate</div>
                            <div className="performance-value">
                                {stats.totalCollections > 0 ?
                                    `$${(stats.collectionRevenue / stats.totalCollections).toFixed(2)}` :
                                    '$0.00'
                                }
                            </div>
                            <div className="performance-details">
                                per collection
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AdminDashboard;