package com.ycyw.poc.assistance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ycyw.poc.assistance.application.MessagingService;
import com.ycyw.poc.assistance.domain.model.ConversationId;
import com.ycyw.poc.assistance.domain.model.DeliveryState;
import com.ycyw.poc.assistance.domain.model.Message;
import com.ycyw.poc.assistance.domain.model.ParticipantId;
import com.ycyw.poc.assistance.domain.port.MessageRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Parcours complet sur un vrai PostgreSQL.
 *
 * <p>Ce test vérifie ce que les tests unitaires ne peuvent pas vérifier : que les migrations
 * s'appliquent, que le mappage vers les schémas cloisonnes fonctionne, que la chaîne de sécurité
 * laisse passer la session légitime et refuse les autres. C'est la mise en œuvre réelle de la
 * structure de données, pas seulement sa modélisation.
 *
 * <p>Il exige un moteur de conteneurs local. Pour l'exclure de la boucle rapide :
 * {@code mvn test -DexcludedGroups=docker}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Testcontainers
@Tag("docker")
@DisplayName("Parcours d'assistance de bout en bout")
class AssistanceIntegrationTest {

    private static final String MOT_DE_PASSE = "motdepasse-de-test";

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("ycyw")
                    .withUsername("ycyw")
                    .withPassword("ycyw");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MessagingService messaging;
    @Autowired private MessageRepository messages;

    @Test
    @DisplayName("le client ouvre une demande, l'agent la prend en charge, l'échange est persisté")
    void parcoursNominal() throws Exception {
        Cookie sessionClient = seConnecter("alice.client@example.test");
        Cookie sessionAgent = seConnecter("sam.agent@example.test");

        String conversationId =
                lireChamp(
                        mockMvc.perform(
                                        post("/api/conversations")
                                                .cookie(sessionClient)
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        """
                                                        {"subject": "Decaler ma location"}
                                                        """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value("WAITING"))
                                .andReturn(),
                        "id");

        // La demande apparaît dans la file des agents, avec son temps d'attente (US-26).
        // Le filtre sur l'identifiant plutôt que sur le premier élément rend le test indépendant
        // des demandes laissées par les autres cas : ils partagent la même base.
        mockMvc.perform(get("/api/agent/queue").cookie(sessionAgent))
                .andExpect(status().isOk())
                .andExpect(jsonPath(demandeEnFile(conversationId) + ".waitingSeconds").isNotEmpty());

        mockMvc.perform(
                        post("/api/agent/conversations/" + conversationId + "/take")
                                .cookie(sessionAgent)
                                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TAKEN"));

        // Une demande prise en charge n'est plus proposée aux autres agents (US-26).
        mockMvc.perform(get("/api/agent/queue").cookie(sessionAgent))
                .andExpect(status().isOk())
                .andExpect(jsonPath(demandeEnFile(conversationId)).isEmpty());

        UUID clientId = UUID.fromString(identifiant(sessionClient));
        UUID agentId = UUID.fromString(identifiant(sessionAgent));
        ConversationId identifiantConversation = ConversationId.of(conversationId);

        Message duClient =
                messaging.post(
                        identifiantConversation,
                        ParticipantId.customer(clientId),
                        "Bonjour, je dois décaler ma location de deux jours.");
        Message deLAgent =
                messaging.post(
                        identifiantConversation,
                        ParticipantId.agent(agentId),
                        "Bonjour, je regarde votre dossier.");

        // L'historique persisté fait autorité : c'est lui que le client recharge après une coupure.
        mockMvc.perform(
                        get("/api/conversations/" + conversationId + "/messages")
                                .cookie(sessionClient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(duClient.id().toString()))
                .andExpect(jsonPath("$[1].id").value(deLAgent.id().toString()))
                .andExpect(jsonPath("$[0].state").value("SENT"));

        // L'agent lit la conversation : seuls les messages qui lui étaient adresses passent à « lu ».
        messaging.markRead(identifiantConversation, ParticipantId.agent(agentId));

        List<Message> relus = messages.findByConversation(identifiantConversation);
        assertThat(relus).hasSize(2);
        assertThat(relus.get(0).state()).isEqualTo(DeliveryState.READ);
        assertThat(relus.get(1).state()).isEqualTo(DeliveryState.SENT);
    }

    @Test
    @DisplayName("un tiers ne peut pas lire une conversation dont il n'est pas participant")
    void cloisonnementDesConversations() throws Exception {
        Cookie sessionAlice = seConnecter("alice.client@example.test");
        Cookie sessionBruno = seConnecter("bruno.client@example.test");

        String conversationId =
                lireChamp(
                        mockMvc.perform(
                                        post("/api/conversations")
                                                .cookie(sessionAlice)
                                                .with(csrf())
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                        {"subject": "Question privee"}
                                                        """))
                                .andExpect(status().isCreated())
                                .andReturn(),
                        "id");

        mockMvc.perform(
                        get("/api/conversations/" + conversationId + "/messages")
                                .cookie(sessionBruno))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("sans session, l'accès est refusé")
    void sansSession() throws Exception {
        mockMvc.perform(get("/api/conversations")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("la file d'attente est réservée aux agents")
    void fileReserveeAuxAgents() throws Exception {
        mockMvc.perform(get("/api/agent/queue").cookie(seConnecter("alice.client@example.test")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("le jeton de session n'est jamais lisible par le script de page")
    void cookieInaccessibleAuScript() throws Exception {
        Cookie session = seConnecter("alice.client@example.test");

        assertThat(session.isHttpOnly()).isTrue();
        assertThat(session.getValue()).isNotBlank();
    }

    /** Sélecteur de la demande considérée dans la file, quelles que soient les autres. */
    private static String demandeEnFile(String conversationId) {
        return "$[?(@.id=='" + conversationId + "')]";
    }

    private Cookie seConnecter(String email) throws Exception {
        MvcResult resultat =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .with(csrf())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email": "%s", "password": "%s"}
                                                """
                                                        .formatted(email, MOT_DE_PASSE)))
                        .andExpect(status().isOk())
                        .andReturn();
        Cookie cookie = resultat.getResponse().getCookie("ycyw_session");
        assertThat(cookie).as("cookie de session pour %s", email).isNotNull();
        return cookie;
    }

    private String identifiant(Cookie session) throws Exception {
        return lireChamp(
                mockMvc.perform(get("/api/auth/session").cookie(session))
                        .andExpect(status().isOk())
                        .andReturn(),
                "userId");
    }

    private String lireChamp(MvcResult resultat, String champ) throws Exception {
        JsonNode noeud =
                objectMapper.readTree(resultat.getResponse().getContentAsString());
        return noeud.get(champ).asText();
    }
}
