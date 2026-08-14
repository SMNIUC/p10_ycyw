package com.ycyw.poc.assistance.adapter.in.ws;

/** Accuse de reception d'un message par son destinataire : etat « remis » d'US-24. */
public record AcknowledgeCommand(String messageId) {}
