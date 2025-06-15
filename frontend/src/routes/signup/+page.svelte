<script>
    import { authStore } from '$lib/stores/authStore';
    import { goto } from '$app/navigation';

    let username = '';
    let email = '';
    let password = '';
    let dateOfBirth = ''; // Changed from 'age' to 'dateOfBirth'
    let errorMessage = '';
    let successMessage = '';
    let isLoading = false;

    async function handleSignup() {
        errorMessage = '';
        successMessage = '';
        isLoading = true;

        // Construct userData to send dateOfBirth instead of age
        // The dateOfBirth should already be in 'YYYY-MM-DD' format from the date input
        const userData = { username, email, password, dateOfBirth };
        
        // Ensure dateOfBirth is not empty before sending, as the backend expects it.
        // The `required` attribute on the input field helps, but a final check is good.
        if (!dateOfBirth) {
            errorMessage = 'Please enter your date of birth.';
            isLoading = false;
            return;
        }

        const result = await authStore.signupUser(userData);

        if (result.success) {
            successMessage = result.message + " Redirecting to login...";
            setTimeout(() => {
                goto('/login'); // Redirect to login after successful signup
            }, 2000);
        } else {
            errorMessage = result.message;
        }
        isLoading = false;
    }
</script>

<div class="flex items-center justify-center min-h-[calc(100vh-160px)]">
    <div class="bg-white dark:bg-gray-800 p-8 rounded-lg outline-2 dark:outline-none shadow-lg w-full max-w-md transition-colors duration-300">
        <h1 class="text-3xl font-bold text-center mb-6 text-gray-900 dark:text-gray-100">Sign Up</h1>

        <form on:submit|preventDefault={handleSignup}>
            <div class="mb-4">
                <label for="username" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Username</label>
                <input
                    type="text"
                    id="username"
                    bind:value={username}
                    class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
                    placeholder="Choose a username"
                    required
                    minlength="3"
                    maxlength="20"
                />
            </div>

            <div class="mb-4">
                <label for="email" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Email</label>
                <input
                    type="email"
                    id="email"
                    bind:value={email}
                    class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
                    placeholder="Enter your email"
                    required
                    maxlength="50"
                />
            </div>

            <div class="mb-4">
                <label for="password" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Password</label>
                <input
                    type="password"
                    id="password"
                    bind:value={password}
                    class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 mb-3 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
                    placeholder="Create a strong password"
                    required
                    minlength="6"
                    maxlength="40"
                />
            </div>

            <div class="mb-6">
                <label for="dateOfBirth" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Date of Birth</label>
                <input
                    type="date"
                    id="dateOfBirth"
                    bind:value={dateOfBirth}
                    class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
                    required
                />
            </div>

            {#if errorMessage}
                <p class="text-red-500 text-xs italic mb-4 text-center">{errorMessage}</p>
            {/if}
            {#if successMessage}
                <p class="text-green-500 text-xs italic mb-4 text-center">{successMessage}</p>
            {/if}

            <div class="flex items-center justify-between">
                <button
                    type="submit"
                    class="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline transition-colors duration-300 w-full"
                    disabled={isLoading}
                >
                    {#if isLoading}
                        Signing up...
                    {:else}
                        Sign Up
                    {/if}
                </button>
            </div>
            <p class="mt-4 text-center text-gray-600 dark:text-gray-300">
                Already have an account? <a href="/login" class="text-blue-500 hover:underline">Login</a>
            </p>
        </form>
    </div>
</div>