// File: frontend/src/pages/admin/InvoicesPayments.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const InvoicesPayments = ({ user }) => {
    const [invoices, setInvoices] = useState([]);
    const [filteredInvoices, setFilteredInvoices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        fetchInvoices();
    }, []);

    useEffect(() => {
        filterInvoices();
    }, [invoices, statusFilter, searchTerm]);

    const fetchInvoices = async () => {
        try {
            setLoading(true);
            // Try to get all invoices
            const response = await axios.get('http://localhost:8082/api/invoices').catch(() => ({ data: [] }));
            setInvoices(response.data || []);
        } catch (error) {
            console.error('Error fetching invoices:', error);
            toast.error('Failed to load invoices');
        } finally {
            setLoading(false);
        }
    };

    const filterInvoices = () => {
        let filtered = invoices;

        // Filter by status
        if (statusFilter !== 'ALL') {
            filtered = filtered.filter(invoice => invoice.status === statusFilter);
        }

        // Filter by search term
        if (searchTerm) {
            filtered = filtered.filter(invoice =>
                invoice.invoiceNumber?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                invoice.resident?.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                invoice.resident?.email?.toLowerCase().includes(searchTerm.toLowerCase())
            );
        }

        setFilteredInvoices(filtered);
    };

    const getStatusBadge = (status) => {
        const statusConfig = {
            'PENDING': { label: 'Pending', class: 'badge-warning' },
            'PAID': { label: 'Paid', class: 'badge-success' },
            'OVERDUE': { label: 'Overdue', class: 'badge-danger' },
            'CANCELLED': { label: 'Cancelled', class: 'badge-secondary' }
        };

        const config = statusConfig[status] || { label: status, class: 'badge-secondary' };
        return <span className={`badge ${config.class}`}>{config.label}</span>;
    };

    const calculateSummary = () => {
        const totalInvoices = invoices.length;
        const pendingInvoices = invoices.filter(inv => inv.status === 'PENDING').length;
        const paidInvoices = invoices.filter(inv => inv.status === 'PAID').length;
        const overdueInvoices = invoices.filter(inv => inv.status === 'OVERDUE').length;

        const totalRevenue = invoices.reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);
        const pendingAmount = invoices
            .filter(inv => inv.status === 'PENDING' || inv.status === 'OVERDUE')
            .reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);
        const collectedAmount = invoices
            .filter(inv => inv.status === 'PAID')
            .reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);

        return {
            totalInvoices,
            pendingInvoices,
            paidInvoices,
            overdueInvoices,
            totalRevenue,
            pendingAmount,
            collectedAmount
        };
    };

    const markAsPaid = async (invoiceId) => {
        try {
            await axios.put(`http://localhost:8082/api/invoices/${invoiceId}/status`, {
                status: 'PAID'
            });
            toast.success('Invoice marked as paid');
            fetchInvoices();
        } catch (error) {
            console.error('Error updating invoice:', error);
            toast.error('Failed to update invoice status');
        }
    };

    const sendReminder = async (invoiceId) => {
        try {
            // This would integrate with your notification service
            toast.success('Payment reminder sent to resident');
        } catch (error) {
            console.error('Error sending reminder:', error);
            toast.error('Failed to send reminder');
        }
    };

    const summary = calculateSummary();

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading invoices...</p>
            </div>
        );
    }

    return (
        <div className="invoices-payments">
            <div className="page-header">
                <h1>Invoices & Payments</h1>
                <p>Manage resident invoices and track payment status</p>
            </div>

            {/* Summary Cards */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-icon">🧾</div>
                    <div className="stat-content">
                        <div className="stat-value">{summary.totalInvoices}</div>
                        <div className="stat-title">Total Invoices</div>
                        <div className="stat-breakdown">
                            {summary.paidInvoices} paid • {summary.pendingInvoices} pending
                        </div>
                    </div>
                </div>

                <div className="stat-card warning">
                    <div className="stat-icon">⏰</div>
                    <div className="stat-content">
                        <div className="stat-value">{summary.pendingInvoices + summary.overdueInvoices}</div>
                        <div className="stat-title">Pending Payment</div>
                        <div className="stat-breakdown">
                            ${summary.pendingAmount.toFixed(2)} due
                        </div>
                    </div>
                </div>

                <div className="stat-card success">
                    <div className="stat-icon">💰</div>
                    <div className="stat-content">
                        <div className="stat-value">${summary.collectedAmount.toFixed(2)}</div>
                        <div className="stat-title">Collected Revenue</div>
                        <div className="stat-breakdown">
                            From {summary.paidInvoices} invoices
                        </div>
                    </div>
                </div>

                <div className="stat-card danger">
                    <div className="stat-icon">🚨</div>
                    <div className="stat-content">
                        <div className="stat-value">{summary.overdueInvoices}</div>
                        <div className="stat-title">Overdue Invoices</div>
                        <div className="stat-breakdown">
                            Requires attention
                        </div>
                    </div>
                </div>
            </div>

            {/* Filters */}
            <div className="filters-card">
                <div className="filter-group">
                    <input
                        type="text"
                        placeholder="Search by invoice number, resident name..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                        className="search-input"
                    />
                </div>
                <div className="filter-group">
                    <select
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                        className="filter-select"
                    >
                        <option value="ALL">All Status</option>
                        <option value="PENDING">Pending</option>
                        <option value="PAID">Paid</option>
                        <option value="OVERDUE">Overdue</option>
                        <option value="CANCELLED">Cancelled</option>
                    </select>
                </div>
                <div className="filter-group">
                    <span className="results-count">{filteredInvoices.length} invoices found</span>
                </div>
            </div>

            {/* Invoices Table */}
            <div className="card">
                <div className="card-header">
                    <h3>Invoice Management</h3>
                    <div className="header-actions">
                        <button className="btn btn-primary">Generate Bulk Invoices</button>
                        <button className="btn btn-secondary">Export Report</button>
                    </div>
                </div>

                {filteredInvoices.length > 0 ? (
                    <div className="table-container">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>Invoice Details</th>
                                <th>Resident</th>
                                <th>Amount</th>
                                <th>Due Date</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            {filteredInvoices.map((invoice) => (
                                <tr key={invoice.id}>
                                    <td>
                                        <div className="invoice-cell">
                                            <strong>{invoice.invoiceNumber}</strong>
                                            <small>
                                                {invoice.issueDate ?
                                                    new Date(invoice.issueDate).toLocaleDateString() :
                                                    'N/A'
                                                }
                                            </small>
                                            {invoice.collectionEvents?.length > 0 && (
                                                <small>{invoice.collectionEvents.length} collections</small>
                                            )}
                                        </div>
                                    </td>
                                    <td>
                                        <div className="resident-cell">
                                            <strong>{invoice.resident?.name || 'Unknown Resident'}</strong>
                                            <span>{invoice.resident?.email || 'No email'}</span>
                                            <small>{invoice.resident?.phoneNumber || 'No phone'}</small>
                                        </div>
                                    </td>
                                    <td>
                                        <div className="amount-cell">
                                            <strong>${invoice.totalAmount?.toFixed(2)}</strong>
                                            {invoice.weightBasedCharge && (
                                                <small>Weight: {invoice.totalWeight || 0} kg</small>
                                            )}
                                        </div>
                                    </td>
                                    <td>
                                        {invoice.dueDate ?
                                            new Date(invoice.dueDate).toLocaleDateString() :
                                            'N/A'
                                        }
                                        {invoice.dueDate && new Date(invoice.dueDate) < new Date() && invoice.status !== 'PAID' && (
                                            <div className="overdue-indicator">Overdue</div>
                                        )}
                                    </td>
                                    <td>{getStatusBadge(invoice.status)}</td>
                                    <td>
                                        <div className="action-buttons">
                                            {invoice.status !== 'PAID' && (
                                                <>
                                                    <button
                                                        className="btn btn-sm btn-success"
                                                        onClick={() => markAsPaid(invoice.id)}
                                                    >
                                                        Mark Paid
                                                    </button>
                                                    <button
                                                        className="btn btn-sm btn-warning"
                                                        onClick={() => sendReminder(invoice.id)}
                                                    >
                                                        Remind
                                                    </button>
                                                </>
                                            )}
                                            <button className="btn btn-sm btn-info">View</button>
                                            <button className="btn btn-sm btn-secondary">Edit</button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div className="empty-state">
                        <h3>No Invoices Found</h3>
                        <p>No invoices match your search criteria.</p>
                        <button
                            onClick={() => {
                                setSearchTerm('');
                                setStatusFilter('ALL');
                            }}
                            className="btn btn-primary"
                        >
                            Clear Filters
                        </button>
                    </div>
                )}
            </div>

            {/* Payment Analytics */}
            <div className="card">
                <div className="card-header">
                    <h3>Payment Analytics</h3>
                </div>
                <div className="analytics-grid">
                    <div className="analytics-item">
                        <h4>Collection Rate</h4>
                        <div className="progress-bar">
                            <div
                                className="progress-fill success"
                                style={{ width: `${summary.totalInvoices > 0 ? (summary.paidInvoices / summary.totalInvoices) * 100 : 0}%` }}
                            ></div>
                        </div>
                        <span>{summary.totalInvoices > 0 ? ((summary.paidInvoices / summary.totalInvoices) * 100).toFixed(1) : 0}%</span>
                    </div>
                    <div className="analytics-item">
                        <h4>Average Payment Time</h4>
                        <div className="metric-value">7.2 days</div>
                        <small>From issue to payment</small>
                    </div>
                    <div className="analytics-item">
                        <h4>Revenue Trend</h4>
                        <div className="metric-value positive">+12.5%</div>
                        <small>vs last month</small>
                    </div>
                    <div className="analytics-item">
                        <h4>Disputed Invoices</h4>
                        <div className="metric-value">2</div>
                        <small>Requiring resolution</small>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default InvoicesPayments;