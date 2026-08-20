package com.ycyw.poc.assistance;

import com.ycyw.poc.assistance.application.ConversationLifecycleService;
import com.ycyw.poc.assistance.application.ConversationQueryService;
import com.ycyw.poc.assistance.application.MessagingService;
import com.ycyw.poc.assistance.domain.port.ChatEventPublisher;
import com.ycyw.poc.assistance.domain.port.ConversationRepository;
import com.ycyw.poc.assistance.domain.port.IdGenerator;
import com.ycyw.poc.assistance.domain.port.MessageRepository;
import com.ycyw.poc.assistance.domain.port.TimeProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage du module Assistance.
 *
 * <p><b>Pourquoi une configuration explicite plutôt que des annotations sur les services.</b> Le
 * domaine et les services applicatifs ne portent aucune annotation de framework (DA-05) : c'est ce
 * qui les rend instanciables dans un test unitaire, sans contexte applicatif ni base de données.
 * Le prix de cette indépendance est ce fichier — une déclaration par service, écrite une fois.
 *
 * <p>C'est aussi le seul endroit où l'on voit d'un coup d'oeil <b>ce dont le domaine à besoin</b> :
 * deux dépôts, un diffuseur, une horloge, un générateur d'identifiants. Cinq ports, et rien d'autre.
 */
@Configuration
public class AssistanceModuleConfiguration {

    @Bean
    ConversationLifecycleService conversationLifecycleService(
            ConversationRepository conversations,
            ChatEventPublisher publisher,
            TimeProvider time,
            IdGenerator ids) {
        return new ConversationLifecycleService(conversations, publisher, time, ids);
    }

    @Bean
    MessagingService messagingService(
            ConversationRepository conversations,
            MessageRepository messages,
            ChatEventPublisher publisher,
            TimeProvider time,
            IdGenerator ids) {
        return new MessagingService(conversations, messages, publisher, time, ids);
    }

    @Bean
    ConversationQueryService conversationQueryService(
            ConversationRepository conversations, MessageRepository messages, TimeProvider time) {
        return new ConversationQueryService(conversations, messages, time);
    }
}
