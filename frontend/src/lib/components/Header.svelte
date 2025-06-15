<script>
    import { authStore } from '$lib/stores/authStore';
    import { themeStore } from '$lib/stores/themeStore';
    import { goto } from '$app/navigation'; // For programmatic navigation

    let showUserDropdown = false;
    let searchTerm = ''; // State variable for the search input

    // Reactive variable to easily check if the current user is an admin
    $: isAdmin = $authStore.user && $authStore.user.roles && $authStore.user.roles.includes('ROLE_ADMIN');

    function toggleTheme() {
        themeStore.update(currentTheme => (currentTheme === 'dark' ? 'light' : 'dark'));
    }

    function handleLogout() {
        authStore.logout();
        showUserDropdown = false;
        goto('/login'); // Redirect to login page after logout
    }

    // Function to handle the search submission
    function handleSearch() {
        if (searchTerm.trim()) { // Ensure the search term isn't empty
            // Navigate to a search results page, passing the query as a URL parameter
            // The /search page will then fetch posts based on this 'q' parameter.
            goto(`/search?q=${encodeURIComponent(searchTerm.trim())}`);
            searchTerm = ''; // Clear the search bar after submission
        }
    }

    // Function to close dropdown when clicking outside
    function handleClickOutside(event) {
        // Check if the click is outside the dropdown's parent (.relative div)
        // We add a check for event.target.closest('.user-dropdown-container') to ensure clicks
        // inside the dropdown button itself don't close it immediately.
        if (showUserDropdown && !event.target.closest('.user-dropdown-container')) {
            showUserDropdown = false;
        }
    }

    function navigateToAdminDashboard() {
        goto('/admin/dashboard');
        showUserDropdown = false; // Close dropdown after navigation
    }

</script>

<svelte:window on:click={handleClickOutside} />

<header class="bg-white dark:bg-gray-800 text-white p-4 sticky top-0 z-10 transition-colors duration-300">
    <div class="container mx-auto flex justify-between items-center">
        <div class="flex items-center space-x-2">
            <a href="/"><img src="/favicon.png" alt="Logo" class="h-13 w-13 hover:opacity-80" /></a>
            <a href="/" class="text-black dark:text-white text-2xl font-bold hover:opacity-80 transition-opacity">TExTNest</a>
        </div>
        
        <div class="flex-grow mx-4 max-w-md">
            <form on:submit|preventDefault={handleSearch} class="flex">
                <input
                    type="text"
                    bind:value={searchTerm}
                    placeholder="Search posts or tags..."
                    class="font-bold flex-grow h-10 p-2 rounded-l-md text-gray-900 focus:outline-none bg-gray-200 dark:bg-gray-700 dark:text-white dark:placeholder-gray-400"
                />
                <button
                    type="submit"
                    class="h-10 bg-gray-200 dark:bg-gray-700 p-2 rounded-r-md hover:bg-gray-500 dark:hover:bg-gray-500 transition-colors"
                    aria-label="Search"
                >
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                </button>
            </form>
        </div>

        <nav class="flex items-center space-x-4">
            <a href="/" class="font-bold text-black dark:text-white hover:text-blue-600 dark:hover:text-gray-300 transition-colors">Home</a>
            <a href="/submit" class="font-bold text-black dark:text-white hover:text-blue-500 dark:hover:text-gray-300 transition-colors">Create Post</a>
            
{#if $authStore.isAuthenticated}
                <div class="relative user-dropdown-container"> <button on:click={() => showUserDropdown = !showUserDropdown} class="flex items-center space-x-2 bg-blue-700 dark:bg-gray-700 py-2 px-3 rounded-md hover:bg-blue-800 dark:hover:bg-gray-700 transition-colors">
                        <span class="font-semibold">{ $authStore.user ? $authStore.user.username : 'Loading...' }</span>
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
                    </button>

                    {#if showUserDropdown}
                        <div class="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-700 rounded-md shadow-lg py-1 z-20">
                            <a href="/profile" class="block px-4 py-2 text-gray-800 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-600" on:click={() => showUserDropdown = false}>Profile</a>
                            
                            {#if isAdmin}
                                <button on:click={navigateToAdminDashboard} class="block w-full text-left px-4 py-2 text-blue-600 dark:text-blue-300 hover:bg-gray-100 dark:hover:bg-gray-600">Admin Dashboard</button>
                            {/if}

                            <button on:click={handleLogout} class="block w-full text-left px-4 py-2 text-red-600 hover:bg-gray-100 dark:hover:bg-gray-600">Logout</button>
                        </div>
                    {/if}
                </div>
            {:else}
                <a href="/login" class="font-bold bg-blue-700 dark:bg-gray-700 py-2 px-4 rounded-md hover:bg-blue-800 dark:hover:bg-gray-700 transition-colors">Login</a>
                <a href="/signup" class="font-bold bg-blue-700 dark:bg-gray-700 py-2 px-4 rounded-md hover:bg-blue-800 dark:hover:bg-gray-700 transition-colors">Sign Up</a>
            {/if}
        </nav>
    </div>
</header>