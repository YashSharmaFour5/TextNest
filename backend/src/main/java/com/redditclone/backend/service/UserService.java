package com.redditclone.backend.service;

import java.time.LocalDate; // Import for LocalDate
import java.time.LocalDateTime;
import java.time.Period; // Import for Period to calculate age
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.redditclone.backend.model.User;
import com.redditclone.backend.payload.request.LoginRequest;
import com.redditclone.backend.payload.request.SignupRequest;
import com.redditclone.backend.payload.request.UserProfileUpdateRequest;
import com.redditclone.backend.payload.response.JwtResponse;
import com.redditclone.backend.payload.response.UserProfileResponse;
import com.redditclone.backend.repository.UserRepository;
import com.redditclone.backend.security.jwt.JwtUtils;
import com.redditclone.backend.security.services.UserDetailsImpl;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // Lombok: Generates constructor for final fields
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    // Define the minimum age requirement
    private static final int MINIMUM_AGE = 13;
    private static final int ADULT_AGE_THRESHOLD = 18; // Define what constitutes an "adult"

    /**
     * Registers a new user.
     * @param signupRequest The signup request DTO.
     * @return User object of the newly registered user.
     * @throws RuntimeException if username or email already exists, or if age requirement is not met.
     */
    public User registerUser(SignupRequest signupRequest) {
        // Check if username already exists
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
        // Check if email already exists
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        // Calculate age from dateOfBirth
        int calculatedAge = calculateAge(signupRequest.getDateOfBirth());

        // Validate minimum age here
        if (calculatedAge < MINIMUM_AGE) {
            throw new RuntimeException("You must be at least " + MINIMUM_AGE + " years old to sign up.");
        }

        // Create new user's account
        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(encoder.encode(signupRequest.getPassword()));
        user.setDateOfBirth(signupRequest.getDateOfBirth()); // Set the date of birth

        // `age` and `isAdult` are @Transient in User model, so we don't set them directly here for persistence.
        // They will be calculated on demand when constructing UserProfileResponse.

        // Assign default role (e.g., "ROLE_USER")
        user.setRoles(new HashSet<>(List.of("ROLE_USER")));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Authenticates a user and generates a JWT token.
     * @param loginRequest The login request DTO.
     * @return JwtResponse containing the JWT token and user details.
     */
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles);
    }

    /**
     * Finds a user by ID.
     * @param id User ID.
     * @return Optional of User.
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    /**
     * Finds a user by username.
     * @param username Username.
     * @return Optional of User.
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Helper method to get the currently authenticated User entity.
     * Extracts the username from the SecurityContext and fetches the User from the database.
     * @return The User entity of the authenticated user.
     * @throws RuntimeException if user is not found in the database (should not happen if authenticated).
     */
    private User getCurrentAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;

        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else {
            // Fallback for non-UserDetails principal (less common for authenticated users)
            if (principal != null) {
                username = principal.toString();
            } else {
                throw new RuntimeException("Error: No authenticated principal found.");
            }
        }

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Error: Authenticated user not found with username: " + username));
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     * @return UserProfileResponse containing the user's profile details.
     * @throws RuntimeException if the authenticated user cannot be found.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        User currentUser = getCurrentAuthenticatedUser();
        // Use the new UserProfileResponse constructor which handles age calculation
        return new UserProfileResponse(currentUser);
    }

    /**
     * Updates the profile of the currently authenticated user.
     * This method now handles updating dateOfBirth and re-calculates age/isAdult.
     * @param updateRequest DTO containing fields to update (email, dateOfBirth).
     * @return UserProfileResponse with the updated profile details.
     * @throws RuntimeException if the authenticated user is not found or email is already in use.
     */
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(UserProfileUpdateRequest updateRequest) {
        User currentUser = getCurrentAuthenticatedUser();

        // Update email if provided and different from current email
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(currentUser.getEmail())) {
            // Check if another user with this email exists (excluding the current user)
            if (userRepository.existsByEmailAndIdNot(updateRequest.getEmail(), currentUser.getId())) {
                throw new RuntimeException("Error: Email is already in use by another account!");
            }
            currentUser.setEmail(updateRequest.getEmail());
        }

        // Update dateOfBirth if provided
        if (updateRequest.getDateOfBirth() != null) {
            // Optional: Re-validate minimum age if you allow DOB updates that might make them younger than MINIMUM_AGE
            // int newAge = calculateAge(updateRequest.getDateOfBirth());
            // if (newAge < MINIMUM_AGE) {
            //     throw new RuntimeException("Updated date of birth would make you younger than " + MINIMUM_AGE + " years old.");
            // }
            currentUser.setDateOfBirth(updateRequest.getDateOfBirth());
        }

        // Update the updatedAt timestamp
        currentUser.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(currentUser);
        // Use the new UserProfileResponse constructor which handles age calculation
        return new UserProfileResponse(updatedUser);
    }

    /**
     * Updates a user's profile by their ID (useful for admin actions or specific scenarios).
     * The `updateCurrentUserProfile` method above is preferred for a user updating their *own* profile.
     * This method now handles updating dateOfBirth and re-calculates age/isAdult.
     * @param userId User ID.
     * @param updateRequest The update request DTO.
     * @return Updated User object.
     */
    @Transactional
    public User updateUserProfile(String userId, UserProfileUpdateRequest updateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Update email if provided and different from current email
        if (updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(updateRequest.getEmail(), user.getId())) {
                throw new RuntimeException("Error: Email is already in use by another account!");
            }
            user.setEmail(updateRequest.getEmail());
        }

        // Update dateOfBirth if provided
        if (updateRequest.getDateOfBirth() != null) {
            // Optional: Re-validate minimum age for admin updates
            // int newAge = calculateAge(updateRequest.getDateOfBirth());
            // if (newAge < MINIMUM_AGE) {
            //     throw new RuntimeException("Updated date of birth would make user younger than " + MINIMUM_AGE + " years old.");
            // }
            user.setDateOfBirth(updateRequest.getDateOfBirth());
        }

        // Update the updatedAt timestamp
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Retrieves a list of all users. This method is secured to be accessible
     * only by users with the 'ROLE_ADMIN' authority.
     * @return A list of all User objects.
     */
    @PreAuthorize("hasRole('ADMIN')") // <<< Security annotation!
    @Transactional(readOnly = true) // Optimal for read-only operations
    public List<User> getAllUsers() {
        return userRepository.findAll(); // This method is inherited from MongoRepository
    }

    @PreAuthorize("hasRole('ADMIN')") // <<< Security annotation!
    @Transactional
    public User updateUserRoles(String userId, Set<String> newRoles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Basic validation: Ensure roles are not empty or invalid
        if (newRoles == null || newRoles.isEmpty()) {
            throw new IllegalArgumentException("Roles cannot be empty.");
        }
        // You might want to validate if roles exist in your system (e.g., against an enum or DB table)

        user.setRoles(newRoles);
        user.setUpdatedAt(LocalDateTime.now()); // Update timestamp
        return userRepository.save(user);
    }

    /**
     * Deletes a user by ID.
     * @param userId User ID.
     */
    @Transactional
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        userRepository.delete(user);
    }

    /**
     * Checks if a user is an adult based on their date of birth.
     * @param userId User ID.
     * @return true if the user is an adult, false otherwise.
     */
    @Transactional(readOnly = true)
    public boolean isUserAdult(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Calculate and return isAdult based on DOB
        if (user.getDateOfBirth() != null) {
            int age = calculateAge(user.getDateOfBirth());
            return age >= ADULT_AGE_THRESHOLD;
        }
        return false; // Or throw an exception if DOB is mandatory
    }

    /**
     * Helper method to calculate age from a LocalDate date of birth.
     * @param dateOfBirth The date of birth.
     * @return The calculated age in years. Returns 0 if dateOfBirth is null.
     */
    private int calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth cannot be null for age calculation."); // Or throw an IllegalArgumentException if DOB is mandatory for age calculation
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}