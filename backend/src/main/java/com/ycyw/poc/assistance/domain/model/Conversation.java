package com.ycyw.poc.assistance.domain.model;

import com.ycyw.poc.assistance.domain.model.exception.ConversationAlreadyTakenException;
import com.ycyw.poc.assistance.domain.model.exception.ConversationClosedException;
import com.ycyw.poc.assistance.domain.model.exception.NotAParticipantException;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Racine d'agrégat du contexte Assistance.
 *
 * <p>Invariants portés ici, et nulle part ailleurs :
 *
 * <ol>
 *   <li>seul un participant de la conversation peut y écrire (US-24) ;
 *   <li>une conversation clôturée n'accepte plus de message ;
 *   <li>une conversation déjà prise en charge ne peut pas être reprise par un second agent
 *       (US-26 : « elle ne lui est plus proposée ») ;
 *   <li>un marqueur de lecture n'est jamais recule.
 * </ol>
 *
 * <p>Aucune annotation de framework, aucun accesseur en écriture public : ces deux règles sont
 * vérifiées au build par les tests d'architecture (DA-04), et non laissées à la vigilance.
 */
public final class Conversation {

    private final ConversationId id;
    private final String subject;
    private final ParticipantId customer;
    private final Instant openedAt;
    private final Map<UUID, Participant> participants = new LinkedHashMap<>();

    private ParticipantId agent;
    private ConversationStatus status;
    private Instant takenAt;
    private Instant closedAt;

    private Conversation(
            ConversationId id,
            String subject,
            ParticipantId customer,
            ParticipantId agent,
            ConversationStatus status,
            Instant openedAt,
            Instant takenAt,
            Instant closedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.subject = requireSubject(subject);
        this.customer = Objects.requireNonNull(customer, "customer");
        this.agent = agent;
        this.status = Objects.requireNonNull(status, "status");
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt");
        this.takenAt = takenAt;
        this.closedAt = closedAt;
    }

    /** Ouverture par le client (US-23). La conversation entre dans la file d'attente des agents. */
    public static Conversation open(
            ConversationId id, String subject, ParticipantId customer, Instant openedAt) {
        if (customer.role() != ParticipantRole.CUSTOMER) {
            throw new IllegalArgumentException("Une conversation est ouverte par un client.");
        }
        Conversation conversation =
                new Conversation(
                        id,
                        subject,
                        customer,
                        null,
                        ConversationStatus.WAITING,
                        openedAt,
                        null,
                        null);
        conversation.participants.put(
                customer.userId(), Participant.joining(customer, openedAt));
        return conversation;
    }

    /** Reconstruction depuis la persistance. Réservé à l'adaptateur secondaire. */
    public static Conversation rehydrate(
            ConversationId id,
            String subject,
            ParticipantId customer,
            ParticipantId agent,
            ConversationStatus status,
            Instant openedAt,
            Instant takenAt,
            Instant closedAt,
            Collection<Participant> knownParticipants) {
        Conversation conversation =
                new Conversation(id, subject, customer, agent, status, openedAt, takenAt, closedAt);
        knownParticipants.forEach(p -> conversation.participants.put(p.id().userId(), p));
        return conversation;
    }

    /**
     * Prise en charge par un agent (US-26).
     *
     * @throws ConversationAlreadyTakenException si un agent l'a déjà prise, même le même
     * @throws ConversationClosedException si elle est clôturée
     */
    public void takeOver(ParticipantId newAgent, Instant at) {
        Objects.requireNonNull(newAgent, "agent");
        if (newAgent.role() != ParticipantRole.AGENT) {
            throw new IllegalArgumentException("Seul un agent prend en charge une conversation.");
        }
        requireOpen();
        if (status == ConversationStatus.TAKEN) {
            throw new ConversationAlreadyTakenException(id, agent);
        }
        this.agent = newAgent;
        this.status = ConversationStatus.TAKEN;
        this.takenAt = at;
        this.participants.put(newAgent.userId(), Participant.joining(newAgent, at));
    }

    /** Clôture par l'un des participants (US-26 : « quand je la clôture, le client en est informé »). */
    public void close(ParticipantId by, Instant at) {
        requireOpen();
        requireParticipant(by);
        this.status = ConversationStatus.CLOSED;
        this.closedAt = at;
    }

    /**
     * Crée un message dans la conversation.
     *
     * <p>La création reste dans l'agrégat parce que ce sont ses règles — appartenance et cycle de
     * vie — qui autorisent ou refusent l'écriture. Le service applicatif ne fait que persister puis
     * diffuser l'objet retourne.
     */
    public Message post(MessageId messageId, ParticipantId author, MessageBody body, Instant at) {
        requireOpen();
        requireParticipant(author);
        return Message.sent(messageId, id, author, body, at);
    }

    /** Avance le marqueur de lecture d'un participant (compteur de non-lus, US-24 / US-28). */
    public void markReadBy(ParticipantId reader, Instant at) {
        requireParticipant(reader);
        participants.get(reader.userId()).readUpTo(at);
    }

    public boolean hasParticipant(UUID userId) {
        return participants.containsKey(userId);
    }

    public Optional<Participant> participant(UUID userId) {
        return Optional.ofNullable(participants.get(userId));
    }

    private void requireOpen() {
        if (status == ConversationStatus.CLOSED) {
            throw new ConversationClosedException(id);
        }
    }

    private void requireParticipant(ParticipantId who) {
        if (!hasParticipant(who.userId())) {
            throw new NotAParticipantException(id, who);
        }
    }

    private static String requireSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Une conversation porte un objet.");
        }
        String stripped = subject.strip();
        if (stripped.length() > 160) {
            throw new IllegalArgumentException("L'objet est limité à 160 caractères.");
        }
        return stripped;
    }

    public ConversationId id() {
        return id;
    }

    public String subject() {
        return subject;
    }

    public ParticipantId customer() {
        return customer;
    }

    public Optional<ParticipantId> agent() {
        return Optional.ofNullable(agent);
    }

    public ConversationStatus status() {
        return status;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Instant takenAt() {
        return takenAt;
    }

    public Instant closedAt() {
        return closedAt;
    }

    public List<Participant> participants() {
        return List.copyOf(participants.values());
    }
}
