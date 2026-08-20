package com.ycyw.poc.assistance.adapter.in.ws;

/**
 * Erreur renvoyée à l'émetteur sur sa destination privée.
 *
 * <p>Le message est destiné à être affiche : il doit rester compréhensible et ne jamais exposer de
 * détail technique. L'interface l'annonce sous forme textuelle (ENF-03).
 */
public record ChatErrorPayload(String code, String message) {}
