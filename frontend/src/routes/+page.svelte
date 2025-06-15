<script>
    import { posts } from '$lib/api';
    import { onMount, setContext } from 'svelte'; // Ensure setContext is imported
    import { authStore } from '$lib/stores/authStore';
    import PostCard from '$lib/components/PostCard.svelte'; // Import PostCard
    import Spinner from '$lib/components/Spinner.svelte'; // Import Spinner

    let postsData = [];
    let loading = true;
    let error = '';
    let currentPage = 0;
    let pageSize = 10;
    let hasMore = true;
    let currentTagFilters = []; // Now an array to hold multiple tags, or empty for all posts

    // Set context for filterByTag function to be accessible by children (PostCard)
    // This allows PostCard to dispatch a tag and HomePage to handle the filtering
    setContext('filterByTag', filterByTag);

    // Function to fetch posts
    async function fetchPosts() {
        if (!hasMore && currentPage > 0) return;

        loading = true;
        error = '';
        try {
            // Pass currentTagFilters (which is an array) to posts.getAll
            const response = await posts.getAll(currentPage, pageSize, currentTagFilters); // CORRECTED: Pass the array
            if (currentPage === 0) {
                postsData = response.data.content;
            } else {
                postsData = [...postsData, ...response.data.content];
            }
            hasMore = !response.data.last;
        } catch (err) {
            console.error('Failed to fetch posts:', err);
            // More robust error handling
            error = err.response?.data?.message || 'Failed to load posts. Please try again later.';
        } finally {
            loading = false;
        }
    }

    // Function to handle tag filtering (called from sidebar and PostCard)
    function filterByTag(event) {
        const tag = typeof event === 'string' ? event : event.detail; // Handle both direct call and CustomEvent

        // Logic to update currentTagFilters array:
        // If tag is empty, clear all filters.
        // Otherwise, if the tag is already in the array, remove it (toggle off).
        // If the tag is not in the array, add it (toggle on).
        if (tag === '') {
            currentTagFilters = []; // Clear all filters
        } else {
            const index = currentTagFilters.indexOf(tag);
            if (index > -1) {
                // Tag already exists, remove it (toggle off)
                currentTagFilters = currentTagFilters.filter(t => t !== tag);
            } else {
                // Tag doesn't exist, add it (toggle on)
                currentTagFilters = [...currentTagFilters, tag];
            }
        }

        currentPage = 0; // Reset pagination
        postsData = []; // Clear current posts
        hasMore = true;
        fetchPosts(); // Refetch posts with the new filters
    }

    // Function to load more posts
    function loadMore() {
        if (!loading && hasMore) {
            currentPage++;
            fetchPosts();
        }
    }

    // Update post data when a PostCard dispatches 'postUpdated' (e.g., after a like/unlike)
    function handlePostUpdated(event) {
        const updatedPost = event.detail;
        postsData = postsData.map(post =>
            post.id === updatedPost.id ? updatedPost : post
        );
    }

    onMount(() => {
        fetchPosts();
    });
</script>

<div class="container mx-auto p-4">
    <h1 class="text-3xl font-bold mb-6 text-gray-800 dark:text-white">
        Latest Posts
        {#if currentTagFilters.length > 0}
            <span class="text-blue-600 dark:text-blue-400">
                (Filtered by: {currentTagFilters.map(tag => `#${tag}`).join(', ')})
            </span>
        {/if}
    </h1>

    {#if error}
        <p class="text-red-500 text-center">{error}</p>
    {:else if postsData.length === 0 && !loading}
        <p class="text-gray-600 dark:text-gray-400 text-center">No posts found matching the current filters.</p>
    {:else}
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {#each postsData as post (post.id)}
                <PostCard {post} on:postUpdated={handlePostUpdated} on:filterByTag={filterByTag} />
            {/each}
        </div>

        {#if loading}
            <div class="mt-4 flex justify-center"><Spinner /></div> {:else if hasMore}
            <div class="text-center mt-4">
                <button
                    on:click={loadMore}
                    class="px-6 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-md transition-colors"
                >
                    Load More
                </button>
            </div>
        {:else}
            <p class="text-center text-gray-600 dark:text-gray-400 mt-4">You've reached the end of the line!</p>
        {/if}
    {/if}
</div>