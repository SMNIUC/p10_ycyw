package com.ycyw.poc.assistance.fixture;

import com.ycyw.poc.identity.internal.DefaultIdentityService;

/**
 * Classe volontairement fautive : elle atteint l'interieur du module Identite au lieu de passer par
 * son contrat publie. Sert a verifier que la regle correspondante detecte bien la faute.
 */
public class ModuleIndiscretFautif {

    private DefaultIdentityService interneDUnAutreModule;

    public Object dependance() {
        return interneDUnAutreModule;
    }
}
