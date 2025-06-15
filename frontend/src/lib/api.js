// src/lib/api.js
import axios from 'axios';
import { get } from 'svelte/store';
import { authStore } from './stores/authStore';
import { goto } from '$app/navigation';

const API_BASE_URL = 'http://localhost:8080/api'; // Your Spring Boot backend URL

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Interceptor to add JWT token to requests if authenticated
api.interceptors.request.use(
    (config) => {
        const { token } = get(authStore);
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Interceptor for response errors, especially 401/403
api.interceptors.response.use(
    (response) => response,
    (error) => {
        // Access the current auth store state for potential logout
        const { isAuthenticated } = get(authStore);

        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            console.error("Unauthorized or Forbidden response from API. Attempting logout...", error.response);
            // Only logout if they were previously authenticated
            if (isAuthenticated) {
                authStore.logout(); // Clear token and user from store/cookies
                // Ensure goto is only called in a browser environment
                if (typeof window !== 'undefined') {
                    goto('/login');
                }
            }
        }
        return Promise.reject(error);
    }
);

// Auth Endpoints
export const auth = {
    signup: (userData) => api.post('/auth/signup', userData),
    login: (credentials) => api.post('/auth/login', credentials),
};

// Post Endpoints
export const posts = {
    getAll: (page = 0, size = 10, tags = []) => {
        let url = `/posts?page=${page}&size=${size}`;
        if (tags && tags.length > 0) {
            url += `&tags=${tags.map(t => encodeURIComponent(t)).join(',')}`;
        }
        return api.get(url);
    },
    getById: (id) => api.get(`/posts/${id}`),
    create: (formData) => api.post('/posts', formData, {headers: {'Content-Type': undefined}}),
    toggleLike: (postId) => api.put(`/posts/${postId}/like`),
    update: (postId, postData) => api.put(`/posts/${postId}`, postData),
    delete: (postId) => api.delete(`/posts/${postId}`),
    search: (query, page = 0, size = 10) => {
        const encodedQuery = encodeURIComponent(query);
        return api.get(`/posts/search?q=${encodedQuery}&page=${page}&size=${size}`);
    }
};

// Comments Endpoints
export const comments = {
    getByPostId: async (postId) => {
        return await api.get(`/posts/${postId}/comments`);
    },
    create: async (postId, content) => {
        return await api.post(`/comments`, { postId, content });
    },
    delete: async (commentId) => {
        return await api.delete(`/comments/${commentId}`);
    }
};

// Messaging (WebSockets will be handled separately)
export const messages = {
    getConversation: (userId) => api.get(`/messages/${userId}`),
};

// Admin Endpoints
export const admin = {
    // Fetches a list of all users (requires ADMIN role on backend)
    getAllUsers: async () => {
        // The API_BASE_URL already includes /api, so we just add /admin/users
        return await api.get('/admin/users');
    },
    // Deletes a user by ID (requires ADMIN role on backend)
    deleteUser: async (userId) => {
        return await api.delete(`/admin/users/${userId}`);
    },
    // Updates user roles (requires ADMIN role on backend)
    updateUserRoles: async (userId, roles) => {
        // Backend typically expects a JSON object for the body
        // e.g., { "roles": ["ROLE_USER", "ROLE_MODERATOR"] }
        return await api.put(`/admin/users/${userId}/roles`, { roles });
    }
};

export default api;