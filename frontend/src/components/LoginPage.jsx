// File: frontend/src/components/LoginPage.jsx
import React, { useState } from 'react';
import axios from 'axios';

// We add a new prop 'onNavigateToSignup'
function LoginPage({ onLoginSuccess, onNavigateToSignup }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);

    try {
      const response = await axios.post('http://localhost:8082/api/auth/login', {
        email: email,
        password: password,
      });
      onLoginSuccess(response.data);
    } catch (err) {
      setError('Login failed. Please check your email and password.');
      console.error('Login error:', err);
    }
  };

  return (
    <div className="login-container">
      <form onSubmit={handleSubmit} className="login-form">
        <h2>Smart Waste Management Login</h2>
        <div className="form-group">
          <label htmlFor="email">Email</label>
          <input type="email" id="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>
        <div className="form-group">
          <label htmlFor="password">Password</label>
          <input type="password" id="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>
        {error && <p className="error-message">{error}</p>}
        <button type="submit">Login</button>

        {/* This is the new navigation link */}
        <p className="navigation-link" onClick={onNavigateToSignup}>
          Don't have an account? Sign Up
        </p>
      </form>
    </div>
  );
}

export default LoginPage;