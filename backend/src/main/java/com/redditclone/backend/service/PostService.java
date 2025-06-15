package com.redditclone.backend.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.redditclone.backend.model.Post;
import com.redditclone.backend.model.User;
import com.redditclone.backend.payload.request.PostUpdateRequest;
import com.redditclone.backend.repository.PostRepository;
import com.redditclone.backend.repository.UserRepository;
import com.redditclone.backend.security.services.UserDetailsImpl;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // This creates a constructor for all final fields, including the new MongoTemplate
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final MongoTemplate mongoTemplate; // NEW: Inject MongoTemplate

    /**
     * Creates a new post.
     * @param title Title of the post.
     * @param content Optional content of the post.
     * @param tags Tags associated with the post.
     * @param isNsfw True if the post is 18+.
     * @param mediaFiles Optional list of media files to upload.
     * @param authorId The ID of the post author.
     * @return The created Post object.
     * @throws IOException If media upload fails.
     */
    public Post createPost(String title, String content, List<String> tags, boolean isNsfw,
                           List<MultipartFile> mediaFiles, String authorId) throws IOException {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author not found with id: " + authorId));

        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setAuthor(author);

        // Ensure tags are lowercase and handle null case gracefully
        post.setTags(tags != null ? tags.stream().map(String::toLowerCase).collect(Collectors.toList()) : new ArrayList<>()); // Changed List.of() to new ArrayList<>() for mutability if needed elsewhere

        post.setNsfw(isNsfw);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        // Upload media files to Cloudinary and store URLs
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            for (MultipartFile file : mediaFiles) {
                if (!file.isEmpty()) { // Check if file is not empty before uploading
                    String mediaUrl = cloudinaryService.uploadFile(file);
                    post.getMediaUrls().add(mediaUrl);
                }
            }
        }

        return postRepository.save(post);
    }

    /**
     * Retrieves posts with pagination and filtering for NSFW content based on user's adult status.
     * Now supports filtering by a list of tags (logical OR).
     * @param page The page number (0-indexed).
     * @param size The number of posts per page.
     * @param isAuthenticated True if the user is authenticated.
     * @param isAdult True if the authenticated user is an adult.
     * @param tags Optional list of tags to filter posts by (logical OR).
     * @return A Page of Post objects.
     */
    public Page<Post> getPosts(int page, int size, boolean isAuthenticated, boolean isAdult, List<String> tags) { // MODIFIED: Changed Optional<String> tag to List<String> tags
        Pageable pageable = PageRequest.of(page, size);
        Query query = new Query(); // NEW: Use MongoTemplate Query
        Criteria criteria = new Criteria();

        // Apply NSFW filtering
        if (!isAuthenticated || !isAdult) {
            criteria.and("isNsfw").is(false);
        }

        // Apply tag filtering if tags are provided (logical OR using $in operator)
        if (tags != null && !tags.isEmpty()) {
            List<String> lowercasedTags = tags.stream()
                                            .map(String::toLowerCase)
                                            .collect(Collectors.toList());
            criteria.and("tags").in(lowercasedTags); // Find posts where the 'tags' array contains ANY of the provided tags
        }

        query.addCriteria(criteria); // Apply combined criteria
        query.with(Sort.by(Sort.Direction.DESC, "createdAt")); // Sort by creation date descending
        query.with(pageable); // Apply pagination to the query

        // Execute the query
        List<Post> posts = mongoTemplate.find(query, Post.class);
        // Get total count for proper pagination metadata
        long count = mongoTemplate.count(query, Post.class);

        return PageableExecutionUtils.getPage(posts, pageable, () -> count);
    }


    /**
     * Retrieves a single post by ID.
     * @param id The ID of the post.
     * @return Optional of Post.
     */
    public Optional<Post> getPostById(String id) {
        return postRepository.findById(id);
    }

    /**
     * Handles liking/unliking a post.
     * @param postId The ID of the post.
     * @param userId The ID of the user performing the action.
     * @return The updated Post object.
     * @throws RuntimeException if post not found.
     */
    public Post toggleLike(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        if (post.getLikedBy().contains(userId)) {
            post.removeLike(userId); // User unlikes
        } else {
            post.addLike(userId); // User likes
        }
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    /**
     * Updates an existing post. Only author can update (or admin, handled in controller).
     * @param postId The ID of the post to update.
     * @param updatedPost The updated post details (only title, content, tags, isNsfw are directly used).
     * @param userId The ID of the user attempting to update.
     * @return The updated Post object.
     * @throws RuntimeException if post not found or user not authorized.
     *
    public Post updatePost(String postId, Post updatedPost, String userId) {
        Post existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        // Authorization check should ideally involve fetching roles from the User object
        // if not already done in the controller, but given your controller logic,
        // it seems authorization based on role is handled there.
        // This method strictly checks for author match.
        if (!existingPost.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to update this post.");
        }

        existingPost.setTitle(updatedPost.getTitle());
        existingPost.setContent(updatedPost.getContent());

        // Update tags: ensure lowercase and handle null list gracefully
        existingPost.setTags(updatedPost.getTags() != null ?
                             updatedPost.getTags().stream().map(String::toLowerCase).collect(Collectors.toList()) :
                             new ArrayList<>());

        existingPost.setNsfw(updatedPost.isNsfw());
        existingPost.setUpdatedAt(LocalDateTime.now());

        // Handle media updates if needed (e.g., removal, adding new ones - more complex logic required)
        // For simplicity here, we assume media is handled separately or through a different endpoint.
        // For a full implementation, you'd need logic to remove old media from Cloudinary if URLs are removed.

        return postRepository.save(existingPost);
    }*/

    /**
     * Updates an existing post. Only author can update.
     * @param postId The ID of the post to update.
     * @param updateRequest DTO containing the updated post details (title, content, tags, isNsfw).
     * @param userId The ID of the user attempting to update (from SecurityContext).
     * @return The updated Post object.
     * @throws RuntimeException if post not found or user not authorized.
     */
    @Transactional // Ensure it's transactional
    public Post updatePost(String postId, PostUpdateRequest updateRequest, String userId) { // MODIFIED: Changed Post to PostUpdateRequest
        Post existingPost = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        // Authorization check
        if (!existingPost.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized to update this post.");
        }

        // Update fields from the DTO
        existingPost.setTitle(updateRequest.getTitle());
        existingPost.setContent(updateRequest.getContent());

        // Update tags: ensure lowercase and handle null list gracefully
        existingPost.setTags(updateRequest.getTags() != null ?
                               updateRequest.getTags().stream().map(String::toLowerCase).collect(Collectors.toList()) :
                               new ArrayList<>());

        existingPost.setNsfw(updateRequest.isNsfw()); // Update NSFW status
        existingPost.setUpdatedAt(LocalDateTime.now()); // Set updated timestamp

        // Media URLs are not part of this update request. If you need to update media,
        // you'd typically have separate endpoints/logic for adding/removing media.

        return postRepository.save(existingPost);
    }

    /**
     * Deletes a post. Only author or admin can delete.
     * @param postId The ID of the post to delete.
     * @param userId The ID of the user attempting to delete.
     * @param isAdmin True if the user is an admin (authorization from controller).
     * @throws RuntimeException if post not found or user not authorized.
     */
    public void deletePost(String postId, String userId, boolean isAdmin) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        if (!post.getAuthor().getId().equals(userId) && !isAdmin) {
            throw new RuntimeException("Unauthorized to delete this post.");
        }

        // Optionally, delete media from Cloudinary before deleting the post
        post.getMediaUrls().forEach(cloudinaryService::deleteFileByUrl);

        postRepository.delete(post);
    }

     // --- Helper to get current authenticated user's ID ---
    // You can use this if you need the User object from the current auth context
    public User getCurrentAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userId;

        if (principal instanceof UserDetailsImpl userDetails) {
            userId = userDetails.getId();
        } else {
            // This case might happen if not logged in or a different principal type (e.g., "anonymousUser")
            throw new RuntimeException("No authenticated user found.");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: Authenticated user not found with ID: " + userId));
    }


    // Your existing PostResponse mapping method (if you have one, ensure it's up to date)
    // Example:
    // private PostResponse mapPostToPostResponse(Post post) {
    //     return new PostResponse(
    //         post.getId(),
    //         post.getTitle(),
    //         post.getContent(),
    //         post.getAuthor().getUsername(), // Make sure PostResponse can get author username
    //         post.getCreatedAt(),
    //         post.getUpdatedAt(),
    //         post.getMediaUrls(),
    //         post.getTags(),
    //         post.isNsfw(),
    //         post.getLikeCount(),
    //         post.getLikedBy().contains(currentUserId) // For client-side, if applicable
    //     );
    // }

    /**
     * Searches posts by a general query string across title, content, and tags.
     * Filters NSFW content if the user is not authenticated or not an adult.
     * @param queryTerm The search query string.
     * @param page The page number (0-indexed).
     * @param size The number of posts per page.
     * @param isAuthenticated True if the user is authenticated.
     * @param isAdult True if the authenticated user is an adult.
     * @return A Page of Post objects matching the search query.
     */
    public Page<Post> searchPosts(String queryTerm, int page, int size, boolean isAuthenticated, boolean isAdult) { // NEW METHOD
        Pageable pageable = PageRequest.of(page, size);
        Query query = new Query();
        Criteria criteria = new Criteria();

        // Apply NSFW filtering
        if (!isAuthenticated || !isAdult) {
            criteria.and("isNsfw").is(false);
        }

        // Build criteria for searching across title, content, and tags
        String lowerCaseQueryTerm = queryTerm.toLowerCase(); // Convert query term to lowercase once

        // Use $or to search across multiple fields.
        // For 'tags', we use $in as we're checking if the tag list contains the specific query term.
        // For 'title' and 'content', we use regex for partial, case-insensitive matches.
        Criteria searchCriteria = new Criteria().orOperator(
            Criteria.where("title").regex(lowerCaseQueryTerm, "i"), // 'i' for case-insensitive
            Criteria.where("content").regex(lowerCaseQueryTerm, "i"),
            Criteria.where("tags").in(lowerCaseQueryTerm) // Check if the tag list contains the exact query term
        );

        criteria.andOperator(searchCriteria); // Combine NSFW filter with search criteria

        query.addCriteria(criteria);
        query.with(pageable); // Apply pagination

        List<Post> posts = mongoTemplate.find(query, Post.class);
        long count = mongoTemplate.count(query, Post.class); // Get total count for pagination

        return PageableExecutionUtils.getPage(posts, pageable, () -> count);
    }

}