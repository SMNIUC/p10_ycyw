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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
 * Socle de securite (DA-17, DA-18).
 *
 * <p>Quatre decisions y sont lisibles, chacune rattachee a un constat de l'audit :
 *
 * <ul>
 *   <li><b>BCrypt cout 12</b> pour les empreintes de mot de passe — l'audit releve SHA-1 encore en
 *       service sur la plateforme europeenne (F-11) ;
 *   <li><b>jeton en cookie inaccessible au script</b> plutot qu'en stockage navigateur ;
 *   <li><b>application sans etat</b> : aucune session serveur, condition de la replication en
 *       plusieurs instances (ENF-19) ;
 *   <li><b>jeton anti-rejeu (CSRF)</b> obligatoire des lors que l'authentification voyage par
 *       cookie : sans lui, un site tiers pourrait declencher une action authentifiee.
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
                                        // La poignee de main WebSocket est une requete GET : elle
                                        // n'est pas concernee par le jeton anti-rejeu, et reste
                                        // protegee par le controle d'origine du serveur.
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
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .build();
    }

    /**
     * Cout 12, aligne sur la plateforme la plus saine de l'existant (F-11). Le cout est un
     * parametre de securite, pas une preference : il rend l'attaque par force brute couteuse.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
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

    /** Le role porte par le jeton devient une habilitation Spring Security. */
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
