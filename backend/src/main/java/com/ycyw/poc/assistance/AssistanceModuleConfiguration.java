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
 * Cablage du module Assistance.
 *
 * <p><b>Pourquoi une configuration explicite plutot que des annotations sur les services.</b> Le
 * domaine et les services applicatifs ne portent aucune annotation de framework (DA-05) : c'est ce
 * qui les rend instanciables dans un test unitaire, sans contexte applicatif ni base de donnees.
 * Le prix de cette independance est ce fichier — une declaration par service, ecrite une fois.
 *
 * <p>C'est aussi le seul endroit ou l'on voit d'un coup d'oeil <b>ce dont le domaine a besoin</b> :
 * deux depots, un diffuseur, une horloge, un generateur d'identifiants. Cinq ports, et rien d'autre.
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
