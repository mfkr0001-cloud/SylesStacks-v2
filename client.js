import { APP_CONFIG } from '../core/config.js';

export async function apiRequest(path, options = {}) {
  if (!APP_CONFIG.apiBaseUrl) throw new Error('Сначала укажи apiBaseUrl в js/core/config.js');
  const response = await fetch(new URL(path, APP_CONFIG.apiBaseUrl), {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options,
  });
  if (!response.ok) throw new Error(`API error: ${response.status}`);
  return response.status === 204 ? null : response.json();
}
