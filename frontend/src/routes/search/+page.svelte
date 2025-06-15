<script>
    import { page } from '$app/stores';
    import { onMount, onDestroy } from 'svelte'; // onDestroy might be useful for cleanup, but not strictly needed here
    import { posts } from '$lib/api';
    import PostCard from '$lib/components/PostCard.svelte';
    import Spinner from '$lib/components/Spinner.svelte';

    let query = '';
    let searchResults = [];
    let loading = true;
    let error = null;

    // --- Key Change: Use a reactive Svelte statement for $page.url.searchParams ---
    // This block of code will re-run whenever $page.url.searchParams changes.
    $: {
        const newQuery = $page.url.searchParams.get('q') || '';
        if (newQuery !== query) { // Only re-fetch if the query term has actually changed
            query = newQuery;
            if (query) {
                fetchSearchResults();
            } else {
                loading = false;
                searchResults = [];
            }
        }
    }
    // --- End Key Change ---

    async function fetchSearchResults() {
        loading = true;
        error = null;
        try {
            const response = await posts.search(query, 0, 20); // Using page 0, size 20
            searchResults = response.data.content;
        } catch (err) {
            console.error('Error fetching search results:', err);
            if (err.response && err.response.data && err.response.data.message) {
                error = `Failed to load search results: ${err.response.data.message}`;
            } else {
                error = 'Failed to load search results. Please try again.';
            }
        } finally {
            loading = false;
        }
    }

    // You can keep onMount for initial data fetching or other setup if needed,
    // but the reactive statement handles query changes.
    // In this specific case, the reactive statement makes onMount redundant for fetching.
    onMount(() => {
        // If you were to remove the $: block, you'd fetch here:
        // if (query) { fetchSearchResults(); } else { loading = false; }
        // But with the $: block, the initial query is handled by it too.
    });
</script>

<div class="container mx-auto p-4">
    <h1 class="text-3xl font-bold mb-6 text-gray-800 dark:text-white">
        Search Results for: "<span class="text-blue-600 dark:text-blue-400">{query}</span>"
    </h1>

    {#if loading}
        <div class="mt-4 flex justify-center"><Spinner /></div>
    {:else if error}
        <p class="text-red-600 dark:text-red-400 text-center">{error}</p>
    {:else if searchResults.length > 0}
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {#each searchResults as post (post.id)}
                <PostCard {post} />
            {/each}
        </div>
    {:else}
        <p class="text-gray-600 dark:text-gray-400 text-center">No posts found matching your search query.</p>
    {/if}
</div>  