package com.redditclone.backend.security.jwt;

import java.security.Key;
import java.util.Date;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.redditclone.backend.security.services.UserDetailsImpl;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private int jwtExpirationMs;

    // Generates a JWT token for the authenticated user
    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername())) // Set username as subject
                .claim("id", userPrincipal.getId()) // Add user ID as a claim
                .claim("roles", userPrincipal.getAuthorities()) // Add roles as a claim
                .claim("isAdult", userPrincipal.isAdult()) // Add isAdult claim
                .setIssuedAt(new Date()) // Set token issuance date
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Set expiration date
                .signWith(key(), SignatureAlgorithm.HS256) // Sign with HS256 algorithm and secret key
                .compact(); // Build the JWT
    }

    // Helper method to get the signing key
    private Key key() {
        logger.info("--- JwtUtils: Attempting to get signing key ---"); // Add this
        logger.info("Raw jwtSecret from properties: {}", jwtSecret); // ADD THIS (for debugging only, remove in prod!)
        try {
            // Decode the hexadecimal string from application.properties into bytes
            byte[] keyBytes = Hex.decodeHex(jwtSecret.toCharArray());
            logger.info("Decoded keyBytes length: {}", keyBytes.length); // ADD THIS
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (DecoderException e) {
            logger.error("Error decoding JWT secret from hex: {}", e.getMessage());
            // This is a critical error, you might want to throw a runtime exception
            // or handle it more robustly to prevent application startup.
            throw new RuntimeException("Failed to decode JWT secret", e);
        }
    }

    // Extracts username from JWT token
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    // Validates the JWT token - MODIFIED (added SignatureException catch)
    public boolean validateJwtToken(String authToken) {
        logger.info("--- JwtUtils: Attempting to validate JWT token ---"); // Add this
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(authToken);
            logger.info("JWT token successfully validated."); // Add this
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage()); // THIS IS WHAT WE NEED TO SEE
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty or malformed: {}", e.getMessage()); // Clarified message
        }
        logger.error("JWT token validation FAILED for unknown reason or uncaught exception type."); // Add this
        return false;
    }
}