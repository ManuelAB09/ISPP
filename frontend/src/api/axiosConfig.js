import axios from 'axios';

// Historically we used port 4010 during the early stages of the
// project; the backend now listens on 8080.  Use the same environment-check
// logic as client.js so both fetch() and axios use the same base URL.
const rawUrl = process.env.REACT_APP_API_URL;
const API_BASE_URL =
  rawUrl && rawUrl.trim() && rawUrl.trim() !== ':8080'
    ? rawUrl
    : 'http://localhost:8080';

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status } = error.response;
      if (status === 401) {
        localStorage.removeItem('accessToken');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
