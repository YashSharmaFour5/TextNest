package com.redditclone.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.redditclone.backend.model.Post;

public interface PostRepository extends MongoRepository<Post, String> {
    // Find posts by a specific tag
    List<Post> findByTagsContaining(String tag);

    // Find posts by author
    List<Post> findByAuthorId(String authorId);
    List<Post> findByAuthorUsername(String username);
    //List<Post> findByNsfwFalse();
    // Find all posts, with pagination and filtering for NSFW content
    // This example shows finding all or filtering NSFW, more complex queries would be in service
    // findAll(Pageable pageable) is inherited from MongoRepository
    //Page<Post> findByTagsContaining(String tag, Pageable pageable);
    //Page<Post> findByTagsContainingAndIsNsfwFalse(String tag, Pageable pageable);
    Page<Post> findByIsNsfwFalse(Pageable pageable); // For non-adult content
    // 'findByTagsIn' will match posts where the 'tags' list field contains any of the strings in the provided List<String>
    Page<Post> findByTagsInAndIsNsfwFalse(List<String> tags, Pageable pageable);
}