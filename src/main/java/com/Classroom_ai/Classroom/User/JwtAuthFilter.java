package com.Classroom_ai.Classroom.User;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.util.ArrayList;


@WebFilter("/api/**") // Appliquer ce filtre aux routes protégées
public class JwtAuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Récupérer le token JWT du header Authorization
        String token = httpRequest.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // Supprimer le "Bearer " du token

            try {
                // Valider et extraire l'utilisateur du token
                if (!JwtTokenUtil.isTokenExpired(token)) {
                    String userEmail = JwtTokenUtil.getUserEmailFromToken(token);
                    // Créer l'objet Authentication (authentification)
                    if (userEmail != null) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userEmail, null, new ArrayList<>());

                        // Ajouter l'authentification au contexte de sécurité
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // En cas d'erreur, on peut loguer ou envoyer une réponse d'erreur (par exemple, 401 Unauthorized)
                response.getWriter().write("Invalid Token: " + e.getMessage());
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}