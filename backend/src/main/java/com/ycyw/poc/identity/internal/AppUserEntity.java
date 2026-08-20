package com.ycyw.poc.identity.internal;

import com.ycyw.poc.identity.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Compte persisté, dans le schéma {@code identity}.
 *
 * <p>Le mot de passe n'est jamais stocké : seule son empreinte l'est, produite par <b>Argon2id</b>
 * (DA-17). L'audit relève que la plateforme européenne historique utilise encore SHA-1 (C-11) ;
 * la preuve de concept met en œuvre la correction décidée, elle ne se contente pas de la
 * recommander.
 *
 * <p><b>Aucun accesseur en écriture</b> : un compte se crée entier, par le constructeur. La preuve
 * de concept ne modifie jamais un compte existant, et le jour où elle le fera, ce sera par une
 * méthode nommée — pas par une suite d'affectations.
 */
@Entity
@Table(name = "app_user", schema = "identity")
@Getter(AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUserEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Constructeur écrit à la main, volontairement.
     *
     * <p>Un constructeur généré se contenterait de reprendre l'ordre des champs — or trois d'entre
     * eux sont des chaînes consécutives. Le jour où quelqu'un réordonnerait les declarations,
     * l'empreinte du mot de passe se retrouverait enregistrée dans la colonne du courriel, sans la
     * moindre erreur de compilation. Le gain de six lignes ne vaut pas ce risque.
     */
    AppUserEntity(
            UUID id,
            String email,
            String displayName,
            String passwordHash,
            UserRole role,
            Instant createdAt) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }
}
