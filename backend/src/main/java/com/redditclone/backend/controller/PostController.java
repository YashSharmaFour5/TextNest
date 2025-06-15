package com.redditclone.backend.controller;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.redditclone.backend.model.Comment;
import com.redditclone.backend.model.Post;
import com.redditclone.backend.payload.request.PostUpdateRequest;
import com.redditclone.backend.payload.response.MessageResponse;
import com.redditclone.backend.security.services.UserDetailsImpl;
import com.redditclone.backend.service.CommentService;
import com.redditclone.backend.service.PostService;
import com.redditclone.backend.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserService userService; // To get current user details
    private final CommentService commentService; // Assuming you have a CommentService for comments
    /**
     * Creates a new post.
     * Requires authentication.
     * @param title Post title.
     * @param content Optional post content.
     * @param tags Optional comma-separated tags.
     * @param isNsfw True if 18+ content.
     * @param mediaFiles Optional media files.
     * @return The created Post object.
     */
    @PostMapping
    public ResponseEntity<?> createPost(
            @RequestParam("title") String title,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam("isNsfw") boolean isNsfw,
            @RequestParam(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles) {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String userId = userDetails.getId();

            Post createdPost = postService.createPost(title, content, tags, isNsfw, mediaFiles, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Failed to upload media or create post: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * Retrieves a paginated list of posts.
     * Filters NSFW content if the user is not authenticated or not an adult.
     * @param page Page number (default 0).
     * @param size Number of posts per page (default 10).
     * @param tag Optional tag to filter posts.
     * @return A page of Post objects.
     */
    @GetMapping
    public ResponseEntity<Page<Post>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Optional<String> tag) {
        boolean isAuthenticated = false;
        boolean isAdult = false;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            isAuthenticated = true;
            isAdult = userService.findById(userDetails.getId()).map(user -> user.isAdult()).orElse(false);
        }

        List<String> tagList = tag
                .map(t -> List.of(t.split(",")))
                .orElse(List.of());

        Page<Post> postsPage = postService.getPosts(page, size, isAuthenticated, isAdult, tagList);
        return ResponseEntity.ok(postsPage);
    }

    /**
     * Retrieves a single post by ID.
     * @param id Post ID.
     * @return The Post object.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Post> getPostById(@PathVariable String id) {
        Optional<Post> post = postService.getPostById(id);
        return post.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Post>> searchPosts(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        boolean isAuthenticated = false;
        boolean isAdult = false;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            isAuthenticated = true;
            isAdult = userService.findById(userDetails.getId()).map(user -> user.isAdult()).orElse(false);
        }
        Page<Post> postsPage = postService.searchPosts(q, page, size, isAuthenticated, isAdult);
        return ResponseEntity.ok(postsPage);
    }

    /**
     * Toggles a like on a post.
     * Requires authentication.
     * @param id Post ID.
     * @return The updated Post object.
     */
    @PutMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable String id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String userId = userDetails.getId();

            Post updatedPost = postService.toggleLike(id, userId);
            return ResponseEntity.ok(updatedPost);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * Updates an existing post.
     * Requires authentication and authorization (only author or admin).
     * @param id Post ID.
     * @param post Updated post details.
     * @return The updated Post object.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable String id, @Valid @RequestBody PostUpdateRequest updateRequest) { // MODIFIED: Changed Post to PostUpdateRequest and added @Valid
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String userId = userDetails.getId();

            Post updatedPost = postService.updatePost(id, updateRequest, userId); // MODIFIED: Pass updateRequest
            return ResponseEntity.ok(updatedPost);
        } catch (RuntimeException e) {
            // For authorization failures, return FORBIDDEN
            if (e.getMessage().contains("Unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(e.getMessage()));
            }
            // For other issues like post not found, return NOT_FOUND or BAD_REQUEST
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        }
    } 

    /**
     * Deletes a post.
     * Requires authentication and authorization (only author or admin).
     * @param id Post ID.
     * @return Success message.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable String id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String userId = userDetails.getId();
            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

            postService.deletePost(id, userId, isAdmin);
            return ResponseEntity.ok(new MessageResponse("Post deleted successfully."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * Retrieves all comments for a specific post.
     * This endpoint should match the frontend call: /api/posts/{postId}/comments
     * @param postId The ID of the post.
     * @return A list of comments for the given post. Returns empty list if no comments found (200 OK).
     */
    @GetMapping("/{postId}/comments") // Matches /api/posts/{postId}/comments
    public ResponseEntity<List<Comment>> getCommentsForPost(@PathVariable String postId) {
        // Retrieve current user details if logged in (for potential future personalization, e.g., vote status)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = null;
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            currentUserId = userDetails.getId();
        }

        // Call your CommentService to get comments by post ID
        // Ensure getCommentsByPostId returns an empty list if no comments are found, not null.
        List<Comment> comments = commentService.getCommentsByPostId(postId, currentUserId);

        // Return 200 OK even if the list is empty, as the endpoint was successfully reached and processed.
        return ResponseEntity.ok(comments);
    }
}