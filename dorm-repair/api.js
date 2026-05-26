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
    },

    announcement: {
        list(params) {
            return api.get('/announcement/page', params);
        }
    }
};
