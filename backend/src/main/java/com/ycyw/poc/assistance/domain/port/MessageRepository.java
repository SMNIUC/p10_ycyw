package com.ycyw.poc.assistance.domain.port;

import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.Message;
import com.ycyw.poc.assistance.domain.model.MessageId;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import java.util.List;
import java.util.Optional;

/** Port secondaire de persistance des messages. */
public interface MessageRepository {

    void save(Message message);

    Optional<Message> findById(MessageId id);

    /** Historique complet, par ordre d'envoi (US-25). */
    List<Message> findByConversation(ConversationId conversationId);

    /**
     * Messages reçus par un participant et pas encore lus.
     *
     * <p>Sert deux usages : le compteur de non-lus (US-28) et le passage à l'état « lu » quand la
     * conversation est ouverte (US-24).
     */
    List<Message> findUnreadFor(ConversationId conversationId, ParticipantId recipient);
}
