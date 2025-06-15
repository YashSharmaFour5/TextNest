<script>
    import { onMount } from 'svelte';
    import { authStore } from '$lib/stores/authStore';
    import { goto } from '$app/navigation';
    import { admin } from '$lib/api'; // Import your admin API service
    import Spinner from '$lib/components/Spinner.svelte'; // Assuming you have this component

    let users = [];
    let loading = true;
    let error = '';

    // State for the "Update Roles" modal
    let showRoleModal = false;
    let selectedUser = null;
    let newRoles = new Set(); // Use a Set for roles in the modal

    // Possible roles to display in the modal (adjust based on your backend)
    const availableRoles = ['ROLE_USER', 'ROLE_ADMIN', 'ROLE_MODERATOR']; // Add other roles if you have them

    // Reactive statements for authentication and authorization
    $: user = $authStore.user;
    $: isAuthenticated = $authStore.isAuthenticated;
    $: isAdmin = user && user.roles.includes('ROLE_ADMIN');

    /**
     * Fetches the list of all users from the backend.
     */
    async function fetchUsers() {
        loading = true;
        error = '';
        try {
            // Only fetch if authenticated and admin. The backend will also enforce this.
            if (!isAdmin) {
                console.warn('Attempted to fetch admin data without ADMIN role.');
                error = 'Access Denied: You do not have administrator privileges.';
                goto('/login'); // Redirect if not admin
                return;
            }
            const response = await admin.getAllUsers();
            users = response.data; // Axios responses have data nested under .data
        } catch (err) {
            console.error('Failed to fetch users for admin dashboard:', err.response?.data || err.message);
            error = 'Failed to load user data. Access denied or server error.';
            // Specifically handle 403 Forbidden
            if (err.response && err.response.status === 403) {
                authStore.logout(); // Clear potentially stale token
                goto('/login'); // Redirect to login if forbidden
            }
        } finally {
            loading = false;
        }
    }

    /**
     * Handles the deletion of a user.
     * @param {string} userId The ID of the user to delete.
     */
    async function handleDeleteUser(userId) {
        if (!confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
            return;
        }

        loading = true; // Show loading spinner during deletion
        try {
            await admin.deleteUser(userId);
            // Remove the deleted user from the local list
            users = users.filter(u => u.id !== userId);
            alert('User deleted successfully!');
        } catch (err) {
            console.error('Failed to delete user:', err.response?.data || err.message);
            error = `Failed to delete user: ${err.response?.data?.message || err.message}`;
            if (err.response && err.response.status === 403) {
                authStore.logout();
                goto('/login');
            }
        } finally {
            loading = false;
        }
    }

    /**
     * Opens the role update modal for a specific user.
     * @param {object} user The user object to edit roles for.
     */
    function openRoleModal(userToEdit) {
        selectedUser = userToEdit;
        // Initialize newRoles with the user's current roles
        newRoles = new Set(userToEdit.roles);
        showRoleModal = true;
    }

    /**
     * Closes the role update modal.
     */
    function closeRoleModal() {
        showRoleModal = false;
        selectedUser = null;
        newRoles = new Set();
        error = ''; // Clear any previous errors from the modal
    }

    /**
     * Handles the change in role checkboxes within the modal.
     * @param {string} role The role string (e.g., 'ROLE_USER').
     * @param {boolean} checked Whether the checkbox is checked.
     */
    function handleRoleChange(role, checked) {
        if (checked) {
            newRoles.add(role);
        } else {
            newRoles.delete(role);
        }
        // Ensure newRoles is reactive for Svelte
        newRoles = new Set(newRoles);
    }

    /**
     * Submits the updated roles for the selected user.
     */
    async function handleUpdateRoles() {
        if (!selectedUser) return;

        // Basic validation: ensure at least one role is selected
        if (newRoles.size === 0) {
            alert('A user must have at least one role.');
            return;
        }

        loading = true; // Show loading spinner during update
        try {
            // Convert Set to Array for the API call
            const updatedUser = await admin.updateUserRoles(selectedUser.id, Array.from(newRoles));
            // Update the user in the local 'users' array
            users = users.map(u => (u.id === updatedUser.data.id ? updatedUser.data : u));
            alert(`Roles for ${selectedUser.username} updated successfully!`);
            closeRoleModal();
        } catch (err) {
            console.error('Failed to update user roles:', err.response?.data || err.message);
            error = `Failed to update roles: ${err.response?.data?.message || err.message}`;
            if (err.response && err.response.status === 403) {
                authStore.logout();
                goto('/login');
            }
        } finally {
            loading = false;
        }
    }

    onMount(() => {
        // Initial check for admin status before fetching data
        if (!isAdmin) {
            console.warn('Redirecting: User is not an admin.');
            goto('/login'); // Redirect immediately if not admin
            return;
        }
        fetchUsers();
    });
</script>

