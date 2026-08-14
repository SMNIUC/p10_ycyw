package com.ycyw.poc.assistance.adapter.in.ws;

/**
 * Erreur renvoyee a l'emetteur sur sa destination privee.
 *
 * <p>Le message est destine a etre affiche : il doit rester comprehensible et ne jamais exposer de
 * detail technique. L'interface l'annonce sous forme textuelle (ENF-03).
 */
public record ChatErrorPayload(String code, String message) {}
