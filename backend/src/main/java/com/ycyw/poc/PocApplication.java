package com.ycyw.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree de la preuve de concept.
 *
 * <p>Le deployable ne contient qu'un seul contexte borne — Assistance (DA-02) — plus le strict
 * necessaire d'Identite pour authentifier les deux profils qui dialoguent. Les autres contextes
 * sont concus et documentes dans la proposition d'architecture, jamais implementes ici.
 */
@SpringBootApplication
public class PocApplication {

    public static void main(String[] args) {
        SpringApplication.run(PocApplication.class, args);
    }
}