<div class="container mx-auto p-6 bg-white dark:bg-gray-800 rounded-lg shadow-lg my-8 transition-colors duration-300">
    <h1 class="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-8 text-center">Admin Dashboard</h1>

    {#if error}
        <p class="text-red-600 dark:text-red-400 text-center text-lg">{error}</p>
    {:else if loading && users.length === 0} <div class="flex justify-center items-center h-48">
            <Spinner />
            <p class="ml-2 text-gray-600 dark:text-gray-400">Loading user data...</p>
        </div>
    {:else if users.length === 0}
        <p class="text-gray-600 dark:text-gray-400 text-center">No users found.</p>
    {:else}
        <div class="overflow-x-auto shadow-md rounded-lg">
            <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                <thead class="bg-gray-100 dark:bg-gray-700">
                    <tr>
                        <th class="py-3 px-6 text-left text-xs font-medium text-gray-600 dark:text-gray-300 uppercase tracking-wider">Username</th>
                        <th class="py-3 px-6 text-left text-xs font-medium text-gray-600 dark:text-gray-300 uppercase tracking-wider">Email</th>
                        <th class="py-3 px-6 text-left text-xs font-medium text-gray-600 dark:text-gray-300 uppercase tracking-wider">Age</th>
                        <th class="py-3 px-6 text-left text-xs font-medium text-gray-600 dark:text-gray-300 uppercase tracking-wider">Roles</th>
                        <th class="py-3 px-6 text-left text-xs font-medium text-gray-600 dark:text-gray-300 uppercase tracking-wider">Member Since</th>
                        <th class="py-3 px-6 text-center text-xs font-medium text-gray-600 dark:text-gray-300 uppercase tracking-wider">Actions</th>
                    </tr>
                </thead>
                <tbody class="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                    {#each users as user (user.id)}
                        <tr class="hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors duration-200">
                            <td class="py-4 px-6 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-gray-100">{user.username}</td>
                            <td class="py-4 px-6 whitespace-nowrap text-sm text-gray-700 dark:text-gray-300">{user.email}</td>
                            <td class="py-4 px-6 whitespace-nowrap text-sm text-gray-700 dark:text-gray-300">{user.age}</td>
                            <td class="py-4 px-6 whitespace-nowrap text-sm text-gray-700 dark:text-gray-300">
                                {#if user.roles}
                                    {user.roles.join(', ')}
                                {:else}
                                    N/A
                                {/if}
                            </td>
                            <td class="py-4 px-6 whitespace-nowrap text-sm text-gray-700 dark:text-gray-300">{new Date(user.createdAt).toLocaleDateString()}</td>
                            <td class="py-4 px-6 whitespace-nowrap text-center text-sm font-medium space-x-2">
                                <button
                                    on:click={() => openRoleModal(user)}
                                    class="text-indigo-600 hover:text-indigo-900 dark:text-indigo-400 dark:hover:text-indigo-600"
                                >
                                    Edit Roles
                                </button>
                                <button
                                    on:click={() => handleDeleteUser(user.id)}
                                    class="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-600"
                                >
                                    Delete
                                </button>
                            </td>
                        </tr>
                    {/each}
                </tbody>
            </table>
        </div>
    {/if}

    {#if loading && users.length > 0} <div class="mt-4 flex justify-center"><Spinner /></div>
    {/if}

    {#if showRoleModal}
        <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
            <div class="bg-white dark:bg-gray-800 p-8 rounded-lg shadow-xl w-full max-w-md transition-colors duration-300">
                <h2 class="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-6 text-center">Update Roles for {selectedUser?.username}</h2>

                <div class="space-y-4 mb-6">
                    {#each availableRoles as role (role)}
                        <label class="flex items-center text-gray-800 dark:text-gray-200 cursor-pointer">
                            <input
                                type="checkbox"
                                value={role}
                                checked={newRoles.has(role)}
                                on:change={(e) => handleRoleChange(role, e.target.checked)}
                                class="form-checkbox h-5 w-5 text-blue-600 dark:text-blue-400 rounded focus:ring-blue-500 transition-colors duration-200"
                            />
                            <span class="ml-3 text-lg">{role.replace('ROLE_', '')}</span>
                        </label>
                    {/each}
                </div>

                {#if error}
                    <p class="text-red-600 dark:text-red-400 text-center mb-4">{error}</p>
                {/if}

                <div class="flex justify-end space-x-3">
                    <button
                        on:click={closeRoleModal}
                        class="px-4 py-2 bg-gray-300 hover:bg-gray-400 dark:bg-gray-600 dark:hover:bg-gray-700 text-gray-800 dark:text-gray-200 rounded-md transition-colors"
                    >
                        Cancel
                    </button>
                    <button
                        on:click={handleUpdateRoles}
                        class="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md transition-colors"
                        disabled={loading}
                    >
                        {#if loading}
                            <Spinner size="sm" /> Updating...
                        {:else}
                            Update Roles
                        {/if}
                    </button>
                </div>
            </div>
        </div>
    {/if}
</div>