package com.ycyw.poc.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Force la materialisation du jeton anti-rejeu, donc l'emission de son cookie.
 *
 * <p>Sans cet appel, le jeton n'est calcule que lorsqu'une ecriture le reclame — et le client n'a
 * alors jamais recu le cookie qui lui permettrait de la faire aboutir. Un appel de lecture au
 * demarrage de l'application cliente suffit ainsi a l'amorcer.
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
