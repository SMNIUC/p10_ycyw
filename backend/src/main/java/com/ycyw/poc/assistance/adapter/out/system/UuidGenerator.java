package com.ycyw.poc.assistance.adapter.out.system;

import com.ycyw.poc.assistance.domain.port.IdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de génération d'identifiants.
 *
 * <p>UUID aléatoire : l'identifiant ne révèle ni volumétrie ni antériorité, contrairement à une
 * séquence. Sur les tables à forte croissance, un identifiant ordonne dans le temps serait
 * préférable pour la localité d'index — point identifié, non traité dans la preuve de concept.
 */
@Component
public class UuidGenerator implements IdGenerator {

    @Override
    public UUID newId() {
        return UUID.randomUUID();
    }
}
