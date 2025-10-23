// File: frontend/src/pages/admin/BillingManagement.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import toast from 'react-hot-toast';

const BillingManagement = ({ user }) => {
    const [billingModels, setBillingModels] = useState([]);
    const [showCreateForm, setShowCreateForm] = useState(false);
    const [loading, setLoading] = useState(true);
    const [formData, setFormData] = useState({
        city: '',
        billingType: 'WEIGHT_BASED',
        ratePerKg: '',
        monthlyFlatFee: '',
        baseFee: '',
        additionalRatePerKg: '',
        active: true
    });

    useEffect(() => {
        fetchBillingModels();
    }, []);

    const fetchBillingModels = async () => {
        try {
            // Use the correct endpoint to get all billing models
            const response = await axios.get('http://localhost:8082/api/billing/models');
            console.log('Billing models response:', response.data);
            setBillingModels(response.data || []);
        } catch (error) {
            console.error('Error fetching billing models:', error);
            // Try alternative endpoint
            try {
                const altResponse = await axios.get('http://localhost:8082/api/billing/models/city/Default City');
                setBillingModels(altResponse.data || []);
            } catch (altError) {
                console.error('Alternative endpoint also failed:', altError);
                toast.error('Failed to load billing models');
            }
        } finally {
            setLoading(false);
        }
    };

    const handleCreateModel = async (e) => {
        e.preventDefault();
        try {
            const response = await axios.post('http://localhost:8082/api/billing/models', formData);
            console.log('Create billing model response:', response.data);
            toast.success('Billing model created successfully');
            setShowCreateForm(false);
            setFormData({
                city: '',
                billingType: 'WEIGHT_BASED',
                ratePerKg: '',
                monthlyFlatFee: '',
                baseFee: '',
                additionalRatePerKg: '',
                active: true
            });
            fetchBillingModels();
        } catch (error) {
            console.error('Error creating billing model:', error);
            toast.error(error.response?.data?.error || 'Failed to create billing model');
        }
    };

    const handleInputChange = (e) => {
        const { name, value, type, checked } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: type === 'checkbox' ? checked : value
        }));
    };

    const getBillingTypeLabel = (type) => {
        switch(type) {
            case 'WEIGHT_BASED': return 'Weight Based';
            case 'FLAT_FEE': return 'Flat Fee';
            case 'HYBRID': return 'Hybrid';
            default: return type;
        }
    };

    const formatBillingDetails = (model) => {
        switch(model.billingType) {
            case 'WEIGHT_BASED':
                return `Rate: $${model.ratePerKg}/kg`;
            case 'FLAT_FEE':
                return `Fee: $${model.monthlyFlatFee}/month`;
            case 'HYBRID':
                return `Base: $${model.baseFee} + $${model.additionalRatePerKg}/kg`;
            default:
                return 'Unknown billing type';
        }
    };

    if (loading) {
        return (
            <div className="loading-state">
                <div className="loading-spinner"></div>
                <p>Loading billing models...</p>
            </div>
        );
    }

    return (
        <div className="billing-management">
            <div className="page-header">
                <h1>Billing Models</h1>
                <p>Manage pricing and billing configurations for different cities</p>
                <button
                    onClick={() => setShowCreateForm(true)}
                    className="btn btn-primary"
                >
                    + Create Billing Model
                </button>
            </div>

            {/* Create Billing Model Form */}
            {showCreateForm && (
                <div className="modal-overlay">
                    <div className="modal">
                        <div className="modal-header">
                            <h3>Create New Billing Model</h3>
                            <button
                                onClick={() => setShowCreateForm(false)}
                                className="btn-close"
                            >
                                ×
                            </button>
                        </div>
                        <form onSubmit={handleCreateModel}>
                            <div className="form-grid">
                                <div className="form-group">
                                    <label>City Name</label>
                                    <input
                                        type="text"
                                        name="city"
                                        value={formData.city}
                                        onChange={handleInputChange}
                                        placeholder="Enter city name"
                                        required
                                        className="form-input"
                                    />
                                </div>

                                <div className="form-group">
                                    <label>Billing Type</label>
                                    <select
                                        name="billingType"
                                        value={formData.billingType}
                                        onChange={handleInputChange}
                                        required
                                        className="form-input"
                                    >
                                        <option value="WEIGHT_BASED">Weight Based</option>
                                        <option value="FLAT_FEE">Flat Fee</option>
                                        <option value="HYBRID">Hybrid</option>
                                    </select>
                                </div>

                                {formData.billingType === 'WEIGHT_BASED' && (
                                    <div className="form-group">
                                        <label>Rate per kg ($)</label>
                                        <input
                                            type="number"
                                            step="0.01"
                                            name="ratePerKg"
                                            value={formData.ratePerKg}
                                            onChange={handleInputChange}
                                            placeholder="0.00"
                                            required
                                            className="form-input"
                                        />
                                    </div>
                                )}

                                {formData.billingType === 'FLAT_FEE' && (
                                    <div className="form-group">
                                        <label>Monthly Flat Fee ($)</label>
                                        <input
                                            type="number"
                                            step="0.01"
                                            name="monthlyFlatFee"
                                            value={formData.monthlyFlatFee}
                                            onChange={handleInputChange}
                                            placeholder="0.00"
                                            required
                                            className="form-input"
                                        />
                                    </div>
                                )}

                                {formData.billingType === 'HYBRID' && (
                                    <>
                                        <div className="form-group">
                                            <label>Base Fee ($)</label>
                                            <input
                                                type="number"
                                                step="0.01"
                                                name="baseFee"
                                                value={formData.baseFee}
                                                onChange={handleInputChange}
                                                placeholder="0.00"
                                                required
                                                className="form-input"
                                            />
                                        </div>
                                        <div className="form-group">
                                            <label>Additional Rate per kg ($)</label>
                                            <input
                                                type="number"
                                                step="0.01"
                                                name="additionalRatePerKg"
                                                value={formData.additionalRatePerKg}
                                                onChange={handleInputChange}
                                                placeholder="0.00"
                                                required
                                                className="form-input"
                                            />
                                        </div>
                                    </>
                                )}

                                <div className="form-group checkbox-group">
                                    <label className="checkbox-label">
                                        <input
                                            type="checkbox"
                                            name="active"
                                            checked={formData.active}
                                            onChange={handleInputChange}
                                            className="checkbox-input"
                                        />
                                        Active
                                    </label>
                                </div>
                            </div>

                            <div className="modal-actions">
                                <button
                                    type="button"
                                    onClick={() => setShowCreateForm(false)}
                                    className="btn btn-secondary"
                                >
                                    Cancel
                                </button>
                                <button type="submit" className="btn btn-primary">
                                    Create Model
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Billing Models List */}
            <div className="card">
                <div className="card-header">
                    <h3>Active Billing Models</h3>
                    <span className="badge">{billingModels.length} models</span>
                </div>

                {billingModels.length > 0 ? (
                    <div className="billing-models-list">
                        {billingModels.map((model) => (
                            <div key={model.id} className="billing-model-item">
                                <div className="model-info">
                                    <div className="model-header">
                                        <h4>🏙️ {model.city}</h4>
                                        <span className={`status-badge ${model.active ? 'status-active' : 'status-inactive'}`}>
                                            {model.active ? 'Active' : 'Inactive'}
                                        </span>
                                    </div>
                                    <div className="model-details">
                                        <span className="model-type">{getBillingTypeLabel(model.billingType)}</span>
                                        <span>{formatBillingDetails(model)}</span>
                                    </div>
                                </div>
                                <div className="model-actions">
                                    <button className="btn btn-sm btn-secondary">Edit</button>
                                    <button className="btn btn-sm btn-danger">Delete</button>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="empty-state">
                        <h3>No Billing Models</h3>
                        <p>Create your first billing model to start charging for waste collection services.</p>
                        <button
                            onClick={() => setShowCreateForm(true)}
                            className="btn btn-primary"
                        >
                            Create First Model
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default BillingManagement;