<script>
    import { onMount } from 'svelte';
    import axios from 'axios';
    import { goto } from '$app/navigation';
    import { authStore } from '$lib/stores/authStore';

    // Reactive state for user profile data and form inputs
    let profile = {
        id: '',
        username: '',
        email: '',
        // Removed 'age' and 'isAdult' from initial profile state,
        // as they are derived from dateOfBirth from the backend
        dateOfBirth: '', // Now stores the date of birth string (YYYY-MM-DD)
        createdAt: '',
        updatedAt: '',
        age:null,
        adult: false // This will be derived from dateOfBirth
    };

    let email = ''; // This will be bound to the email input for updates
    let dateOfBirth = ''; // This will be bound to the dateOfBirth input for updates

    let loading = true; // For initial data fetch
    let updating = false; // For profile update submission
    let errorMessage = '';
    let successMessage = '';

    const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

    // Get the token directly from the authStore
    let token = '';
    // Subscribe to authStore to get the token reactively
    const unsubscribe = authStore.subscribe(value => {
        token = value.token;
    });

    async function fetchUserProfile() {
        loading = true;
        errorMessage = '';
        successMessage = '';

        if (!token) {
            errorMessage = 'You are not logged in. Please log in to view your profile.';
            goto('/login');
            return;
        }

        try {
            const response = await axios.get(`${API_BASE_URL}/users/profile`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            profile = response.data;
            // Initialize form fields with current profile data
            email = profile.email;
            // Ensure dateOfBirth is correctly set for the input field
            // Backend should return it in YYYY-MM-DD format already
            dateOfBirth = profile.dateOfBirth || ''; // Use empty string if null
        } catch (error) {
            console.error('Failed to fetch user profile:', error);
            if (axios.isAxiosError(error) && error.response) {
                if (error.response.status === 401 || error.response.status === 403) {
                    errorMessage = 'Session expired or unauthorized. Please log in again.';
                    authStore.logout();
                    goto('/login');
                } else if (error.response.data && error.response.data.message) {
                    errorMessage = error.response.data.message;
                } else {
                    errorMessage = `Error fetching profile: ${error.response.status} ${error.response.statusText}`;
                }
            } else {
                errorMessage = 'Network error or server unreachable. Please try again later.';
            }
        } finally {
            loading = false;
        }
    }

    async function handleSubmit() {
        errorMessage = '';
        successMessage = '';
        updating = true;

        if (!token) {
            errorMessage = 'You are not logged in. Please log in to update your profile.';
            goto('/login');
            return;
        }

        try {
            const updatePayload = {
                email: email,
                // --- IMPORTANT CHANGE HERE ---
                // Send dateOfBirth instead of age
                dateOfBirth: dateOfBirth // This will be in YYYY-MM-DD format from the input
            };

            const response = await axios.put(`${API_BASE_URL}/users/profile`, updatePayload, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });
            profile = response.data; // Update local profile state with the response
            // Re-initialize the input value in case the backend sent back a slightly different format
            dateOfBirth = profile.dateOfBirth;
            successMessage = 'Profile updated successfully!';
        } catch (error) {
            console.error('Failed to update user profile:', error);
            if (axios.isAxiosError(error) && error.response) {
                if (error.response.status === 401 || error.response.status === 403) {
                    errorMessage = 'Session expired or unauthorized. Please log in again.';
                    authStore.logout();
                    goto('/login');
                } else if (error.response.data && error.response.data.message) {
                    errorMessage = error.response.data.message;
                } else {
                    errorMessage = `Error updating profile: ${error.response.status} ${error.response.statusText}`;
                }
            } else {
                errorMessage = 'Network error or server unreachable. Please try again later.';
            }
        } finally {
            updating = false;
        }
    }

    // Fetch user profile data when the component mounts
    onMount(() => {
        fetchUserProfile();
        return () => {
            unsubscribe(); // Clean up subscription when component is unmounted
        };
    });
</script>

<div class="flex items-center justify-center min-h-[calc(100vh-160px)]">
    <div class="bg-white dark:bg-gray-800 p-8 rounded-lg outline-2 dark:outline-none shadow-lg w-full max-w-md transition-colors duration-300">
        <h1 class="text-3xl font-bold text-center mb-6 text-gray-900 dark:text-gray-100">User Profile</h1>

        {#if loading}
            <p class="text-center text-gray-700 dark:text-gray-300">Loading profile data...</p>
        {:else if errorMessage && !successMessage}
            <p class="text-red-500 text-xs italic mb-4 text-center">{errorMessage}</p>
        {:else if successMessage && !errorMessage}
            <p class="text-green-500 text-xs italic mb-4 text-center">{successMessage}</p>
        {:else}
            <div class="mb-6 text-gray-700 dark:text-gray-300 space-y-2">
                <p><strong>Username:</strong> {profile.username}</p>
                <p><strong>Email:</strong> {profile.email}</p>
                <p><strong>Date of Birth:</strong> {profile.dateOfBirth ? new Date(profile.dateOfBirth + 'T00:00:00').toLocaleDateString() : 'N/A'}</p>
                <p><strong>Age:</strong> {profile.age !== undefined && profile.age !== null ? profile.age : 'N/A'}</p>
                <p><strong>Adult:</strong> {profile.adult ? 'Yes' : 'No'}</p>
                <p><strong>Member Since:</strong> {new Date(profile.createdAt).toLocaleDateString()}</p>
            </div>

            <h2 class="text-2xl font-bold text-center mb-4 text-gray-900 dark:text-gray-100">Update Profile</h2>
            <form on:submit|preventDefault={handleSubmit}>
                <div class="mb-4">
                    <label for="email" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Email</label>
                    <input
                        type="email"
                        id="email"
                        bind:value={email}
                        class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
                        placeholder="Enter new email"
                    />
                </div>

                <div class="mb-6">
                    <label for="dateOfBirth" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Date of Birth</label>
                    <input
                        type="date"
                        id="dateOfBirth"
                        bind:value={dateOfBirth}
                        class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 mb-3 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
                        required
                    />
                </div>

                <div class="flex items-center justify-between">
                    <button
                        type="submit"
                        class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline transition-colors duration-300 w-full"
                        disabled={updating}
                    >
                        {#if updating}
                            Updating...
                        {:else}
                            Update Profile
                        {/if}
                    </button>
                </div>
            </form>
        {/if}
    </div>
</div>