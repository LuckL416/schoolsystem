// 宿舍报修系统 — 公共 API 层
const API_BASE = 'http://localhost:8080/api';

const api = {
    getToken() {
        return localStorage.getItem('token');
    },

    headers() {
        const token = this.getToken();
        return token ? { Authorization: 'Bearer ' + token } : {};
    },

    async get(url, params) {
        return axios.get(API_BASE + url, { headers: this.headers(), params });
    },

    async post(url, data) {
        return axios.post(API_BASE + url, data, { headers: this.headers() });
    },

    // 登录用（不需要 token）
    async loginPost(url, data) {
        return axios.post(API_BASE + url, data, {
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        });
    },

    goLogin() {
        localStorage.removeItem('token');
        location.href = 'login.html';
    }
};

// ========== Notification APIs ==========
api.notification = {
  list: (page = 1, size = 20) => api.get('/notification/list', { params: { page, size } }),
  unreadCount: () => api.get('/notification/unread-count'),
  markRead: (id) => api.put(`/notification/${id}/read`),
  markAllRead: () => api.put('/notification/read-all'),
};

// ========== Announcement APIs ==========
api.announcement = {
  list: (params = {}) => api.get('/announcement/list', { params }),
  detail: (id) => api.get(`/announcement/${id}`),
  create: (data) => api.post('/announcement', data),
  update: (id, data) => api.put(`/announcement/${id}`, data),
  delete: (id) => api.delete(`/announcement/${id}`),
};

// ========== Inventory APIs ==========
api.inventory = {
  list: (params = {}) => api.get('/inventory/list', { params }),
  create: (data) => api.post('/inventory', data),
  update: (id, data) => api.put(`/inventory/${id}`, data),
  stockIn: (id, data) => api.post(`/inventory/${id}/in`, data),
  stockOut: (id, data) => api.post(`/inventory/${id}/out`, data),
  records: (id, params = {}) => api.get(`/inventory/${id}/records`, { params }),
  lowStock: () => api.get('/inventory/low-stock'),
};
