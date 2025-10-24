import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const Invoices = ({ user }) => {
    const [invoices, setInvoices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedInvoice, setSelectedInvoice] = useState(null);
    const [showPaymentModal, setShowPaymentModal] = useState(false);
    const [processingPayment, setProcessingPayment] = useState(false);
    const [showDetailsModal, setShowDetailsModal] = useState(false);
    const [detailedInvoice, setDetailedInvoice] = useState(null);
    const [autoRefresh, setAutoRefresh] = useState(false);

    useEffect(() => {
        fetchInvoices();

        // Set up auto-refresh when component mounts
        const interval = setInterval(() => {
            if (autoRefresh) {
                fetchInvoices();
            }
        }, 3000); // Check every 3 seconds

        return () => clearInterval(interval);
    }, [user.id, autoRefresh]);

    // Start auto-refresh when payment is processing
    useEffect(() => {
        if (processingPayment) {
            setAutoRefresh(true);
            // Stop auto-refresh after 2 minutes
            const timeout = setTimeout(() => {
                setAutoRefresh(false);
            }, 120000);
            return () => clearTimeout(timeout);
        }
    }, [processingPayment]);

    // ADD THIS FUNCTION: Normalize invoice data from backend
    const normalizeInvoice = (invoice) => {
        console.log('🔄 Normalizing invoice:', invoice.invoiceNumber);

        return {
            ...invoice,
            // Map backend fields to frontend expected fields
            recyclingCredits: invoice.refundAmount || invoice.recyclingCredits || 0,
            weightBasedCharge: invoice.weightBasedCharge || 0,
            baseCharge: invoice.baseCharge || 0,
            totalAmount: invoice.finalAmount || invoice.totalAmount || 0,
            // Ensure all required fields have values
            otherCharges: invoice.otherCharges || 0
        };
    };

    const fetchInvoices = async () => {
        try {
            console.log('🔄 Fetching invoices for user ID:', user.id);
            const response = await axios.get(`http://localhost:8082/api/invoices/resident/${user.id}`);
            console.log('✅ Raw API response:', response.data);

            let invoicesData = [];

            if (Array.isArray(response.data)) {
                invoicesData = response.data;
            } else if (response.data && Array.isArray(response.data.invoices)) {
                invoicesData = response.data.invoices;
            } else {
                console.log('❌ Unexpected response format:', response.data);
                invoicesData = [];
            }

            // 🎯 NORMALIZE the invoice data
            const normalizedInvoices = invoicesData.map(normalizeInvoice);

            // Check if any invoice status changed to PAID
            const previousInvoices = invoices;
            if (previousInvoices.length > 0) {
                normalizedInvoices.forEach(newInvoice => {
                    const oldInvoice = previousInvoices.find(inv => inv.id === newInvoice.id);
                    if (oldInvoice && oldInvoice.status === 'PENDING' && newInvoice.status === 'PAID') {
                        toast.success(`Invoice ${newInvoice.invoiceNumber} has been paid! 🎉`);
                        setAutoRefresh(false); // Stop auto-refresh once we see a payment
                    }
                });
            }

            setInvoices(normalizedInvoices);

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

    const handleRefreshInvoices = async () => {
        setLoading(true);
        await fetchInvoices();
        toast.success('Invoices refreshed!');
    };

    const handleViewDetails = (invoice) => {
        setDetailedInvoice(invoice);
        setShowDetailsModal(true);
    };

    const handlePayInvoice = (invoice) => {
        setSelectedInvoice(invoice);
        setShowPaymentModal(true);
    };

    // NEW: PayHere Payment Integration
    const processPayment = async () => {
        if (!selectedInvoice) return;

        setProcessingPayment(true);
        setAutoRefresh(true); // Start auto-refresh when payment begins

        try {
            console.log('🔄 Initiating PayHere payment for invoice:', selectedInvoice.id);
            console.log('   - Invoice Number:', selectedInvoice.invoiceNumber);
            console.log('   - Amount: $', selectedInvoice.totalAmount);

            // CORRECTED ENDPOINT: Use /api/payhere/initiate
            const response = await axios.post(`http://localhost:8082/api/payhere/initiate/${selectedInvoice.id}`);
            const { checkoutUrl, paymentData } = response.data;

            console.log('✅ PayHere payment data received:');
            console.log('   - Checkout URL:', checkoutUrl);
            console.log('   - Payment Data:', paymentData);

            // DEBUG: Check if all required fields are present
            const requiredFields = ['merchant_id', 'return_url', 'cancel_url', 'order_id', 'items', 'currency', 'amount', 'hash'];
            const missingFields = requiredFields.filter(field => !paymentData[field]);

            if (missingFields.length > 0) {
                throw new Error(`Missing required fields: ${missingFields.join(', ')}`);
            }

            console.log('🔍 Payment Data Validation:');
            console.log('   - Merchant ID:', paymentData.merchant_id);
            console.log('   - Order ID:', paymentData.order_id);
            console.log('   - Amount:', paymentData.amount);
            console.log('   - Currency:', paymentData.currency);
            console.log('   - Hash present:', !!paymentData.hash);

            // Create hidden form for PayHere
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = checkoutUrl;
            form.style.display = 'none';

            // Add all payment parameters as hidden inputs
            Object.keys(paymentData).forEach(key => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = key;
                input.value = paymentData[key];
                form.appendChild(input);
            });

            // Add form to page and submit
            document.body.appendChild(form);
            console.log('🚀 Redirecting to PayHere...');

            // Show success message and instructions
            toast.success('Redirecting to PayHere. Page will auto-refresh when payment is complete.');

            form.submit();

        } catch (error) {
            console.error('❌ Payment initiation failed:', error);
            console.error('Error details:', error.response?.data);

            const errorMessage = error.response?.data?.error ||
                error.message ||
                'Payment initiation failed. Please try again.';

            toast.error(errorMessage);
            setProcessingPayment(false);
            setAutoRefresh(false);
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

    const calculateTotalRecyclingSavings = () => {
        return invoices.reduce((sum, inv) => sum + (inv.recyclingCredits || 0), 0);
    };

    // Check if any payments are being processed
    const hasPendingPayments = invoices.some(inv =>
        inv.status === 'PENDING' && autoRefresh
    );

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
                    {autoRefresh && (
                        <div className="auto-refresh-indicator">
                            <span className="refresh-dot"></span>
                            Auto-refreshing for payment updates...
                        </div>
                    )}
                </div>
                <div className="header-actions">
                    <button
                        onClick={handleGenerateInvoice}
                        className="btn btn-primary"
                        disabled={processingPayment}
                    >
                        Generate Monthly Invoice
                    </button>
                    <button
                        onClick={handleRefreshInvoices}
                        className="btn btn-secondary"
                        disabled={loading}
                    >
                        {loading ? 'Refreshing...' : 'Refresh Invoices'}
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

                <div className="stat-card success">
                    <div className="stat-title">Recycling Savings</div>
                    <div className="stat-value">${calculateTotalRecyclingSavings().toFixed(2)}</div>
                    <div className="stat-change">Total refunds earned</div>
                </div>
            </div>

            {/* Auto-refresh status */}
            {autoRefresh && (
                <div className="refresh-status">
                    <div className="refresh-spinner"></div>
                    <span>Waiting for payment confirmation... Auto-refreshing every 3 seconds</span>
                    <button
                        onClick={() => setAutoRefresh(false)}
                        className="btn btn-sm btn-outline"
                    >
                        Stop Auto-refresh
                    </button>
                </div>
            )}

            {/* Invoices Table */}
            <div className="card">
                <div className="card-header">
                    <h3>Invoice History</h3>
                    <div className="card-actions">
                        <span className="text-muted">
                            {invoices.filter(inv => inv.status === 'PAID').length} paid •
                            {invoices.filter(inv => inv.status === 'PENDING').length} pending
                        </span>
                    </div>
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
                                <tr key={invoice.id} className={invoice.status === 'PAID' ? 'row-paid' : ''}>
                                    <td>
                                        <strong>{invoice.invoiceNumber}</strong>
                                        {invoice.status === 'PAID' && (
                                            <span className="paid-badge">Paid</span>
                                        )}
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
                                                    disabled={processingPayment}
                                                >
                                                    {processingPayment ? 'Processing...' : 'Pay Now'}
                                                </button>
                                            ) : invoice.status === 'PAID' ? (
                                                <span className="text-success">✅ Paid</span>
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

            {/* Invoice Details Modal */}
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
                                        {detailedInvoice.paymentReference && (
                                            <div className="detail-item">
                                                <span>Transaction ID:</span>
                                                <span>{detailedInvoice.paymentReference}</span>
                                            </div>
                                        )}
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

            {/* Payment Modal */}
            {showPaymentModal && selectedInvoice && (
                <div className="modal-overlay">
                    <div className="modal">
                        <div className="modal-header">
                            <h3>Pay Invoice #{selectedInvoice.invoiceNumber}</h3>
                            <button
                                onClick={() => {
                                    setShowPaymentModal(false);
                                    setAutoRefresh(false);
                                }}
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

                            <div className="payment-info">
                                <div className="payment-info-card">
                                    <div className="payment-icon">🔒</div>
                                    <div className="payment-info-content">
                                        <h5>Secure Payment via PayHere</h5>
                                        <p>You will be redirected to PayHere's secure payment page to complete your transaction.</p>
                                        <ul className="payment-features">
                                            <li>✅ Secure SSL encryption</li>
                                            <li>✅ Multiple payment options</li>
                                            <li>✅ Instant confirmation</li>
                                            <li>✅ Auto-refresh after payment</li>
                                        </ul>
                                        {autoRefresh && (
                                            <div className="auto-refresh-note">
                                                <small>✅ Page will auto-refresh to show payment status</small>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>

                            <div className="modal-actions">
                                <button
                                    onClick={processPayment}
                                    disabled={processingPayment}
                                    className="btn btn-primary btn-block"
                                >
                                    {processingPayment ? 'Redirecting to PayHere...' : `Pay $${(selectedInvoice.totalAmount || 0).toFixed(2)}`}
                                </button>
                                <p className="payment-note">
                                    By clicking "Pay", you will be redirected to PayHere to complete your payment securely.
                                    The page will automatically update when your payment is confirmed.
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Invoices;