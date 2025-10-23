import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const Invoices = ({ user }) => {
    const [invoices, setInvoices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedInvoice, setSelectedInvoice] = useState(null);
    const [showPaymentModal, setShowPaymentModal] = useState(false);
    const [paymentMethod, setPaymentMethod] = useState('CREDIT_CARD');
    const [processingPayment, setProcessingPayment] = useState(false);
    // NEW: State for invoice details modal
    const [showDetailsModal, setShowDetailsModal] = useState(false);
    const [detailedInvoice, setDetailedInvoice] = useState(null);

    useEffect(() => {
        fetchInvoices();
    }, [user.id]);

    const fetchInvoices = async () => {
        try {
            console.log('🔄 Fetching invoices for user ID:', user.id);
            const response = await axios.get(`http://localhost:8082/api/invoices/resident/${user.id}`);
            console.log('✅ Raw API response:', response.data);

            if (Array.isArray(response.data)) {
                console.log(`📋 Found ${response.data.length} invoices`);
                setInvoices(response.data);
            } else if (response.data && Array.isArray(response.data.invoices)) {
                console.log(`📋 Found ${response.data.invoices.length} invoices in nested property`);
                setInvoices(response.data.invoices);
            } else {
                console.log('❌ Unexpected response format:', response.data);
                setInvoices([]);
            }
        } catch (error) {
            console.error('❌ Error fetching invoices:', error);
            console.error('Error response:', error.response?.data);
            toast.error('Failed to load invoices');
        } finally {
            setLoading(false);
        }
    };

    const handleGenerateInvoice = async () => {
        try {
            await axios.post(`http://localhost:8082/api/invoices/generate/${user.id}`);
            toast.success('Monthly invoice generated successfully!');
            fetchInvoices();
        } catch (error) {
            toast.error(error.response?.data?.error || 'Failed to generate invoice');
        }
    };

    // NEW: View invoice details
    const handleViewDetails = (invoice) => {
        setDetailedInvoice(invoice);
        setShowDetailsModal(true);
    };

    const handlePayInvoice = (invoice) => {
        setSelectedInvoice(invoice);
        setShowPaymentModal(true);
    };

    const processPayment = async () => {
        if (!selectedInvoice) return;

        setProcessingPayment(true);
        try {
            const paymentData = {
                paymentMethod: paymentMethod,
                transactionId: `TXN-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
            };

            await axios.post(`http://localhost:8082/api/invoices/${selectedInvoice.id}/pay`, paymentData);
            toast.success('Payment processed successfully!');
            setShowPaymentModal(false);
            setSelectedInvoice(null);
            fetchInvoices();
        } catch (error) {
            toast.error(error.response?.data?.error || 'Payment failed. Please try again.');
        } finally {
            setProcessingPayment(false);
        }
    };

    const getStatusBadge = (status) => {
        const statusConfig = {
            PENDING: { class: 'pending', label: 'Pending' },
            PAID: { class: 'paid', label: 'Paid' },
            OVERDUE: { class: 'overdue', label: 'Overdue' },
            PARTIALLY_PAID: { class: 'warning', label: 'Partial' },
            CANCELLED: { class: 'inactive', label: 'Cancelled' }
        };

        const config = statusConfig[status] || { class: 'pending', label: status };
        return <span className={`status-badge status-${config.class}`}>{config.label}</span>;
    };

    const calculateTotalPending = () => {
        return invoices
            .filter(inv => inv.status === 'PENDING')
            .reduce((sum, inv) => sum + (inv.totalAmount || 0), 0);
    };

    // NEW: Calculate total recycling savings
    const calculateTotalRecyclingSavings = () => {
        return invoices.reduce((sum, inv) => sum + (inv.recyclingCredits || 0), 0);
    };

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading your invoices...</p>
            </div>
        );
    }

    return (
        <div className="invoices-page">
            <div className="page-header">
                <div>
                    <h1>Billing & Invoices</h1>
                    <p>Manage your waste management bills and payments</p>
                </div>
                <div className="header-actions">
                    <button
                        onClick={handleGenerateInvoice}
                        className="btn btn-primary"
                    >
                        Generate Monthly Invoice
                    </button>
                </div>
            </div>

            {/* Summary Cards */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-title">Total Invoices</div>
                    <div className="stat-value">{invoices.length}</div>
                    <div className="stat-change">All time</div>
                </div>

                <div className="stat-card warning">
                    <div className="stat-title">Pending Invoices</div>
                    <div className="stat-value">
                        {invoices.filter(inv => inv.status === 'PENDING').length}
                    </div>
                    <div className="stat-change">Awaiting payment</div>
                </div>

                <div className="stat-card danger">
                    <div className="stat-title">Total Due</div>
                    <div className="stat-value">${calculateTotalPending().toFixed(2)}</div>
                    <div className="stat-change">Outstanding balance</div>
                </div>

                {/* NEW: Recycling Savings Card */}
                <div className="stat-card success">
                    <div className="stat-title">Recycling Savings</div>
                    <div className="stat-value">${calculateTotalRecyclingSavings().toFixed(2)}</div>
                    <div className="stat-change">Total refunds earned</div>
                </div>
            </div>

            {/* Invoices Table */}
            <div className="card">
                <div className="card-header">
                    <h3>Invoice History</h3>
                </div>

                {invoices.length > 0 ? (
                    <div className="table-container">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>Invoice #</th>
                                <th>Issue Date</th>
                                <th>Due Date</th>
                                <th>Billing Period</th>
                                <th>Base Charge</th>
                                <th>Weight Charge</th>
                                <th>Recycling Credits</th>
                                <th>Total Amount</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                            </thead>
                            <tbody>
                            {invoices.map((invoice) => (
                                <tr key={invoice.id}>
                                    <td>
                                        <strong>{invoice.invoiceNumber}</strong>
                                    </td>
                                    <td>{invoice.invoiceDate}</td>
                                    <td>
                      <span className={new Date(invoice.dueDate) < new Date() && invoice.status === 'PENDING' ? 'text-danger' : ''}>
                        {invoice.dueDate}
                      </span>
                                    </td>
                                    <td>
                                        {invoice.periodStart} to {invoice.periodEnd}
                                    </td>
                                    <td>${(invoice.baseCharge || 0).toFixed(2)}</td>
                                    <td>${(invoice.weightBasedCharge || 0).toFixed(2)}</td>
                                    <td className="text-success">
                                        {/* NEW: Enhanced recycling credits display */}
                                        <div className="recycling-credits-cell">
                                            <strong>-${(invoice.recyclingCredits || 0).toFixed(2)}</strong>
                                            {invoice.recyclingCredits > 0 && (
                                                <button
                                                    className="btn btn-sm btn-link"
                                                    onClick={() => handleViewDetails(invoice)}
                                                    title="View recycling details"
                                                >
                                                    📋
                                                </button>
                                            )}
                                        </div>
                                    </td>
                                    <td>
                                        <strong>${(invoice.totalAmount || 0).toFixed(2)}</strong>
                                    </td>
                                    <td>{getStatusBadge(invoice.status)}</td>
                                    <td>
                                        <div className="action-buttons">
                                            {invoice.status === 'PENDING' && invoice.totalAmount > 0 ? (
                                                <button
                                                    onClick={() => handlePayInvoice(invoice)}
                                                    className="btn btn-sm btn-primary"
                                                >
                                                    Pay Now
                                                </button>
                                            ) : invoice.status === 'PAID' ? (
                                                <span className="text-success">Paid</span>
                                            ) : (
                                                <span className="text-muted">No action</span>
                                            )}
                                            <button
                                                onClick={() => handleViewDetails(invoice)}
                                                className="btn btn-sm btn-secondary"
                                            >
                                                Details
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
                        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        <h3>No Invoices Found</h3>
                        <p>You don't have any invoices yet. Generate your first monthly invoice to get started.</p>
                    </div>
                )}
            </div>

            {/* NEW: Invoice Details Modal */}
            {showDetailsModal && detailedInvoice && (
                <div className="modal-overlay">
                    <div className="modal modal-lg">
                        <div className="modal-header">
                            <h3>Invoice Details - {detailedInvoice.invoiceNumber}</h3>
                            <button
                                onClick={() => setShowDetailsModal(false)}
                                className="btn btn-sm btn-secondary"
                            >
                                Close
                            </button>
                        </div>

                        <div className="modal-body">
                            <div className="invoice-details-grid">
                                <div className="detail-section">
                                    <h4>Basic Information</h4>
                                    <div className="detail-item">
                                        <span>Invoice Number:</span>
                                        <span>{detailedInvoice.invoiceNumber}</span>
                                    </div>
                                    <div className="detail-item">
                                        <span>Issue Date:</span>
                                        <span>{detailedInvoice.invoiceDate}</span>
                                    </div>
                                    <div className="detail-item">
                                        <span>Due Date:</span>
                                        <span>{detailedInvoice.dueDate}</span>
                                    </div>
                                    <div className="detail-item">
                                        <span>Billing Period:</span>
                                        <span>{detailedInvoice.periodStart} to {detailedInvoice.periodEnd}</span>
                                    </div>
                                    <div className="detail-item">
                                        <span>Status:</span>
                                        <span>{getStatusBadge(detailedInvoice.status)}</span>
                                    </div>
                                </div>

                                <div className="detail-section">
                                    <h4>Charges Breakdown</h4>
                                    <div className="charge-breakdown">
                                        <div className="charge-item">
                                            <span>Base Service Charge:</span>
                                            <span>${(detailedInvoice.baseCharge || 0).toFixed(2)}</span>
                                        </div>
                                        <div className="charge-item">
                                            <span>Weight-Based Charge:</span>
                                            <span>${(detailedInvoice.weightBasedCharge || 0).toFixed(2)}</span>
                                        </div>
                                        <div className="charge-item">
                                            <span>Other Charges:</span>
                                            <span>${(detailedInvoice.otherCharges || 0).toFixed(2)}</span>
                                        </div>

                                        {/* NEW: Enhanced Recycling Credits Display */}
                                        <div className="charge-item text-success">
                                            <span>
                                                <strong>Recycling Credits:</strong>
                                                {detailedInvoice.recyclingCredits > 0 && (
                                                    <small className="ml-1">(Thank you for recycling! ♻️)</small>
                                                )}
                                            </span>
                                            <span><strong>-${(detailedInvoice.recyclingCredits || 0).toFixed(2)}</strong></span>
                                        </div>

                                        <div className="charge-item total">
                                            <span><strong>Total Amount:</strong></span>
                                            <span><strong>${(detailedInvoice.totalAmount || 0).toFixed(2)}</strong></span>
                                        </div>
                                    </div>
                                </div>

                                {/* NEW: Recycling Impact Section */}
                                {detailedInvoice.recyclingCredits > 0 && (
                                    <div className="detail-section">
                                        <h4>♻️ Your Recycling Impact</h4>
                                        <div className="recycling-impact">
                                            <div className="impact-item">
                                                <div className="impact-icon">💰</div>
                                                <div className="impact-content">
                                                    <strong>Money Saved</strong>
                                                    <p>You saved ${(detailedInvoice.recyclingCredits || 0).toFixed(2)} through recycling</p>
                                                </div>
                                            </div>
                                            <div className="impact-item">
                                                <div className="impact-icon">🌱</div>
                                                <div className="impact-content">
                                                    <strong>Environmental Impact</strong>
                                                    <p>Your recycling helps reduce landfill waste and conserve resources</p>
                                                </div>
                                            </div>
                                            <div className="impact-item">
                                                <div className="impact-icon">📊</div>
                                                <div className="impact-content">
                                                    <strong>Community Contribution</strong>
                                                    <p>You're supporting sustainable waste management practices</p>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                )}

                                {/* Payment Information */}
                                {detailedInvoice.status === 'PAID' && (
                                    <div className="detail-section">
                                        <h4>Payment Information</h4>
                                        <div className="detail-item">
                                            <span>Payment Date:</span>
                                            <span>{detailedInvoice.paymentDate || 'N/A'}</span>
                                        </div>
                                        <div className="detail-item">
                                            <span>Payment Method:</span>
                                            <span>{detailedInvoice.paymentMethod || 'N/A'}</span>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="modal-actions">
                            {detailedInvoice.status === 'PENDING' && (
                                <button
                                    onClick={() => {
                                        setShowDetailsModal(false);
                                        handlePayInvoice(detailedInvoice);
                                    }}
                                    className="btn btn-primary"
                                >
                                    Pay Invoice
                                </button>
                            )}
                            <button
                                onClick={() => setShowDetailsModal(false)}
                                className="btn btn-secondary"
                            >
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Payment Modal (Existing - No changes needed) */}
            {showPaymentModal && selectedInvoice && (
                <div className="modal-overlay">
                    <div className="modal">
                        <div className="modal-header">
                            <h3>Pay Invoice #{selectedInvoice.invoiceNumber}</h3>
                            <button
                                onClick={() => setShowPaymentModal(false)}
                                className="btn btn-sm btn-secondary"
                                disabled={processingPayment}
                            >
                                Cancel
                            </button>
                        </div>

                        <div className="modal-body">
                            <div className="payment-summary">
                                <h4>Payment Summary</h4>
                                <div className="payment-details">
                                    <div className="payment-item">
                                        <span>Invoice Amount:</span>
                                        <span>${(selectedInvoice.totalAmount || 0).toFixed(2)}</span>
                                    </div>
                                    {selectedInvoice.recyclingCredits > 0 && (
                                        <div className="payment-item text-success">
                                            <span>Recycling Credits Applied:</span>
                                            <span>-${(selectedInvoice.recyclingCredits || 0).toFixed(2)}</span>
                                        </div>
                                    )}
                                    <div className="payment-item">
                                        <span>Due Date:</span>
                                        <span>{selectedInvoice.dueDate}</span>
                                    </div>
                                    <div className="payment-item total">
                                        <span>Total to Pay:</span>
                                        <span>${(selectedInvoice.totalAmount || 0).toFixed(2)}</span>
                                    </div>
                                </div>
                            </div>

                            <div className="payment-method">
                                <h4>Select Payment Method</h4>
                                <div className="payment-options">
                                    <label className="payment-option">
                                        <input
                                            type="radio"
                                            name="paymentMethod"
                                            value="CREDIT_CARD"
                                            checked={paymentMethod === 'CREDIT_CARD'}
                                            onChange={(e) => setPaymentMethod(e.target.value)}
                                        />
                                        <div className="payment-option-content">
                                            <div className="payment-icon">💳</div>
                                            <div>
                                                <strong>Credit Card</strong>
                                                <p>Visa, MasterCard, American Express</p>
                                            </div>
                                        </div>
                                    </label>

                                    <label className="payment-option">
                                        <input
                                            type="radio"
                                            name="paymentMethod"
                                            value="DEBIT_CARD"
                                            checked={paymentMethod === 'DEBIT_CARD'}
                                            onChange={(e) => setPaymentMethod(e.target.value)}
                                        />
                                        <div className="payment-option-content">
                                            <div className="payment-icon">🏦</div>
                                            <div>
                                                <strong>Debit Card</strong>
                                                <p>Direct bank payment</p>
                                            </div>
                                        </div>
                                    </label>

                                    <label className="payment-option">
                                        <input
                                            type="radio"
                                            name="paymentMethod"
                                            value="ONLINE_BANKING"
                                            checked={paymentMethod === 'ONLINE_BANKING'}
                                            onChange={(e) => setPaymentMethod(e.target.value)}
                                        />
                                        <div className="payment-option-content">
                                            <div className="payment-icon">🌐</div>
                                            <div>
                                                <strong>Online Banking</strong>
                                                <p>Internet banking transfer</p>
                                            </div>
                                        </div>
                                    </label>

                                    <label className="payment-option">
                                        <input
                                            type="radio"
                                            name="paymentMethod"
                                            value="MOBILE_WALLET"
                                            checked={paymentMethod === 'MOBILE_WALLET'}
                                            onChange={(e) => setPaymentMethod(e.target.value)}
                                        />
                                        <div className="payment-option-content">
                                            <div className="payment-icon">📱</div>
                                            <div>
                                                <strong>Mobile Wallet</strong>
                                                <p>Pay via mobile payment apps</p>
                                            </div>
                                        </div>
                                    </label>
                                </div>
                            </div>

                            <div className="modal-actions">
                                <button
                                    onClick={processPayment}
                                    disabled={processingPayment}
                                    className="btn btn-primary btn-block"
                                >
                                    {processingPayment ? 'Processing Payment...' : `Pay $${(selectedInvoice.totalAmount || 0).toFixed(2)}`}
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Invoices;