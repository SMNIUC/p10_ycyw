package com.ycyw.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de la preuve de concept.
 *
 * <p>Le déployable ne contient qu'un seul contexte borné — Assistance (DA-02) — plus le strict
 * nécessaire d'identité pour authentifier les deux profils qui dialoguent. Les autres contextes
 * sont conçus et documentés dans la proposition d'architecture, jamais implémentés ici.
 */
@SpringBootApplication
public class PocApplication {

    public static void main(String[] args) {
        SpringApplication.run(PocApplication.class, args);
    }
}
