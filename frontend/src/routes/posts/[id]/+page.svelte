<script>
    import { posts, comments } from '$lib/api'; // Import comments API
    import { onMount } from 'svelte';
    import { page } from '$app/stores';
    import { authStore } from '$lib/stores/authStore';
    import Spinner from '$lib/components/Spinner.svelte';
    import { goto } from '$app/navigation'; // Import goto for redirection
    import axios from 'axios';

    let postId = $page.params.id;
    let post = null;
    let loading = true;
    let error = '';
    let successMessage = '';
    let editing = false; // Controls edit form visibility
    let updating = false; // State for update operation
    let deleting = false; // NEW: State for delete operation

    // --- Edit Form Data ---
    let editedTitle = '';
    let editedContent = '';
    let editedTags = [];
    let editedIsNsfw = false;
    // --- End Edit Form Data ---

    // --- Comments Feature Variables ---
    let commentsList = [];
    let commentText = '';
    let submittingComment = false;
    let commentsError = '';
    // --- End Comments Feature Variables ---

    const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

    async function fetchPost() {
        loading = true;
        error = '';
        try {
            const response = await posts.getById(postId);
            post = response.data;
             // Initialize edit form data
            editedTitle = post.title;
            editedContent = post.content;
            editedTags = post.tags || [];
            editedIsNsfw = post.isNsfw || false;
            // After fetching the post, fetch its comments
            await fetchComments();
        } catch (err) {
            console.error('Failed to fetch post:', err);
            if (err.response && err.response.status === 404) {
                error = 'Post not found.';
            } else {
                error = 'Failed to load post. It might not exist or there was a network error.';
            }
        } finally {
            loading = false;
        }
    }

    async function toggleLike() {
        if (!$authStore.isAuthenticated) {
            goto('/login');
            return;
        }
        try {
            const response = await posts.toggleLike(post.id);
            post = response.data; // Update post data with the response
        } catch (err) {
            console.error('Failed to toggle like:', err);
            alert('Failed to like/unlike post.');
        }
    }

    async function handleUpdatePost() {
        if (!$authStore.isAuthenticated) {
            goto('/login');
            return;
        }

        updating = true;
        error = '';
        successMessage = '';

        try {
            const token = $authStore.token;
            const updatePayload = {
                title: editedTitle,
                content: editedContent,
                tags: editedTags,
                isNsfw: editedIsNsfw
            };

            const response = await axios.put(`${API_BASE_URL}/posts/${postId}`, updatePayload, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            post = response.data; // Update local post data
            editing = false; // Hide edit form
            successMessage = 'Post updated successfully!';
            // No need to fetchPost again, as `post` is updated directly.
            // If there were other dependent data, a refetch might be needed.
        } catch (err) {
            console.error('Failed to update post:', err);
            if (axios.isAxiosError(err) && err.response) {
                error = err.response.data.message || 'Failed to update post.';
            } else {
                error = 'Network error occurred.';
            }
        } finally {
            updating = false;
        }
    }

    function toggleEdit() {
        editing = !editing;
        if (editing) {
            // Populate form fields with current post data
            editedTitle = post.title;
            editedContent = post.content;
            editedTags = post.tags || [];
            editedIsNsfw = post.isNsfw;
        }
    }

    function handleTagInputChange(event) {
        editedTags = event.target.value.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);
    }

    function handleCommentInputChange(event) {
        commentText = event.target.value;
    }

    function handleNsfwChange(event) {
        editedIsNsfw = event.target.checked;
    }

    // --- NEW: Delete Post Function ---
    async function handleDeletePost() {
        if (!$authStore.isAuthenticated) {
            goto('/login');
            return;
        }

        const confirmDelete = confirm('Are you sure you want to delete this post? This action cannot be undone.');

        if (!confirmDelete) {
            return;
        }

        deleting = true;
        error = '';
        successMessage = '';

        try {
            const token = $authStore.token;
            await axios.delete(`${API_BASE_URL}/posts/${postId}`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            successMessage = 'Post deleted successfully! Redirecting...';
            // Redirect to home page or posts list after successful deletion
            setTimeout(() => {
                goto('/');
            }, 2000); // Redirect after 2 seconds to show success message
        } catch (err) {
            console.error('Failed to delete post:', err);
            if (axios.isAxiosError(err) && err.response) {
                error = err.response.data.message || 'Failed to delete post.';
            } else {
                error = 'Network error occurred.';
            }
        } finally {
            deleting = false;
        }
    }
    // --- End Delete Post Function ---

    // --- Comments Feature Functions ---
    async function fetchComments() {
        commentsError = ''; // Clear previous comment errors
        try {
            const response = await comments.getByPostId(postId);
            commentsList = response.data; // Assuming response.data is an array of comments
        } catch (err) {
            console.error('Failed to fetch comments:', err);
            commentsError = 'Failed to load comments.';
        }
    }

    async function submitComment() {
        if (!commentText.trim()) {
            commentsError = 'Comment cannot be empty.';
            return;
        }
        if (!$authStore.isAuthenticated) {
            goto('/login');
            return;
        }

        submittingComment = true;
        commentsError = ''; // Clear previous errors

        try {
            const response = await comments.create(postId, commentText);
            // Assuming the backend returns the newly created comment
            commentsList = [response.data, ...commentsList]; // Add new comment to the top
            commentText = ''; // Clear the input field
        } catch (err) {
            console.error('Failed to submit comment:', err);
            if (err.response && err.response.data && err.response.data.message) {
                commentsError = `Failed to submit comment: ${err.response.data.message}`;
            } else {
                commentsError = 'Failed to submit comment. Please try again.';
            }
        } finally {
            submittingComment = false;
        }
    }

    // Function to handle comment deletion
    async function handleDeleteComment(commentId, authorId) {
        if (!$authStore.isAuthenticated) {
            goto('/login');
            return;
        }

        // Frontend authorization check for immediate UI feedback
        // The backend will *always* re-verify this.
        const currentUser = $authStore.user;
        const isAuthor = currentUser && currentUser.id === authorId;
        const isAdmin = currentUser && currentUser.roles.includes('ROLE_ADMIN');

        if (!isAuthor && !isAdmin) {
            alert('You are not authorized to delete this comment.');
            return;
        }

        const confirmDelete = confirm('Are you sure you want to delete this comment?');
        if (!confirmDelete) {
            return;
        }

        try {
            await comments.delete(commentId);
            // Remove the deleted comment from the commentsList to update UI
            commentsList = commentsList.filter(comment => comment.id !== commentId);
            // Optionally, show a temporary success message to the user
            // successMessage = 'Comment deleted successfully!';
            // setTimeout(() => successMessage = '', 3000); // Clear message after 3 seconds
        } catch (err) {
            console.error('Failed to delete comment:', err);
            // Display an error message to the user
            commentsError = err.response?.data?.message || 'Failed to delete comment.';
            // Optionally, clear the error message after a few seconds
            // setTimeout(() => commentsError = '', 5000);
        }
    }

    // --- End Comments Feature Functions ---

    onMount(() => {
        fetchPost();
    });
</script>

<div class="max-w-3xl mx-auto">
    {#if loading}
        <div class="p-8 bg-white dark:bg-gray-800 rounded-lg shadow-md text-center">
            <Spinner />
            <p class="mt-2 text-gray-600 dark:text-gray-400">Loading post...</p>
        </div>
    {:else if error && !successMessage}
        <div class="p-8 bg-red-100 dark:bg-red-900 text-red-700 dark:text-red-200 rounded-lg shadow-md text-center">
            <p>{error}</p>
        </div>
    {:else if successMessage && !error}
        <div class="p-8 bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-200 rounded-lg shadow-md text-center">
            <p>{successMessage}</p>
        </div>
    {:else if post}
        <div class="bg-white dark:bg-gray-800 p-8 rounded-lg shadow-lg mb-8 transition-colors duration-300">
            {#if editing}
                <h2 class="text-2xl font-bold mb-4 text-gray-900 dark:text-gray-100">Edit Post</h2>
                <form on:submit|preventDefault={handleUpdatePost} class="mb-6">
                    <div class="mb-4">
                        <label for="editedTitle" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Title</label>
                        <input type="text" id="editedTitle" bind:value={editedTitle} required class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600" />
                    </div>
                    <div class="mb-4">
                        <label for="editedContent" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Content</label>
                        <textarea id="editedContent" bind:value={editedContent} rows="4" class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600" required></textarea>
                    </div>
                     <div class="mb-4">
                        <label for="editedTags" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Tags (comma-separated)</label>
                        <input
                            type="text"
                            id="editedTags"
                            value={editedTags.join(', ')}
                            on:input={handleTagInputChange}
                            class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
                            placeholder="tag1, tag2, tag3"
                        />
                    </div>
                    <div class="flex justify-end space-x-2">
                        <button type="button" on:click={toggleEdit} class="px-4 py-2 text-gray-700 dark:text-gray-300 rounded-md hover:bg-gray-200 dark:hover:bg-gray-700 focus:outline-none">Cancel</button>
                        <button type="submit" class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none" disabled={updating}>
                            {updating ? 'Updating...' : 'Update Post'}
                        </button>
                    </div>
                </form>
            {:else}
                <h1 class="text-3xl font-bold mb-4 text-gray-900 dark:text-gray-100">{post.title}</h1>

                {#if post.isNsfw && (!$authStore.user || !$authStore.user.isAdult)}
                    <div class="bg-red-100 dark:bg-red-900 border border-red-400 text-red-700 dark:text-red-200 px-4 py-3 rounded relative mb-4">
                        <span class="block sm:inline">⚠️ This content is 18+. Log in as an adult to view.</span>
                    </div>
                {:else}
                    {#if post.content}
                        <p class="text-gray-700 dark:text-gray-300 mb-6 whitespace-pre-wrap">{post.content}</p>
                    {/if}

                    {#if post.mediaUrls && post.mediaUrls.length > 0}
                        <div class="grid grid-cols-1 gap-4 mb-6">
    {#each post.mediaUrls as url}
        {#if url.match(/\.(jpeg|jpg|gif|png|webp)$/i)}
            <img src={url} alt="Post media" class="w-full h-auto rounded-md object-cover" />
        {:else if url.match(/\.(mp4|webm|ogg)$/i)}
            <video controls class="w-full h-auto rounded-md">
                <source src={url} type="video/mp4" />
                <track kind="captions" label="Captions" />
                Your browser does not support the video tag.
            </video>
        {:else if (url.includes("youtube.com/embed/") || url.includes("youtu.be/") || url.includes("youtube.com/watch?v="))}
            {#if url.match(/(?:youtube\.com\/(?:[^\/]+\/.+\/|\/(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([a-zA-Z0-9_-]{11})/)}
                {#key url}
                    <div class="aspect-w-16 aspect-h-9 w-full">
                        <iframe
                            src={`https://www.youtube.com/embed/${url.match(/(?:youtube\.com\/(?:[^\/]+\/.+\/|\/(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/)([a-zA-Z0-9_-]{11})/)?.[1]}`}
                            frameborder="0"
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                            allowfullscreen
                            title="Embedded video"
                            class="w-full h-full rounded-md"></iframe>
                    </div>
                {/key}
            {:else}
                <a href={url} target="_blank" rel="noopener noreferrer" class="text-blue-500 hover:underline">View YouTube Link</a>
            {/if}
        {:else}
            <a href={url} target="_blank" rel="noopener noreferrer" class="text-blue-500 hover:underline">View Document/Link</a>
        {/if}
    {/each}
</div>
                    {/if}

                    <div class="flex items-center justify-between text-gray-600 dark:text-gray-400 text-sm mb-4">
                        <span>Posted by <span class="font-semibold">{post.author?.username || 'Unknown'}</span> on {new Date(post.createdAt).toLocaleDateString('en-GB')}</span>
                    </div>

                    <div class="flex items-center space-x-4">
                        <button
                            on:click={toggleLike}
                            class="flex items-center space-x-1 px-3 py-1 rounded-full
                            {post.likedBy?.includes($authStore.user?.id) ? 'bg-blue-200 text-blue-700 dark:bg-blue-700 dark:text-blue-200' : 'bg-gray-200 text-gray-700 dark:bg-gray-700 dark:text-gray-300'}
                            hover:bg-blue-300 dark:hover:bg-blue-600 transition-colors"
                        >
                            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clip-rule="evenodd"></path></svg>
                            <span>{post.likeCount} Likes</span>
                        </button>
                        {#if $authStore.isAuthenticated && $authStore.user && post.author}
                            {#if $authStore.user.id === post.author.id}
                                <button on:click={toggleEdit} class="px-4 py-2 bg-blue-500 hover:bg-blue-700 text-white font-bold rounded">
                                    {editing ? 'Cancel Edit' : 'Edit Post'}
                                </button>
                            {/if}

                            {#if $authStore.user.id === post.author.id || $authStore.user.roles.includes('ROLE_ADMIN')}
                                <button on:click={handleDeletePost} disabled={deleting} class="px-4 py-2 bg-red-600 hover:bg-red-700 text-white font-bold rounded">
                                    {deleting ? 'Deleting...' : 'Delete Post'}
                                </button>
                            {/if}
                        {/if} 
                    </div>

                    {#if post.tags && post.tags.length > 0}
                        <div class="mt-4 text-sm text-gray-500 dark:text-gray-400">
                            Tags:
                            {#each post.tags as tag}
                                <span class="inline-block bg-gray-200 dark:bg-gray-700 rounded-full px-3 py-1 text-xs font-semibold text-gray-700 dark:text-gray-300 mr-2 mb-2">#{tag}</span>
                            {/each}
                        </div>
                    {/if}
                {/if}
            {/if}
        </div>

        <div class="bg-white dark:bg-gray-800 p-8 rounded-lg shadow-lg transition-colors duration-300">
            <h2 class="text-2xl font-bold mb-4 text-gray-900 dark:text-gray-100">Comments ({commentsList.length})</h2>

            {#if commentsError}
                <p class="text-red-600 dark:text-red-400 mb-4">{commentsError}</p>
            {/if}

            {#if $authStore.isAuthenticated}
                <form on:submit|preventDefault={submitComment} class="mb-6">
                    <textarea
                        bind:value={commentText}
                        on:input={handleCommentInputChange}
                        placeholder="Write a comment..."
                        rows="3"
                        class="w-full p-3 border border-gray-300 dark:border-gray-600 rounded-md bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500 dark:focus:ring-blue-400"
                        aria-label="Write a comment"
                    ></textarea>
                    <button
                        type="submit"
                        class="mt-3 px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 dark:bg-blue-500 dark:hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors duration-200"
                        disabled={submittingComment}
                    >
                        {#if submittingComment}
                            Submitting...
                        {:else}
                            Post Comment
                        {/if}
                    </button>
                </form>
            {:else}
                <p class="text-gray-600 dark:text-gray-400 mb-4">
                    <a href="/login" class="text-blue-600 dark:text-blue-400 hover:underline">Log in</a> to post a comment.
                </p>
            {/if}

            {#if commentsList.length > 0}
                <div class="space-y-4">
                    {#each commentsList as comment (comment.id)}
                        <div class="bg-gray-50 dark:bg-gray-700 p-4 rounded-md shadow-sm">
                            <div class="flex items-center mb-2">
                                <div class="w-7 h-7 bg-purple-400 rounded-full flex items-center justify-center text-white text-xs font-bold mr-2">
                                    {comment.authorUsername.charAt(0).toUpperCase()}
                                </div>
                                <p class="text-sm text-gray-800 dark:text-gray-200 font-semibold">{comment.authorUsername}</p>
                                <p class="text-xs text-gray-500 dark:text-gray-400 ml-auto">
                                    {new Date(comment.createdAt).toLocaleString('en-GB')}
                                </p>

                                {#if $authStore.isAuthenticated && $authStore.user && (comment.author.id === $authStore.user.id || $authStore.user.roles.includes('ROLE_ADMIN'))}
                                    <button
                                        on:click={() => handleDeleteComment(comment.id, comment.author.id)}
                                        class="ml-2 text-red-500 hover:text-red-700 dark:text-red-400 dark:hover:text-red-500 focus:outline-none"
                                        aria-label="Delete comment"
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                                            <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm6 0a1 1 0 01-2 0v6a1 1 0 112 0V8z" clip-rule="evenodd" />
                                        </svg>
                                    </button>
                                {/if}
                            </div>
                            <p class="text-gray-700 dark:text-gray-300 text-base whitespace-pre-wrap">{comment.content}</p>
                        </div>
                    {/each}
                </div>
            {:else}
                {#if !loading}
                    <p class="text-gray-600 dark:text-gray-400">No comments yet. Be the first to comment!</p>
                {/if}
            {/if}
        </div>
        {:else}
        <div class="p-8 bg-yellow-100 dark:bg-yellow-900 text-yellow-700 dark:text-yellow-200 rounded-lg shadow-md text-center">
            <p>No post found.</p>
        </div>
    {/if}
</div>