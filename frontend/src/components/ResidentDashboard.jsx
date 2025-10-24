// File: frontend/src/components/ResidentDashboard.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

function ResidentDashboard({ user }) {
    const [invoices, setInvoices] = useState([]);
    const [bins, setBins] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [activeTab, setActiveTab] = useState('invoices');

    // Fetch invoices
    const fetchInvoices = async () => {
        try {
            const response = await axios.get(`http://localhost:8082/api/invoices/resident/${user.id}`);
            setInvoices(response.data);
        } catch (err) {
            setError('Could not fetch invoices.');
            console.error(err);
        }
    };

    // Fetch waste bins
    const fetchBins = async () => {
        try {
            const response = await axios.get(`http://localhost:8082/api/waste/bins?residentId=${user.id}`);
            setBins(response.data);
        } catch (err) {
            console.error('Could not fetch bins:', err);
        }
    };

    useEffect(() => {
        const loadData = async () => {
            setLoading(true);
            await Promise.all([fetchInvoices(), fetchBins()]);
            setLoading(false);
        };
        loadData();
    }, [user.id]);

    // Handle invoice payment
    const handlePayInvoice = async (invoiceId) => {
        try {
            const paymentData = {
                paymentMethod: "ONLINE",
                transactionId: `TXN-${Date.now()}`
            };

            await axios.post(`http://localhost:8082/api/invoices/${invoiceId}/pay`, paymentData);
            await fetchInvoices(); // Refresh invoices
            alert('Payment processed successfully!');
        } catch (err) {
            alert('Payment failed: ' + (err.response?.data?.error || 'Server error'));
        }
    };

    // Generate new invoice
    const handleGenerateInvoice = async () => {
        try {
            await axios.post(`http://localhost:8082/api/invoices/generate/${user.id}`);
            await fetchInvoices();
            alert('Invoice generated successfully!');
        } catch (err) {
            alert('Failed to generate invoice: ' + (err.response?.data?.error || 'Server error'));
        }
    };

    return (
        <div className="dashboard-container">
            <h2 className="dashboard-title">Resident Dashboard</h2>

            {/* Tab Navigation */}
            <div className="tab-navigation">
                <button
                    className={activeTab === 'invoices' ? 'tab-active' : ''}
                    onClick={() => setActiveTab('invoices')}
                >
                    My Invoices
                </button>
                <button
                    className={activeTab === 'bins' ? 'tab-active' : ''}
                    onClick={() => setActiveTab('bins')}
                >
                    My Waste Bins
                </button>
            </div>

            {/* Invoices Tab */}
            {activeTab === 'invoices' && (
                <div className="dashboard-card">
                    <div className="card-header">
                        <h3>My Invoices</h3>
                        <button onClick={handleGenerateInvoice} className="generate-button">
                            Generate Monthly Invoice
                        </button>
                    </div>

                    {loading && <p>Loading invoices...</p>}
                    {error && <p className="error-message">{error}</p>}

                    {!loading && !error && (
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>Invoice #</th>
                                <th>Date</th>
                                <th>Due Date</th>
                                <th>Period</th>
                                <th>Amount</th>
                                <th>Status</th>
                                <th>Action</th>
                            </tr>
                            </thead>
                            <tbody>
                            {invoices.length > 0 ? (
                                invoices.map((invoice) => (
                                    <tr key={invoice.id}>
                                        <td>{invoice.invoiceNumber}</td>
                                        <td>{invoice.invoiceDate}</td>
                                        <td>{invoice.dueDate}</td>
                                        <td>{invoice.periodStart} to {invoice.periodEnd}</td>
                                        <td>${invoice.totalAmount?.toFixed(2)}</td>
                                        <td>
                        <span className={`status-badge status-${invoice.status?.toLowerCase()}`}>
                          {invoice.status}
                        </span>
                                        </td>
                                        <td>
                                            {invoice.status === 'PENDING' && invoice.totalAmount > 0 && (
                                                <button
                                                    onClick={() => handlePayInvoice(invoice.id)}
                                                    className="pay-button"
                                                >
                                                    Pay Now
                                                </button>
                                            )}
                                            {invoice.status === 'PAID' && (
                                                <span className="paid-text">Paid</span>
                                            )}
                                        </td>
                                    </tr>
                                ))
                            ) : (
                                <tr>
                                    <td colSpan="7">No invoices found.</td>
                                </tr>
                            )}
                            </tbody>
                        </table>
                    )}
                </div>
            )}

            {/* Bins Tab */}
            {activeTab === 'bins' && (
                <div className="dashboard-card">
                    <h3>My Waste Bins</h3>
                    {loading && <p>Loading bins...</p>}
                    {!loading && (
                        <div className="bins-grid">
                            {bins.length > 0 ? (
                                bins.map((bin) => (
                                    <div key={bin.binId} className="bin-card">
                                        <h4>Bin {bin.binId}</h4>
                                        <p><strong>Location:</strong> {bin.location}</p>
                                        <p><strong>Type:</strong> {bin.binType}</p>
                                        <p><strong>Capacity:</strong> {bin.capacity}L</p>
                                        <p><strong>Current Level:</strong> {bin.currentLevel}%</p>
                                        <p><strong>Status:</strong>
                                            <span className={`status-badge status-${bin.status?.toLowerCase()}`}>
                        {bin.status}
                      </span>
                                        </p>
                                    </div>
                                ))
                            ) : (
                                <p>No waste bins registered.</p>
                            )}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default ResidentDashboard;