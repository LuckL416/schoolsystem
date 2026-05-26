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

    async put(url, data) {
        return axios.put(API_BASE + url, data || {}, { headers: this.headers() });
    },

    async delete(url) {
        return axios.delete(API_BASE + url, { headers: this.headers() });
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
    },

    announcement: {
        list(params) {
            return api.get('/announcement/list', params);
        },
        detail(id) {
            return api.get('/announcement/' + id);
        },
        create(data) {
            return api.post('/announcement', data);
        },
        update(id, data) {
            return api.put('/announcement/' + id, data);
        },
        del(id) {
            return api.delete('/announcement/' + id);
        }
    },

    notification: {
        list(page, size) {
            return api.get('/notification/list', { page, size });
        },
        unreadCount() {
            return api.get('/notification/unread-count');
        },
        markRead(id) {
            return api.put('/notification/' + id + '/read');
        },
        markAllRead() {
            return api.put('/notification/read-all');
        }
    },

    inventory: {
        list(params) {
            return api.get('/inventory/list', params);
        },
        create(data) {
            return api.post('/inventory', data);
        },
        update(id, data) {
            return axios.put(API_BASE + '/inventory/' + id, data, { headers: api.headers() });
        },
        records(itemId, params) {
            return api.get('/inventory/' + itemId + '/records', params);
        },
        lowStock() {
            return api.get('/inventory/low-stock');
        }
    }
};
