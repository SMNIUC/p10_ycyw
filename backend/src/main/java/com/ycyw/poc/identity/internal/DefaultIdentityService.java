package com.ycyw.poc.identity.internal;

import com.ycyw.poc.identity.AppUser;
import com.ycyw.poc.identity.IdentityApi;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mise en oeuvre du contrat publie du contexte Identite. */
@Service
public class DefaultIdentityService implements IdentityApi {

    private final AppUserJpaRepository users;
    private final PasswordEncoder passwordEncoder;

    DefaultIdentityService(AppUserJpaRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Un identifiant inconnu et un mot de passe faux donnent le meme resultat : rien dans la reponse
     * ne permet de decouvrir quels comptes existent.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> authenticate(String email, String rawPassword) {
        return users.findByEmailIgnoreCase(email)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
                .map(DefaultIdentityService::toApi);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppUser> findById(UUID id) {
        return users.findById(id).map(DefaultIdentityService::toApi);
    }

    private static AppUser toApi(AppUserEntity entity) {
        return new AppUser(
                entity.getId(), entity.getEmail(), entity.getDisplayName(), entity.getRole());
    }
}
