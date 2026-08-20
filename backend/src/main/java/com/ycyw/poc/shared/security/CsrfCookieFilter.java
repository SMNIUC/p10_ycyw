package com.ycyw.poc.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Force la matérialisation du jeton anti-rejeu, donc l'émission de son cookie.
 *
 * <p>Sans cet appel, le jeton n'est calculé que lorsqu'une écriture le réclame — et le client n'a
 * alors jamais reçu le cookie qui lui permettrait de la faire aboutir. Un appel de lecture au
 * démarrage de l'application cliente suffit ainsi à l'amorcer.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
