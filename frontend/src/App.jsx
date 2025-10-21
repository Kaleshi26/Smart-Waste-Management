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
import StaffDashboard from './pages/StaffDashboard'; // Add this import
import Layout from './components/Layout';
import './App.css';

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

    // Add this function to render different dashboards based on user role
    const renderDashboard = () => {
        if (!user || !user.role) {
            return <p>Loading user information...</p>;
        }

        switch (user.role) {
            case 'ROLE_ADMIN':
                return <div>Admin Dashboard - Coming Soon</div>; // You can create AdminDashboard later
            case 'ROLE_STAFF':
                return <StaffDashboard user={user} />;
            case 'ROLE_RESIDENT':
                return <Dashboard user={user} />;
            default:
                return <p>Unknown role. Please contact support.</p>;
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
                    <Route
                        path="/login"
                        element={
                            user ? <Navigate to="/dashboard" /> : <Login onLogin={handleLogin} />
                        }
                    />
                    <Route
                        path="/register"
                        element={
                            user ? <Navigate to="/dashboard" /> : <Register onLogin={handleLogin} />
                        }
                    />
                    <Route
                        path="/"
                        element={
                            user ? <Navigate to="/dashboard" /> : <Navigate to="/login" />
                        }
                    />

                    {/* Protected Routes */}
                    {user && (
                        <Route element={<Layout user={user} onLogout={handleLogout} />}>
                            <Route path="/dashboard" element={renderDashboard()} />
                            <Route path="/profile" element={<Profile user={user} />} />
                            <Route path="/bins" element={<Bins user={user} />} />
                            <Route path="/invoices" element={<Invoices user={user} />} />
                            <Route path="/payments" element={<Payments user={user} />} />
                        </Route>
                    )}

                    <Route path="*" element={<Navigate to={user ? "/dashboard" : "/login"} />} />
                </Routes>
            </div>
        </Router>
    );
}

export default App;