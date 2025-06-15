import { writable } from 'svelte/store';
import Cookies from 'js-cookie';

const USER_KEY = 'auth_user';
const TOKEN_KEY = 'auth_token';

function loadInitialState() {
    let user = null;
    let token = null;

    if (typeof window !== 'undefined') {
        try {
            const userString = Cookies.get(USER_KEY);
            const tokenString = Cookies.get(TOKEN_KEY);

            if (userString) {
                user = JSON.parse(userString);
            }
            if (tokenString) {
                token = tokenString;
            }
        } catch (e) {
            console.error("Failed to parse auth data from cookies:", e);
            Cookies.remove(USER_KEY);
            Cookies.remove(TOKEN_KEY);
        }
    }

    return {
        isAuthenticated: !!token,
        user: user,
        token: token,
    };
}

const { subscribe, set, update } = writable(loadInitialState());

export const authStore = {
    subscribe,
    login: async (username, password) => {
        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ username, password })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Unknown error from server' }));
                console.error('Login API failed:', response.status, errorData);
                set({ ...loadInitialState(), error: errorData.message || 'Login failed: Invalid credentials.' });
                return { success: false, message: errorData.message || 'Login failed: Invalid credentials or server error.' };
            }

            const data = await response.json();
            console.log('Login API successful, response data:', data); // Inspect this log carefully

            const receivedToken = data.token; // IMPORTANT: Ensure 'data.token' is the correct property from your backend's JwtResponse
            const receivedUser = { // Adjust based on your backend JwtResponse structure
                id: data.id,
                username: data.username,
                email: data.email,
                roles: data.roles,
                type: data.type
            };

            if (!receivedToken) {
                console.error("Authentication token was not found in the login response!");
                set({ ...loadInitialState(), error: "Login response missing token." });
                return { success: false, message: "Login failed: Server did not provide a token." };
            }

            set({
                isAuthenticated: true,
                user: receivedUser,
                token: receivedToken,
                error: null
            });

            // Set cookies for persistence (your original logic, keeping it)
            if (typeof window !== 'undefined') {
                Cookies.set(USER_KEY, JSON.stringify(receivedUser), { expires: 7, secure: window.location.protocol === 'https:', sameSite: 'Lax' });
                Cookies.set(TOKEN_KEY, receivedToken, { expires: 7, secure: window.location.protocol === 'https:', sameSite: 'Lax' });
            }

            return { success: true, message: data.message || 'Logged in successfully!', user: receivedUser, token: receivedToken };

        } catch (error) {
            console.error('Network or unexpected error during login:', error);
            set({ ...loadInitialState(), error: error.message || 'An unexpected network error occurred.' });
            return { success: false, message: error.message || 'An unexpected network error occurred during login.' };
        }
    },
    logout: () => {
        if (typeof window !== 'undefined') {
            Cookies.remove(USER_KEY);
            Cookies.remove(TOKEN_KEY);
        }
        set({
            isAuthenticated: false,
            user: null,
            token: null,
            error: null
        });
    },
    signupUser: async (userData) => {
        // --- IMPORTANT CHANGE HERE ---
        // Destructure dateOfBirth, not age
        const { username, email, password, dateOfBirth } = userData;

        try {
            const response = await fetch('/api/auth/signup', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                // --- IMPORTANT CHANGE HERE ---
                // Send dateOfBirth, not age
                body: JSON.stringify({ username, email, password, dateOfBirth })
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Unknown error from server' }));
                console.error('Signup failed:', response.status, errorData);
                return { success: false, message: errorData.message || 'Signup failed with an unknown error.' };
            }

            const data = await response.json();
            console.log('Signup successful, server response:', data);

            return { success: true, message: data.message || 'Signup successful!' };
        } catch (error) {
            console.error('Network or unexpected error during signup:', error);
            return { success: false, message: error.message || 'An unexpected error occurred during signup.' };
        }
    },
    // The fetchWithAuth method is already good.
    // If you decide to consistently use this, you'd refactor Profile.svelte to use authStore.fetchWithAuth
    // and pass the full API_BASE_URL to it.
    fetchWithAuth: async (url, options = {}) => {
        let currentToken;
        authStore.subscribe(value => {
            currentToken = value.token;
        })(); // Immediately invoke to get current value

        if (!currentToken) {
            console.warn('No authentication token found. Logging out...');
            authStore.logout();
            // This goto is optional and might be better handled by the component that calls fetchWithAuth
            // import { goto } from '$app/navigation'; goto('/login');
            throw new Error('Not authenticated.');
        }

        const authHeaders = {
            ...options.headers,
            'Authorization': `Bearer ${currentToken}`
        };

        try {
            const response = await fetch(url, {
                ...options,
                headers: authHeaders
            });

            if (response.status === 401 || response.status === 403) {
                console.error('Authentication failed (401/403). Logging out.');
                authStore.logout();
                // This goto is optional and might be better handled by the component that calls fetchWithAuth
                // import { goto } from '$app/navigation'; goto('/login');
                throw new Error('Session expired or unauthorized.');
            }

            return response;
        } catch (error) {
            console.error('Error in authenticated request:', error);
            throw error;
        }
    },
    // Optional: Add a method to update the user object in the store
    // This could be useful if your profile update returns the full user object
    updateUser: (newUserProfile) => {
        update(store => {
            const updatedUser = { ...store.user, ...newUserProfile };
            if (typeof window !== 'undefined') {
                Cookies.set(USER_KEY, JSON.stringify(updatedUser), { expires: 7, secure: window.location.protocol === 'https:', sameSite: 'Lax' });
            }
            return { ...store, user: updatedUser };
        });
    }
};