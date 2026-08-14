package com.ycyw.poc.assistance.adapter.out.system;

import com.ycyw.poc.assistance.domain.port.IdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de generation d'identifiants.
 *
 * <p>UUID aleatoire : l'identifiant ne revele ni volumetrie ni anteriorite, contrairement a une
 * sequence. Sur les tables a forte croissance, un identifiant ordonne dans le temps serait
 * preferable pour la localite d'index — point identifie, non traite dans la preuve de concept.
 */
@Component
public class UuidGenerator implements IdGenerator {

    @Override
    public UUID newId() {
        return UUID.randomUUID();
    }
}
