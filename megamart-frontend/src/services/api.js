import axios from 'axios';

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080', withCredentials: true, headers: { 'Content-Type': 'application/json' } });
api.interceptors.response.use((r) => r, (error) => {
  if (error.response?.status === 401) window.dispatchEvent(new Event('megamart:unauthorized'));
  return Promise.reject(error);
});
export const dataOf = (response) => response.data?.data;
export const messageOf = (error) => error.response?.data?.message || error.response?.data?.error || error.message || 'Something went wrong';
export default api;
