// File: frontend/src/pages/Profile.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const Profile = ({ user }) => {
    const [profile, setProfile] = useState(user);
    const [loading, setLoading] = useState(false);
    const [activeTab, setActiveTab] = useState('personal');
    const [editMode, setEditMode] = useState(false);

    useEffect(() => {
        // Refresh user data
        fetchUserProfile();
    }, []);

    const fetchUserProfile = async () => {
        try {
            // In a real app, you might have a dedicated profile endpoint
            // For now, we'll use the existing user data
            setProfile(user);
        } catch (error) {
            console.error('Error fetching profile:', error);
        }
    };

    const handleInputChange = (e) => {
        setProfile({
            ...profile,
            [e.target.name]: e.target.value
        });
    };

    const handleSaveProfile = async () => {
        setLoading(true);
        try {
            // In a real app, you would call an API to update the profile
            // For demo purposes, we'll just show a success message
            setTimeout(() => {
                toast.success('Profile updated successfully!');
                setEditMode(false);
                setLoading(false);
            }, 1000);
        } catch (error) {
            toast.error('Failed to update profile');
            setLoading(false);
        }
    };

    const getRoleDisplay = (role) => {
        const roleMap = {
            'ROLE_RESIDENT': 'Resident',
            'ROLE_STAFF': 'Staff Member',
            'ROLE_ADMIN': 'Administrator'
        };
        return roleMap[role] || role.replace('ROLE_', '');
    };

    return (
        <div className="profile-page">
            <div className="page-header">
                <div>
                    <h1>My Profile</h1>
                    <p>Manage your account information and preferences</p>
                </div>
                <div className="header-actions">
                    {!editMode ? (
                        <button
                            onClick={() => setEditMode(true)}
                            className="btn btn-primary"
                        >
                            Edit Profile
                        </button>
                    ) : (
                        <div className="edit-actions">
                            <button
                                onClick={() => setEditMode(false)}
                                className="btn btn-secondary"
                                disabled={loading}
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleSaveProfile}
                                className="btn btn-primary"
                                disabled={loading}
                            >
                                {loading ? 'Saving...' : 'Save Changes'}
                            </button>
                        </div>
                    )}
                </div>
            </div>

            <div className="profile-layout">
                {/* Sidebar */}
                <div className="profile-sidebar">
                    <div className="profile-summary">
                        <div className="profile-avatar">
                            {profile.name?.charAt(0).toUpperCase()}
                        </div>
                        <div className="profile-info">
                            <h3>{profile.name}</h3>
                            <p className="profile-role">{getRoleDisplay(profile.role)}</p>
                            <p className="profile-email">{profile.email}</p>
                        </div>
                    </div>

                    <nav className="profile-nav">
                        <button
                            className={activeTab === 'personal' ? 'active' : ''}
                            onClick={() => setActiveTab('personal')}
                        >
                            Personal Information
                        </button>
                        <button
                            className={activeTab === 'account' ? 'active' : ''}
                            onClick={() => setActiveTab('account')}
                        >
                            Account Settings
                        </button>
                        <button
                            className={activeTab === 'preferences' ? 'active' : ''}
                            onClick={() => setActiveTab('preferences')}
                        >
                            Preferences
                        </button>
                    </nav>
                </div>

                {/* Main Content */}
                <div className="profile-content">
                    {/* Personal Information Tab */}
                    {activeTab === 'personal' && (
                        <div className="profile-card">
                            <h3>Personal Information</h3>
                            <p className="card-description">
                                Update your personal details and contact information
                            </p>

                            <div className="form-grid">
                                <div className="form-group">
                                    <label className="form-label">Full Name</label>
                                    <input
                                        type="text"
                                        name="name"
                                        className="form-input"
                                        value={profile.name || ''}
                                        onChange={handleInputChange}
                                        disabled={!editMode}
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Email Address</label>
                                    <input
                                        type="email"
                                        name="email"
                                        className="form-input"
                                        value={profile.email || ''}
                                        onChange={handleInputChange}
                                        disabled={!editMode}
                                    />
                                </div>

                                <div className="form-group">
                                    <label className="form-label">Phone Number</label>
                                    <input
                                        type="tel"
                                        name="phone"
                                        className="form-input"
                                        value={profile.phone || ''}
                                        onChange={handleInputChange}
                                        disabled={!editMode}
                                        placeholder="Enter your phone number"
                                    />
                                </div>

                                <div className="form-group full-width">
                                    <label className="form-label">Address</label>
                                    <textarea
                                        name="address"
                                        className="form-input"
                                        rows="3"
                                        value={profile.address || ''}
                                        onChange={handleInputChange}
                                        disabled={!editMode}
                                        placeholder="Enter your complete address"
                                    />
                                </div>
                            </div>

                            {!editMode && (
                                <div className="info-grid">
                                    <div className="info-item">
                                        <label>User ID:</label>
                                        <span>{profile.id}</span>
                                    </div>
                                    <div className="info-item">
                                        <label>Resident ID:</label>
                                        <span>{profile.residentId || 'Not assigned'}</span>
                                    </div>
                                    <div className="info-item">
                                        <label>Account Created:</label>
                                        <span>{profile.accountActivationDate || 'Not available'}</span>
                                    </div>
                                    <div className="info-item">
                                        <label>Account Role:</label>
                                        <span className="role-badge">{getRoleDisplay(profile.role)}</span>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Account Settings Tab */}
                    {activeTab === 'account' && (
                        <div className="profile-card">
                            <h3>Account Settings</h3>
                            <p className="card-description">
                                Manage your account security and preferences
                            </p>

                            <div className="settings-list">
                                <div className="setting-item">
                                    <div className="setting-info">
                                        <h4>Change Password</h4>
                                        <p>Update your password to keep your account secure</p>
                                    </div>
                                    <button className="btn btn-secondary">
                                        Change Password
                                    </button>
                                </div>

                                <div className="setting-item">
                                    <div className="setting-info">
                                        <h4>Two-Factor Authentication</h4>
                                        <p>Add an extra layer of security to your account</p>
                                    </div>
                                    <div className="setting-status">
                                        <span className="status-badge status-inactive">Disabled</span>
                                        <button className="btn btn-sm btn-secondary">
                                            Enable
                                        </button>
                                    </div>
                                </div>

                                <div className="setting-item">
                                    <div className="setting-info">
                                        <h4>Login History</h4>
                                        <p>View your recent login activity and devices</p>
                                    </div>
                                    <button className="btn btn-secondary">
                                        View History
                                    </button>
                                </div>

                                <div className="setting-item">
                                    <div className="setting-info">
                                        <h4>Account Status</h4>
                                        <p>Your account is active and in good standing</p>
                                    </div>
                                    <span className="status-badge status-active">Active</span>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Preferences Tab */}
                    {activeTab === 'preferences' && (
                        <div className="profile-card">
                            <h3>Preferences</h3>
                            <p className="card-description">
                                Customize your waste management experience
                            </p>

                            <div className="preferences-list">
                                <div className="preference-item">
                                    <div className="preference-info">
                                        <h4>Email Notifications</h4>
                                        <p>Receive email alerts for new invoices and collection schedules</p>
                                    </div>
                                    <label className="toggle">
                                        <input type="checkbox" defaultChecked />
                                        <span className="toggle-slider"></span>
                                    </label>
                                </div>

                                <div className="preference-item">
                                    <div className="preference-info">
                                        <h4>SMS Notifications</h4>
                                        <p>Get text messages for urgent updates and bin collection</p>
                                    </div>
                                    <label className="toggle">
                                        <input type="checkbox" />
                                        <span className="toggle-slider"></span>
                                    </label>
                                </div>

                                <div className="preference-item">
                                    <div className="preference-info">
                                        <h4>Monthly Reports</h4>
                                        <p>Receive detailed monthly waste management reports</p>
                                    </div>
                                    <label className="toggle">
                                        <input type="checkbox" defaultChecked />
                                        <span className="toggle-slider"></span>
                                    </label>
                                </div>

                                <div className="preference-item">
                                    <div className="preference-info">
                                        <h4>Payment Reminders</h4>
                                        <p>Get reminders for upcoming invoice due dates</p>
                                    </div>
                                    <label className="toggle">
                                        <input type="checkbox" defaultChecked />
                                        <span className="toggle-slider"></span>
                                    </label>
                                </div>
                            </div>

                            <div className="preference-section">
                                <h4>Billing Preferences</h4>
                                <div className="form-group">
                                    <label className="form-label">Preferred Payment Method</label>
                                    <select className="form-input" defaultValue="ONLINE_BANKING">
                                        <option value="CREDIT_CARD">Credit Card</option>
                                        <option value="DEBIT_CARD">Debit Card</option>
                                        <option value="ONLINE_BANKING">Online Banking</option>
                                        <option value="MOBILE_WALLET">Mobile Wallet</option>
                                    </select>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Profile;