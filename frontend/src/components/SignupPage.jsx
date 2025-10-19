// File: frontend/src/components/SignupPage.jsx
import React, { useState } from 'react';
import axios from 'axios';

// The 'onNavigateToLogin' prop is a function we will pass from App.jsx
// to allow this component to switch the view back to the login page.
function SignupPage({ onNavigateToLogin }) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [address, setAddress] = useState('');
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setSuccessMessage(null);

    // We'll register all new users as RESIDENTS by default from the public signup form.
    const newUser = {
      name,
      email,
      password,
      address,
      role: 'ROLE_RESIDENT',
    };

    try {
      await axios.post('http://localhost:8082/api/auth/register', newUser);
      setSuccessMessage('Registration successful! You can now log in.');
    } catch (err) {
      setError('Registration failed. The email might already be in use.');
      console.error('Signup error:', err);
    }
  };

  return (
    <div className="login-container">
      <form onSubmit={handleSubmit} className="login-form">
        <h2>Create an Account</h2>

        <div className="form-group">
          <label htmlFor="name">Full Name</label>
          <input type="text" id="name" value={name} onChange={(e) => setName(e.target.value)} required />
        </div>

        <div className="form-group">
          <label htmlFor="email">Email</label>
          <input type="email" id="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </div>

        <div className="form-group">
          <label htmlFor="password">Password</label>
          <input type="password" id="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>

        <div className="form-group">
          <label htmlFor="address">Address</label>
          <input type="text" id="address" value={address} onChange={(e) => setAddress(e.target.value)} required />
        </div>

        {error && <p className="error-message">{error}</p>}
        {successMessage && <p className="success-message">{successMessage}</p>}

        <button type="submit">Sign Up</button>

        <p className="navigation-link" onClick={onNavigateToLogin}>
          Already have an account? Login
        </p>
      </form>
    </div>
  );
}

export default SignupPage;