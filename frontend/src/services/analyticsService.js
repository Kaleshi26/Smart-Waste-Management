// File: frontend/src/services/analyticsService.js
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8082/api';

/**
 * Analytics Service - Handles all analytics-related API calls
 * Follows Single Responsibility Principle - only handles analytics data fetching
 */
class AnalyticsService {
    /**
     * Get comprehensive analytics data for the specified time range
     * @param {string} range - Time range filter ('7', '30', 'all')
     * @returns {Promise<Object>} Analytics data object
     */
    async getAnalyticsData(range = '30') {
        try {
            const response = await axios.get(`${API_BASE_URL}/waste/analytics`, {
                params: { range }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching analytics data:', error);
            throw new Error('Failed to fetch analytics data');
        }
    }

    /**
     * Get KPIs (Key Performance Indicators) data
     * @param {string} range - Time range filter
     * @returns {Promise<Object>} KPIs data
     */
    async getKPIs(range = '30') {
        try {
            const response = await axios.get(`${API_BASE_URL}/waste/analytics/kpis`, {
                params: { range }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching KPIs:', error);
            throw new Error('Failed to fetch KPIs data');
        }
    }

    /**
     * Get monthly waste collection data for charts
     * @param {string} range - Time range filter
     * @returns {Promise<Array>} Monthly data array
     */
    async getMonthlyData(range = '30') {
        try {
            const response = await axios.get(`${API_BASE_URL}/waste/analytics/monthly`, {
                params: { range }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching monthly data:', error);
            throw new Error('Failed to fetch monthly data');
        }
    }

    /**
     * Get collection records for the specified time range
     * @param {string} range - Time range filter
     * @returns {Promise<Array>} Collection records array
     */
    async getCollectionRecords(range = '30') {
        try {
            const response = await axios.get(`${API_BASE_URL}/waste/analytics/collections`, {
                params: { range }
            });
            return response.data;
        } catch (error) {
            console.error('Error fetching collection records:', error);
            throw new Error('Failed to fetch collection records');
        }
    }

    /**
     * Get bin status overview data
     * @returns {Promise<Array>} Bin status data array
     */
    async getBinStatusOverview() {
        try {
            const response = await axios.get(`${API_BASE_URL}/waste/analytics/bin-status`);
            return response.data;
        } catch (error) {
            console.error('Error fetching bin status overview:', error);
            throw new Error('Failed to fetch bin status overview');
        }
    }

    /**
     * Export analytics data as CSV
     * @param {string} range - Time range filter
     * @param {string} format - Export format ('csv', 'json')
     * @returns {Promise<Blob>} File blob for download
     */
    async exportData(range = '30', format = 'csv') {
        try {
            const response = await axios.get(`${API_BASE_URL}/waste/analytics/export`, {
                params: { range, format },
                responseType: 'blob'
            });
            return response.data;
        } catch (error) {
            console.error('Error exporting data:', error);
            throw new Error('Failed to export data');
        }
    }
}

// Export singleton instance following Singleton pattern
const analyticsService = new AnalyticsService();
export default analyticsService;
