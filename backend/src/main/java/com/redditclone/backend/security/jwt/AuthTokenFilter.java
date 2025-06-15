package com.redditclone.backend.security.jwt;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.redditclone.backend.security.services.UserDetailsServiceImpl;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private static final Logger authLogger = LoggerFactory.getLogger(AuthTokenFilter.class);

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // These are paths that should be COMPLETELY bypassed by the filter,
    // typically for public, non-authenticated operations like login/register.
    // We are intentionally removing the public GET /api/posts paths from here
    // so the filter processes tokens on them if present.
    private static final List<String> STRICTLY_EXCLUDED_PATHS = Arrays.asList(
            "/api/auth/**", // Login, Register, etc.
            "/api/test/**", // Spring Security's permitAll should handle this, but filter can also skip
            "/ws/**" // WebSockets often have their own handshakes
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        authLogger.info("--- AuthTokenFilter: Processing request for path: {} (Method: {}) ---", request.getServletPath(), request.getMethod());

        try {
            String jwt = parseJwt(request);

            if (jwt != null) {
                authLogger.info("AuthTokenFilter: JWT found in request. Attempting to validate.");
                if (jwtUtils.validateJwtToken(jwt)) {
                    String username = jwtUtils.getUserNameFromJwtToken(jwt);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    authLogger.info("AuthTokenFilter: Successfully set authentication for user: {}", username);
                } else {
                    authLogger.warn("AuthTokenFilter: JWT validation failed for token. Authentication not set for path: {}", request.getServletPath());
                }
            } else {
                authLogger.info("AuthTokenFilter: No JWT found in request for path: {}. Proceeding without authentication.", request.getServletPath());
            }
        } catch (io.jsonwebtoken.JwtException e) {
            authLogger.error("AuthTokenFilter: JWT processing error for path {}: {}", request.getServletPath(), e.getMessage());
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            authLogger.error("AuthTokenFilter: User not found for path {}: {}", request.getServletPath(), e.getMessage());
        } catch (Exception e) {
            authLogger.error("AuthTokenFilter: Unexpected error during filter processing for path {}: {}", request.getServletPath(), e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String servletPath = request.getServletPath();
        String method = request.getMethod();

        // Check if the path should be strictly excluded (e.g., auth endpoints, websockets)
        for (String pattern : STRICTLY_EXCLUDED_PATHS) {
            if (pathMatcher.match(pattern, servletPath)) {
                 authLogger.info("--- AuthTokenFilter: shouldNotFilter result for path {} ({}): true (STRICTLY EXCLUDED) ---", servletPath, method);
                return true; // Skip filter for these paths
            }
        }

        // Handle global OPTIONS requests (pre-flight CORS) - these should always be skipped
        if (HttpMethod.OPTIONS.matches(method)) {
             authLogger.info("--- AuthTokenFilter: shouldNotFilter result for path {} ({}): true (Global OPTIONS) ---", servletPath, method);
            return true; // Always skip OPTIONS requests
        }

        // For all other paths (including /api/posts, /api/posts/*, /api/posts/*/comments for GET),
        // we WANT the filter to run (return false). This allows JWT processing if a token is present,
        // providing user context. Spring Security's WebSecurityConfig will then determine if
        // authentication is strictly required or if it's permitAll().
        authLogger.info("--- AuthTokenFilter: shouldNotFilter result for path {} ({}): false (Filter will run) ---", servletPath, method);
        return false;
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            authLogger.info("AuthTokenFilter: Found Bearer token in Authorization header.");
            return headerAuth.substring(7);
        }
        authLogger.info("AuthTokenFilter: No Bearer token found in Authorization header.");
        return null;
    }
}