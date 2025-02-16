package com.Classroom_ai.Classroom.User;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
                .addFilterBefore(new JwtAuthFilter(), UsernamePasswordAuthenticationFilter.class) // Ajouter le filtre de validation JWT
                .cors(corsConfigurer -> corsConfigurer
                        .configurationSource(corsConfigurationSource())) // Appliquer la configuration CORS
                .csrf(AbstractHttpConfigurer::disable) // Désactiver CSRF pour les API
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("Templates/components/courses/course_card/**").permitAll()
                        .requestMatchers("/api/courses/**").authenticated() // Protéger l'endpoint de création de cours
                        .anyRequest().authenticated()); // Assurez-vous que d'autres routes nécessitent une authentification
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:5175"); // Autoriser les requêtes depuis le frontend React
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE")); // Définir les méthodes HTTP autorisées
        configuration.addAllowedHeader("*"); // Accepter tous les en-têtes
        configuration.setAllowCredentials(true); // Autoriser les cookies et autres données de session

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Appliquer la configuration à toutes les routes
        return source;
    }
}
