import React from 'react';
import { Link, useLocation } from 'react-router-dom';

const PaymentCancel = () => {
    const location = useLocation();

    const getCancelReason = () => {
        const urlParams = new URLSearchParams(location.search);
        const statusCode = urlParams.get('status_code');

        switch(statusCode) {
            case '-1': return "You canceled the payment process.";
            case '-2': return "The payment failed. Please try again.";
            case '-3': return "There was an issue with the payment.";
            default: return "The payment process was interrupted.";
        }
    };

    return (
        <div className="page-container">
            <div className="payment-result-page">
                <div className="cancel-message">
                    <div className="cancel-icon">❌</div>
                    <h1>Payment Cancelled</h1>
                    <p>{getCancelReason()}</p>
                    <p>Don't worry, you can try again anytime. Your invoice will remain pending until paid.</p>

                    <div className="action-buttons">
                        <Link to="/invoices" className="btn btn-primary">
                            Back to Invoices
                        </Link>
                        <button onClick={() => window.history.back()} className="btn btn-secondary">
                            Try Again
                        </button>
                        <Link to="/dashboard" className="btn btn-outline">
                            Go to Dashboard
                        </Link>
                    </div>

                    <div className="help-section">
                        <h3>Need Help?</h3>
                        <p>If you're experiencing issues with payments, please contact our support team.</p>
                        <div className="contact-info">
                            <p>📞 Support: +94 77 123 4567</p>
                            <p>✉️ Email: support@wastemanagement.com</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PaymentCancel;