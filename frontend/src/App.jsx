// File: frontend/src/App.jsx
import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Profile from './pages/Profile';
import Bins from './pages/Bins';
import Invoices from './pages/Invoices';
import Payments from './pages/Payments';
import StaffDashboard from './pages/StaffDashboard';
import StaffScan from './pages/StaffScan';
import StaffCollections from './pages/StaffCollections';
import StaffProfile from './pages/StaffProfile';
import Layout from './components/Layout';
import StaffLayout from './components/StaffLayout';
import Schedules from './pages/Schedules';
import AdminLayout from './components/AdminLayout';
import AdminDashboard from './pages/admin/AdminDashboard';
import BillingManagement from './pages/admin/BillingManagement';
import AdminAnalyticsDashboard from './pages/admin/AdminAnalyticsDashboard';
import './App.css';
import UserManagement from "./pages/admin/UserManagement.jsx";
import CollectionReports from "./pages/admin/CollectionReports.jsx";
import InvoicesPayments from "./pages/admin/InvoicesPayments.jsx";
import BinMonitoring from "./pages/admin/BinMonitoring.jsx";
import ScheduleManagement from "./pages/admin/ScheduleManagement.jsx";
import PaymentSuccess from './pages/PaymentSuccess';
import PaymentCancel from './pages/PaymentCancel';
import staffScanOffline from "./pages/StaffScanOffline.jsx";
import StaffScanOffline from "./pages/StaffScanOffline.jsx";
function App() {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Check if user is logged in (from localStorage)
        const savedUser = localStorage.getItem('currentUser');
        if (savedUser) {
            try {
                const userData = JSON.parse(savedUser);
                // Ensure user object has proper structure
                setUser(normalizeUser(userData));
            } catch (error) {
                console.error('Error parsing saved user:', error);
                localStorage.removeItem('currentUser');
            }
        }
        setLoading(false);
    }, []);

    // Normalize user object to ensure it has the expected structure
    const normalizeUser = (userData) => {
        // Handle different response formats from backend
        if (userData.user) {
            return userData.user; // If response has { user: {...} }
        }
        if (userData.id) {
            return userData; // If response is the user object directly
        }
        // Fallback: create a normalized user object
        return {
            id: userData.id || userData.userId || Math.random().toString(36).substr(2, 9),
            name: userData.name || 'User',
            email: userData.email || '',
            role: userData.role || 'ROLE_RESIDENT',
            address: userData.address || '',
            phone: userData.phone || '',
            residentId: userData.residentId || null
        };
    };

    const handleLogin = (userData) => {
        const normalizedUser = normalizeUser(userData);
        setUser(normalizedUser);
        localStorage.setItem('currentUser', JSON.stringify(normalizedUser));
    };

    const handleLogout = () => {
        setUser(null);
        localStorage.removeItem('currentUser');
    };

    // Function to render different dashboards based on user role
    const renderDashboard = () => {
        if (!user || !user.role) {
            return <p>Loading user information...</p>;
        }

        switch (user.role) {
            case 'ROLE_ADMIN':
                return <div>Admin Dashboard - Coming Soon</div>;
            case 'ROLE_STAFF':
                return <StaffDashboard user={user} />;
            case 'ROLE_RESIDENT':
                return <Dashboard user={user} />;
            default:
                return <p>Unknown role. Please contact support.</p>;
        }
    };

    // Function to determine default route based on user role
    const getDefaultRoute = () => {
        if (!user) return '/login';

        switch (user.role) {
            case 'ROLE_STAFF':
                return '/staff/dashboard';
            case 'ROLE_ADMIN':
                return '/admin/dashboard'; // You can create this later
            case 'ROLE_RESIDENT':
            default:
                return '/dashboard';
        }
    };

    if (loading) {
        return (
            <div className="loading-screen">
                <div className="loading-spinner"></div>
                <p>Loading Smart Waste Management...</p>
            </div>
        );
    }

    return (
        <Router>
            <div className="App">
                <Toaster position="top-right" />
                <Routes>
                    {/* Public Routes */}
                    <Route
                        path="/login"
                        element={
                            user ? <Navigate to={getDefaultRoute()} /> : <Login onLogin={handleLogin} />
                        }
                    />
                    <Route
                        path="/register"
                        element={
                            user ? <Navigate to={getDefaultRoute()} /> : <Register onLogin={handleLogin} />
                        }
                    />
                    <Route
                        path="/"
                        element={
                            user ? <Navigate to={getDefaultRoute()} /> : <Navigate to="/login" />
                        }
                    />

                    {/* Resident Routes */}
                    {user && user.role === 'ROLE_RESIDENT' && (
                        <Route element={<Layout user={user} onLogout={handleLogout} />}>
                            <Route path="/dashboard" element={<Dashboard user={user} />} />
                            <Route path="/profile" element={<Profile user={user} />} />
                            <Route path="/schedules" element={<Schedules user={user} />} />

                            <Route path="/bins" element={<Bins user={user} />} />
                            <Route path="/invoices" element={<Invoices user={user} />} />
                            <Route path="/payments" element={<Payments user={user} />} />
                            {/* NEW PAYMENT ROUTES */}
                            <Route path="/payment/success" element={<PaymentSuccess />} />
                            <Route path="/payment/cancel" element={<PaymentCancel />} />
                        </Route>
                    )}

                    {/* Staff Routes */}
                    {user && user.role === 'ROLE_STAFF' && (
                        <Route element={<StaffLayout user={user} onLogout={handleLogout} />}>
                            <Route path="/staff/dashboard" element={<StaffDashboard user={user} />} />
                            <Route path="/staff/scan" element={<StaffScan user={user} />} />
                            <Route path="/staff/collections" element={<StaffCollections user={user} />} />
                            <Route path="/staff/scan-offline" element={<StaffScanOffline user={user} />} />
                            <Route path="/staff/profile" element={<StaffProfile user={user} />} />
                            {/* Redirect staff users from regular dashboard to staff dashboard */}
                            <Route path="/dashboard" element={<Navigate to="/staff/dashboard" />} />
                            <Route path="/profile" element={<Navigate to="/staff/profile" />} />
                        </Route>
                    )}

                    {/* Admin Routes - You can add these later */}
                    {user && user.role === 'ROLE_ADMIN' && (
                        <Route element={<AdminLayout user={user} onLogout={handleLogout} />}>                            <Route path="/admin/dashboard" element={<AdminDashboard user={user} />} />
                            <Route path="/admin/billing" element={<BillingManagement user={user} />} />
                            <Route path="/admin/users" element={<UserManagement user={user} />} />
                            <Route path="/admin/collections" element={<CollectionReports user={user} />} />
                            <Route path="/admin/invoices" element={<InvoicesPayments user={user} />} />
                            <Route path="/admin/bins" element={<BinMonitoring user={user} />} />
                            <Route path="/admin/schedules" element={<ScheduleManagement user={user} />} />
                            <Route path="/admin/analytics" element={<AdminAnalyticsDashboard user={user} />} />

                        </Route>
                    )}

                    {/* Fallback Routes */}
                    <Route
                        path="*"
                        element={<Navigate to={user ? getDefaultRoute() : "/login"} />}
                    />
                </Routes>
            </div>
        </Router>
    );
}

export default App;