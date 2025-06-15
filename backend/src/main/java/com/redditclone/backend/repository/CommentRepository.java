package com.redditclone.backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.redditclone.backend.model.Comment;
import com.redditclone.backend.model.Post;

public interface CommentRepository extends MongoRepository<Comment, String> {
    // Find all comments for a specific post
    List<Comment> findByPost(Post post);
    List<Comment> findByPostId(String postId);
    List<Comment> findByAuthorId(String authorId);
}