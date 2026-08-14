# Your Car Your Way — preuve de concept « messagerie temps réel »

**Projet 10 · OpenClassrooms — Concevez une solution fonctionnelle pour une application full-stack**

Ce dépôt contient **la preuve de concept** du projet Your Car Your Way. Les deux livrables rédigés
qui l'entourent — le cahier des charges et la proposition d'architecture — sont remis
séparément (voir § 14).

La preuve de concept met en œuvre **une seule fonctionnalité : la messagerie entre un client et un
agent du service client**. Les autres domaines — réservation, paiement, catalogue, profil — sont
**conçus et documentés** dans la proposition d'architecture, **jamais implémentés**. C'est
volontaire : l'objectif n'est pas de livrer l'application, mais de **vérifier que les décisions
d'architecture fonctionnent ensemble sur un cas réel**.

---

## Sommaire

1. [Ce que la preuve de concept démontre](#1-ce-que-la-preuve-de-concept-démontre)
2. [Prérequis](#2-prérequis)
3. [Démarrage en une commande](#3-démarrage-en-une-commande)
4. [Le scénario à jouer](#4-le-scénario-à-jouer)
5. [Structure du dépôt](#5-structure-du-dépôt)
6. [Comment le code est organisé, et pourquoi](#6-comment-le-code-est-organisé-et-pourquoi)
7. [Exécuter les tests](#7-exécuter-les-tests)
8. [Développer sans conteneurs](#8-développer-sans-conteneurs)
9. [Structure de données](#9-structure-de-données)
10. [Sécurité](#10-sécurité)
11. [Accessibilité](#11-accessibilité)
12. [En cas de problème](#12-en-cas-de-problème)
13. [Ce que la preuve de concept ne fait pas](#13-ce-que-la-preuve-de-concept-ne-fait-pas)
14. [Livrables rédigés](#14-livrables-rédigés)

---

## 1. Ce que la preuve de concept démontre

| Élément démontré | Décision d'architecture validée | Où le voir |
|---|---|---|
| Connexion temps réel bidirectionnelle | DA-11 — WebSocket + STOMP | `ChatStompController`, `chat.service.ts` |
| **Diffusion entre plusieurs instances par un broker externe** | DA-11 — c'est le point que la plupart des démonstrations omettent | `WebSocketConfig`, `docker-compose.yml` |
| Persistance des messages **avant** diffusion | DA-11 — condition de la reprise sans perte (US-24) | `MessagingService.post` |
| Domaine testable sans aucune infrastructure | DA-05, DA-06 — ports et adaptateurs | `assistance/domain`, tests unitaires |
| Frontières de module vérifiées **au build** | DA-04 | `ArchitectureRulesTest` |
| Structure de données réellement mise en œuvre | DA-14, § 6 de la proposition | `db/migration/V1__…sql` |
| Accessibilité du composant de messagerie | DA-20, § 10.2 | `conversation.component.ts` |
| Environnement conteneurisé démarrant en une commande | DA-13, § 12.4 | `docker-compose.yml` |

**Le point central, en une phrase.** L'exigence de haute disponibilité impose **plusieurs instances**
de l'application. Deux interlocuteurs d'une même conversation peuvent donc être connectés à **deux
instances différentes**. Le broker intégré au framework vit en mémoire d'instance : il ne relaie
rien entre instances, et le message du client n'atteindrait jamais l'agent. C'est pourquoi la
composition démarre **deux instances** — pour que l'on puisse vérifier, et pas seulement affirmer,
que la diffusion passe par le broker externe.

---

## 2. Prérequis

| Outil | Version | Nécessaire pour |
|---|---|---|
| **Docker** avec Docker Compose | 24 ou plus récent | Tout démarrer en une commande (chemin recommandé) |
| **JDK** | 21 ou plus récent | Développer le backend sans conteneur |
| **Maven** | 3.9 ou plus récent | Idem (ou utiliser l'intégration Maven de l'IDE) |
| **Node.js** | 20.19+, 22.12+ ou 24+ | Développer le frontend sans conteneur |

Un IDE (IntelliJ IDEA, VS Code) et **git** complètent l'environnement de développement standard.

> **Node.js en version impaire (21, 23…)** : l'outillage Angular affiche un avertissement et
> fonctionne, mais ces versions ne sont pas des versions de maintenance longue. La construction en
> conteneur, elle, utilise Node 22 et n'est pas concernée.

---

## 3. Démarrage en une commande

```bash
cp .env.example .env      # puis remplacer les valeurs d'exemple
docker compose up --build
```

`.env` est **ignoré par git** : aucun secret n'est versionné, même en développement. Générer un
secret de signature convenable :

```bash
openssl rand -base64 48
```

Une fois les cinq services démarrés :

| Adresse | Service |
|---|---|
| <http://localhost:4200> | Application cliente |
| <http://localhost:8081> | Instance 1 de l'application |
| <http://localhost:8082> | Instance 2 de l'application |
| <http://localhost:15672> | Console d'administration du broker (identifiants : `.env`) |
| `localhost:5433` | Base de données PostgreSQL |

**Comptes de démonstration** (fictifs, créés au démarrage par le profil `demo`) :

| Adresse | Rôle |
|---|---|
| `alice.client@example.test` | Cliente |
| `bruno.client@example.test` | Client |
| `sam.agent@example.test` | Agent du service client |

Mot de passe : la valeur de `POC_DEMO_PASSWORD` dans votre `.env`.

Arrêter et nettoyer :

```bash
docker compose down          # arrête tout
docker compose down -v       # arrête tout ET efface la base
```

---

## 4. Le scénario à jouer

C'est la démonstration elle-même. **Deux onglets suffisent**, dans le même navigateur.

1. **Onglet 1 — la cliente.** Ouvrir <http://localhost:4200>, choisir **Instance 1**, se connecter
   comme `alice.client@example.test`. Ouvrir une demande, par exemple « Décaler ma location ».
2. **Onglet 2 — l'agent.** Ouvrir <http://localhost:4200>, choisir **Instance 2**, se connecter
   comme `sam.agent@example.test`. La demande apparaît dans la file d'attente, **avec son temps
   d'attente**. La prendre en charge.
3. **Échanger.** Écrire des deux côtés. Le bandeau indique quelle instance sert chaque session :
   les messages traversent bien deux instances distinctes.
4. **Observer les états.** Sous chaque message envoyé : *envoyé*, puis *remis*, puis *lu* — en
   toutes lettres, jamais par une icône seule.
5. **Couper la connexion de la cliente.** Dans un terminal :

   ```bash
   docker compose stop app-1
   ```

   Dans l'onglet de la cliente, l'état passe à « Connexion perdue, reconnexion en cours… ».
   **Pendant la coupure, faire répondre l'agent** depuis l'onglet 2 : son message est persisté.
   Puis relancer l'instance :

   ```bash
   docker compose start app-1
   ```

   La cliente se reconnecte seule, **l'historique est rechargé depuis la base**, et le message émis
   pendant la coupure apparaît : la conversation reprend sans perte.
6. **Vérifier que le broker est bien le passage obligé.** Console du broker
   (<http://localhost:15672>, onglet *Exchanges*, `amq.topic`) : les messages y transitent.

> **Pourquoi deux sessions cohabitent dans le même navigateur.** Le cookie de session de l'instance
> 2 est émis avec le chemin `/instance-2` ; celui de l'instance 1 avec `/`. Les deux ne s'écrasent
> donc pas. C'est un artifice de démonstration : en production, un répartiteur de charge distribue
> les connexions et personne ne choisit son instance.

---

## 5. Structure du dépôt

```
p10_code/
├── docker-compose.yml        Environnement complet : 2 instances, base, broker, serveur frontal
├── .env.example              Modèle de configuration — à copier en .env
├── backend/                  Application Spring Boot
├── frontend/                 Application cliente Angular
└── infra/rabbitmq/           Image du broker, avec le relais STOMP activé
```

Le backend suit un découpage **par contexte métier**, jamais par couche technique :

```
backend/src/main/java/com/ycyw/poc/
├── assistance/                 ← le contexte borné mis en œuvre
│   ├── domain/                 ← LE CŒUR : aucune dépendance technique
│   │   ├── model/              agrégat Conversation, Message, objets-valeurs, exceptions
│   │   ├── event/              événements du domaine
│   │   └── port/               interfaces déclarées PAR le domaine (5 ports)
│   ├── application/            services applicatifs : orchestration des cas d'usage
│   └── adapter/                tout ce qui touche à la technique
│       ├── in/rest/            contrôleurs REST
│       ├── in/ws/              point d'entrée temps réel et contrôle des abonnements
│       ├── out/persistence/    entités JPA, dépôts
│       ├── out/broker/         diffusion STOMP
│       └── out/system/         horloge, génération d'identifiants
├── identity/                   ← contexte générique : juste de quoi authentifier
│   ├── IdentityApi             contrat publié — seule surface visible des autres modules
│   └── internal/               tout le reste, inaccessible depuis l'extérieur
└── shared/                     sécurité et configuration du transport
```

---

## 6. Comment le code est organisé, et pourquoi

### 6.1 Le domaine ne connaît rien de la technique

Ouvrez `assistance/domain/model/Conversation.java` : **aucune annotation**, aucun import de
framework, aucune référence à une base de données. Les règles y sont écrites une fois — qui peut
écrire, dans quel état, qui peut prendre en charge une demande — et **l'agrégat les fait respecter
quel que soit l'appelant**.

Le domaine déclare ce dont il a besoin sous forme de **ports** — cinq interfaces, dans
`domain/port/` :

| Port | Ce qu'il représente | Pourquoi il existe |
|---|---|---|
| `ConversationRepository` | Persistance de l'agrégat | Inverser la dépendance : le domaine appelle, la persistance dépend de lui |
| `MessageRepository` | Persistance des messages | Idem |
| `ChatEventPublisher` | Diffusion | Le domaine ignore STOMP, le broker et le nombre d'instances |
| `TimeProvider` | Horloge | **Testabilité** : sans lui, un test sur un temps d'attente dépendrait de sa date d'exécution |
| `IdGenerator` | Identifiants | Un message est identifié avant d'être persisté |

Le port d'horloge illustre la règle : aucun fournisseur d'horloge ne sera jamais « changé ». Le
port existe pour la testabilité, pas pour la substitution.

### 6.2 Les services applicatifs n'ont pas d'annotation non plus

Ils sont déclarés **explicitement**, dans `AssistanceModuleConfiguration`. C'est le prix de leur
indépendance — et le bénéfice se lit dans les tests : ils s'instancient avec des dépôts en mémoire,
sans contexte applicatif ni base de données, et s'exécutent en quelques millisecondes.

Conséquence à connaître : **la transaction est ouverte par l'adaptateur primaire** (contrôleur REST
ou contrôleur temps réel), puisque le service ne peut pas porter l'annotation correspondante. La
limite transactionnelle coïncide donc avec l'entrée dans le cas d'usage.

### 6.3 L'ordre « persister, puis diffuser » n'est pas un détail

Dans `MessagingService.post` :

```java
messages.save(message);                        // 1. persistance : elle fait foi
publisher.publish(new MessagePosted(message)); // 2. diffusion : livraison anticipée
```

L'inverse paraîtrait équivalent. Il ne l'est pas : un message diffusé mais non enregistré
**disparaîtrait à la reconnexion**, et la conversation ne reprendrait pas « sans perte de message »
comme l'exige US-24. Un test unitaire vérifie explicitement cet ordre — pas seulement le résultat.

### 6.4 Deux canaux, une seule logique métier

| Canal | Ce qu'il porte |
|---|---|
| **REST** | Ouverture d'une demande, historique, file d'attente, prise en charge, clôture |
| **Temps réel** | Envoi des messages, accusés de réception et de lecture |

Les deux appellent **les mêmes services applicatifs**. Aucune règle n'est dupliquée — condition
pour qu'un troisième point d'entrée, l'API des applications d'agence, puisse être ajouté sans
réécrire de métier.

### 6.5 Une contrainte du broker qu'il faut connaître

Les destinations de conversation s'écrivent `/topic/conversations.<identifiant>`, **avec un point**,
et non `/topic/conversations/<identifiant>`. Le broker traite ce qui suit `/topic/` comme une clé de
routage unique et refuse un second niveau de chemin.

C'est exactement le genre de contrainte qu'une démonstration sur broker en mémoire ne révèle
jamais, et qui apparaît au premier déploiement réel. C'est une des raisons pour lesquelles cette
preuve de concept utilise un vrai broker.

---

### 6.6 Lombok : sur les entités et les services, jamais dans le domaine

Les accesseurs des entités de persistance et les constructeurs des services applicatifs sont
**générés**. Le partage est délibéré :

| Où | Ce qui est utilisé | Pourquoi |
|---|---|---|
| Entités JPA | `@Getter(PACKAGE)`, `@Setter(PACKAGE)`, `@NoArgsConstructor(PROTECTED)` | Ces classes ne portent aucune règle : elles traduisent le domaine vers des colonnes. La portée **paquet** garde les accesseurs à l'intérieur de l'adaptateur |
| Services applicatifs | `@RequiredArgsConstructor` | La liste des champs `final` suffit à montrer de quels ports le cas d'usage dépend |
| **Domaine** | **rien** | Un `@Setter` permettrait d'imposer un état à un agrégat, donc de contourner ses invariants. Et le domaine expose `id()`, `status()` — pas `getId()` |

Quelques garde-fous, dans [`backend/lombok.config`](backend/lombok.config) : `@Data`, `@Value`,
`@ToString` et `@EqualsAndHashCode` **font échouer la compilation**. Sur une entité, ils
généreraient un `equals` sur des champs mutables — identité instable dès qu'une entité change
d'état — et un `toString` qui déclenche des chargements différés hors transaction. Les interdire à
la racine évite d'avoir à le rappeler en revue.

> Deux exceptions volontaires, commentées dans le code : le constructeur d'`AppUserEntity` reste
> écrit à la main (trois chaînes consécutives — un constructeur généré rendrait un réordonnancement
> de champs silencieusement destructeur), et la collection `participants` n'a pas d'accesseur en
> écriture (la remplacer casserait la suppression des orphelins).

---

## 7. Exécuter les tests

```bash
cd backend

mvn test                              # tout, y compris le test d'intégration (Docker requis)
mvn test -DexcludedGroups=docker      # boucle rapide : tout sauf le test d'intégration
```

| Suite | Ce qu'elle vérifie | Infrastructure |
|---|---|---|
| **Tests du domaine** | Règles de l'agrégat : qui peut écrire, prise en charge unique, marqueur de lecture monotone | Aucune |
| **Tests des cas d'usage** | Comportement des services, dont **l'ordre persistance → diffusion** | Aucune |
| **Tests d'architecture** | Les cinq règles de frontières (voir ci-dessous) | Aucune |
| **Test d'« effectivité »** | Que les règles d'architecture **détectent bien** ce qu'elles prétendent détecter | Aucune |
| **Test d'intégration** | Migrations, schémas cloisonnés, chaîne de sécurité, parcours complet | PostgreSQL en conteneur |

### Les règles vérifiées au build

1. Le domaine et les cas d'usage ne dépendent d'aucun framework.
2. Le domaine ne dépend d'aucun adaptateur.
3. Aucune classe du domaine n'expose d'accesseur en écriture public.
4. Aucun module n'atteint l'intérieur d'un autre — seulement son contrat publié.
5. Chaque table déclare le schéma de son module.

**Une règle qui ne trouve rien ne prouve rien.** « Aucune violation » peut signifier que le code est
sain, ou que le contrôle ne contrôle rien — un paquet mal orthographié suffit. C'est pourquoi
`ArchitectureRulesAreEffectiveTest` confronte chaque règle à une classe volontairement fautive et
**exige qu'elle échoue**. Si vous ajoutez une règle, ajoutez son cas fautif.

---

## 8. Développer sans conteneurs

Démarrer uniquement les dépendances, puis lancer l'application depuis l'IDE :

```bash
docker compose up -d postgres rabbitmq
```

Variables minimales à définir dans la configuration d'exécution :

```
SPRING_PROFILES_ACTIVE=demo
POC_DB_URL=jdbc:postgresql://localhost:5433/ycyw
POC_DB_USERNAME=ycyw
POC_DB_PASSWORD=…            # valeur de .env
POC_JWT_SECRET=…             # 32 octets minimum, valeur de .env
POC_DEMO_PASSWORD=…          # valeur de .env
POC_BROKER_RELAY_ENABLED=true
POC_BROKER_LOGIN=…           # valeur de .env
POC_BROKER_PASSCODE=…        # valeur de .env
```

Frontend en rechargement automatique :

```bash
cd frontend
npm install
npm start        # http://localhost:4200, appels relayés vers 8081 et 8082
```

> `POC_BROKER_RELAY_ENABLED=false` fait basculer sur le broker en mémoire d'instance. Utile pour
> démarrer sans broker — mais **la diffusion entre instances cesse alors de fonctionner**, sans
> aucune erreur visible. À n'utiliser qu'en connaissance de cause.

---

## 9. Structure de données

Deux schémas dans une base unique, **un par contexte borné** : `identity` et `assistance`. Le
cloisonnement par schéma matérialise la frontière entre modules et permet de la vérifier
mécaniquement.

| Table | Rôle |
|---|---|
| `identity.app_user` | Comptes. Empreinte BCrypt (coût 12), jamais le mot de passe |
| `assistance.conversation` | Demande d'assistance : objet, état, client, agent, verrou optimiste |
| `assistance.participant` | Présence dans une conversation **et marqueur de lecture** — c'est lui qui porte le compteur de messages non lus |
| `assistance.message` | Message, auteur, horodatage et **état d'acheminement** (envoyé, remis, lu) |

**Aucune clé étrangère ne relie `assistance` à `identity`.** Une conversation référence un
utilisateur par son identifiant seul. Ce n'est pas un oubli : c'est ce qui rend le module Assistance
extractible sans refonte, et ce qui permettra plus tard d'anonymiser un compte sans casser
l'historique des échanges.

Le schéma est produit par des **migrations versionnées** (`backend/src/main/resources/db/migration`),
jamais par le framework de persistance, qui se contente de **valider** au démarrage que le code et
la base ne divergent pas.

---

## 10. Sécurité

| Mesure | Motif |
|---|---|
| Empreintes **BCrypt coût 12** | L'audit relève SHA-1 encore en service sur la plateforme européenne : la preuve de concept applique la correction |
| Jeton de session en **cookie inaccessible au script** | Un vol par injection de script devient sans effet ; le code client ne voit jamais le jeton |
| **Aucune session serveur** | Condition de la réplication en plusieurs instances |
| **Jeton anti-rejeu** sur toute écriture | Nécessaire dès lors que l'authentification voyage par cookie |
| **Contrôle des abonnements temps réel** | Une destination est une chaîne prévisible : sans contrôle, on lirait la conversation d'autrui |
| **Hôte virtuel du broker imposé par le serveur** | Le client ne choisit pas l'espace auquel il se connecte |
| Secrets **exclusivement** dans l'environnement | L'application refuse de démarrer sans secret de signature |
| Conteneur exécuté **en utilisateur non privilégié** | Une faille applicative ne donne pas les droits d'administration du conteneur |

Les comptes de démonstration utilisent des identités **fictives** et le domaine `example.test`,
réservé à cet usage : aucune donnée personnelle réelle ne circule dans un environnement non
productif.

---

## 11. Accessibilité

Le tchat est le composant le plus délicat à rendre accessible, et c'est aussi le canal d'assistance
**principal** des personnes sourdes ou malentendantes. Quatre points structurent
`conversation.component.ts` :

| Exigence | Mise en œuvre |
|---|---|
| Un message qui arrive est annoncé **sans que le focus soit déplacé** | La conversation est une région de journal (`role="log"`, `aria-live="polite"`, `aria-relevant="additions"`). Déplacer le focus interromprait la saisie en cours — l'erreur classique de ces composants |
| Saisie, envoi et parcours de l'historique **sans souris** | Ordre de tabulation naturel, envoi par la touche Entrée (Maj + Entrée pour aller à la ligne), zone de conversation atteignable au clavier |
| L'état du message est indiqué | *envoyé* / *remis* / *lu* **en toutes lettres** — jamais une icône ni une couleur seule |
| L'état de connexion est indiqué | Région d'état textuelle, mise à jour à chaque changement |

Le reste suit les mêmes principes : langue déclarée, hiérarchie de titres unique, lien d'évitement,
libellés associés à chaque champ, erreurs annoncées en région d'alerte, focus toujours visible,
contrastes au-delà du seuil AA, respect de la préférence « animations réduites ».

---

## 12. En cas de problème

| Symptôme | Cause probable | Correction |
|---|---|---|
| `address already in use` sur 5432 ou 5672 | Un PostgreSQL ou un broker tourne déjà sur le poste | Changer `POSTGRES_PORT` dans `.env`, ou arrêter le service local |
| L'application s'arrête au démarrage : *jwt-secret est obligatoire* | `.env` absent ou secret trop court | `cp .env.example .env`, puis `openssl rand -base64 48` |
| Connexion refusée alors que le mot de passe est bon | Les comptes ont été créés avec un **autre** `POC_DEMO_PASSWORD` | `docker compose down -v` puis redémarrer |
| Les messages n'arrivent pas à l'autre instance | Broker arrêté, ou `POC_BROKER_RELAY_ENABLED=false` | `docker compose ps`, puis vérifier la variable |
| `is not a valid topic destination` | Destination écrite avec `/` au lieu de `.` | Voir § 6.5 |
| Les tests échouent sur *Could not find a valid Docker environment* | Moteur de conteneurs arrêté | Le démarrer, ou `mvn test -DexcludedGroups=docker` |

Journaux utiles :

```bash
docker compose logs -f app-1        # une instance
docker compose logs -f rabbitmq     # le broker
docker compose ps                   # état et santé des services
```

---

## 13. Ce que la preuve de concept ne fait pas

Énoncé explicitement, pour qu'aucune absence ne passe pour un oubli.

| Hors périmètre | Pourquoi |
|---|---|
| Réservation, paiement, catalogue, profil, API d'agence | Conçus et documentés dans la proposition d'architecture ; les implémenter n'apporterait rien à la démonstration |
| Contexte client affiché à l'agent (US-27) | Suppose le contexte Réservation, hors périmètre |
| Notification par courriel d'une réponse (US-28) | Le compteur de messages non lus couvre le retour sur la page ; l'envoi de courriel relève de la table d'attente d'émission décrite dans l'architecture |
| Internationalisation de l'interface | L'architecture la prévoit ; l'interface de démonstration est en français seul |
| Tests automatisés du frontend | L'effort de test a été porté sur le domaine, les frontières et l'intégration — là où se situent les décisions à valider |
| Limitation du nombre de tentatives de connexion | Nécessaire en production, sans lien avec les décisions démontrées ici |
| Chiffrement du transport (TLS) | Assuré en production par le répartiteur de charge ; `POC_COOKIE_SECURE=true` doit alors être activé |

L'interface reste **volontairement minimale** : c'est la structure technique qui est démontrée, non
l'apparence. L'accessibilité, elle, n'est pas minimale.

---

## 14. Livrables rédigés

Deux documents accompagnent cette preuve de concept. **Ils ne sont pas versionnés dans ce dépôt**
et sont remis au format PDF avec le reste du projet :

| Document                                         | Contenu                                                                                                                                                                                                                                   |
|--------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Cahier des charges                               | Analyse des besoins utilisateurs et spécifications fonctionnelles — user stories priorisées et critères d'acceptation                                                                                                                     |
| Audit de l'existant + Proposition d'architecture | Constats chiffrés sur les quatre plateformes actuelles, au regard de la maintenabilité, de la performance et de l'évolutivité + Décisions d'architecture, modélisation UML, modèle de données, choix technologiques et leur justification |

Les références de type **DA-11**, **US-24** ou **ENF-19** employées dans ce document et dans les
commentaires du code renvoient à ces deux livrables : décisions d'architecture, user stories et
exigences non fonctionnelles. Un commentaire qui cite une décision n'explique pas seulement ce que
fait le code — il indique **où est écrit pourquoi**.
