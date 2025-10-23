// File: frontend/src/pages/Payments.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

const Payments = ({ user }) => {
    const [payments, setPayments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('all');

    useEffect(() => {
        fetchPayments();
    }, [user.id]);

    const fetchPayments = async () => {
        try {
            // Since we don't have a direct payments endpoint, we'll get invoices and extract payment info
            const response = await axios.get(`http://localhost:8082/api/invoices/resident/${user.id}`);
            const paidInvoices = response.data.filter(inv => inv.status === 'PAID');

            // Transform invoices into payment records
            const paymentRecords = paidInvoices.map(invoice => ({
                id: invoice.id,
                invoiceNumber: invoice.invoiceNumber,
                amount: invoice.totalAmount,
                paymentDate: invoice.invoiceDate, // Using invoice date as payment date for demo
                paymentMethod: 'ONLINE', // Default for demo
                status: 'COMPLETED',
                transactionId: `TXN-${invoice.id}-${invoice.invoiceNumber}`
            }));

            setPayments(paymentRecords);
        } catch (error) {
            console.error('Error fetching payments:', error);
        } finally {
            setLoading(false);
        }
    };

    const filteredPayments = payments.filter(payment => {
        if (filter === 'all') return true;
        return payment.status === filter;
    });

    const getStatusBadge = (status) => {
        const statusConfig = {
            COMPLETED: { class: 'success', label: 'Completed' },
            PENDING: { class: 'pending', label: 'Pending' },
            FAILED: { class: 'danger', label: 'Failed' },
            REFUNDED: { class: 'warning', label: 'Refunded' }
        };

        const config = statusConfig[status] || { class: 'pending', label: status };
        return <span className={`status-badge status-${config.class}`}>{config.label}</span>;
    };

    const getPaymentMethodIcon = (method) => {
        const icons = {
            CREDIT_CARD: '💳',
            DEBIT_CARD: '🏦',
            ONLINE_BANKING: '🌐',
            MOBILE_WALLET: '📱',
            ONLINE: '💻'
        };
        return icons[method] || '💳';
    };

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading payment history...</p>
            </div>
        );
    }

    return (
        <div className="payments-page">
            <div className="page-header">
                <div>
                    <h1>Payment History</h1>
                    <p>Track your payment transactions and receipts</p>
                </div>
                <div className="header-actions">
          <span className="payment-count">
            {payments.length} payment{payments.length !== 1 ? 's' : ''} total
          </span>
                </div>
            </div>

            {/* Summary Cards */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-title">Total Payments</div>
                    <div className="stat-value">{payments.length}</div>
                    <div className="stat-change">All time</div>
                </div>

                <div className="stat-card success">
                    <div className="stat-title">Total Paid</div>
                    <div className="stat-value">
                        ${payments.reduce((sum, payment) => sum + (payment.amount || 0), 0).toFixed(2)}
                    </div>
                    <div className="stat-change">Amount processed</div>
                </div>

                <div className="stat-card">
                    <div className="stat-title">Successful</div>
                    <div className="stat-value">
                        {payments.filter(p => p.status === 'COMPLETED').length}
                    </div>
                    <div className="stat-change">Completed payments</div>
                </div>

                <div className="stat-card">
                    <div className="stat-title">Avg. Payment</div>
                    <div className="stat-value">
                        ${payments.length > 0 ? (payments.reduce((sum, p) => sum + p.amount, 0) / payments.length).toFixed(2) : '0.00'}
                    </div>
                    <div className="stat-change">Per transaction</div>
                </div>
            </div>

            {/* Filters */}
            <div className="filters-section">
                <div className="filter-buttons">
                    <button
                        className={filter === 'all' ? 'active' : ''}
                        onClick={() => setFilter('all')}
                    >
                        All Payments
                    </button>
                    <button
                        className={filter === 'COMPLETED' ? 'active' : ''}
                        onClick={() => setFilter('COMPLETED')}
                    >
                        Completed
                    </button>
                    <button
                        className={filter === 'PENDING' ? 'active' : ''}
                        onClick={() => setFilter('PENDING')}
                    >
                        Pending
                    </button>
                </div>
            </div>

            {/* Payments Table */}
            <div className="card">
                <div className="card-header">
                    <h3>Transaction History</h3>
                </div>

                {filteredPayments.length > 0 ? (
                    <div className="table-container">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>Transaction ID</th>
                                <th>Invoice #</th>
                                <th>Date</th>
                                <th>Payment Method</th>
                                <th>Amount</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredPayments.map((payment) => (
                                <tr key={payment.id}>
                                    <td>
                                        <code>{payment.transactionId}</code>
                                    </td>
                                    <td>
                                        <strong>{payment.invoiceNumber}</strong>
                                    </td>
                                    <td>{payment.paymentDate}</td>
                                    <td>
                                        <div className="payment-method-display">
                        <span className="payment-icon">
                          {getPaymentMethodIcon(payment.paymentMethod)}
                        </span>
                                            {payment.paymentMethod.replace(/_/g, ' ')}
                                        </div>
                                    </td>
                                    <td>
                                        <strong>${(payment.amount || 0).toFixed(2)}</strong>
                                    </td>
                                    <td>{getStatusBadge(payment.status)}</td>
                                    <td>
                                        <div className="action-buttons">
                                            <button className="btn btn-sm btn-secondary">
                                                Receipt
                                            </button>
                                            {payment.status === 'COMPLETED' && (
                                                <button className="btn btn-sm btn-secondary">
                                                    Download
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="empty-state">
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" />
                        </svg>
                        <h3>No Payments Found</h3>
                        <p>
                            {filter === 'all'
                                ? "You haven't made any payments yet. Payments will appear here once you pay your invoices."
                                : `No ${filter.toLowerCase()} payments found.`
                            }
                        </p>
                    </div>
                )}
            </div>

            {/* Payment Statistics */}
            {payments.length > 0 && (
                <div className="dashboard-grid">
                    <div className="card">
                        <h3>Payment Methods</h3>
                        <div className="payment-method-stats">
                            {Object.entries(
                                payments.reduce((acc, payment) => {
                                    acc[payment.paymentMethod] = (acc[payment.paymentMethod] || 0) + 1;
                                    return acc;
                                }, {})
                            ).map(([method, count]) => (
                                <div key={method} className="method-stat">
                                    <div className="method-info">
                    <span className="method-icon">
                      {getPaymentMethodIcon(method)}
                    </span>
                                        <span>{method.replace(/_/g, ' ')}</span>
                                    </div>
                                    <div className="method-count">
                                        {count} payment{count !== 1 ? 's' : ''}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="card">
                        <h3>Recent Activity</h3>
                        <div className="recent-activity">
                            {payments.slice(0, 3).map((payment) => (
                                <div key={payment.id} className="activity-item">
                                    <div className="activity-icon success">✓</div>
                                    <div className="activity-content">
                                        <strong>Payment to {payment.invoiceNumber}</strong>
                                        <p>${(payment.amount || 0).toFixed(2)} • {payment.paymentDate}</p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Payments;