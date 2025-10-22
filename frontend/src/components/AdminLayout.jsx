// File: frontend/src/components/AdminLayout.jsx
import React from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';

const AdminLayout = ({ user, onLogout }) => {
    const navigate = useNavigate();

    const handleLogout = () => {
        onLogout();
        navigate('/login');
    };

    return (
        <div className="layout admin-layout">
            {/* Sidebar */}
            <div className="sidebar admin-sidebar">
                <div className="sidebar-header">
                    <h2>🗑️ EcoWaste Admin</h2>
                    <div className="user-info">
                        <span className="user-name">{user.name}</span>
                        <span className="user-role">Administrator</span>
                    </div>
                </div>

                <nav className="sidebar-nav">
                    <NavLink to="/admin/dashboard" className="nav-item">
                        <span className="nav-icon">📊</span>
                        Dashboard
                    </NavLink>
                    <NavLink to="/admin/billing" className="nav-item">
                        <span className="nav-icon">💰</span>
                        Billing Models
                    </NavLink>
                    <NavLink to="/admin/users" className="nav-item">
                        <span className="nav-icon">👥</span>
                        User Management
                    </NavLink>
                    <NavLink to="/admin/collections" className="nav-item">
                        <span className="nav-icon">🚛</span>
                        Collection Reports
                    </NavLink>
                    <NavLink to="/admin/invoices" className="nav-item">
                        <span className="nav-icon">🧾</span>
                        Invoices & Payments
                    </NavLink>
                    <NavLink to="/admin/bins" className="nav-item">
                        <span className="nav-icon">🗑️</span>
                        Bin Monitoring
                    </NavLink>
                    <NavLink to="/admin/schedules" className="nav-item">
                        <span className="nav-icon">📅</span>
                        Schedules
                    </NavLink>
                    <NavLink to="/admin/staff" className="nav-item">
                        <span className="nav-icon">👷</span>
                        Staff Performance
                    </NavLink>
                </nav>

                <div className="sidebar-footer">
                    <NavLink to="/admin/profile" className="nav-item">
                        <span className="nav-icon">👤</span>
                        Profile
                    </NavLink>
                    <button onClick={handleLogout} className="nav-item logout-btn">
                        <span className="nav-icon">🚪</span>
                        Logout
                    </button>
                </div>
            </div>

            {/* Main Content */}
            <div className="main-content admin-content">
                <Outlet />
            </div>
        </div>
    );
};

export default AdminLayout;