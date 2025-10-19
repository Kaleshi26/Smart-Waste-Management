// File: frontend/src/components/AdminDashboard.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import WasteChart from './WasteChart';

function AdminDashboard({ user }) {
  const [stats, setStats] = useState(null);
  const [events, setEvents] = useState([]);
  const [monthlyData, setMonthlyData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState('all'); // State to track the active filter

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      setError(null);

      // Create date parameters based on the current filter
      let params = {};
      if (filter !== 'all') {
        const endDate = new Date();
        const startDate = new Date();
        const daysToSubtract = filter === '7days' ? 7 : 30;
        startDate.setDate(endDate.getDate() - daysToSubtract);
        params = {
          start: startDate.toISOString(),
          end: endDate.toISOString(),
        };
      }

      try {
        const [statsResponse, eventsResponse, monthlyResponse] = await Promise.all([
          axios.get('http://localhost:8082/api/reports/dashboard-stats', { params }),
          axios.get('http://localhost:8082/api/reports/collection-events', { params }),
          axios.get('http://localhost:8082/api/reports/monthly-waste'), // Monthly chart is not filtered
        ]);
        setStats(statsResponse.data);
        setEvents(eventsResponse.data);
        setMonthlyData(monthlyResponse.data);
      } catch (err) {
        setError('Failed to load dashboard data.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [filter]); // Re-run this effect every time the 'filter' state changes

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <div>
          <h1 className="dashboard-title">Admin Dashboard</h1>
          <p className="dashboard-welcome">Overview of the waste management system.</p>
        </div>
        <div className="filter-buttons">
          <button onClick={() => setFilter('7days')} className={filter === '7days' ? 'active' : ''}>Last 7 Days</button>
          <button onClick={() => setFilter('30days')} className={filter === '30days' ? 'active' : ''}>Last 30 Days</button>
          <button onClick={() => setFilter('all')} className={filter === 'all' ? 'active' : ''}>All Time</button>
        </div>
      </div>

      <div className="stats-grid">
        {loading && <p>Loading stats...</p>}
        {error && <p className="error-message">{error}</p>}
        {stats && (
          <>
            <div className="stat-card"><h3 className="stat-title">Total Waste Collected</h3><p className="stat-value">{stats.totalWeightKg.toFixed(2)} Kg</p></div>
            <div className="stat-card"><h3 className="stat-title">Total Collections</h3><p className="stat-value">{stats.totalCollections}</p></div>
            <div className="stat-card"><h3 className="stat-title">Registered Bins</h3><p className="stat-value">{stats.totalBins}</p></div>
          </>
        )}
      </div>

      <div className="data-card">
        {loading ? <p>Loading chart...</p> : error ? <p className="error-message">{error}</p> : (
          <WasteChart chartData={monthlyData} />
        )}
      </div>

      <div className="data-card">
        <h3>Recent Collection Events</h3>
        {loading ? <p>Loading events...</p> : error ? <p className="error-message">{error}</p> : (
          <table>
            <thead>
              <tr>
                <th>Event ID</th><th>Bin ID</th><th>Resident</th><th>Staff Member</th><th>Time</th><th>Weight (Kg)</th>
              </tr>
            </thead>
            <tbody>
              {events.length > 0 ? (
                events.map((event) => (
                  <tr key={event.id}>
                    <td>{event.id}</td><td>{event.binId}</td><td>{event.residentName}</td><td>{event.staffName}</td>
                    <td>{new Date(event.collectionTime).toLocaleString()}</td><td>{event.weightInKg.toFixed(2)}</td>
                  </tr>
                ))
              ) : (
                <tr><td colSpan="6">No collection events found for this period.</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default AdminDashboard;