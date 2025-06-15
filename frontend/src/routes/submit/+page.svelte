<script>
    import { posts } from '$lib/api';
    import { goto } from '$app/navigation';
    import { authStore } from '$lib/stores/authStore';

    let title = '';
    let content = '';
    let mediaFiles = [];
    let tagsInput = ''; // Comma-separated string for tags
    let isNsfw = false;
    let errorMessage = '';
    let successMessage = '';
    let isLoading = false;

    // Redirect if not authenticated
    authStore.subscribe(auth => {
        if (!auth.isAuthenticated) {
            goto('/login');
        }
    });

    function handleFileChange(event) {
        mediaFiles = Array.from(event.target.files);
    }

    async function handleSubmit() {
        errorMessage = '';
        successMessage = '';
        isLoading = true;

        const formData = new FormData();
        formData.append('title', title);
        if (content) {
            formData.append('content', content);
        }
        formData.append('isNsfw', isNsfw.toString()); // Convert boolean to string

        // Process tags
        const tagsArray = tagsInput.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);
        tagsArray.forEach(tag => formData.append('tags', tag)); // Append each tag separately

        mediaFiles.forEach(file => {
            formData.append('mediaFiles', file);
        });

        try {
            const response = await posts.create(formData);
            successMessage = 'Post created successfully!';
            title = '';
            content = '';
            mediaFiles = [];
            tagsInput = '';
            isNsfw = false;
            // Optionally redirect to the new post or home page
            setTimeout(() => {
                goto('/');
            }, 1500);
        } catch (error) {
            console.error('Failed to create post:', error.response?.data || error);
            errorMessage = error.response?.data?.message || 'Failed to create post. Please try again.';
        } finally {
            isLoading = false;
        }
    }
</script>

<div class="max-w-2xl mx-auto bg-white dark:bg-gray-800 p-8 rounded-lg shadow-lg transition-colors duration-300">
    <h1 class="text-3xl font-bold text-center mb-6 text-gray-900 dark:text-gray-100">Create New Post</h1>

    <form on:submit|preventDefault={handleSubmit}>
        <div class="mb-4">
            <label for="title" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Title</label>
            <input
                type="text"
                id="title"
                bind:value={title}
                class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
                placeholder="Enter post title"
                required
            />
        </div>

        <div class="mb-4">
            <label for="content" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Content</label>
            <textarea
                id="content"
                bind:value={content}
                class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
                rows="5"
                placeholder="Enter your post content..."
            ></textarea>
        </div>

        <div class="mb-4">
            <label for="mediaFiles" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Photos/Videos/Docs (Optional)</label>
            <input
                type="file"
                id="mediaFiles"
                multiple
                on:change={handleFileChange}
                accept="image/*,video/*,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                class="block w-full text-sm text-gray-700 dark:text-gray-200
                       file:mr-4 file:py-2 file:px-4
                       file:rounded-full file:border-0
                       file:text-sm file:font-semibold
                       file:bg-blue-50 file:text-blue-700
                       hover:file:bg-blue-100 dark:file:bg-blue-900 dark:file:text-blue-200 dark:hover:file:bg-blue-800"
            />
            {#if mediaFiles.length > 0}
                <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">{mediaFiles.length} file(s) selected.</p>
            {/if}
        </div>

        <div class="mb-4">
            <label for="tags" class="block text-gray-700 dark:text-gray-300 text-sm font-bold mb-2">Tags (comma-separated)</label>
            <input
                type="text"
                id="tags"
                bind:value={tagsInput}
                class="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 dark:text-gray-200 leading-tight focus:outline-none focus:shadow-outline bg-gray-50 dark:bg-gray-700 dark:border-gray-600"
                placeholder="e.g., tech, gaming, news"
            />
        </div>

        <!--div class="mb-6 flex items-center">
            <input
                type="checkbox"
                id="isNsfw"
                bind:checked={isNsfw}
                class="mr-2 h-4 w-4 text-red-600 bg-gray-100 border-gray-300 rounded focus:ring-red-500 dark:focus:ring-red-600 dark:ring-offset-gray-800 focus:ring-2 dark:bg-gray-700 dark:border-gray-600"
            />
            <label for="isNsfw" class="text-gray-700 dark:text-gray-300 text-sm font-bold">18+ Content (NSFW)</label>
        </div!-->

        {#if errorMessage}
            <p class="text-red-500 text-xs italic mb-4 text-center">{errorMessage}</p>
        {/if}
        {#if successMessage}
            <p class="text-green-500 text-xs italic mb-4 text-center">{successMessage}</p>
        {/if}

        <div class="flex items-center justify-between">
            <button
                type="submit"
                class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:shadow-outline transition-colors duration-300 w-full"
                disabled={isLoading}
            >
                {#if isLoading}
                    Creating Post...
                {:else}
                    Submit Post
                {/if}
            </button>
        </div>
    </form>
</div>
