package com.ycyw.poc.identity.internal;

import com.ycyw.poc.identity.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jeu de comptes de demonstration, cree au demarrage du profil {@code demo} uniquement.
 *
 * <p>Deux precautions volontaires. Les identites sont <b>fictives</b> et le domaine
 * {@code example.test} est reserve a cet usage : aucune donnee personnelle reelle ne circule dans
 * un environnement non productif (§ 12.4 de la proposition d'architecture). Le mot de passe est lu
 * dans la configuration et n'a pas de valeur par defaut en dur : hors profil {@code demo}, ce code
 * n'existe pas dans le contexte applicatif.
 */
@Component
@Profile("demo")
public class DemoUsersSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUsersSeeder.class);

    private final AppUserJpaRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    DemoUsersSeeder(
            AppUserJpaRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${poc.demo.password}") String demoPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfAbsent("alice.client@example.test", "Alice Martin", UserRole.CUSTOMER);
        createIfAbsent("bruno.client@example.test", "Bruno Lopez", UserRole.CUSTOMER);
        createIfAbsent("sam.agent@example.test", "Sam Okafor", UserRole.AGENT);
    }

    private void createIfAbsent(String email, String displayName, UserRole role) {
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }
        users.save(
                new AppUserEntity(
                        UUID.randomUUID(),
                        email,
                        displayName,
                        passwordEncoder.encode(demoPassword),
                        role,
                        Instant.now()));
        log.info("Compte de demonstration cree : {} ({})", email, role);
    }
}
