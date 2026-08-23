package com.civicsense.security;

import com.civicsense.auth.service.JwtService;
import com.civicsense.user.entity.User;
import com.civicsense.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        System.out.println("===== JWT FILTER START =====");
        System.out.println("REQUEST = "
                + request.getMethod()
                + " "
                + request.getRequestURI());

        String authorizationHeader =
                request.getHeader("Authorization");

        System.out.println("AUTH HEADER = " + authorizationHeader);

        // No Authorization header
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith("Bearer ")) {

            System.out.println("NO BEARER TOKEN");
            System.out.println("===== JWT FILTER END =====");

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader.substring(7);

        System.out.println("TOKEN RECEIVED = " + token);

        // Validate JWT
        if (!jwtService.isTokenValid(token)) {

            System.out.println("JWT INVALID");
            System.out.println("===== JWT FILTER END =====");

            filterChain.doFilter(request, response);
            return;
        }

        System.out.println("JWT VALID");

        try {

            // Extract User ID
            UUID userId =
                    jwtService.extractUserId(token);

            System.out.println(
                    "USER ID FROM JWT = " + userId
            );

            // Find user
            User user =
                    userRepository.findById(userId)
                            .orElse(null);

            if (user == null) {

                System.out.println("USER NOT FOUND");

                filterChain.doFilter(request, response);
                return;
            }

            System.out.println(
                    "USER FOUND = " + user.getMobileNumber()
            );

            // Convert roles into Spring Security authorities
            List<SimpleGrantedAuthority> authorities =
                    user.getRoles()
                            .stream()
                            .map(role ->
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + role.getName()
                                    )
                            )
                            .toList();

            System.out.println(
                    "AUTHORITIES = " + authorities
            );

            // Create authenticated user
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            authorities
                    );

            // Put authentication into SecurityContext
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            System.out.println(
                    "AUTHENTICATION SET = "
                            + SecurityContextHolder
                            .getContext()
                            .getAuthentication()
            );

        } catch (Exception exception) {

            System.out.println(
                    "JWT FILTER ERROR = "
                            + exception.getClass().getName()
            );

            System.out.println(
                    "MESSAGE = "
                            + exception.getMessage()
            );

            SecurityContextHolder.clearContext();
        }

        System.out.println("===== JWT FILTER END =====");

        filterChain.doFilter(request, response);
    }
}