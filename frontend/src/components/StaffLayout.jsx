// File: frontend/src/components/Layout/StaffLayout.jsx
import React from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import StaffSidebar from './StaffSidebar';

const StaffLayout = ({ user, onLogout }) => {
    const location = useLocation();

    const getPageTitle = () => {
        const path = location.pathname;
        switch (path) {
            case '/staff/dashboard':
                return 'Staff Dashboard';
            case '/staff/scan':
                return 'Scan Bin';
            case '/staff/scan-offline':
                return 'Offline Collection'; // 🆕 NEW
            case '/staff/collections':
                return 'Collection History';
            case '/staff/profile':
                return 'Staff Profile';
            default:
                return 'Staff Dashboard';
        }
    };

    const getPageDescription = () => {
        const path = location.pathname;
        switch (path) {
            case '/staff/dashboard':
                return 'Overview of your waste collection operations';
            case '/staff/scan':
                return 'Scan bins and record waste collection';
            case '/staff/scan-offline':
                return 'Record collections without internet connection'; // 🆕 NEW
            case '/staff/collections':
                return 'View your collection history and performance';
            case '/staff/profile':
                return 'Manage your staff account information';
            default:
                return '';
        }
    };

    return (
        <div className="layout">
            <StaffSidebar user={user} onLogout={onLogout} currentPath={location.pathname} />
            <div className="main-content">
                <header className="content-header">
                    <div>
                        <h1>{getPageTitle()}</h1>
                        <p>{getPageDescription()}</p>
                    </div>
                </header>
                <div className="content-body">
                    <Outlet />
                </div>
            </div>
        </div>
    );
};

export default StaffLayout;