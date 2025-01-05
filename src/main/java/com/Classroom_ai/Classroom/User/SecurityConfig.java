package com.Classroom_ai.Classroom.User;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration

public class SecurityConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(corsConfigurer -> corsConfigurer
                        .configurationSource(corsConfigurationSource())) // Apply custom CORS configuration
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for API-based apps (stateless)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Allow public access to /api/auth/** endpoints
                        .anyRequest().authenticated()          // All other endpoints require authentication
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout") // Custom logout URL
                        .logoutSuccessUrl("/api/auth/login") // Redirect URL after successful logout
                        .invalidateHttpSession(true) // Invalidate the session after logout
                        .clearAuthentication(true) // Clear authentication
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:5173"); // Your React frontend URL
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE")); // Explicitly define allowed methods
        configuration.addAllowedHeader("*");  // Allow all headers
        configuration.setAllowCredentials(true);  // Allow credentials (cookies, etc.)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
