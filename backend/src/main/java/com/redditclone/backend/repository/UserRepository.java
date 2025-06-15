package com.redditclone.backend.repository;

//import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.redditclone.backend.model.User;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username); // Find user by username
    Optional<User> findByEmail(String email);// Find user by email
    Optional<User> findByUsernameOrEmail(String username, String email); // Find user by username or email
    Boolean existsByUsername(String username); // Check if username exists
    Boolean existsByEmail(String email);       // Check if email exists
    Boolean existsByEmailAndIdNot(String email, String id);
}