package com.ycyw.poc.shared.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Socle de sécurité (DA-17, DA-18).
 *
 * <p>Quatre décisions y sont lisibles, chacune rattachee à un constat de l'audit :
 *
 * <ul>
 *   <li><b>Argon2id</b> pour les empreintes de mot de passe (DA-17) — l'audit relève SHA-1 encore
 *       en service sur la plateforme européenne (C-11) ;
 *   <li><b>jeton en cookie inaccessible au script</b> plutôt qu'en stockage navigateur ;
 *   <li><b>application sans état</b> : aucune session serveur, condition de la réplication en
 *       plusieurs instances (ENF-19) ;
 *   <li><b>jeton anti-rejeu (CSRF)</b> obligatoire dès lors que l'authentification voyage par
 *       cookie : sans lui, un site tiers pourrait déclencher une action authentifiée.
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, CookieBearerTokenResolver tokenResolver) throws Exception {
        return http.csrf(
                        csrf ->
                                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                                        // La poignée de main WebSocket est une requête GET : elle
                                        // n'est pas concernée par le jeton anti-rejeu, et reste
                                        // protégée par le contrôle d'origine du serveur.
                                        .ignoringRequestMatchers("/ws/**"))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.POST, "/api/auth/login")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/auth/session")
                                        .permitAll()
                                        .requestMatchers("/actuator/health/**")
                                        .permitAll()
                                        .requestMatchers("/api/agent/**")
                                        .hasRole("AGENT")
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.bearerTokenResolver(tokenResolver)
                                        .jwt(
                                                jwt ->
                                                        jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter())))
                .exceptionHandling(
                        handling ->
                                handling.authenticationEntryPoint(
                                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // Aucun de ces deux mécanismes n'a de place ici : l'authentification passe par un
                // jeton en cookie (DA-18). Les laisser actifs offrirait un second chemin d'entrée,
                // non couvert par les décisions prises.
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }

    /**
     * <b>Argon2id</b>, conformément à DA-17. Le choix n'est pas une préférence : la fonction est
     * lente <i>et</i> gourmande en mémoire, ce qui prive un attaquant du gain qu'il tire d'un
     * matériel spécialisé — là où SHA-1, rapide par conception, le lui offre (C-11).
     *
     * <p>C'est aussi la généralisation d'une pratique déjà en service au Canada (F-04) : la
     * décision diffuse un savoir-faire interne, elle n'en acquiert pas un nouveau.
     *
     * <p>Paramètres par défaut de Spring Security : 16 Mo de mémoire, 2 itérations, sel de
     * 16 octets. L'empreinte encodée tient en 97 caractères, sous la limite de la colonne.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    JwtEncoder jwtEncoder(SecurityProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
    }

    @Bean
    JwtDecoder jwtDecoder(SecurityProperties properties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Le rôle porté par le jeton devient une habilitation Spring Security. */
    static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> AuthorityUtils.createAuthorityList("ROLE_" + jwt.getClaimAsString("role")));
        return converter;
    }

    private static SecretKeySpec secretKey(SecurityProperties properties) {
        return new SecretKeySpec(properties.jwtSecret().getBytes(), "HmacSHA256");
    }
}
