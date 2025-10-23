// File: frontend/src/pages/admin/UserManagement.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const UserManagement = ({ user }) => {
    const [users, setUsers] = useState([]);
    const [filteredUsers, setFilteredUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [roleFilter, setRoleFilter] = useState('ALL');
    const [showCreateForm, setShowCreateForm] = useState(false);
    const [formData, setFormData] = useState({
        name: '',
        email: '',
        password: '',
        role: 'ROLE_RESIDENT',
        phoneNumber: '',
        address: ''
    });

    useEffect(() => {
        fetchUsers();
    }, []);

    useEffect(() => {
        filterUsers();
    }, [users, searchTerm, roleFilter]);

    const fetchUsers = async () => {
        try {
            setLoading(true);
            // Fetch all users
            const response = await axios.get('http://localhost:8082/api/auth/users');
            console.log('Users response:', response.data);
            setUsers(response.data || []);
        } catch (error) {
            console.error('Error fetching users:', error);
            toast.error('Failed to load users');
        } finally {
            setLoading(false);
        }
    };

    const filterUsers = () => {
        let filtered = users;

        // Filter by search term
        if (searchTerm) {
            filtered = filtered.filter(user =>
                user.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                user.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                user.phoneNumber?.includes(searchTerm)
            );
        }

        // Filter by role
        if (roleFilter !== 'ALL') {
            filtered = filtered.filter(user => user.role === roleFilter);
        }

        setFilteredUsers(filtered);
    };

    const handleCreateUser = async (e) => {
        e.preventDefault();
        try {
            const response = await axios.post('http://localhost:8082/api/auth/register', formData);
            toast.success('User created successfully');
            setShowCreateForm(false);
            setFormData({
                name: '',
                email: '',
                password: '',
                role: 'ROLE_RESIDENT',
                phoneNumber: '',
                address: ''
            });
            fetchUsers();
        } catch (error) {
            console.error('Error creating user:', error);
            toast.error(error.response?.data?.error || 'Failed to create user');
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const getRoleBadge = (role) => {
        const roleConfig = {
            'ROLE_RESIDENT': { label: 'Resident', class: 'badge-primary' },
            'ROLE_STAFF': { label: 'Staff', class: 'badge-warning' },
            'ROLE_ADMIN': { label: 'Admin', class: 'badge-danger' },
            'ROLE_CITY_MANAGER': { label: 'City Manager', class: 'badge-info' }
        };

        const config = roleConfig[role] || { label: role, class: 'badge-secondary' };
        return <span className={`badge ${config.class}`}>{config.label}</span>;
    };

    const getStatusBadge = (user) => {
        if (user.enabled === false) return <span className="badge badge-danger">Disabled</span>;
        return <span className="badge badge-success">Active</span>;
    };

    const toggleUserStatus = async (userId, currentStatus) => {
        try {
            const endpoint = currentStatus ?
                `http://localhost:8082/api/auth/users/${userId}/disable` :
                `http://localhost:8082/api/auth/users/${userId}/enable`;

            await axios.put(endpoint);
            toast.success(`User ${currentStatus ? 'disabled' : 'enabled'} successfully`);
            fetchUsers();
        } catch (error) {
            console.error('Error toggling user status:', error);
            toast.error('Failed to update user status');
        }
    };

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading users...</p>
            </div>
        );
    }

    return (
        <div className="user-management">
            <div className="page-header">
                <h1>User Management</h1>
                <p>Manage system users, roles, and permissions</p>
                <button
                    onClick={() => setShowCreateForm(true)}
                    className="btn btn-primary"
                >
                    + Create User
                </button>
            </div>

            {/* Filters */}
            <div className="filters-card">
                <div className="filter-group">
                    <input
                        type="text"
                        placeholder="Search users by name, email, or phone..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="search-input"
                    />
                </div>
                <div className="filter-group">
                    <select
                        value={roleFilter}
                        onChange={(e) => setRoleFilter(e.target.value)}
                        className="filter-select"
                    >
                        <option value="ALL">All Roles</option>
                        <option value="ROLE_RESIDENT">Residents</option>
                        <option value="ROLE_STAFF">Staff</option>
                        <option value="ROLE_ADMIN">Admins</option>
                        <option value="ROLE_CITY_MANAGER">City Managers</option>
                    </select>
                </div>
                <div className="filter-group">
                    <span className="results-count">{filteredUsers.length} users found</span>
                </div>
            </div>

            {/* Create User Modal */}
            {showCreateForm && (
                <div className="modal-overlay">
                    <div className="modal">
                        <div className="modal-header">
                            <h3>Create New User</h3>
                            <button
                                onClick={() => setShowCreateForm(false)}
                                className="btn-close"
                            >
                                ×
                            </button>
                        </div>
                        <form onSubmit={handleCreateUser}>
                            <div className="form-grid">
                                <div className="form-group">
                                    <label>Full Name</label>
                                    <input
                                        type="text"
                                        name="name"
                                        value={formData.name}
                                        onChange={handleInputChange}
                                        required
                                        className="form-input"
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Email</label>
                                    <input
                                        type="email"
                                        name="email"
                                        value={formData.email}
                                        onChange={handleInputChange}
                                        required
                                        className="form-input"
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Password</label>
                                    <input
                                        type="password"
                                        name="password"
                                        value={formData.password}
                                        onChange={handleInputChange}
                                        required
                                        className="form-input"
                                    />
                                </div>
                                <div className="form-group">
                                    <label>Role</label>
                                    <select
                                        name="role"
                                        value={formData.role}
                                        onChange={handleInputChange}
                                        required
                                        className="form-input"
                                    >
                                        <option value="ROLE_RESIDENT">Resident</option>
                                        <option value="ROLE_STAFF">Staff</option>
                                        <option value="ROLE_ADMIN">Admin</option>
                                        <option value="ROLE_CITY_MANAGER">City Manager</option>
                                    </select>
                                </div>
                                <div className="form-group">
                                    <label>Phone Number</label>
                                    <input
                                        type="tel"
                                        name="phoneNumber"
                                        value={formData.phoneNumber}
                                        onChange={handleInputChange}
                                        className="form-input"
                                    />
                                </div>
                                <div className="form-group full-width">
                                    <label>Address</label>
                                    <textarea
                                        name="address"
                                        value={formData.address}
                                        onChange={handleInputChange}
                                        className="form-input"
                                        rows="3"
                                    />
                                </div>
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
                                    Create User
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Users Table */}
            <div className="card">
                <div className="card-header">
                    <h3>System Users</h3>
                    <span className="badge">{users.length} total users</span>
                </div>

                {filteredUsers.length > 0 ? (
                    <div className="table-container">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>User</th>
                                <th>Contact</th>
                                <th>Role</th>
                                <th>Status</th>
                                <th>Registration Date</th>
                                <th>Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredUsers.map((user) => (
                                <tr key={user.id}>
                                    <td>
                                        <div className="user-cell">
                                            <div className="user-avatar">
                                                {user.name?.charAt(0).toUpperCase()}
                                            </div>
                                            <div className="user-details">
                                                <strong>{user.name}</strong>
                                                <span>{user.email}</span>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div className="contact-info">
                                            <span>{user.phoneNumber || 'N/A'}</span>
                                            <small>{user.address ? `${user.address.substring(0, 30)}...` : 'No address'}</small>
                                        </div>
                                    </td>
                                    <td>{getRoleBadge(user.role)}</td>
                                    <td>{getStatusBadge(user)}</td>
                                    <td>
                                        {user.registrationDate ?
                                            new Date(user.registrationDate).toLocaleDateString() :
                                            'N/A'
                                        }
                                    </td>
                                    <td>
                                        <div className="action-buttons">
                                            <button
                                                className="btn btn-sm btn-secondary"
                                                onClick={() => toggleUserStatus(user.id, user.enabled)}
                                            >
                                                {user.enabled ? 'Disable' : 'Enable'}
                                            </button>
                                            <button className="btn btn-sm btn-info">Edit</button>
                                            <button className="btn btn-sm btn-danger">Delete</button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="empty-state">
                        <h3>No Users Found</h3>
                        <p>No users match your search criteria.</p>
                        <button
                            onClick={() => {
                                setSearchTerm('');
                                setRoleFilter('ALL');
                            }}
                            className="btn btn-primary"
                        >
                            Clear Filters
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default UserManagement;