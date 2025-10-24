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
            // Use the correct admin endpoint
            const response = await axios.get('http://localhost:8082/api/invoices/admin/all');
            console.log('📊 Admin invoices data:', response.data);

            if (Array.isArray(response.data)) {
                setInvoices(response.data);
            } else {
                console.error('Unexpected response format:', response.data);
                setInvoices([]);
            }
        } catch (error) {
            console.error('Error fetching invoices:', error);
            // Fallback: try to get invoices from resident endpoint and filter
            try {
                const fallbackResponse = await axios.get('http://localhost:8082/api/invoices');
                if (Array.isArray(fallbackResponse.data)) {
                    setInvoices(fallbackResponse.data);
                } else {
                    setInvoices([]);
                }
            } catch (fallbackError) {
                console.error('Fallback also failed:', fallbackError);
                toast.error('Failed to load invoices');
                setInvoices([]);
            }
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
                invoice.resident?.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                invoice.resident?.phone?.toLowerCase().includes(searchTerm.toLowerCase())
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

        const totalRevenue = invoices
            .filter(inv => inv.status === 'PAID')
            .reduce((sum, inv) => sum + (inv.finalAmount || inv.totalAmount || 0), 0);

        const pendingAmount = invoices
            .filter(inv => inv.status === 'PENDING' || inv.status === 'OVERDUE')
            .reduce((sum, inv) => sum + (inv.finalAmount || inv.totalAmount || 0), 0);

        const collectedAmount = totalRevenue;

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
            await axios.put(`http://localhost:8082/api/invoices/admin/${invoiceId}/status`, {
                status: 'PAID'
            });
            toast.success('Invoice marked as paid');
            fetchInvoices(); // Refresh the list
        } catch (error) {
            console.error('Error updating invoice:', error);
            toast.error('Failed to update invoice status');
        }
    };

    const sendReminder = async (invoiceId) => {
        try {
            // Implement your notification service integration here
            console.log('Sending reminder for invoice:', invoiceId);
            toast.success('Payment reminder sent to resident');
        } catch (error) {
            console.error('Error sending reminder:', error);
            toast.error('Failed to send reminder');
        }
    };

    const formatCurrency = (amount) => {
        return new Intl.NumberFormat('en-US', {
            style: 'currency',
            currency: 'USD'
        }).format(amount || 0);
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'N/A';
        return new Date(dateString).toLocaleDateString();
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
                <button
                    onClick={fetchInvoices}
                    className="btn btn-secondary"
                    disabled={loading}
                >
                    {loading ? 'Refreshing...' : 'Refresh Data'}
                </button>
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
                            {formatCurrency(summary.pendingAmount)} due
                        </div>
                    </div>
                </div>

                <div className="stat-card success">
                    <div className="stat-icon">💰</div>
                    <div className="stat-content">
                        <div className="stat-value">{formatCurrency(summary.collectedAmount)}</div>
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
                        placeholder="Search by invoice number, resident name, email..."
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
                    </select>
                </div>
                <div className="filter-group">
                    <span className="results-count">
                        {filteredInvoices.length} invoices found
                    </span>
                </div>
            </div>

            {/* Debug Info - Remove in production */}
            {process.env.NODE_ENV === 'development' && (
                <div className="debug-info">
                    <details>
                        <summary>Debug Data ({invoices.length} invoices)</summary>
                        <pre>{JSON.stringify(invoices.slice(0, 2), null, 2)}</pre>
                    </details>
                </div>
            )}

            {/* Invoices Table */}
            <div className="card">
                <div className="card-header">
                    <h3>Invoice Management</h3>
                    <div className="header-actions">
                        <button className="btn btn-primary" onClick={fetchInvoices}>
                            Refresh All
                        </button>
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
                                                Issued: {formatDate(invoice.invoiceDate)}
                                            </small>
                                            <small>
                                                Period: {formatDate(invoice.periodStart)} - {formatDate(invoice.periodEnd)}
                                            </small>
                                        </div>
                                    </td>
                                    <td>
                                        <div className="resident-cell">
                                            <strong>{invoice.resident?.name || 'Unknown Resident'}</strong>
                                            <span>{invoice.resident?.email || 'No email'}</span>
                                            <small>{invoice.resident?.phone || 'No phone'}</small>
                                        </div>
                                    </td>
                                    <td>
                                        <div className="amount-cell">
                                            <strong>{formatCurrency(invoice.finalAmount || invoice.totalAmount)}</strong>
                                            {invoice.recyclingCredits > 0 && (
                                                <small className="text-success">
                                                    -{formatCurrency(invoice.recyclingCredits)} credits
                                                </small>
                                            )}
                                        </div>
                                    </td>
                                    <td>
                                        <div className={`due-date-cell ${
                                            new Date(invoice.dueDate) < new Date() &&
                                            invoice.status !== 'PAID' ? 'overdue' : ''
                                        }`}>
                                            {formatDate(invoice.dueDate)}
                                            {new Date(invoice.dueDate) < new Date() && invoice.status !== 'PAID' && (
                                                <div className="overdue-indicator">Overdue</div>
                                            )}
                                        </div>
                                    </td>
                                    <td>{getStatusBadge(invoice.status)}</td>
                                    <td>
                                        <div className="action-buttons">
                                            {invoice.status !== 'PAID' && (
                                                <>
                                                    <button
                                                        className="btn btn-sm btn-success"
                                                        onClick={() => markAsPaid(invoice.id)}
                                                        title="Mark as paid"
                                                    >
                                                        ✓ Paid
                                                    </button>
                                                    <button
                                                        className="btn btn-sm btn-warning"
                                                        onClick={() => sendReminder(invoice.id)}
                                                        title="Send payment reminder"
                                                    >
                                                        ⏰ Remind
                                                    </button>
                                                </>
                                            )}
                                            <button
                                                className="btn btn-sm btn-info"
                                                onClick={() => {
                                                    // View invoice details
                                                    console.log('Invoice details:', invoice);
                                                    toast.info(`Viewing ${invoice.invoiceNumber}`);
                                                }}
                                            >
                                                👁️ View
                                            </button>
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
        </div>
    );
};

export default InvoicesPayments;