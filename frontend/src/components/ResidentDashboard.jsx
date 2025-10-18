// File: frontend/src/components/ResidentDashboard.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';

function ResidentDashboard({ user }) {
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Function to fetch invoices from the backend
  const fetchInvoices = async () => {
    setLoading(true);
    try {
      // We use the logged-in user's ID to fetch their specific invoices
      const response = await axios.get(`http://localhost:8082/api/invoices/user/${user.id}`);
      setInvoices(response.data);
    } catch (err) {
      setError('Could not fetch invoices.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // useEffect runs this function once when the component is first displayed
  useEffect(() => {
    fetchInvoices();
  }, [user.id]); // Dependency array ensures it re-runs if the user changes

  // Function to handle the "Pay Now" button click
  const handlePayInvoice = async (invoiceId) => {
    try {
      // Send a POST request to our payment simulation endpoint
      await axios.post(`http://localhost:8082/api/invoices/pay/${invoiceId}`);
      // After successful payment, refresh the invoice list to show the updated status
      fetchInvoices();
    } catch (err) {
      alert('Payment failed: ' + (err.response?.data || 'Server error'));
    }
  };

  return (
    <div className="dashboard-container">
      <h2 className="dashboard-title">My Invoices</h2>
      <p className="dashboard-welcome">View your billing history and make payments.</p>

      <div className="dashboard-card">
        {loading && <p>Loading invoices...</p>}
        {error && <p className="error-message">{error}</p>}
        {!loading && !error && (
          <table>
            <thead>
              <tr>
                <th>Invoice Date</th>
                <th>Due Date</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {invoices.length > 0 ? (
                invoices.map((invoice) => (
                  <tr key={invoice.id}>
                    <td>{invoice.invoiceDate}</td>
                    <td>{invoice.dueDate}</td>
                    <td>${invoice.amount.toFixed(2)}</td>
                    <td>
                      <span className={`status-badge status-${invoice.status.toLowerCase()}`}>
                        {invoice.status}
                      </span>
                    </td>
                    <td>
                      {invoice.status === 'UNPAID' && (
                        <button
                          onClick={() => handlePayInvoice(invoice.id)}
                          className="pay-button"
                        >
                          Pay Now
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="5">You have no invoices.</td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default ResidentDashboard;