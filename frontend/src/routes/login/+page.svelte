<script>
    import { authStore } from '$lib/stores/authStore';
    import { goto } from '$app/navigation'; // For redirection after login

    let username = '';
    let password = '';
    let errorMessage = '';
    let isLoading = false;

    async function handleLogin() {
        errorMessage = ''; // Clear previous errors
        isLoading = true;
        const result = await authStore.login(username, password);
        if (result.success) {
            goto('/'); // Redirect to home page on success
        } else {
            errorMessage = result.message;
        }
        isLoading = false;
    }
</script>

<div class="flex items-center justify-center min-h-[calc(100vh-160px)]">
    <div class="bg-white dark:bg-gray-800 p-8 rounded-lg outline-2 dark:outline-none shadow-lg w-full max-w-md transition-colors duration-300">
        <h1 class="text-3xl font-bold text-center mb-6 text-gray-900 dark:text-gray-100">Login</h1>

        <form on:submit|preventDefault={handleLogin}>
            <div class="mb-4">
                <label for="username" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Username</label>
                <input
                    type="text"
                    id="username"
                    bind:value={username}
                    class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
                    placeholder="Enter your username"
                    required
                />
            </div>

            <div class="mb-6">
                <label for="password" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Password</label>
                <input
                    type="password"
                    id="password"
                    bind:value={password}
                    class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 mb-3 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
                    placeholder="******************"
                    required
                />
            </div>

            {#if errorMessage}
                <p class="text-red-500 text-xs italic mb-4 text-center">{errorMessage}</p>
            {/if}

            <div class="flex items-center justify-between">
                <button
                    type="submit"
                    class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline transition-colors duration-300 w-full"
                    disabled={isLoading}
                >
                    {#if isLoading}
                        Logging in...
                    {:else}
                        Login
                    {/if}
                </button>
            </div>
            <p class="mt-4 text-center text-gray-600 dark:text-gray-300">
                Don't have an account? <a href="/signup" class="text-blue-500 hover:underline">Sign up</a>
            </p>
        </form>
    </div>
</div>