// File: frontend/src/pages/Payments.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const Payments = ({ user }) => {
    const [payments, setPayments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('all');

    useEffect(() => {
        fetchPayments();
    }, [user.id]);

    const fetchPayments = async () => {
        try {
            console.log('🔄 Fetching payment history for user:', user.id);

            // First, get all invoices for the user
            const invoicesResponse = await axios.get(`http://localhost:8082/api/invoices/resident/${user.id}`);
            const invoices = invoicesResponse.data;

            console.log('✅ Invoices received:', invoices);

            // Filter only PAID invoices and extract real payment data
            const paidInvoices = invoices.filter(inv =>
                inv.status === 'PAID' || inv.status === 'PAID'
            );

            console.log('💰 Paid invoices found:', paidInvoices);

            // Transform paid invoices into proper payment records using REAL data
            const paymentRecords = paidInvoices.map(invoice => ({
                id: invoice.id,
                invoiceNumber: invoice.invoiceNumber,
                amount: invoice.finalAmount || invoice.totalAmount,
                paymentDate: invoice.paymentDate || invoice.invoiceDate, // Use actual payment date if available
                paymentMethod: invoice.paymentMethod || 'ONLINE',
                status: 'COMPLETED',
                transactionId: invoice.paymentReference || `PAY-${invoice.invoiceNumber}`,
                // Include additional real data
                periodStart: invoice.periodStart,
                periodEnd: invoice.periodEnd,
                baseCharge: invoice.baseCharge,
                weightBasedCharge: invoice.weightBasedCharge,
                recyclingCredits: invoice.recyclingCredits || invoice.refundAmount
            }));

            console.log('📊 Payment records created:', paymentRecords);
            setPayments(paymentRecords);

        } catch (error) {
            console.error('❌ Error fetching payments:', error);
            console.error('Error details:', error.response?.data);
            toast.error('Failed to load payment history');
        } finally {
            setLoading(false);
        }
    };

    const fetchAllPayments = async () => {
        try {
            // If you have a dedicated payments endpoint, use this instead:
            // const response = await axios.get(`http://localhost:8082/api/payments/user/${user.id}`);
            // setPayments(response.data);

            // For now, we'll use the invoices-based approach
            await fetchPayments();
            toast.success('Payments refreshed!');
        } catch (error) {
            console.error('Error refreshing payments:', error);
            toast.error('Failed to refresh payments');
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
            ONLINE: '💻',
            PAYHERE: '🔗',
            CASH: '💵',
            BANK_TRANSFER: '🏦'
        };
        return icons[method] || '💳';
    };

    const formatDate = (dateString) => {
        if (!dateString) return 'N/A';
        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
            });
        } catch (error) {
            return dateString;
        }
    };


    // Alternative PDF generation using html2canvas and jsPDF (more advanced)
    const handleDownloadReceiptAdvanced = async (payment) => {
        try {
            const { jsPDF } = await import('jspdf');
            const html2canvas = await import('html2canvas');

            // Create a temporary div with receipt content
            const receiptDiv = document.createElement('div');
            receiptDiv.style.position = 'absolute';
            receiptDiv.style.left = '-9999px';
            receiptDiv.style.top = '0';
            receiptDiv.style.width = '210mm';
            receiptDiv.style.padding = '20mm';
            receiptDiv.style.fontFamily = 'Arial, sans-serif';
            receiptDiv.style.backgroundColor = 'white';
            receiptDiv.innerHTML = `
                <div style="text-align: center; margin-bottom: 30px;">
                    <h1 style="color: #219653; margin: 0; font-size: 24px;">WASTE MANAGEMENT SERVICE</h1>
                    <h2 style="color: #666; margin: 5px 0; font-size: 16px;">PAYMENT RECEIPT</h2>
                </div>
                
                <div style="border-bottom: 1px solid #ddd; padding-bottom: 15px; margin-bottom: 20px;">
                    <h3 style="margin: 0 0 10px 0; font-size: 14px;">RECEIPT DETAILS</h3>
                    <table style="width: 100%; font-size: 12px;">
                        <tr>
                            <td style="font-weight: bold; padding: 4px 0;">Invoice Number:</td>
                            <td>${payment.invoiceNumber}</td>
                        </tr>
                        <tr>
                            <td style="font-weight: bold; padding: 4px 0;">Transaction ID:</td>
                            <td>${payment.transactionId}</td>
                        </tr>
                        <tr>
                            <td style="font-weight: bold; padding: 4px 0;">Payment Date:</td>
                            <td>${formatDate(payment.paymentDate)}</td>
                        </tr>
                        <tr>
                            <td style="font-weight: bold; padding: 4px 0;">Payment Method:</td>
                            <td>${payment.paymentMethod.replace(/_/g, ' ')}</td>
                        </tr>
                        <tr>
                            <td style="font-weight: bold; padding: 4px 0;">Amount Paid:</td>
                            <td style="font-weight: bold; color: #219653;">$${payment.amount.toFixed(2)}</td>
                        </tr>
                    </table>
                </div>
                
                <div style="margin-bottom: 20px;">
                    <h3 style="margin: 0 0 10px 0; font-size: 14px;">BILLING PERIOD</h3>
                    <p style="margin: 0; font-size: 12px; color: #666;">${payment.periodStart} to ${payment.periodEnd}</p>
                </div>
                
                <div style="margin-bottom: 30px;">
                    <h3 style="margin: 0 0 10px 0; font-size: 14px;">CHARGES BREAKDOWN</h3>
                    <table style="width: 100%; font-size: 12px; border-collapse: collapse;">
                        <tr>
                            <td style="padding: 8px 0;">Base Service Charge:</td>
                            <td style="text-align: right; padding: 8px 0;">$${(payment.baseCharge || 0).toFixed(2)}</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px 0;">Weight-Based Charge:</td>
                            <td style="text-align: right; padding: 8px 0;">$${(payment.weightBasedCharge || 0).toFixed(2)}</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px 0; color: #219653;">Recycling Credits:</td>
                            <td style="text-align: right; padding: 8px 0; color: #219653;">-$${(payment.recyclingCredits || 0).toFixed(2)}</td>
                        </tr>
                        <tr style="border-top: 1px solid #ddd;">
                            <td style="padding: 12px 0; font-weight: bold;">TOTAL AMOUNT:</td>
                            <td style="text-align: right; padding: 12px 0; font-weight: bold;">$${payment.amount.toFixed(2)}</td>
                        </tr>
                    </table>
                </div>
                
                <div style="text-align: center; color: #999; font-size: 10px; margin-top: 40px;">
                    <p style="margin: 5px 0;">Thank you for choosing our waste management services!</p>
                    <p style="margin: 5px 0;">This receipt is proof of your payment.</p>
                    <p style="margin: 5px 0;">Generated on ${new Date().toLocaleDateString()} • Receipt ID: ${payment.transactionId}</p>
                </div>
            `;

            document.body.appendChild(receiptDiv);

            // Convert to canvas then to PDF
            const canvas = await html2canvas.default(receiptDiv);
            const imgData = canvas.toDataURL('image/png');

            const pdf = new jsPDF('p', 'mm', 'a4');
            const imgWidth = 210; // A4 width in mm
            const pageHeight = 295; // A4 height in mm
            const imgHeight = (canvas.height * imgWidth) / canvas.width;
            let heightLeft = imgHeight;

            let position = 0;

            pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
            heightLeft -= pageHeight;

            while (heightLeft >= 0) {
                position = heightLeft - imgHeight;
                pdf.addPage();
                pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
                heightLeft -= pageHeight;
            }

            pdf.save(`receipt-${payment.invoiceNumber}.pdf`);

            // Clean up
            document.body.removeChild(receiptDiv);

            toast.success('PDF receipt downloaded!');

        } catch (error) {
            console.error('Error generating advanced PDF:', error);
            // Fallback to basic PDF
            handleDownloadReceipt(payment);
        }
    };
    const handleViewDetails = (payment) => {
        // Show payment details in a modal or alert
        const details = `
Invoice: ${payment.invoiceNumber}
Amount: $${payment.amount.toFixed(2)}
Date: ${formatDate(payment.paymentDate)}
Method: ${payment.paymentMethod}
Transaction: ${payment.transactionId}
        `;
        alert(details);
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
                    <button
                        onClick={fetchAllPayments}
                        className="btn btn-secondary"
                        disabled={loading}
                    >
                        {loading ? 'Refreshing...' : 'Refresh Payments'}
                    </button>
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
                    <div className="stat-title">This Month</div>
                    <div className="stat-value">
                        {payments.filter(p => {
                            const paymentDate = new Date(p.paymentDate);
                            const now = new Date();
                            return paymentDate.getMonth() === now.getMonth() &&
                                paymentDate.getFullYear() === now.getFullYear();
                        }).length}
                    </div>
                    <div className="stat-change">Recent payments</div>
                </div>
            </div>

            {/* Filters */}
            <div className="filters-section">
                <div className="filter-buttons">
                    <button
                        className={filter === 'all' ? 'active' : ''}
                        onClick={() => setFilter('all')}
                    >
                        All Payments ({payments.length})
                    </button>
                    <button
                        className={filter === 'COMPLETED' ? 'active' : ''}
                        onClick={() => setFilter('COMPLETED')}
                    >
                        Completed ({payments.filter(p => p.status === 'COMPLETED').length})
                    </button>
                </div>
            </div>

            {/* Payments Table */}
            <div className="card">
                <div className="card-header">
                    <h3>Transaction History</h3>
                    <div className="card-actions">
                        <span className="text-muted">
                            Showing {filteredPayments.length} of {payments.length} payments
                        </span>
                    </div>
                </div>

                {filteredPayments.length > 0 ? (
                    <div className="table-container">
                        <table className="data-table">
                            <thead>
                            <tr>
                                <th>Invoice #</th>
                                <th>Transaction ID</th>
                                <th>Payment Date</th>
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
                                        <strong>{payment.invoiceNumber}</strong>
                                    </td>
                                    <td>
                                        <code className="transaction-id">{payment.transactionId}</code>
                                    </td>
                                    <td>{formatDate(payment.paymentDate)}</td>
                                    <td>
                                        <div className="payment-method-display">
                                                <span className="payment-icon">
                                                    {getPaymentMethodIcon(payment.paymentMethod)}
                                                </span>
                                            {payment.paymentMethod.replace(/_/g, ' ')}
                                        </div>
                                    </td>
                                    <td>
                                        <strong className="amount-paid">
                                            ${(payment.amount || 0).toFixed(2)}
                                        </strong>
                                    </td>
                                    <td>{getStatusBadge(payment.status)}</td>
                                    <td>
                                        <div className="action-buttons">
                                            <button
                                                className="btn btn-sm btn-secondary"
                                                onClick={() => handleViewDetails(payment)}
                                            >
                                                Details
                                            </button>
                                            <button
                                                className="btn btn-sm btn-primary"
                                                onClick={() => handleDownloadReceipt(payment)}
                                                title="Download PDF Receipt"
                                            >
                                                📄 PDF Receipt
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
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" />
                        </svg>
                        <h3>No Payments Found</h3>
                        <p>
                            {filter === 'all'
                                ? "You haven't made any payments yet. Payments will appear here once you pay your invoices."
                                : `No ${filter.toLowerCase()} payments found.`
                            }
                        </p>
                        {payments.length === 0 && (
                            <div className="empty-state-actions">
                                <p>Pay your pending invoices to see payment history here.</p>
                            </div>
                        )}
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
                            {payments
                                .sort((a, b) => new Date(b.paymentDate) - new Date(a.paymentDate))
                                .slice(0, 5)
                                .map((payment) => (
                                    <div key={payment.id} className="activity-item">
                                        <div className="activity-icon success">✓</div>
                                        <div className="activity-content">
                                            <strong>Payment: {payment.invoiceNumber}</strong>
                                            <p>${(payment.amount || 0).toFixed(2)} • {formatDate(payment.paymentDate)}</p>
                                            <small className="text-muted">{payment.transactionId}</small>
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