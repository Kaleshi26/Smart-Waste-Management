// File: frontend/src/components/Layout/StaffSidebar.jsx
import React from 'react';
import { Link } from 'react-router-dom';

const StaffSidebar = ({ user, onLogout, currentPath }) => {
    const staffNavItems = [
        {
            path: '/staff/dashboard',
            label: 'Dashboard',
            icon: (
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z" />
                </svg>
            )
        },
        {
            path: '/staff/scan',
            label: 'Scan Bin',
            icon: (
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
                </svg>
            )
        },
        {
            path: '/staff/scan-offline',
            label: 'Offline Mode',
            icon: (
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.141 0M1.394 9.393c5.857-5.857 15.355-5.857 21.213 0" />
                </svg>
            )
        },
        {
            path: '/staff/collections',
            label: 'Collections',
            icon: (
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
            )
        },
        {
            path: '/staff/profile',
            label: 'Profile',
            icon: (
                <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
            )
        }
    ];

    return (
        <div className="sidebar staff-sidebar">
            <div className="sidebar-header">
                <h2>♻️ EcoWaste</h2>
                <p>Staff Portal</p>
            </div>

            <div className="sidebar-user staff">
                <div className="user-avatar staff">
                    {user?.name?.charAt(0).toUpperCase()}
                </div>
                <div className="user-details">
                    <strong>{user?.name}</strong>
                    <span className="user-role">Collection Staff</span>
                    <span className="user-id">ID: {user?.id}</span>
                </div>
            </div>

            <nav className="sidebar-nav">
                <div className="nav-section">
                    <h4>Collection Tools</h4>
                    <ul className="nav-links">
                        {staffNavItems.map((item) => (
                            <li key={item.path}>
                                <Link
                                    to={item.path}
                                    className={`nav-link ${currentPath === item.path ? 'active' : ''}`}
                                >
                                    {item.icon}
                                    {item.label}
                                    {item.path === '/staff/scan-offline' && (
                                        <span className="offline-badge">Offline</span>
                                    )}
                                </Link>
                            </li>
                        ))}
                    </ul>
                </div>
            </nav>

            <div className="sidebar-footer">
                <div className="staff-status">
                    <div className="status-indicator online"></div>
                    <span>On Duty</span>
                </div>
                <button onClick={onLogout} className="btn btn-secondary btn-block">
                    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="16" height="16">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                    Logout
                </button>
            </div>
        </div>
    );
};

export default StaffSidebar;