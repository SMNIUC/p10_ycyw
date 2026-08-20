-- ---------------------------------------------------------------------------------------------
-- Structure de données de la preuve de concept.
--
-- Deux schémas, un par contexte borné (proposition d'architecture, § 7.1) : le cloisonnement par
-- schéma matérialise la frontière de modules et permet de la vérifier. Une seule base, conformément
-- à CT-02 qui exige un modèle unique et partage.
--
-- Point volontaire : aucune clé étrangère ne relie assistance.conversation à identity.app_user.
-- Une conversation référence un utilisateur par son identifiant, sans dépendance de schéma. C'est
-- ce qui rend le module Assistance extractible sans refonte (§ 4.6) et ce qui permettra
-- l'anonymisation d'un compte sans casser l'historique des échanges (RG-13, § 7.7).
-- ---------------------------------------------------------------------------------------------

create schema if not exists identity;
create schema if not exists assistance;

-- --------------------------------------------------------------------------------------------
-- Contexte Identité
-- --------------------------------------------------------------------------------------------

create table identity.app_user (
    id            uuid         primary key,
    email         varchar(255) not null unique,
    display_name  varchar(120) not null,
    -- Empreinte Argon2id (DA-17), jamais le mot de passe. L'audit relève SHA-1 encore actif sur
    -- la plateforme européenne historique (C-11) : la preuve de concept applique la correction.
    -- 120 caractères suffisent : une empreinte Argon2id encodée en occupe 97.
    password_hash varchar(120) not null,
    role          varchar(20)  not null,
    created_at    timestamptz  not null,
    constraint app_user_role_known check (role in ('CUSTOMER', 'AGENT'))
);

-- --------------------------------------------------------------------------------------------
-- Contexte Assistance
-- --------------------------------------------------------------------------------------------

create table assistance.conversation (
    id          uuid         primary key,
    subject     varchar(160) not null,
    status      varchar(20)  not null,
    customer_id uuid         not null,
    agent_id    uuid,
    opened_at   timestamptz  not null,
    taken_at    timestamptz,
    closed_at   timestamptz,
    -- Verrou optimiste : deux agents ne peuvent pas prendre la même demande en charge (US-26).
    version     bigint       not null default 0,
    constraint conversation_status_known check (status in ('WAITING', 'TAKEN', 'CLOSED')),
    -- L'état et les dates ne peuvent pas se contredire : une demande en attente n'a pas d'agent,
    -- une demande prise en charge en a un, une demande clôturée porte sa date de clôture. Un
    -- client peut clôturer une demande jamais prise en charge : ce cas reste donc valide.
    constraint conversation_status_consistent check (
        (status = 'WAITING' and agent_id is null and taken_at is null and closed_at is null)
        or (status = 'TAKEN' and agent_id is not null and taken_at is not null and closed_at is null)
        or (status = 'CLOSED' and closed_at is not null)
    )
);

-- File d'attente des agents : lecture par statut, par ordre d'arrivée (US-26).
create index conversation_queue_idx on assistance.conversation (status, opened_at);

create table assistance.participant (
    conversation_id uuid        not null references assistance.conversation (id) on delete cascade,
    user_id         uuid        not null,
    role            varchar(20) not null,
    joined_at       timestamptz not null,
    -- Marqueur de lecture : porte le compteur de messages non lus (US-24, US-28).
    last_read_at    timestamptz,
    primary key (conversation_id, user_id),
    constraint participant_role_known check (role in ('CUSTOMER', 'AGENT'))
);

-- Retrouver les conversations d'un utilisateur (US-25).
create index participant_user_idx on assistance.participant (user_id);

create table assistance.message (
    id              uuid         primary key,
    conversation_id uuid         not null references assistance.conversation (id) on delete cascade,
    author_id       uuid         not null,
    author_role     varchar(20)  not null,
    body            varchar(4000) not null,
    sent_at         timestamptz  not null,
    -- État d'acheminement exige par US-24 : envoyé, remis, lu.
    state           varchar(20)  not null,
    constraint message_state_known check (state in ('SENT', 'DELIVERED', 'READ')),
    constraint message_author_role_known check (author_role in ('CUSTOMER', 'AGENT'))
);

-- Lecture de l'historique dans l'ordre d'envoi (US-25), et reprise après coupure (US-24).
create index message_conversation_idx on assistance.message (conversation_id, sent_at);
