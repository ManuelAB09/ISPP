// src/api/client.js
// Compute base URL with a safe fallback. during development the
// environment variable may accidentally be set to ":8080" or another
// invalid value (see recent console errors).  In that case we ignore it
// and default to localhost so the frontend keeps working without any
// manual intervention.
const rawUrl = process.env.REACT_APP_API_URL;
const API_BASE_URL =
  rawUrl && rawUrl.trim() && rawUrl.trim() !== ':8080'
    ? rawUrl
    : 'http://localhost:8080';

// Helpful diagnostic when debugging networking issues:
console.log('API_BASE_URL =', API_BASE_URL);

class ApiClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
    this.token = localStorage.getItem('accessToken') || null;
  }

  setToken(token) {
    this.token = token;
  }

  async request(method, path, body = null) {
    const headers = {
      'Content-Type': 'application/json',
    };

    // Always read the freshest token available
    const currentToken = this.token || localStorage.getItem('accessToken');
    if (currentToken) {
      headers['Authorization'] = `Bearer ${currentToken}`;
    }

    const options = {
      method,
      headers,
    };

    if (body) {
      options.body = JSON.stringify(body);
    }

    const response = await fetch(`${this.baseUrl}${path}`, options);

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new ApiError(response.status, error.message || 'Error desconocido');
    }

    // Si la respuesta es 204 No Content, no hay body
    if (response.status === 204) {
      return null;
    }

    return response.json();
  }

  get(path) {
    return this.request('GET', path);
  }

  post(path, body) {
    return this.request('POST', path, body);
  }

  put(path, body) {
    return this.request('PUT', path, body);
  }

  patch(path, body) {
    return this.request('PATCH', path, body);
  }

  delete(path) {
    return this.request('DELETE', path);
  }
}

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
    this.name = 'ApiError';
  }
}

export const apiClient = new ApiClient(API_BASE_URL);