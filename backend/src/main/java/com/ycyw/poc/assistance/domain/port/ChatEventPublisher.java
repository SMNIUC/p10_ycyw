package com.ycyw.poc.assistance.domain.port;

import com.ycyw.poc.assistance.domain.event.ChatEvent;

/**
 * Port secondaire de diffusion.
 *
 * <p>Le domaine publie un evenement ; il ignore qu'un broker externe existe, qu'il y a plusieurs
 * instances de l'application et que la trame est du STOMP. Cette ignorance est ce qui permet de
 * tester le domaine sans infrastructure, et de changer de transport sans y toucher (DA-06).
 */
public interface ChatEventPublisher {

    void publish(ChatEvent event);
}
