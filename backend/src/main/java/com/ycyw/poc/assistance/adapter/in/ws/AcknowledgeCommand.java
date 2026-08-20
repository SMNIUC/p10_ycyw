package com.ycyw.poc.assistance.adapter.in.ws;

/** Accusé de réception d'un message par son destinataire : état « remis » d'US-24. */
public record AcknowledgeCommand(String messageId) {}
