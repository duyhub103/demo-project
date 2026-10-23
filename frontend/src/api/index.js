import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor - attach token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor - handle 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

// Auth
export const authAPI = {
  login: (data) => api.post('/auth/login', data),
};

// Catalog
export const catalogAPI = {
  getMedicines: (keyword = '', page = 0) => api.get(`/catalog/medicines?keyword=${keyword}&page=${page}&size=20`),
  createMedicine: (data) => api.post('/catalog/medicines', data),
  updateMedicine: (id, data) => api.put(`/catalog/medicines/${id}`, data),
  getSuppliers: () => api.get('/catalog/suppliers'),
  createSupplier: (data) => api.post('/catalog/suppliers', data),
  getWarehouses: () => api.get('/catalog/warehouses'),
  createWarehouse: (data) => api.post('/catalog/warehouses', data),
  getBatches: (warehouseId, medicineId) => {
    let url = `/catalog/batches?warehouseId=${warehouseId}`;
    if (medicineId) url += `&medicineId=${medicineId}`;
    return api.get(url);
  },
};

// Import Orders
export const importAPI = {
  create: (data) => api.post('/import-orders', data),
  list: (warehouseId, status, page = 0) => {
    let url = `/import-orders?page=${page}&size=10`;
    if (warehouseId) url += `&warehouseId=${warehouseId}`;
    if (status) url += `&status=${status}`;
    return api.get(url);
  },
  getById: (id) => api.get(`/import-orders/${id}`),
  approve: (id, approved, rejectReason) => api.post(`/import-orders/${id}/approve?approved=${approved}${rejectReason ? `&rejectReason=${rejectReason}` : ''}`),
};

// Export Orders
export const exportAPI = {
  create: (data) => api.post('/export-orders', data),
  list: (warehouseId, status, page = 0) => {
    let url = `/export-orders?page=${page}&size=10`;
    if (warehouseId) url += `&warehouseId=${warehouseId}`;
    if (status) url += `&status=${status}`;
    return api.get(url);
  },
  getById: (id) => api.get(`/export-orders/${id}`),
  approve: (id, approved) => api.post(`/export-orders/${id}/approve?approved=${approved}`),
};

// Transfer Orders
export const transferAPI = {
  create: (data) => api.post('/transfer-orders', data),
  list: (warehouseId, status, page = 0) => {
    let url = `/transfer-orders?page=${page}&size=10`;
    if (warehouseId) url += `&warehouseId=${warehouseId}`;
    if (status) url += `&status=${status}`;
    return api.get(url);
  },
  send: (id) => api.post(`/transfer-orders/${id}/send`),
  confirmDelivery: (id) => api.post(`/transfer-orders/${id}/confirm-delivery`),
  cancel: (id) => api.post(`/transfer-orders/${id}/cancel`),
};

// Alerts & Inventory
export const inventoryAPI = {
  getAlerts: (warehouseId) => api.get(`/inventory/alerts?warehouseId=${warehouseId}`),
  createCheck: (warehouseId, note) => api.post(`/inventory/checks?warehouseId=${warehouseId}&note=${note || ''}`),
  listChecks: (warehouseId, page = 0) => api.get(`/inventory/checks?warehouseId=${warehouseId}&page=${page}&size=10`),
  updateActual: (checkId, data) => api.put(`/inventory/checks/${checkId}/actual-quantities`, data),
  approve: (checkId, approved) => api.post(`/inventory/checks/${checkId}/approve?approved=${approved}`),
};

// Users
export const userAPI = {
  getAll: () => api.get('/users'),
  create: (data) => api.post('/users', data),
  toggleStatus: (id) => api.patch(`/users/${id}/toggle-status`),
};
