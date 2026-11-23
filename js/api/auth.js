import { apiRequest } from './client.js';
export const getAuthConfig = () => apiRequest('/api/config');
export const signOut = () => apiRequest('/api/logout', { method: 'POST' });
// Google credential exchange is intentionally server-side; never put SESSION_SECRET in frontend.
