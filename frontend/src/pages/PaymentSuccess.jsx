import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import toast from 'react-hot-toast';

const PaymentSuccess = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [invoiceData, setInvoiceData] = useState(null);

    useEffect(() => {
        const processPaymentSuccess = async () => {
            try {
                // Get parameters from URL that PayHere sends back
                const urlParams = new URLSearchParams(location.search);
                const orderId = urlParams.get('order_id');
                const paymentId = urlParams.get('payment_id');
                const statusCode = urlParams.get('status_code');

                console.log('✅ Payment success parameters:', {
                    orderId,
                    paymentId,
                    statusCode
                });

                if (orderId) {
                    // Verify payment status with backend
                    try {
                        const response = await axios.get(`http://localhost:8082/api/payments/status/${orderId}`);
                        setInvoiceData(response.data);
                        console.log('📊 Invoice status verified:', response.data);
                    } catch (error) {
                        console.warn('Could not verify invoice status:', error);
                    }

                    toast.success('Payment completed successfully! 🎉');

                    // Redirect to invoices page after 5 seconds
                    setTimeout(() => {
                        navigate('/invoices');
                    }, 5000);
                } else {
                    toast.error('Invalid payment response');
                    setTimeout(() => navigate('/invoices'), 3000);
                }

            } catch (error) {
                console.error('Error processing payment success:', error);
                toast.error('Error processing payment confirmation');
            } finally {
                setLoading(false);
            }
        };

        processPaymentSuccess();
    }, [location, navigate]);

    if (loading) {
        return (
            <div className="page-container">
                <div className="loading-state">
                    <div className="loading-spinner"></div>
                    <h2>Processing Your Payment</h2>
                    <p>Please wait while we confirm your payment...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="page-container">
            <div className="payment-result-page">
                <div className="success-message">
                    <div className="success-icon">✅</div>
                    <h1>Payment Successful!</h1>
                    <p>Thank you for your payment. Your transaction has been completed successfully.</p>

                    {invoiceData && (
                        <div className="payment-details">
                            <h3>Payment Details</h3>
                            <div className="detail-item">
                                <span>Invoice Number:</span>
                                <span>{invoiceData.invoiceNumber}</span>
                            </div>
                            <div className="detail-item">
                                <span>Status:</span>
                                <span className="status-badge status-paid">PAID</span>
                            </div>
                            <div className="detail-item">
                                <span>Amount Paid:</span>
                                <span>${invoiceData.finalAmount}</span>
                            </div>
                            {invoiceData.paymentReference && (
                                <div className="detail-item">
                                    <span>Transaction ID:</span>
                                    <span>{invoiceData.paymentReference}</span>
                                </div>
                            )}
                        </div>
                    )}

                    <div className="success-actions">
                        <p>You will be redirected to invoices page in a few seconds...</p>
                        <div className="action-buttons">
                            <Link to="/invoices" className="btn btn-primary">
                                Go to Invoices Now
                            </Link>
                            <Link to="/dashboard" className="btn btn-secondary">
                                Back to Dashboard
                            </Link>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PaymentSuccess;