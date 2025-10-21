// File: frontend/src/components/Layout/Layout.jsx
import React from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Sidebar from './Sidebar';

const Layout = ({ user, onLogout }) => {
    const location = useLocation();
    const navigate = useNavigate();

    const getPageTitle = () => {
        const path = location.pathname;
        switch (path) {
            case '/dashboard':
                return 'Dashboard';
            case '/profile':
                return 'My Profile';
            case '/bins':
                return 'My Waste Bins';
            case '/invoices':
                return 'Billing & Invoices';
            case '/payments':
                return 'Payment History';
            default:
                return 'Dashboard';
        }
    };

    const getPageDescription = () => {
        const path = location.pathname;
        switch (path) {
            case '/dashboard':
                return 'Overview of your waste management account';
            case '/profile':
                return 'Manage your personal information and settings';
            case '/bins':
                return 'View and manage your registered waste bins';
            case '/invoices':
                return 'View invoices and make payments';
            case '/payments':
                return 'Track your payment history and receipts';
            default:
                return '';
        }
    };

    return (
        <div className="layout">
            <Sidebar user={user} onLogout={onLogout} currentPath={location.pathname} />
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

export default Layout;