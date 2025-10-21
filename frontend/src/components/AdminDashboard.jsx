// File: frontend/src/components/AdminDashboard.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

function AdminDashboard({ user }) {
    const [stats, setStats] = useState(null);
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeTab, setActiveTab] = useState('overview');

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);
            setError(null);

            try {
                const [usersResponse, revenueResponse] = await Promise.all([
                    axios.get('http://localhost:8082/api/auth/users'),
                    axios.get('http://localhost:8082/api/invoices/revenue?start=2025-01-01&end=2025-12-31')
                ]);

                setUsers(usersResponse.data);

                // Calculate basic stats from users data
                const residents = usersResponse.data.filter(u => u.role === 'ROLE_RESIDENT');
                const staff = usersResponse.data.filter(u => u.role === 'ROLE_STAFF');

                setStats({
                    totalResidents: residents.length,
                    totalStaff: staff.length,
                    totalUsers: usersResponse.data.length,
                    totalRevenue: revenueResponse.data.totalRevenue || 0
                });

            } catch (err) {
                setError('Failed to load dashboard data.');
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    return (
        <div className="dashboard-container">
            <div className="dashboard-header">
                <div>
                    <h1 className="dashboard-title">Admin Dashboard</h1>
                    <p className="dashboard-welcome">System Overview and User Management</p>
                </div>
                <div className="tab-navigation">
                    <button
                        className={activeTab === 'overview' ? 'tab-active' : ''}
                        onClick={() => setActiveTab('overview')}
                    >
                        Overview
                    </button>
                    <button
                        className={activeTab === 'users' ? 'tab-active' : ''}
                        onClick={() => setActiveTab('users')}
                    >
                        User Management
                    </button>
                </div>
            </div>

            {/* Overview Tab */}
            {activeTab === 'overview' && (
                <>
                    <div className="stats-grid">
                        {loading && <p>Loading stats...</p>}
                        {error && <p className="error-message">{error}</p>}
                        {stats && (
                            <>
                                <div className="stat-card">
                                    <h3 className="stat-title">Total Residents</h3>
                                    <p className="stat-value">{stats.totalResidents}</p>
                                </div>
                                <div className="stat-card">
                                    <h3 className="stat-title">Total Staff</h3>
                                    <p className="stat-value">{stats.totalStaff}</p>
                                </div>
                                <div className="stat-card">
                                    <h3 className="stat-title">Total Users</h3>
                                    <p className="stat-value">{stats.totalUsers}</p>
                                </div>
                                <div className="stat-card">
                                    <h3 className="stat-title">Total Revenue</h3>
                                    <p className="stat-value">${stats.totalRevenue.toFixed(2)}</p>
                                </div>
                            </>
                        )}
                    </div>

                    <div className="data-card">
                        <h3>System Information</h3>
                        <p>Welcome to the Smart Waste Management System Admin Dashboard.</p>
                        <p>Use this dashboard to monitor system activity and manage users.</p>
                    </div>
                </>
            )}

            {/* Users Tab */}
            {activeTab === 'users' && (
                <div className="data-card">
                    <h3>User Management</h3>
                    {loading && <p>Loading users...</p>}
                    {error && <p className="error-message">{error}</p>}
                    {!loading && !error && (
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Role</th>
                                <th>Address</th>
                                <th>Resident ID</th>
                            </tr>
                            </thead>
                            <tbody>
                            {users.length > 0 ? (
                                users.map((user) => (
                                    <tr key={user.id}>
                                        <td>{user.id}</td>
                                        <td>{user.name}</td>
                                        <td>{user.email}</td>
                                        <td>
                        <span className={`role-badge role-${user.role.toLowerCase().replace('role_', '')}`}>
                          {user.role.replace('ROLE_', '')}
                        </span>
                                        </td>
                                        <td>{user.address}</td>
                                        <td>{user.residentId || '-'}</td>
                                    </tr>
                                ))
                            ) : (
                                <tr><td colSpan="6">No users found.</td></tr>
                            )}
                            </tbody>
                        </table>
                    )}
                </div>
            )}
        </div>
    );
}

export default AdminDashboard;